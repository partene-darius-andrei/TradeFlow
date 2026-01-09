# Historical Data Testing with Binance API

## Overview

`BinanceDataLoader` provides easy access to real historical BTC price data for integration testing without authentication.

## Quick Start

```kotlin
// Get last 10 hourly candles
val candles = BinanceDataLoader.fetchHistoricalCandles(
    symbol = "BTCUSDT",
    interval = "1h",
    limit = 10
)

// Get last week of data
val weekData = BinanceDataLoader.fetchBtcUsdtLastWeek()

// Get entire year 2024
val yearData = BinanceDataLoader.fetchBtcUsdtYear2024()
```

## API Details

### Binance Klines Endpoint
- **URL**: `https://api.binance.com/api/v3/klines`
- **Authentication**: None required (public endpoint)
- **Rate Limit**: Weight 2 per request
- **Max Candles**: 1000 per request

### Available Intervals
- Seconds: `1s`
- Minutes: `1m`, `3m`, `5m`, `15m`, `30m`
- Hours: `1h`, `2h`, `4h`, `6h`, `8h`, `12h`
- Days: `1d`, `3d`
- Weeks: `1w`
- Months: `1M`

### Parameters
| Param | Type | Description |
|-------|------|-------------|
| `symbol` | String | Trading pair (BTCUSDT, ETHUSDT) |
| `interval` | String | Candle interval (see above) |
| `startTime` | Long | Unix timestamp in milliseconds |
| `endTime` | Long | Unix timestamp in milliseconds |
| `limit` | Int | Default 500, Max 1000 |

## Response Format

Each candle contains:
```json
[
  1499040000000,      // 0: Open time (Unix ms)
  "0.01634790",       // 1: Open price
  "0.80000000",       // 2: High price
  "0.01575800",       // 3: Low price
  "0.01577100",       // 4: Close price
  "148976.11427815",  // 5: Volume
  1499644799999,      // 6: Close time
  "2434.19055334",    // 7: Quote asset volume
  308,                // 8: Number of trades
  "1756.87402397",    // 9: Taker buy base volume
  "28.46694368",      // 10: Taker buy quote volume
  "0"                 // 11: Unused
]
```

## Example Tests

### 1. Quick Connectivity Test (Fast)
```kotlin
@Test
fun `verify Binance API works`() {
    val candles = BinanceDataLoader.fetchHistoricalCandles(
        symbol = "BTCUSDT",
        interval = "1h",
        limit = 10
    )

    assertTrue(candles.size == 10)
    assertTrue(candles.all { it.close > BigDecimal.ZERO })
}
```

### 2. Strategy Backtest (Slow - @Ignore by default)
```kotlin
@Test
@Ignore("Slow - fetches from API")
fun `backtest on 2024 data`() {
    val candles = BinanceDataLoader.fetchBtcUsdtYear2024()
    val decisionEngine = TradingDecisionEngine(...)

    candles.forEach { candle ->
        val decision = decisionEngine.makeDecision(candles, portfolio, config)
        // Analyze results
    }
}
```

### 3. Recent Market Behavior (Medium)
```kotlin
@Test
@Ignore("Slow - fetches from API")
fun `analyze last week`() {
    val candles = BinanceDataLoader.fetchBtcUsdtLastWeek()
    // Run decision engine on recent data
}
```

## Best Practices

### Test Organization
- **Fast tests** (< 100ms): Use small `limit` values (10-50 candles)
- **Slow tests** (> 1s): Mark with `@Ignore("Slow - fetches from API")`
- **CI/CD**: Only run fast tests automatically

### Data Caching
Consider downloading data once and saving to CSV for repeated tests:

```kotlin
// Download and cache
val candles = BinanceDataLoader.fetchBtcUsdtYear2024()
File("src/test/resources/btc_2024.csv").writeText(
    candles.joinToString("\n") { "${it.timestamp},${it.open},${it.high},${it.low},${it.close},${it.volume}" }
)

// Load from cache (blazing fast)
val cachedCandles = File("src/test/resources/btc_2024.csv")
    .readLines()
    .map { /* parse CSV */ }
```

## Differences from Coinbase

| Aspect | Binance | Coinbase |
|--------|---------|----------|
| Symbol | BTCUSDT | BTC-USD |
| Price | ~$1 higher | Slightly lower |
| Volume | Higher | Lower |
| Auth | None | JWT required |
| Rate Limit | Higher | Lower |

**Impact:** Minor price variance (< 0.1%), negligible for strategy testing.

## Unix Timestamp Reference

```bash
# Jan 1, 2024 00:00:00 UTC
1704067200000

# Dec 31, 2024 23:59:59 UTC
1735689599999

# Generate timestamp
date -u -d "2024-06-01 00:00:00" +%s000
```

## Troubleshooting

### Error: "Too many requests"
- Reduce request frequency
- Add delay between calls: `delay(100)`

### Error: "Invalid symbol"
- Use uppercase (BTCUSDT not btcusdt)
- Check supported pairs: https://api.binance.com/api/v3/exchangeInfo

### Empty response
- Verify timestamps are in milliseconds (not seconds)
- Check `startTime < endTime`

## Resources

- **Binance API Docs**: https://binance-docs.github.io/apidocs/spot/en/#kline-candlestick-data
- **Epoch Converter**: https://www.epochconverter.com/
