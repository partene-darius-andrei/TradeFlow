# 🔌 EXCHANGE-API: Repository Interfaces

Effort level: Medium
Priority: High
Status: Not started
Blocked by: DOMAIN: Core Domain Models
Module: :exchange:api

## Objective

Define exchange-agnostic repository interfaces that ALL exchange implementations must follow.

## Module

`:exchange:api` (NO Android dependencies - pure Kotlin)

## Interfaces

### ExchangeRepository

```kotlin
interface ExchangeRepository {
    // Account
    suspend fun getBalances(): Result<List<Balance>>
    suspend fun getPortfolio(): Result<Portfolio>
    
    // Market Data
    suspend fun getCandles(
        productId: String,
        granularity: Granularity,
        limit: Int = 350
    ): Result<List<Candle>>
    
    suspend fun getCurrentPrice(productId: String): Result<Ticker>
    
    // Orders
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
```

### BracketOrderRepository (Optional capability)

```kotlin
interface BracketOrderRepository : ExchangeRepository {
    suspend fun placeBracketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        entryPrice: BigDecimal,
        takeProfit: BigDecimal,
        stopLoss: BigDecimal
    ): Result<Order>
}
```

### ExchangeWebSocket

```kotlin
interface ExchangeWebSocket {
    val connectionState: StateFlow<ConnectionState>
    
    fun connect()
    fun disconnect()
    
    // Market data streams
    fun subscribeTicker(productIds: List<String>): Flow<Ticker>
    fun subscribeCandles(productId: String, granularity: Granularity): Flow<Candle>
    
    // User data (authenticated)
    fun subscribeOrderUpdates(): Flow<Order>
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
}
```

### AuthTokenProvider

```kotlin
interface AuthTokenProvider {
    suspend fun getToken(method: String, path: String): String
    suspend fun getWebSocketToken(): String
    fun invalidate()
}
```

### CredentialStore

```kotlin
interface CredentialStore {
    suspend fun saveCredentials(apiKey: String, secret: String)
    suspend fun getApiKey(): String?
    suspend fun getSecret(): String?
    suspend fun hasCredentials(): Boolean
    suspend fun clearCredentials()
}
```

## Error Types

```kotlin
sealed class ExchangeError : Exception() {
    data class AuthenticationFailed(override val message: String) : ExchangeError()
    data class RateLimited(val retryAfterSeconds: Int) : ExchangeError()
    data class InsufficientFunds(val required: BigDecimal, val available: BigDecimal) : ExchangeError()
    data class OrderRejected(val reason: String) : ExchangeError()
    data class NetworkError(override val cause: Throwable) : ExchangeError()
    data class Unknown(override val message: String) : ExchangeError()
}
```

## File Structure

```
exchange/api/src/main/kotlin/com/tradeflow/exchange/api/
├── repository/
│   ├── ExchangeRepository.kt
│   ├── BracketOrderRepository.kt
│   └── ExchangeWebSocket.kt
├── auth/
│   ├── AuthTokenProvider.kt
│   └── CredentialStore.kt
└── error/
    └── ExchangeError.kt
```

## Acceptance Criteria

- [ ]  All interfaces use domain models (not DTOs)
- [ ]  All async operations return `Result<T>`
- [ ]  No Coinbase-specific types leak into interfaces
- [ ]  WebSocket uses Kotlin Flow for streams