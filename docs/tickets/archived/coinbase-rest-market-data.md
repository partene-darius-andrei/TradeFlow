# 📊 [SUPERSEDED] CoinbaseRestApi - Market Data & Accounts

Effort level: Medium
Priority: High
Status: Done
Blocked by: Replaced by: 🟡 COINBASE: REST API Client

## Objective

Extend REST API client for market data and account information.

## File

`data/remote/CoinbaseRestApi.kt` (extend existing)

## Endpoints

- `GET /api/v3/brokerage/products/{product_id}/candles` - OHLCV data
- `GET /api/v3/brokerage/accounts` - Account balances

## Methods

### getCandles

```kotlin
suspend fun getCandles(
    productId: String,
    granularity: String = "TWO_HOUR",
    limit: Int = 350
): List<Candle>
```

**Note:** Using Ktor HttpClient with coroutines

**Critical Notes:**

- Max 350 candles per request
- For H4: Use `TWO_HOUR` and aggregate pairs
- Available: ONE_MINUTE, FIVE_MINUTE, FIFTEEN_MINUTE, THIRTY_MINUTE, ONE_HOUR, TWO_HOUR, SIX_HOUR, ONE_DAY
- Response `start` is Unix timestamp (seconds), not ISO string

### getAccounts

```kotlin
suspend fun getAccounts(): List<AccountBalance>

data class AccountBalance(
    val currency: String,
    val available: Double
)
```

## H4 Aggregation

```kotlin
fun aggregateToH4(twoHourCandles: List<Candle>): List<Candle> {
    return twoHourCandles.chunked(2).mapNotNull { pair ->
        if (pair.size < 2) return@mapNotNull null
        Candle(
            timestamp = pair[0].timestamp,
            open = pair[0].open,
            high = maxOf(pair[0].high, pair[1].high),
            low = minOf(pair[0].low, pair[1].low),
            close = pair[1].close,
            volume = pair[0].volume + pair[1].volume
        )
    }
}
```

## Acceptance Criteria

- Can fetch 350 candles in single request
- Candles sorted chronologically
- Account balances retrieved for USD and BTC