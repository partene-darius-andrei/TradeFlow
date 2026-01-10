# 🧪 TEST: Integration Tests

Effort level: Medium
Priority: Medium
Blocked by: COINBASE: REST API Client, COINBASE: WebSocket Client
Module: :exchange:coinbase

## Objective

Test real Coinbase API integration with small trades.

## Module

`:exchange:coinbase` (androidTest)

## Prerequisites

- Valid Coinbase API credentials (trade permission)
- Small amount of USD ($50-100) for testing
- **Sandbox is NOT usable** - returns static mock data only

## Test Cases

### 1. Authentication

```kotlin
@Test
fun `JWT token accepted by Coinbase`() = runTest {
    val repository = createTestRepository()
    val result = repository.getBalances()
    assertTrue(result.isSuccess)
}
```

### 2. Market Data

```kotlin
@Test
fun `can fetch 350 candles`() = runTest {
    val repository = createTestRepository()
    val result = repository.getCandles("BTC-USD", Granularity.TWO_HOUR, 350)
    assertTrue(result.isSuccess)
    assertEquals(350, result.getOrThrow().size)
}
```

### 3. Order Placement (Small Real Trade)

```kotlin
@Test
fun `can place and cancel limit order`() = runTest {
    val repository = createTestRepository()
    val currentPrice = repository.getCurrentPrice("BTC-USD").getOrThrow()
    
    // Place order at 50% below market (won't fill)
    val farPrice = currentPrice.price * "0.5".toBigDecimal()
    val result = repository.placeLimitOrder(
        productId = "BTC-USD",
        side = [OrderSide.BUY](http://OrderSide.BUY),
        size = "0.0001".toBigDecimal(),  // ~$10
        price = farPrice
    )
    
    assertTrue(result.isSuccess)
    val order = result.getOrThrow()
    
    // Cancel immediately
    val cancelResult = repository.cancelOrder([order.id](http://order.id))
    assertTrue(cancelResult.isSuccess)
}
```

### 4. WebSocket Connection

```kotlin
@Test
fun `WebSocket receives ticker updates`() = runTest {
    val webSocket = createTestWebSocket()
    webSocket.connect()
    
    val ticker = withTimeout(10_000) {
        webSocket.subscribeTicker(listOf("BTC-USD")).first()
    }
    
    assertTrue(ticker.price > [BigDecimal.ZERO](http://BigDecimal.ZERO))
    webSocket.disconnect()
}
```

### 5. Order Status Updates

```kotlin
@Test
fun `WebSocket receives order updates`() = runTest {
    val webSocket = createTestWebSocket()
    val repository = createTestRepository()
    
    webSocket.connect()
    val orderFlow = webSocket.subscribeOrderUpdates()
    
    // Place order
    val placeResult = repository.placeLimitOrder(...)
    val orderId = placeResult.getOrThrow().id
    
    // Should receive update via WebSocket
    val update = withTimeout(5_000) {
        orderFlow.first { [it.id](http://it.id) == orderId }
    }
    
    assertEquals([OrderStatus.OPEN](http://OrderStatus.OPEN), update.status)
    
    // Cleanup
    repository.cancelOrder(orderId)
    webSocket.disconnect()
}
```

## Safety Notes

- Use minimum order sizes (~$10)
- Place orders far from market
- Always cancel test orders
- Run tests manually, not in CI

## Acceptance Criteria

- [ ]  All API calls succeed with valid credentials
- [ ]  Orders can be placed and cancelled
- [ ]  WebSocket connects and receives data
- [ ]  No unexpected fees incurred
