# 🔌 INTERFACE - ExchangeWebSocketService

Effort level: Medium
Priority: High

## Objective

Define the real-time data abstraction interface for WebSocket streams.

## File

`domain/repository/ExchangeWebSocketService.kt`

## Interface Definition

```kotlin
interface ExchangeWebSocketService {
    val connectionState: StateFlow<ConnectionState>
    
    fun connect()
    fun disconnect()
    
    // Market data streams (no auth required)
    fun subscribeTicker(productIds: List<String>): Flow<TickerUpdate>
    fun subscribeOrderBook(productId: String): Flow<OrderBookUpdate>
    fun subscribeCandles(productId: String, granularity: CandleGranularity): Flow<Candle>
    
    // User data streams (requires auth)
    fun subscribeUserOrders(): Flow<OrderUpdate>
    fun subscribeHeartbeat(): Flow<Heartbeat>
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}
```

## Data Classes

```kotlin
data class TickerUpdate(
    val productId: String,
    val price: BigDecimal,
    val bid: BigDecimal,
    val ask: BigDecimal,
    val volume24h: BigDecimal,
    val timestamp: Instant
)

data class OrderUpdate(
    val orderId: String,
    val clientOrderId: String,
    val status: OrderStatus,
    val filledSize: BigDecimal,
    val avgFilledPrice: BigDecimal,
    val timestamp: Instant
)
```

## Why This Matters

- Each exchange has DIFFERENT WebSocket protocols
- This interface hides all protocol complexity
- Consumers only see clean Kotlin Flows
- Reconnection logic encapsulated per exchange

## Acceptance Criteria

- [ ]  Interface exposes only Flow<T> streams
- [ ]  Connection state observable via StateFlow
- [ ]  No exchange-specific types leak through
- [ ]  Supports both authenticated and public channels