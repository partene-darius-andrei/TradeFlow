# 🔍 Integration Tests - Coinbase API

Effort level: Medium
Priority: Medium
Status: Not started

## Objective

Test real API integration with small trades.

## Prerequisites

- Valid Coinbase API credentials (trade permission)
- Small amount of USD ($50-100) for testing
- **Sandbox is NOT usable** - returns static mock data only

## Test Cases

### 1. Authentication

```kotlin
@Test
fun `JWT token accepted by Coinbase`() {
    val accounts = restApi.getAccounts()
    assertTrue(accounts.isNotEmpty())
}
```

### 2. Market Data

```kotlin
@Test
fun `can fetch 350 candles`() {
    val candles = restApi.getCandles("BTC-USD", "TWO_HOUR", 350)
    assertEquals(350, candles.size)
}
```

### 3. Order Placement (Small Real Trade)

```kotlin
@Test
fun `can place and cancel limit order`() {
    // Place order far from market (won't fill)
    val price = currentPrice * 0.5  // 50% below market
    val result = restApi.placeLimitOrder(
        productId = "BTC-USD",
        side = "BUY",
        baseSize = 0.0001,  // ~$10
        limitPrice = price
    )
    
    assertIs<OrderResult.Success>(result)
    
    // Cancel immediately
    val cancelled = restApi.cancelOrders(listOf(result.exchangeOrderId))
    assertTrue(cancelled)
}
```

### 4. WebSocket Connection

```kotlin
@Test
fun `WebSocket receives ticker updates`() = runTest {
    webSocket.connect(listOf("BTC-USD"))
    
    val update = withTimeout(10_000) {
        webSocket.tickerFlow.first()
    }
    
    assertTrue(update.price > 0)
    webSocket.disconnect()
}
```

### 5. Order Status Updates

```kotlin
@Test
fun `WebSocket receives order updates`() = runTest {
    webSocket.connect(listOf("BTC-USD"))
    
    // Place order
    val result = restApi.placeLimitOrder(...)
    
    // Should receive update via WebSocket
    val update = withTimeout(5_000) {
        webSocket.orderFlow.first { it.orderId == result.exchangeOrderId }
    }
    
    assertEquals("OPEN", update.status)
}
```

## Safety Notes

- Use minimum order sizes (~$10)
- Place orders far from market
- Always cancel test orders
- Run tests manually, not in CI

## Acceptance Criteria

- All API calls succeed with valid credentials
- Orders can be placed and cancelled
- WebSocket connects and receives data
- No unexpected fees incurred