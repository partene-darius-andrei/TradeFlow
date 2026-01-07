# 🧠 EngineDecisionEngine - Strategy Logic

Effort level: Large
Priority: High
Status: Not started

## Objective

Implement the regime-switching decision engine using ta4j indicators.

## File

`domain/strategy/EngineDecisionEngine.kt`

## Indicators

- **SMA(200)** - Trend filter (price above = bullish)
- **ADX(14)** - Trend strength (>25 = trending)
- **ATR(14)** - Volatility for position sizing

## Decision Logic

```
1. Price < SMA(200) → DEFENSE (instant, no hysteresis)
2. Price > SMA(200) AND ADX > 25 for 3 candles → TREND
3. Price > SMA(200) AND ADX < 25 for 3 candles → RANGE
4. Otherwise → WAIT (maintain current state)
```

## Hysteresis

- **DEFENSE:** Instant switch (safety first)
- **TREND:** Requires 3 consecutive H4 candles with ADX > 25
- **RANGE:** Requires 3 consecutive H4 candles with ADX < 25

## Grid Spacing Formula

```kotlin
val spacing = maxOf(
    currentPrice * 0.015,  // 1.5% minimum (fee break-even)
    atr14                   // ATR-based
)
```

## Trend Targets

```kotlin
stopLossPrice = currentPrice - (3 * atr14)
takeProfitPrice = currentPrice + (6 * atr14)  // 2:1 R:R
```

## Dependencies

- ta4j-core library
- Requires 200+ H4 candles for SMA(200)

## Acceptance Criteria

- Correctly identifies all 4 modes
- Hysteresis prevents whipsawing
- Grid spacing never below 1.5%
- Unit tests pass with mock data