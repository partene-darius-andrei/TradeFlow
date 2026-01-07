# 🔌 EXCHANGE-API: Repository Interfaces

Effort level: Medium
Priority: High
Completed: 2026-01-07
PR: #7
Blocked by: DOMAIN: Core Domain Models
Module: :core:domain (implemented here instead of separate :exchange:api)

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

- [x]  All interfaces use domain models (not DTOs)
- [x]  All async operations return `Result<T>`
- [x]  No Coinbase-specific types leak into interfaces
- [x]  WebSocket uses Kotlin Flow for streams

---

## Post-Implementation Notes

**Completed:** 2026-01-07
**PR:** https://github.com/partene-darius-andrei/TradeFlow/pull/7

### Implementation Summary

All repository interfaces created successfully in `:core:domain` instead of separate `:exchange:api` module. This follows Clean Architecture principles where domain layer defines contracts.

### Files Created

**Repository Layer (3 files):**
1. **ExchangeRepository.kt** - 12 methods (accounts, market data, orders)
2. **BracketOrderRepository.kt** - Extends ExchangeRepository with bracket order support
3. **ExchangeWebSocket.kt** - Real-time streams + ConnectionState enum

**Auth Layer (2 files):**
4. **AuthTokenProvider.kt** - Token generation interface (REST + WebSocket)
5. **CredentialStore.kt** - Secure credential storage interface

**Error Handling (1 file):**
6. **ExchangeError.kt** - Sealed class with 6 error types

### Key Decisions

**Module Location:**
- Placed in `:core:domain` instead of new `:exchange:api` module
- Follows Dependency Inversion Principle (domain defines contracts, infrastructure implements)
- Keeps architecture simple - no need for separate API module
- `:exchange:coinbase` will depend on `:core:domain` and implement these interfaces

**Result Type:**
- All async operations return `Result<T>` (not exceptions)
- Consistent error handling across all exchange implementations
- Easy to map to UI states (Success, Error, Loading)

**Flow vs LiveData:**
- WebSocket uses Kotlin `Flow<T>` (not LiveData)
- More flexible, works in pure Kotlin modules
- Better for repository layer (LiveData is UI-focused)

**ConnectionState Enum:**
- Added to ExchangeWebSocket interface
- 5 states: DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
- Helps UI show connection status

