# 🟡 COINBASE: REST API Client

Effort level: Large
Priority: High
Status: Not started
Blocked by: COINBASE: JWT Token Generator
Module: :exchange:coinbase

## Objective

Implement Coinbase Advanced Trade REST API.

## Module

`:exchange:coinbase`

## Implements

- `ExchangeRepository`
- `BracketOrderRepository`

## Endpoints (Validated)

| Operation | Endpoint | Method |
| --- | --- | --- |
| Get Accounts | `/api/v3/brokerage/accounts` | GET |
| Get Candles | `/api/v3/brokerage/products/{product_id}/candles` | GET |
| Create Order | `/api/v3/brokerage/orders` | POST |
| Cancel Orders | `/api/v3/brokerage/orders/batch_cancel` | POST |
| List Orders | `/api/v3/brokerage/orders/historical/batch` | GET |
| Get Order | `/api/v3/brokerage/orders/historical/{order_id}` | GET |

## Rate Limits (Validated)

- Private endpoints: **30 requests/second**
- Public endpoints: **10 requests/second**
- On 429: Wait **30 seconds** before retry

## Implementation

```kotlin
class CoinbaseRepository(
    private val httpClient: HttpClient,
    private val authProvider: AuthTokenProvider,
    private val json: Json
) : BracketOrderRepository {
    
    private val baseUrl = "[https://api.coinbase.com](https://api.coinbase.com)"
    
    override suspend fun getCandles(
        productId: String,
        granularity: Granularity,
        limit: Int
    ): Result<List<Candle>> = runCatching {
        val path = "/api/v3/brokerage/products/$productId/candles"
        val response = authenticatedGet<CandlesResponse>(path) {
            parameter("granularity", granularity.toCoinbase())
            parameter("limit", limit.coerceAtMost(350))
        }
        [response.candles.map](http://response.candles.map) { it.toDomain() }
    }
    
    override suspend fun placeBracketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        entryPrice: BigDecimal,
        takeProfit: BigDecimal,
        stopLoss: BigDecimal
    ): Result<Order> = runCatching {
        val request = CoinbaseOrderRequest(
            productId = productId,
            side = side.toCoinbase(),
            orderConfiguration = OrderConfiguration.TriggerBracketGtc(
                baseSize = size.toPlainString(),
                limitPrice = takeProfit.toPlainString(),  // TP goes here!
                stopTriggerPrice = stopLoss.toPlainString()
            )
        )
        // ...
    }
}
```

## Bracket Order Mapping (Validated ✅)

- `limit_price` = **Take Profit** (counterintuitive!)
- `stop_trigger_price` = **Stop Loss**
- Do NOT include `base_size` in attached config

## DTOs

```
exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
├── api/
│   └── CoinbaseApiService.kt
├── dto/
│   ├── CoinbaseOrderDto.kt
│   ├── CoinbaseCandleDto.kt
│   └── CoinbaseAccountDto.kt
└── mapper/
    ├── OrderMapper.kt
    └── CandleMapper.kt
```

## Acceptance Criteria

- [ ]  All REST endpoints implemented
- [ ]  Maps Coinbase DTOs to domain models
- [ ]  Rate limiting with exponential backoff
- [ ]  Integration test with real API (small trades)