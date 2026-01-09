package com.tradeflow.exchange.coinbase.repository

import com.tradeflow.core.domain.model.Balance
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Granularity
import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.model.Ticker
import com.tradeflow.core.domain.repository.BracketOrderRepository
import com.tradeflow.exchange.coinbase.api.CoinbaseApiClient
import com.tradeflow.exchange.coinbase.mapper.toDomain
import java.math.BigDecimal
import javax.inject.Inject

class CoinbaseRepository @Inject constructor(
    private val apiClient: CoinbaseApiClient
) : BracketOrderRepository {

    override suspend fun getBalances(): Result<List<Balance>> = runCatching {
        val response = apiClient.getAccounts().getOrThrow()
        response.accounts
            .filter { it.active && it.ready }
            .map { it.toDomain() }
    }

    override suspend fun getPortfolio(): Result<Portfolio> {
        TODO("Implement in Ticket 13 - Full REST API Client")
    }

    override suspend fun getCandles(
        productId: String,
        granularity: Granularity,
        limit: Int
    ): Result<List<Candle>> {
        TODO("Implement in Ticket 13 - Full REST API Client")
    }

    override suspend fun getCurrentPrice(productId: String): Result<Ticker> {
        TODO("Implement in Ticket 13 - Full REST API Client")
    }

    override suspend fun placeMarketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal
    ): Result<Order> {
        TODO("Implement in Ticket 13 - Full REST API Client")
    }

    override suspend fun placeLimitOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        price: BigDecimal,
        postOnly: Boolean
    ): Result<Order> {
        TODO("Implement in Ticket 13 - Full REST API Client")
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> {
        TODO("Implement in Ticket 13 - Full REST API Client")
    }

    override suspend fun cancelOrders(orderIds: List<String>): Result<Int> {
        TODO("Implement in Ticket 13 - Full REST API Client")
    }

    override suspend fun getOrder(orderId: String): Result<Order> {
        TODO("Implement in Ticket 13 - Full REST API Client")
    }

    override suspend fun getOpenOrders(productId: String): Result<List<Order>> {
        TODO("Implement in Ticket 13 - Full REST API Client")
    }

    override suspend fun placeBracketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        entryPrice: BigDecimal,
        takeProfit: BigDecimal,
        stopLoss: BigDecimal
    ): Result<Order> {
        TODO("Implement in Ticket 13 - Full REST API Client")
    }
}
