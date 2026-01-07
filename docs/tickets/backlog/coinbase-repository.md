# 🏛️ COINBASE - Repository Implementation

Effort level: Large
Priority: High
Status: Not started

## Objective

Implement `ExchangeRepository` interface for Coinbase Advanced Trade API.

## Files

```
data/exchange/coinbase/
├── CoinbaseRepository.kt      # Implements ExchangeRepository
├── api/
│   └── CoinbaseApiService.kt  # Retrofit/Ktor interface
├── dto/
│   ├── CoinbaseOrderDto.kt
│   ├── CoinbaseCandleDto.kt
│   └── CoinbaseAccountDto.kt
└── mapper/
    ├── CoinbaseOrderMapper.kt
    └── CoinbaseCandleMapper.kt
```

## API Endpoints (VALIDATED)

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/v3/brokerage/accounts` | List accounts |
| GET | `/api/v3/brokerage/products/{product_id}/candles` | OHLCV data |
| POST | `/api/v3/brokerage/orders` | Place order |
| POST | `/api/v3/brokerage/orders/batch_cancel` | Cancel orders |
| GET | `/api/v3/brokerage/orders/historical/{order_id}` | Get order |

## Rate Limits (CORRECTED)

- Private endpoints: **30 requests/second**
- Public endpoints: **10 requests/second**
- On 429: Wait 30 seconds before retry

## Implementation Notes

```kotlin
class CoinbaseRepository @Inject constructor(
    private val api: CoinbaseApiService,
    private val authProvider: AuthTokenProvider,
    private val orderMapper: CoinbaseOrderMapper,
    private val candleMapper: CoinbaseCandleMapper
) : BracketOrderRepository {

    override suspend fun getCandles(
        productId: String,
        granularity: CandleGranularity,
        startTime: Instant,
        endTime: Instant
    ): Result<List<Candle>> = runCatching {
        val response = api.getCandles(
            productId = productId,
            granularity = granularity.toCoinbaseGranularity(),
            start = startTime.epochSecond,
            end = endTime.epochSecond
        )
        [response.candles.map](http://response.candles.map) { candleMapper.toDomain(it) }
    }
    
    override suspend fun placeBracketOrder(
        order: OrderRequest,
        takeProfitPrice: BigDecimal,
        stopLossPrice: BigDecimal
    ): Result<BracketOrderResponse> = runCatching {
        val request = CoinbaseBracketOrderRequest(
            clientOrderId = order.clientOrderId,
            productId = order.productId,
            side = [order.side.name](http://order.side.name),
            orderConfiguration = TriggerBracketGtc(
                baseSize = order.size.toPlainString(),
                limitPrice = takeProfitPrice.toPlainString(),  // TP
                stopTriggerPrice = stopLossPrice.toPlainString()  // SL
            )
        )
        val response = api.createOrder(request)
        orderMapper.toBracketResponse(response)
    }
}
```

## Depends On

- 🔌 INTERFACE - ExchangeRepository
- 🔌 INTERFACE - AuthTokenProvider
- 🏛️ COINBASE - JWT Generator

## Acceptance Criteria

- [ ]  Implements ExchangeRepository interface fully
- [ ]  Implements BracketOrderRepository for bracket orders
- [ ]  All Coinbase DTOs in dto/ package
- [ ]  Mappers convert to/from domain models
- [ ]  Rate limiting handled with retry logic