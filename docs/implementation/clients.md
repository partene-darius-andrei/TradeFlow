# API Clients (REST & WebSocket)

**Parent:** [../reference.md](../reference.md)

Complete Ktor implementation for Coinbase REST API and WebSocket communication.

---

## REST API Client

### CoinbaseRestApi.kt

Complete REST API client with order placement, candle fetching, and account management.

**Note:** This code uses Ktor HTTP client (already configured in project).

```kotlin
// data/remote/CoinbaseRestApi.kt
package com.dpart.tradeflow.data.remote

import android.util.Log
import com.dpart.tradeflow.data.security.JwtGenerator
import com.dpart.tradeflow.domain.model.Candle
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.UUID

class CoinbaseRestApi(private val jwtGenerator: JwtGenerator) {

    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "CoinbaseRestApi"
        private const val BASE_URL = "https://api.coinbase.com"
        private const val HOST = "api.coinbase.com"
    }

    // ==================== ORDER PLACEMENT ====================

    /**
     * Place limit order with entry + attached TP/SL (for TREND mode)
     */
    suspend fun placeBracketOrder(
        productId: String,
        side: String,
        baseSize: Double,
        entryPrice: Double,
        takeProfitPrice: Double,
        stopLossPrice: Double
    ): OrderResult {
        val clientOrderId = UUID.randomUUID().toString()

        val body = buildJsonObject {
            put("client_order_id", clientOrderId)
            put("product_id", productId)
            put("side", side)
            putJsonObject("order_configuration") {
                putJsonObject("limit_limit_gtc") {
                    put("base_size", formatSize(baseSize))
                    put("limit_price", formatPrice(entryPrice))
                }
            }
            putJsonObject("attached_order_configuration") {
                putJsonObject("trigger_bracket_gtc") {
                    put("limit_price", formatPrice(takeProfitPrice))
                    put("stop_trigger_price", formatPrice(stopLossPrice))
                }
            }
        }

        return executeCreateOrder(body, clientOrderId)
    }

    /**
     * Place limit order with post_only (for RANGE/grid mode)
     */
    suspend fun placeLimitOrder(
        productId: String,
        side: String,
        baseSize: Double,
        limitPrice: Double
    ): OrderResult {
        val clientOrderId = UUID.randomUUID().toString()

        val body = buildJsonObject {
            put("client_order_id", clientOrderId)
            put("product_id", productId)
            put("side", side)
            putJsonObject("order_configuration") {
                putJsonObject("limit_limit_gtc") {
                    put("base_size", formatSize(baseSize))
                    put("limit_price", formatPrice(limitPrice))
                    put("post_only", true)  // Maker only - lower fees
                }
            }
        }

        return executeCreateOrder(body, clientOrderId)
    }

    /**
     * Place market order (for emergency liquidation)
     */
    suspend fun placeMarketOrder(
        productId: String,
        side: String,
        baseSize: Double
    ): OrderResult {
        val clientOrderId = UUID.randomUUID().toString()

        val body = buildJsonObject {
            put("client_order_id", clientOrderId)
            put("product_id", productId)
            put("side", side)
            putJsonObject("order_configuration") {
                putJsonObject("market_market_ioc") {
                    put("base_size", formatSize(baseSize))
                }
            }
        }

        return executeCreateOrder(body, clientOrderId)
    }

    private suspend fun executeCreateOrder(body: JsonObject, clientOrderId: String): OrderResult {
        val path = "/api/v3/brokerage/orders"
        val jwt = jwtGenerator.generateRestToken("POST", HOST, path)

        return try {
            val response: HttpResponse = client.post("$BASE_URL$path") {
                header("Authorization", "Bearer $jwt")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }

            val responseBody = response.bodyAsText()
            parseOrderResponse(responseBody, clientOrderId)
        } catch (e: Exception) {
            Log.e(TAG, "Order request failed", e)
            OrderResult.Failed(clientOrderId, null, "Network error: ${e.message}")
        }
    }

    private fun parseOrderResponse(responseBody: String?, clientOrderId: String): OrderResult {
        if (responseBody.isNullOrBlank()) {
            return OrderResult.Failed(clientOrderId, null, "Empty response")
        }

        return try {
            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
            val success = jsonResponse["success"]?.jsonPrimitive?.booleanOrNull ?: false

            if (success) {
                val successData = jsonResponse["success_response"]?.jsonObject
                val orderId = successData?.get("order_id")?.jsonPrimitive?.content ?: "unknown"
                OrderResult.Success(clientOrderId, orderId)
            } else {
                val errorData = jsonResponse["error_response"]?.jsonObject
                val errorMsg = errorData?.get("message")?.jsonPrimitive?.content
                    ?: errorData?.get("error")?.jsonPrimitive?.content
                    ?: "Unknown error"
                OrderResult.Failed(clientOrderId, null, errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response: $responseBody", e)
            OrderResult.Failed(clientOrderId, null, "Parse error: ${e.message}")
        }
    }

    // ==================== ORDER MANAGEMENT ====================

    suspend fun cancelOrders(orderIds: List<String>): Boolean {
        if (orderIds.isEmpty()) return true

        val path = "/api/v3/brokerage/orders/batch_cancel"
        val jwt = jwtGenerator.generateRestToken("POST", HOST, path)

        val body = buildJsonObject {
            putJsonArray("order_ids") { orderIds.forEach { add(it) } }
        }

        return try {
            val response: HttpResponse = client.post("$BASE_URL$path") {
                header("Authorization", "Bearer $jwt")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Cancel request failed", e)
            false
        }
    }

    suspend fun getOpenOrders(productId: String? = null): List<CoinbaseOrder> {
        val path = "/api/v3/brokerage/orders/historical/batch"
        val query = buildString {
            append("?order_status=OPEN")
            productId?.let { append("&product_id=$it") }
        }

        val jwt = jwtGenerator.generateRestToken("GET", HOST, path)

        return try {
            val response: HttpResponse = client.get("$BASE_URL$path$query") {
                header("Authorization", "Bearer $jwt")
            }

            if (!response.status.isSuccess()) return emptyList()

            val body = response.bodyAsText()
            val jsonResponse = json.parseToJsonElement(body).jsonObject
            val ordersArray = jsonResponse["orders"]?.jsonArray ?: return emptyList()

            ordersArray.map { parseOrder(it.jsonObject) }
        } catch (e: Exception) {
            Log.e(TAG, "Get orders failed", e)
            emptyList()
        }
    }

    // ==================== MARKET DATA ====================

    suspend fun getCandles(
        productId: String,
        granularity: String = "TWO_HOUR",
        limit: Int = 350
    ): List<Candle> {
        val path = "/api/v3/brokerage/products/$productId/candles"
        val query = "?granularity=$granularity&limit=$limit"

        val jwt = jwtGenerator.generateRestToken("GET", HOST, path)

        return try {
            val response: HttpResponse = client.get("$BASE_URL$path$query") {
                header("Authorization", "Bearer $jwt")
            }

            if (!response.status.isSuccess()) return emptyList()

            val body = response.bodyAsText()
            val jsonResponse = json.parseToJsonElement(body).jsonObject
            val candlesArray = jsonResponse["candles"]?.jsonArray ?: return emptyList()

            candlesArray.map { candleJson ->
                val obj = candleJson.jsonObject
                Candle(
                    timestamp = Instant.ofEpochSecond(
                        obj["start"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0
                    ),
                    open = obj["open"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    high = obj["high"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    low = obj["low"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    close = obj["close"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    volume = obj["volume"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                )
            }.sortedBy { it.timestamp }  // Ensure chronological order
        } catch (e: Exception) {
            Log.e(TAG, "Get candles failed", e)
            emptyList()
        }
    }

    suspend fun getAccounts(): List<AccountBalance> {
        val path = "/api/v3/brokerage/accounts"
        val jwt = jwtGenerator.generateRestToken("GET", HOST, path)

        return try {
            val response: HttpResponse = client.get("$BASE_URL$path") {
                header("Authorization", "Bearer $jwt")
            }

            if (!response.status.isSuccess()) return emptyList()

            val body = response.bodyAsText()
            val jsonResponse = json.parseToJsonElement(body).jsonObject
            val accountsArray = jsonResponse["accounts"]?.jsonArray ?: return emptyList()

            accountsArray.map { accountJson ->
                val obj = accountJson.jsonObject
                val balanceObj = obj["available_balance"]?.jsonObject
                AccountBalance(
                    currency = obj["currency"]?.jsonPrimitive?.content ?: "",
                    available = balanceObj?.get("value")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get accounts failed", e)
            emptyList()
        }
    }

    // ==================== HELPERS ====================

    private fun parseOrder(obj: JsonObject) = CoinbaseOrder(
        orderId = obj["order_id"]?.jsonPrimitive?.content ?: "",
        clientOrderId = obj["client_order_id"]?.jsonPrimitive?.content ?: "",
        productId = obj["product_id"]?.jsonPrimitive?.content ?: "",
        side = obj["side"]?.jsonPrimitive?.content ?: "",
        status = obj["status"]?.jsonPrimitive?.content ?: "",
        filledSize = obj["filled_size"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
        avgFilledPrice = obj["average_filled_price"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
    )

    private fun formatSize(size: Double) = "%.8f".format(size)
    private fun formatPrice(price: Double) = "%.2f".format(price)
}

// Data classes
sealed class OrderResult {
    data class Success(val clientOrderId: String, val exchangeOrderId: String) : OrderResult()
    data class Failed(val clientOrderId: String, val exchangeOrderId: String?, val error: String) : OrderResult()
}

data class CoinbaseOrder(
    val orderId: String,
    val clientOrderId: String,
    val productId: String,
    val side: String,
    val status: String,
    val filledSize: Double,
    val avgFilledPrice: Double
)

data class AccountBalance(
    val currency: String,
    val available: Double
)
```

---

## WebSocket Client

### CoinbaseWebSocket.kt

WebSocket client for real-time market data and order updates with automatic reconnection.

```kotlin
// data/remote/CoinbaseWebSocket.kt
package com.dpart.tradeflow.data.remote

import android.util.Log
import com.dpart.tradeflow.data.security.JwtGenerator
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class CoinbaseWebSocket(
    private val jwtGenerator: JwtGenerator,
    private val scope: CoroutineScope
) {
    private val client = HttpClient(OkHttp) {
        install(WebSockets) {
            pingInterval = 30_000  // 30 seconds
        }
        engine {
            config {
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    private var marketJob: Job? = null
    private var userJob: Job? = null

    private val _tickerFlow = MutableSharedFlow<TickerUpdate>(replay = 1)
    val tickerFlow: SharedFlow<TickerUpdate> = _tickerFlow

    private val _orderFlow = MutableSharedFlow<OrderUpdate>(replay = 0, extraBufferCapacity = 64)
    val orderFlow: SharedFlow<OrderUpdate> = _orderFlow

    // Reconnection state
    private val isConnecting = AtomicBoolean(false)
    private var backoffMs = 5000L
    private val maxBackoffMs = 60000L

    // Health monitoring
    private val lastMessageTime = AtomicLong(System.currentTimeMillis())
    private var healthCheckJob: Job? = null

    companion object {
        private const val TAG = "CoinbaseWebSocket"
        private const val MARKET_WS_URL = "wss://advanced-trade-ws.coinbase.com"
        private const val USER_WS_URL = "wss://advanced-trade-ws-user.coinbase.com"
        private const val HEALTH_CHECK_INTERVAL_MS = 45_000L
    }

    // ==================== CONNECTION ====================

    fun connect(productIds: List<String>) {
        if (isConnecting.getAndSet(true)) return

        connectMarketData(productIds)
        connectUserData(productIds)
        startHealthCheck(productIds)

        isConnecting.set(false)
    }

    private fun connectMarketData(productIds: List<String>) {
        marketJob?.cancel()
        marketJob = scope.launch {
            try {
                client.webSocket(MARKET_WS_URL) {
                    Log.d(TAG, "Market WebSocket opened")
                    backoffMs = 5000L

                    // Subscribe to channels
                    sendSubscribe("heartbeats", productIds, auth = false)
                    sendSubscribe("ticker", productIds, auth = false)

                    // Receive messages
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                lastMessageTime.set(System.currentTimeMillis())
                                parseMarketMessage(frame.readText())
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Market WebSocket failed", e)
                scheduleReconnect { connectMarketData(productIds) }
            }
        }
    }

    private fun connectUserData(productIds: List<String>) {
        userJob?.cancel()
        userJob = scope.launch {
            try {
                client.webSocket(USER_WS_URL) {
                    Log.d(TAG, "User WebSocket opened")

                    // Subscribe to user channel (requires auth)
                    sendSubscribe("user", productIds, auth = true)

                    // Receive messages
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                lastMessageTime.set(System.currentTimeMillis())
                                parseUserMessage(frame.readText())
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "User WebSocket failed", e)
                scheduleReconnect { connectUserData(productIds) }
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendSubscribe(
        channel: String,
        productIds: List<String>,
        auth: Boolean
    ) {
        val message = buildJsonObject {
            put("type", "subscribe")
            put("channel", channel)
            putJsonArray("product_ids") { productIds.forEach { add(it) } }
            if (auth) {
                put("jwt", jwtGenerator.generateWebSocketToken())
            }
        }
        send(Frame.Text(message.toString()))
    }

    // ==================== HEALTH CHECK ====================

    private fun startHealthCheck(productIds: List<String>) {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (isActive) {
                delay(HEALTH_CHECK_INTERVAL_MS)

                val timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime.get()
                if (timeSinceLastMessage > HEALTH_CHECK_INTERVAL_MS) {
                    Log.w(TAG, "WebSocket appears dead (${timeSinceLastMessage}ms since last msg)")
                    disconnect()
                    delay(1000)
                    connect(productIds)
                }
            }
        }
    }

    // ==================== MESSAGE PARSING ====================

    private fun parseMarketMessage(text: String) {
        try {
            val jsonObj = Json.parseToJsonElement(text).jsonObject
            val channel = jsonObj["channel"]?.jsonPrimitive?.content ?: return

            when (channel) {
                "ticker" -> {
                    val events = jsonObj["events"]?.jsonArray ?: return
                    events.forEach { event ->
                        val tickers = event.jsonObject["tickers"]?.jsonArray ?: return@forEach
                        tickers.forEach { ticker ->
                            val obj = ticker.jsonObject
                            val update = TickerUpdate(
                                productId = obj["product_id"]?.jsonPrimitive?.content ?: "",
                                price = obj["price"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                                bid = obj["best_bid"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                                ask = obj["best_ask"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                                volume24h = obj["volume_24_h"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                            )
                            scope.launch { _tickerFlow.emit(update) }
                        }
                    }
                }
                "heartbeats" -> {
                    // Connection alive - no action needed
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse market message", e)
        }
    }

    private fun parseUserMessage(text: String) {
        try {
            val jsonObj = Json.parseToJsonElement(text).jsonObject
            val channel = jsonObj["channel"]?.jsonPrimitive?.content ?: return

            if (channel == "user") {
                val events = jsonObj["events"]?.jsonArray ?: return
                events.forEach { event ->
                    val eventObj = event.jsonObject
                    val type = eventObj["type"]?.jsonPrimitive?.content

                    val orders = eventObj["orders"]?.jsonArray ?: return@forEach
                    orders.forEach { order ->
                        val obj = order.jsonObject
                        val update = OrderUpdate(
                            orderId = obj["order_id"]?.jsonPrimitive?.content ?: "",
                            clientOrderId = obj["client_order_id"]?.jsonPrimitive?.content ?: "",
                            productId = obj["product_id"]?.jsonPrimitive?.content ?: "",
                            status = obj["status"]?.jsonPrimitive?.content ?: "",
                            side = obj["side"]?.jsonPrimitive?.content ?: "",
                            filledSize = obj["filled_size"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                            avgFilledPrice = obj["average_filled_price"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                            eventType = type ?: "update"
                        )
                        scope.launch { _orderFlow.emit(update) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user message", e)
        }
    }

    // ==================== RECONNECTION ====================

    private fun scheduleReconnect(reconnectAction: () -> Unit) {
        scope.launch {
            Log.d(TAG, "Scheduling reconnect in ${backoffMs}ms")
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(maxBackoffMs)
            reconnectAction()
        }
    }

    fun disconnect() {
        healthCheckJob?.cancel()
        marketJob?.cancel()
        userJob?.cancel()
        marketJob = null
        userJob = null
    }
}

data class TickerUpdate(
    val productId: String,
    val price: Double,
    val bid: Double,
    val ask: Double,
    val volume24h: Double
)

data class OrderUpdate(
    val orderId: String,
    val clientOrderId: String,
    val productId: String,
    val status: String,
    val side: String,
    val filledSize: Double,
    val avgFilledPrice: Double,
    val eventType: String
)
```

---

## Key Implementation Details

### REST API

**Authentication:** Fresh JWT per request (2-minute expiry)
**Timeout:** 30 seconds for all operations
**Error Handling:** Returns sealed `OrderResult` for type-safe handling
**Price/Size Formatting:** Consistent decimal places (2 for price, 8 for size)

### WebSocket

**Dual Connections:**
- Market data: Ticker + heartbeats (unauthenticated)
- User data: Order updates (authenticated with JWT)

**Health Monitoring:**
- Checks every 45 seconds for incoming messages
- Auto-reconnects if no messages received
- Exponential backoff (5s → 60s max)

**Reconnection Strategy:**
- Separate jobs for market and user connections
- Independent reconnection for each
- Preserves subscription state on reconnect

---

## Navigation

- **[Back to Technical Reference](../reference.md)** - Parent document
- **[Previous: Security & Auth](security.md)** - JWT generation
- **[Next: Storage & Service](storage.md)** - Database and trading service
- **[See Also: Coinbase API](../api/coinbase.md)** - API specification
