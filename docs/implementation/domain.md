# Domain Models & Decision Engine

**Parent:** [../reference.md](../reference.md)

Core domain models and strategy decision engine implementation.

---

## Domain Models

### Candle.kt

```kotlin
// domain/model/Candle.kt
package com.dpart.tradeflow.domain.model

import java.time.Instant

data class Candle(
    val timestamp: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)
```

### Decision.kt

```kotlin
// domain/model/Decision.kt
package com.dpart.tradeflow.domain.model

sealed class Decision {
    abstract val confidence: Double  // 0.0 to 1.0 - drives position sizing

    data class Wait(
        val reason: String,
        override val confidence: Double = 0.0
    ) : Decision()

    data class Defense(
        val reason: String,
        override val confidence: Double = 0.0  // No trades in defense
    ) : Decision()

    data class Trend(
        val stopLossPrice: Double,
        val takeProfitPrice: Double,
        val atr: Double,
        override val confidence: Double  // Based on ADX strength + confirmation
    ) : Decision()

    data class Range(
        val gridSpacing: Double,
        val atr: Double,
        override val confidence: Double  // Based on ADX weakness + confirmation
    ) : Decision()
}
```

### Confidence Calculation

**Critical:** Confidence score determines position size. Must be accurate.

```kotlin
// Confidence scoring for TREND mode
fun calculateTrendConfidence(
    adx: Double,
    confirmationCount: Int,
    maxConfirmations: Int = 3
): Double {
    val adxStrength = ((adx - 25.0) / 50.0).coerceIn(0.0, 1.0)  // 25 = 0.0, 75+ = 1.0
    val confirmationFactor = confirmationCount.toDouble() / maxConfirmations

    // Weighted combination: 60% ADX strength, 40% confirmations
    return (0.6 * adxStrength + 0.4 * confirmationFactor).coerceIn(0.75, 1.0)
}

// Confidence scoring for RANGE mode
fun calculateRangeConfidence(
    adx: Double,
    confirmationCount: Int,
    maxConfirmations: Int = 3
): Double {
    val adxWeakness = ((25.0 - adx) / 25.0).coerceIn(0.0, 1.0)  // 25 = 0.0, 0 = 1.0
    val confirmationFactor = confirmationCount.toDouble() / maxConfirmations

    // Weighted combination: 60% ADX weakness, 40% confirmations
    return (0.6 * adxWeakness + 0.4 * confirmationFactor).coerceIn(0.75, 1.0)
}
```

**Examples:**

| Scenario | ADX | Confirmations | Confidence | Result |
|----------|-----|---------------|------------|--------|
| Strong trend | 60 | 3/3 | 0.88 | 3.9% position |
| Weak trend | 28 | 3/3 | 0.78 | 2.2% position |
| Strong range | 10 | 3/3 | 0.88 | 3.9% position |
| Weak range | 22 | 3/3 | 0.77 | 2.1% position |
| Unconfirmed | 50 | 1/3 | 0.53 | No trade (< 0.75) |

**Validation:** Track confidence vs. win rate correlation. If no correlation exists after 50+ trades, disable confidence-based sizing.

---

## Decision Engine

### EngineDecisionEngine.kt

Complete implementation of the regime-switching decision engine with hysteresis logic.

```kotlin
// domain/strategy/EngineDecisionEngine.kt
package com.dpart.tradeflow.domain.strategy

import com.dpart.tradeflow.domain.model.Candle
import com.dpart.tradeflow.domain.model.Decision
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.ATRIndicator
import org.ta4j.core.indicators.SMAIndicator
import org.ta4j.core.indicators.adx.ADXIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import java.time.Duration
import java.time.ZoneOffset

class EngineDecisionEngine {

    // Hysteresis counters (persist across evaluations)
    private var trendConfirmCount = 0
    private var rangeConfirmCount = 0

    companion object {
        private const val SMA_PERIOD = 200
        private const val ADX_PERIOD = 14
        private const val ATR_PERIOD = 14
        private const val ADX_TREND_THRESHOLD = 25.0
        private const val HYSTERESIS_CANDLES = 3
        private const val MIN_GRID_SPACING_PERCENT = 0.015 // 1.5%
        private const val STOP_LOSS_ATR_MULT = 3.0
        private const val TAKE_PROFIT_ATR_MULT = 6.0  // 2:1 R:R
    }

    fun evaluate(candles: List<Candle>, currentPrice: Double): Decision {

        // Need enough history for SMA(200)
        if (candles.size < SMA_PERIOD) {
            return Decision.Wait("Initializing: ${candles.size}/$SMA_PERIOD candles")
        }

        // Build ta4j series from candles
        val series = BaseBarSeriesBuilder().withName("Engine").build()
        candles.forEach { candle ->
            series.addBar(
                Duration.ofHours(4),
                candle.timestamp.atZone(ZoneOffset.UTC),
                candle.open,
                candle.high,
                candle.low,
                candle.close,
                candle.volume
            )
        }

        // Calculate indicators
        val closePrice = ClosePriceIndicator(series)
        val lastIndex = series.endIndex

        val sma200 = SMAIndicator(closePrice, SMA_PERIOD)
            .getValue(lastIndex).doubleValue()

        val adx14 = ADXIndicator(series, ADX_PERIOD)
            .getValue(lastIndex).doubleValue()

        val atr14 = ATRIndicator(series, ATR_PERIOD)
            .getValue(lastIndex).doubleValue()

        // Regime detection
        val isPriceAboveSma = currentPrice > sma200
        val isStrongTrend = adx14 > ADX_TREND_THRESHOLD

        // State machine with hysteresis
        return when {
            // DEFENSE: Instant switch (safety first, no hysteresis)
            !isPriceAboveSma -> {
                resetCounters()
                Decision.Defense(
                    reason = "Price ($currentPrice) below SMA ($sma200)",
                    confidence = 0.0  // No trading in defense
                )
            }

            // TREND: Requires 3 consecutive confirmations
            isStrongTrend -> {
                trendConfirmCount++
                rangeConfirmCount = 0

                if (trendConfirmCount >= HYSTERESIS_CANDLES) {
                    val confidence = calculateTrendConfidence(
                        adx = adx14,
                        confirmationCount = trendConfirmCount,
                        maxConfirmations = HYSTERESIS_CANDLES
                    )

                    Decision.Trend(
                        stopLossPrice = currentPrice - (STOP_LOSS_ATR_MULT * atr14),
                        takeProfitPrice = currentPrice + (TAKE_PROFIT_ATR_MULT * atr14),
                        atr = atr14,
                        confidence = confidence
                    )
                } else {
                    Decision.Wait(
                        reason = "Trend confirming: $trendConfirmCount/$HYSTERESIS_CANDLES",
                        confidence = 0.0
                    )
                }
            }

            // RANGE: Requires 3 consecutive confirmations
            else -> {
                rangeConfirmCount++
                trendConfirmCount = 0

                if (rangeConfirmCount >= HYSTERESIS_CANDLES) {
                    // Grid spacing: max of 1.5% or ATR-based
                    val atrSpacing = atr14
                    val minSpacing = currentPrice * MIN_GRID_SPACING_PERCENT
                    val spacing = maxOf(atrSpacing, minSpacing)

                    val confidence = calculateRangeConfidence(
                        adx = adx14,
                        confirmationCount = rangeConfirmCount,
                        maxConfirmations = HYSTERESIS_CANDLES
                    )

                    Decision.Range(
                        gridSpacing = spacing,
                        atr = atr14,
                        confidence = confidence
                    )
                } else {
                    Decision.Wait(
                        reason = "Range confirming: $rangeConfirmCount/$HYSTERESIS_CANDLES",
                        confidence = 0.0
                    )
                }
            }
        }
    }

    private fun calculateTrendConfidence(
        adx: Double,
        confirmationCount: Int,
        maxConfirmations: Int
    ): Double {
        // ADX strength: 25 = minimum (0.0), 75+ = maximum (1.0)
        val adxStrength = ((adx - 25.0) / 50.0).coerceIn(0.0, 1.0)

        // Confirmation progress: 3/3 = 1.0, 1/3 = 0.33
        val confirmationFactor = confirmationCount.toDouble() / maxConfirmations

        // Weighted: 60% ADX strength, 40% confirmations
        // Result always >= 0.75 (minimum tradeable confidence)
        return (0.6 * adxStrength + 0.4 * confirmationFactor).coerceIn(0.75, 1.0)
    }

    private fun calculateRangeConfidence(
        adx: Double,
        confirmationCount: Int,
        maxConfirmations: Int
    ): Double {
        // ADX weakness: 25 = minimum (0.0), 0 = maximum (1.0)
        val adxWeakness = ((25.0 - adx) / 25.0).coerceIn(0.0, 1.0)

        // Confirmation progress
        val confirmationFactor = confirmationCount.toDouble() / maxConfirmations

        // Weighted: 60% ADX weakness, 40% confirmations
        return (0.6 * adxWeakness + 0.4 * confirmationFactor).coerceIn(0.75, 1.0)
    }

    private fun resetCounters() {
        trendConfirmCount = 0
        rangeConfirmCount = 0
    }

    // For testing/debugging
    fun getState(): String = "trend=$trendConfirmCount, range=$rangeConfirmCount"
}
```

---

## Key Implementation Details

### Hysteresis Logic

The engine uses 3-candle confirmation hysteresis to prevent rapid mode switching:

- **DEFENSE mode:** Activates instantly (no hysteresis) - safety first
- **TREND mode:** Requires ADX > 25 for 3 consecutive candles
- **RANGE mode:** Requires ADX < 25 for 3 consecutive candles
- **Counters persist** across evaluations to track confirmation progress

### Indicator Configuration

| Indicator | Period | Purpose |
|-----------|--------|---------|
| SMA | 200 | Trend direction (bull/bear) |
| ADX | 14 | Trend strength (trending/ranging) |
| ATR | 14 | Volatility for position sizing and grid spacing |

### Grid Spacing Calculation

Grid spacing uses **max(1.5%, ATR-based)** to ensure:
- Minimum 1.5% spacing covers 0.60% maker fees × 2 (buy + sell)
- ATR-based spacing adapts to market volatility
- Always uses the larger of the two values

### Risk-Reward Ratio

Trend mode uses 2:1 R:R ratio:
- Stop Loss: **-3 ATR** (tighter stops)
- Take Profit: **+6 ATR** (let winners run)

---

## Testing

### Unit Test Examples

```kotlin
@Test
fun `defense mode activates immediately when price below SMA`() {
    val engine = EngineDecisionEngine()
    val candles = generateTestCandles(count = 250)
    val currentPrice = 45000.0  // Below SMA(200) of 50000

    val decision = engine.evaluate(candles, currentPrice)

    assertTrue(decision is Decision.Defense)
}

@Test
fun `trend mode requires 3 candle confirmation`() {
    val engine = EngineDecisionEngine()
    val candles = generateTestCandles(count = 250, adx = 30.0)  // Strong trend
    val currentPrice = 55000.0  // Above SMA

    // First evaluation - Wait
    val decision1 = engine.evaluate(candles, currentPrice)
    assertTrue(decision1 is Decision.Wait)

    // Second evaluation - Wait
    val decision2 = engine.evaluate(candles, currentPrice)
    assertTrue(decision2 is Decision.Wait)

    // Third evaluation - Trend confirmed
    val decision3 = engine.evaluate(candles, currentPrice)
    assertTrue(decision3 is Decision.Trend)
}

@Test
fun `grid spacing respects 1_5 percent minimum`() {
    val engine = EngineDecisionEngine()
    val candles = generateTestCandles(count = 250, adx = 15.0)  // Ranging
    val currentPrice = 50000.0

    // After 3 confirmations...
    repeat(3) { engine.evaluate(candles, currentPrice) }

    val decision = engine.evaluate(candles, currentPrice) as Decision.Range

    assertTrue(decision.gridSpacing >= currentPrice * 0.015)
}
```

---

## Navigation

- **[Back to Technical Reference](../reference.md)** - Parent document
- **[Previous: Strategy Specification](../strategy/overview.md)** - Strategy details
- **[Next: Security & Auth](security.md)** - Credential storage and JWT
