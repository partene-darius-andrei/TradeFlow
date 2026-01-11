package com.tradeflow.core.domain.simulator

import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.ExchangeRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

class SimulatedExchange(
    initialUsd: BigDecimal,
    private val feeRate: BigDecimal = BigDecimal("0.004") // Coinbase Advanced Trade: 0.4% taker
) : ExchangeRepository {

    var usdBalance = initialUsd
    var btcBalance = BigDecimal.ZERO
    private val openOrders = mutableListOf<Order>()
    var currentPrice = BigDecimal.ZERO
    private var history = mutableListOf<Candle>()

    fun advanceTime(newCandle: Candle) {
        this.currentPrice = newCandle.close
        this.history.add(newCandle)

        val iterator = openOrders.iterator()
        while (iterator.hasNext()) {
            val order = iterator.next()

            // Check if limit price was touched
            val limitPrice = order.price ?: currentPrice
            val hit = when(order.side) {
                OrderSide.BUY -> newCandle.low <= limitPrice
                OrderSide.SELL -> newCandle.high >= limitPrice
            }

            if (hit) {
                if (canExecute(order)) {
                    // Apply slippage: ±0.1% (BUY pays slightly more, SELL gets slightly less)
                    val fillPrice = applySlippage(limitPrice, order.side)
                    executeOrder(order, fillPrice)

                    // OCO Logic: If order filled, cancel other orders in same group
                    val groupId = order.clientOrderId // Use clientOrderId as group ID for bracket orders
                    if (groupId.isNotEmpty()) {
                        cancelOrderGroup(groupId)
                    }
                }
                iterator.remove()
            }
        }
    }

    /**
     * Applies realistic slippage to simulated fills (±0.1%).
     * - BUY orders: Fill at slightly higher price (+0.1% slippage)
     * - SELL orders: Fill at slightly lower price (-0.1% slippage)
     */
    private fun applySlippage(price: BigDecimal, side: OrderSide): BigDecimal {
        val slippagePercent = BigDecimal("0.001") // 0.1% slippage
        return when (side) {
            OrderSide.BUY -> price * (BigDecimal.ONE + slippagePercent) // Pay more
            OrderSide.SELL -> price * (BigDecimal.ONE - slippagePercent) // Receive less
        }
    }

    /**
     * Cancels all orders in the same OCO group (e.g., TP and SL orders after entry fills).
     */
    private fun cancelOrderGroup(groupId: String) {
        openOrders.removeAll { it.clientOrderId == groupId }
    }

    private fun canExecute(order: Order): Boolean {
        val price = order.price ?: currentPrice
        val cost = order.size * price
        val fee = cost * feeRate
        return if (order.side == OrderSide.BUY) {
            usdBalance >= (cost + fee)
        } else {
            btcBalance >= order.size
        }
    }

    /**
     * Executes an order at the specified fill price (with slippage already applied).
     * Deducts fees and updates balances.
     */
    private fun executeOrder(order: Order, fillPrice: BigDecimal = order.price ?: currentPrice) {
        val cost = order.size * fillPrice
        val fee = cost * feeRate

        if (order.side == OrderSide.BUY) {
            usdBalance -= (cost + fee)
            btcBalance += order.size
        } else {
            usdBalance += (cost - fee)
            btcBalance -= order.size
        }
    }

    fun setHistory(candles: List<Candle>) {
        this.history = candles.toMutableList()
        this.currentPrice = candles.last().close
    }

    fun getTotalEquity(): BigDecimal = usdBalance + (btcBalance * currentPrice)

    override suspend fun getBalances(): Result<List<Balance>> = Result.success(listOf(
        Balance("USD", usdBalance, BigDecimal.ZERO),
        Balance("BTC", btcBalance, BigDecimal.ZERO)
    ))

    override suspend fun getPortfolio(): Result<Portfolio> = Result.success(Portfolio(
        balances = listOf(
            Balance("USD", usdBalance, BigDecimal.ZERO),
            Balance("BTC", btcBalance, BigDecimal.ZERO)
        ),
        totalEquityUsd = getTotalEquity(),
        timestamp = Instant.now()
    ))

    override suspend fun getCandles(productId: String, granularity: Granularity, limit: Int): Result<List<Candle>> = 
        Result.success(history.takeLast(limit))

    override suspend fun getCurrentPrice(productId: String): Result<Ticker> = 
        Result.success(Ticker(productId, currentPrice, currentPrice, currentPrice, BigDecimal.ZERO, Instant.now()))

    override suspend fun placeLimitOrder(productId: String, side: OrderSide, size: BigDecimal, price: BigDecimal, postOnly: Boolean): Result<Order> {
        val order = Order(UUID.randomUUID().toString(), UUID.randomUUID().toString(), productId, side, OrderType.LIMIT, OrderStatus.OPEN, size, price, BigDecimal.ZERO, null, Instant.now())
        openOrders.add(order)
        return Result.success(order)
    }

    override suspend fun placeBracketOrder(productId: String, side: OrderSide, size: BigDecimal, entryPrice: BigDecimal, takeProfit: BigDecimal, stopLoss: BigDecimal): Result<Order> {
        // Apply slippage to entry (market order)
        val entryFillPrice = applySlippage(entryPrice, side)

        val entryOrder = Order(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            productId,
            side,
            OrderType.MARKET,
            OrderStatus.FILLED,
            size,
            entryFillPrice,
            size,
            entryFillPrice,
            Instant.now()
        )

        if (canExecute(entryOrder)) {
            executeOrder(entryOrder, entryFillPrice)

            // Place OCO group: TP and SL orders with shared groupId
            val groupId = UUID.randomUUID().toString()
            val exitSide = if (side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY

            val tpOrder = Order(
                UUID.randomUUID().toString(),
                groupId, // Shared OCO group ID
                productId,
                exitSide,
                OrderType.LIMIT,
                OrderStatus.OPEN,
                size,
                takeProfit,
                BigDecimal.ZERO,
                null,
                Instant.now()
            )

            val slOrder = Order(
                UUID.randomUUID().toString(),
                groupId, // Shared OCO group ID
                productId,
                exitSide,
                OrderType.LIMIT,
                OrderStatus.OPEN,
                size,
                stopLoss,
                BigDecimal.ZERO,
                null,
                Instant.now()
            )

            openOrders.add(tpOrder)
            openOrders.add(slOrder)

            return Result.success(entryOrder)
        }
        return Result.failure(Exception("Insufficient funds"))
    }

    override suspend fun getOpenOrders(productId: String): Result<List<Order>> = Result.success(openOrders)
    override suspend fun cancelOrders(orderIds: List<String>): Result<Int> {
        val count = openOrders.size
        openOrders.clear()
        return Result.success(count)
    }

    override suspend fun placeMarketOrder(productId: String, side: OrderSide, size: BigDecimal): Result<Order> {
        // Apply slippage to market orders
        val fillPrice = applySlippage(currentPrice, side)

        val order = Order(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            productId,
            side,
            OrderType.MARKET,
            OrderStatus.FILLED,
            size,
            fillPrice,
            size,
            fillPrice,
            Instant.now()
        )

        // CRITICAL: Check funds before executing (was missing - FIXED)
        if (!canExecute(order)) {
            return Result.failure(Exception("Insufficient funds for market order"))
        }

        executeOrder(order, fillPrice)
        return Result.success(order)
    }
    override suspend fun cancelOrder(orderId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getOrder(orderId: String): Result<Order> = TODO()

    // Perpetual futures simulation (stub implementation for now)
    override suspend fun getPerpetualPosition(productId: String): Result<PerpetualPosition?> {
        // TODO: Implement full perpetual futures simulation
        // For now, return null (no open position)
        return Result.success(null)
    }

    override suspend fun closePerpetualPosition(productId: String): Result<Unit> {
        // TODO: Implement full perpetual futures simulation
        return Result.success(Unit)
    }

    override suspend fun getFundingRate(productId: String): Result<FundingRate> {
        // TODO: Implement full perpetual futures simulation
        // For now, return a typical low funding rate (0.01% per 8h)
        return Result.success(
            FundingRate(
                productId = productId,
                rate = BigDecimal("0.0001"), // 0.01% per 8h (typical)
                nextFundingTime = Instant.now().plusSeconds(8 * 3600),
                predictedRate = BigDecimal("0.0001")
            )
        )
    }
}
