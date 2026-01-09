package com.tradeflow.core.domain.simulator

import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.BracketOrderRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

class SimulatedExchange(
    initialUsd: BigDecimal,
    private val productId: String = "BTC-USD"
) : BracketOrderRepository {

    var usdBalance = initialUsd
    var btcBalance = BigDecimal.ZERO
    private val openOrders = mutableListOf<Order>()
    var currentPrice = BigDecimal.ZERO
    private var history = mutableListOf<Candle>()
    
    private val feeRate = BigDecimal("0.006")

    fun advanceTime(newCandle: Candle) {
        this.currentPrice = newCandle.close
        this.history.add(newCandle)
        
        val iterator = openOrders.iterator()
        while (iterator.hasNext()) {
            val order = iterator.next()
            val hit = when(order.side) {
                OrderSide.BUY -> newCandle.low <= (order.price ?: currentPrice)
                OrderSide.SELL -> newCandle.high >= (order.price ?: currentPrice)
            }
            
            if (hit) {
                if (canExecute(order)) {
                    executeOrder(order)
                    // OCO Logic: If one part of a trade fills, cancel the others associated with this product
                    if (order.side == OrderSide.SELL) {
                        iterator.remove()
                        clearOpenOrders() // Simplified OCO for simulation
                        return
                    }
                }
                iterator.remove()
            }
        }
    }

    private fun clearOpenOrders() {
        openOrders.clear()
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

    private fun executeOrder(order: Order) {
        val price = order.price ?: currentPrice
        val cost = order.size * price
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
        val order = Order(UUID.randomUUID().toString(), UUID.randomUUID().toString(), productId, side, OrderType.MARKET, OrderStatus.FILLED, size, entryPrice, size, entryPrice, Instant.now())
        if (canExecute(order)) {
            executeOrder(order)
            // Place resting TP/SL orders
            placeLimitOrder(productId, if (side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY, size, takeProfit, true)
            placeLimitOrder(productId, if (side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY, size, stopLoss, true)
            return Result.success(order)
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
        executeOrder(Order("", "", productId, side, OrderType.MARKET, OrderStatus.FILLED, size, currentPrice, size, currentPrice, Instant.now()))
        return Result.success(Order("", "", productId, side, OrderType.MARKET, OrderStatus.FILLED, size, currentPrice, size, currentPrice, Instant.now()))
    }
    override suspend fun cancelOrder(orderId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getOrder(orderId: String): Result<Order> = TODO()
}
