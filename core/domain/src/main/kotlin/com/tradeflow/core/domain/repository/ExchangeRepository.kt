package com.tradeflow.core.domain.repository

import com.tradeflow.core.domain.model.*
import java.math.BigDecimal

interface ExchangeRepository {
    suspend fun getBalances(): Result<List<Balance>>
    suspend fun getPortfolio(): Result<Portfolio>

    suspend fun getCandles(
        productId: String,
        granularity: Granularity,
        limit: Int = 350
    ): Result<List<Candle>>

    suspend fun getCurrentPrice(productId: String): Result<Ticker>

    suspend fun placeMarketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal
    ): Result<Order>

    suspend fun placeLimitOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        price: BigDecimal,
        postOnly: Boolean = true
    ): Result<Order>

    suspend fun cancelOrder(orderId: String): Result<Unit>
    suspend fun cancelOrders(orderIds: List<String>): Result<Int>
    suspend fun getOpenOrders(productId: String): Result<List<Order>>
    suspend fun getOrder(orderId: String): Result<Order>
}
