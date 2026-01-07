# 📡 CoinbaseRestApi - Order Placement

Effort level: Large
Priority: High

## Objective

Implement REST API client for order management.

## File

`data/remote/CoinbaseRestApi.kt`

## Endpoints

- `POST /api/v3/brokerage/orders` - Create order
- `POST /api/v3/brokerage/orders/batch_cancel` - Cancel orders
- `GET /api/v3/brokerage/orders/historical/batch` - List orders

## Order Methods

### placeBracketOrder (TREND mode)

```kotlin
suspend fun placeBracketOrder(
    productId: String,
    side: String,
    baseSize: Double,
    entryPrice: Double,
    takeProfitPrice: Double,  // Goes in limit_price
    stopLossPrice: Double     // Goes in stop_trigger_price
): OrderResult
```

### placeLimitOrder (RANGE/grid mode)

```kotlin
suspend fun placeLimitOrder(
    productId: String,
    side: String,
    baseSize: Double,
    limitPrice: Double
): OrderResult
```

**Must include `post_only: true`** for maker fees (0.60% vs 1.20%)

### placeMarketOrder (Emergency liquidation)

```kotlin
suspend fun placeMarketOrder(
    productId: String,
    side: String,
    baseSize: Double
): OrderResult
```

## Critical Notes

- Rate limit: 10,000 requests/hour
- Use Ktor HttpClient with coroutines - all methods are `suspend` functions
- Parse both success and error responses using kotlinx.serialization.json

## Acceptance Criteria

- Can place all 3 order types
- Can cancel orders by ID
- Can list open orders
- Handles API errors gracefully