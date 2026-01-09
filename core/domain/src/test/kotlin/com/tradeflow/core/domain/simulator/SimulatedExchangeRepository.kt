package com.tradeflow.core.domain.simulator

import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.BracketOrderRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class SimulatedExchangeRepository(
    startingCapitalUsd: BigDecimal,
    private val productId: String = "BTC-USD"
) : BracketOrderRepository {

    private val portfolio = PortfolioSimulator(startingCapitalUsd, productId)
    private val orderBook = OrderBook(productId)
    private val candleHistory = mutableListOf<Candle>()
    private var currentPrice = BigDecimal.ZERO
    private val filledOrders = mutableListOf<Order>()

    companion object {
        private const val SLIPPAGE_PERCENT = 0.001  // 0.1% slippage on market orders
    }

    fun advanceTime(candle: Candle) {
        candleHistory.add(candle)
        currentPrice = candle.close

        // Match any limit orders that hit price during this candle
        val fills = orderBook.matchOrders(candle)
        fills.forEach { fill ->
            portfolio.applyFill(fill.order, fill.fillPrice, fill.isMaker)
            filledOrders.add(fill.order)
        }

        // Update high water mark
        portfolio.updateHighWaterMark(currentPrice)
    }

    fun getCandleHistory(): List<Candle> = candleHistory.toList()

    fun getHighWaterMark(): BigDecimal = portfolio.highWaterMark

    override suspend fun getBalances(): Result<List<Balance>> {
        return Result.success(
            listOf(
                Balance(currency = "USD", available = portfolio.getUsdBalance(), hold = BigDecimal.ZERO),
                Balance(currency = "BTC", available = portfolio.getBtcBalance(), hold = BigDecimal.ZERO)
            )
        )
    }

    override suspend fun getPortfolio(): Result<Portfolio> {
        return Result.success(portfolio.getPortfolio(currentPrice))
    }

    override suspend fun getCandles(
        productId: String,
        granularity: Granularity,
        limit: Int
    ): Result<List<Candle>> {
        return if (productId == this.productId) {
            Result.success(candleHistory.takeLast(limit))
        } else {
            Result.failure(IllegalArgumentException("Unknown product ID: $productId"))
        }
    }

    override suspend fun getCurrentPrice(productId: String): Result<Ticker> {
        return if (productId == this.productId) {
            val slippage = currentPrice * BigDecimal(SLIPPAGE_PERCENT)
            Result.success(
                Ticker(
                    productId = productId,
                    price = currentPrice,
                    bid = currentPrice - slippage,  // Slightly below spot
                    ask = currentPrice + slippage,  // Slightly above spot
                    volume24h = BigDecimal.ZERO,    // Not tracked in simulation
                    timestamp = Instant.now()
                )
            )
        } else {
            Result.failure(IllegalArgumentException("Unknown product ID: $productId"))
        }
    }

    override suspend fun placeMarketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal
    ): Result<Order> {
        if (productId != this.productId) {
            return Result.failure(IllegalArgumentException("Unknown product ID: $productId"))
        }

        // Market orders fill instantly with slippage
        val fillPrice = calculateSlippage(side, currentPrice)

        val order = Order(
            id = UUID.randomUUID().toString(),
            clientOrderId = UUID.randomUUID().toString(),
            productId = productId,
            side = side,
            type = OrderType.MARKET,
            status = OrderStatus.FILLED,
            size = size,
            price = null,  // Market orders don't have limit price
            filledSize = size,
            avgFilledPrice = fillPrice,
            createdAt = Instant.now()
        )

        return try {
            portfolio.applyFill(order, fillPrice, isMaker = false)  // Taker fee
            filledOrders.add(order)
            Result.success(order)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    override suspend fun placeLimitOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        price: BigDecimal,
        postOnly: Boolean
    ): Result<Order> {
        if (productId != this.productId) {
            return Result.failure(IllegalArgumentException("Unknown product ID: $productId"))
        }

        val order = Order(
            id = UUID.randomUUID().toString(),
            clientOrderId = UUID.randomUUID().toString(),
            productId = productId,
            side = side,
            type = OrderType.LIMIT,
            status = OrderStatus.OPEN,
            size = size,
            price = price,
            filledSize = BigDecimal.ZERO,
            avgFilledPrice = null,
            createdAt = Instant.now()
        )

        return try {
            // Reserve funds for the order
            portfolio.reserveForOrder(order, price)
            orderBook.addOrder(order)
            Result.success(order)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    override suspend fun placeBracketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        entryPrice: BigDecimal,
        takeProfit: BigDecimal,
        stopLoss: BigDecimal
    ): Result<Order> {
        // For simulation, bracket order = entry limit order
        // TP and SL are managed by use case layer after entry fills
        return placeLimitOrder(productId, side, size, entryPrice, postOnly = true)
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> {
        val order = orderBook.getOrder(orderId)
        return if (order != null) {
            // Release reserved funds before removing order
            portfolio.releaseReservedFunds(order, order.price ?: currentPrice)
            orderBook.removeOrder(orderId)
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Order not found: $orderId"))
        }
    }

    override suspend fun cancelOrders(orderIds: List<String>): Result<Int> {
        var cancelledCount = 0
        orderIds.forEach { orderId ->
            if (orderBook.removeOrder(orderId)) {
                cancelledCount++
            }
        }
        return Result.success(cancelledCount)
    }

    override suspend fun getOpenOrders(productId: String): Result<List<Order>> {
        return if (productId == this.productId) {
            Result.success(orderBook.getOpenOrders())
        } else {
            Result.success(emptyList())
        }
    }

    override suspend fun getOrder(orderId: String): Result<Order> {
        // Check order book first
        val openOrder = orderBook.getOpenOrders().find { it.id == orderId }
        if (openOrder != null) {
            return Result.success(openOrder)
        }

        // Check filled orders
        val filledOrder = filledOrders.find { it.id == orderId }
        if (filledOrder != null) {
            return Result.success(filledOrder)
        }

        return Result.failure(IllegalArgumentException("Order not found: $orderId"))
    }

    private fun calculateSlippage(side: OrderSide, basePrice: BigDecimal): BigDecimal {
        val slippage = basePrice * BigDecimal(SLIPPAGE_PERCENT)
        return when (side) {
            OrderSide.BUY -> basePrice + slippage   // Pay slightly more (buy at ask)
            OrderSide.SELL -> basePrice - slippage  // Receive slightly less (sell at bid)
        }
    }

    fun reset(startingCapitalUsd: BigDecimal) {
        portfolio.reset(startingCapitalUsd)
        orderBook.clear()
        candleHistory.clear()
        currentPrice = BigDecimal.ZERO
        filledOrders.clear()
    }

    fun getFilledOrders(): List<Order> = filledOrders.toList()
}
