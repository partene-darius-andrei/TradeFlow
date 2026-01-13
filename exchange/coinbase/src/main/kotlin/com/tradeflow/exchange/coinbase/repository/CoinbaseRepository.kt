package com.tradeflow.exchange.coinbase.repository

import com.tradeflow.core.domain.model.Balance
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.CredentialStore
import com.tradeflow.core.domain.model.FundingRate
import com.tradeflow.core.domain.model.Granularity
import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.PerpetualPosition
import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.model.Ticker
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.exchange.coinbase.api.CoinbaseApiClient
import com.tradeflow.exchange.coinbase.auth.JwtRepository
import com.tradeflow.exchange.coinbase.mapper.toDomain
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File
import java.math.BigDecimal
import java.util.Properties

class CoinbaseRepository() : ExchangeRepository {

    private var apiClient: CoinbaseApiClient
    private val httpClient: HttpClient? = null

    init {
        val localPropertiesFile = File("local.properties")
        val props = Properties()
        if (localPropertiesFile.exists()) {
            props.load(localPropertiesFile.inputStream())
        }

        val apiKey = System.getenv("COINBASE_API_KEY")
            ?: props.getProperty("coinbase.api.key")
            ?: error("COINBASE_API_KEY not found")

        val secret = System.getenv("COINBASE_API_SECRET")
            ?: props.getProperty("coinbase.api.secret")
            ?: error("COINBASE_API_SECRET not found")

        val httpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("[HTTP] $message")
                    }
                }
                level = LogLevel.HEADERS
            }
        }

        val credentialStore = object : CredentialStore {
            override suspend fun getApiKey(): String = apiKey
            override suspend fun getSecret(): String = secret
        }

        val authProvider = JwtRepository(credentialStore)
        apiClient = CoinbaseApiClient(httpClient, authProvider)
    }

    fun close() {
        httpClient?.close()
    }

    override suspend fun getBalances(): Result<List<Balance>> = runCatching {
        val response = apiClient.getAccounts().getOrThrow()
        response.accounts
            .filter { it.active && it.ready }
            .map { it.toDomain() }
    }

    override suspend fun getPortfolio(): Result<Portfolio> {
        TODO("Not yet implemented")
    }

    override suspend fun getCandles(
        productId: String,
        granularity: Granularity,
        limit: Int
    ): Result<List<Candle>> {
        TODO("Not yet implemented")
    }

    override suspend fun getCurrentPrice(productId: String): Result<Ticker> {
        TODO("Not yet implemented")
    }

    override suspend fun placeMarketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal
    ): Result<Order> {
        TODO("Not yet implemented")
    }

    override suspend fun placeLimitOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        price: BigDecimal,
        postOnly: Boolean
    ): Result<Order> {
        TODO("Not yet implemented")
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun cancelOrders(orderIds: List<String>): Result<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun getOrder(orderId: String): Result<Order> {
        TODO("Not yet implemented")
    }

    override suspend fun getOpenOrders(productId: String): Result<List<Order>> {
        TODO("Not yet implemented")
    }

    override suspend fun closePerpetualPosition(productId: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getFundingRate(productId: String): Result<FundingRate> {
        TODO("Not yet implemented")
    }

    override suspend fun getPerpetualPosition(productId: String): Result<PerpetualPosition?> {
        TODO("Not yet implemented")
    }

    override suspend fun placeBracketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        entryPrice: BigDecimal,
        takeProfit: BigDecimal,
        stopLoss: BigDecimal
    ): Result<Order> {
        TODO("Not yet implemented")
    }
}
