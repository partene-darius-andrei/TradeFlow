# Ticket 15: Decision Engine - Implementation Plan

**Branch:** `claude/ticket-15-decision-engine`
**Module:** `:core:domain`
**Effort:** Large
**Estimated Time:** 4-6 hours

---

## Objective

Implement regime-switching decision engine with:
- SMA(200) for trend filter
- ADX(14) for trend strength
- ATR(14) for volatility-based positioning
- 3-candle hysteresis to prevent whipsaws

---

## Phase 1: Core Interfaces & Config

### Files to Create:

1. **`core/domain/src/main/kotlin/com/tradeflow/core/domain/strategy/DecisionEngine.kt`**
   ```kotlin
   interface DecisionEngine {
       fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision
   }
   ```

2. **`core/domain/src/main/kotlin/com/tradeflow/core/domain/strategy/StrategyConfig.kt`**
   - SMA, ADX, ATR periods
   - Thresholds (ADX > 25 = trend, ADX < 25 = range)
   - ATR multipliers for stops/targets
   - Position sizing percentages

---

## Phase 2: Indicator Calculators (ta4j wrappers)

### Files to Create:

3. **`core/domain/src/main/kotlin/com/tradeflow/core/domain/indicator/SMACalculator.kt`**
   - Wrap ta4j SMAIndicator
   - Input: List<Candle>, period
   - Output: BigDecimal

4. **`core/domain/src/main/kotlin/com/tradeflow/core/domain/indicator/ADXCalculator.kt`**
   - Wrap ta4j ADXIndicator
   - Input: List<Candle>, period
   - Output: Double (0-100 scale)

5. **`core/domain/src/main/kotlin/com/tradeflow/core/domain/indicator/ATRCalculator.kt`**
   - Wrap ta4j ATRIndicator
   - Input: List<Candle>, period
   - Output: BigDecimal (price units)

---

## Phase 3: Decision Engine Implementation

### File to Create:

6. **`core/domain/src/main/kotlin/com/tradeflow/core/domain/strategy/TradingDecisionEngine.kt`**

**Algorithm:**

```kotlin
class TradingDecisionEngine(
    private val config: StrategyConfig = StrategyConfig(),
    private val smaCalculator: SMACalculator,
    private val adxCalculator: ADXCalculator,
    private val atrCalculator: ATRCalculator
) : DecisionEngine {

    // State for hysteresis
    private var consecutiveTrendCandles = 0
    private var consecutiveRangeCandles = 0

    override fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        // Validation
        require(candles.size >= config.smaPeriod) {
            "Need ${config.smaPeriod} candles for SMA"
        }

        // Calculate indicators
        val sma200 = smaCalculator.calculate(candles, config.smaPeriod)
        val adx14 = adxCalculator.calculate(candles, config.adxPeriod)
        val atr14 = atrCalculator.calculate(candles, config.atrPeriod)

        // Rule 1: DEFENSE (instant, no hysteresis)
        if (currentPrice < sma200) {
            resetCounters()
            return Decision.Defense("Price below SMA($config.smaPeriod)")
        }

        // Rule 2: TREND (requires 3 consecutive candles with ADX > 25)
        if (adx14 > config.adxTrendThreshold) {
            consecutiveTrendCandles++
            consecutiveRangeCandles = 0

            if (consecutiveTrendCandles >= 3) {
                return createTrendDecision(currentPrice, atr14)
            }
        }

        // Rule 3: RANGE (requires 3 consecutive candles with ADX < 25)
        if (adx14 < config.adxRangeThreshold) {
            consecutiveRangeCandles++
            consecutiveTrendCandles = 0

            if (consecutiveRangeCandles >= 3) {
                return createRangeDecision(currentPrice, atr14)
            }
        }

        // Rule 4: WAIT (hysteresis in progress or ADX in dead zone)
        return Decision.Wait("Waiting for confirmation")
    }

    private fun resetCounters() {
        consecutiveTrendCandles = 0
        consecutiveRangeCandles = 0
    }

    private fun createTrendDecision(price: BigDecimal, atr: BigDecimal): Decision.Trend {
        val stopLoss = price - (atr * config.stopLossAtrMultiplier)
        val takeProfit = price + (atr * config.takeProfitAtrMultiplier)

        return Decision.Trend(
            direction = OrderSide.BUY,
            entryPrice = price,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            positionSize = config.trendPositionPercent
        )
    }

    private fun createRangeDecision(price: BigDecimal, atr: BigDecimal): Decision.Range {
        val spacing = maxOf(
            price * config.minGridSpacing,  // 1.5% minimum
            atr                              // Or 1 ATR, whichever is larger
        )

        return Decision.Range(
            gridSpacing = spacing,
            levels = 5,
            positionSizePerLevel = config.gridPositionPercentPerLevel
        )
    }
}
```

---

## Phase 4: Unit Tests

### File to Create:

7. **`core/domain/src/test/kotlin/com/tradeflow/core/domain/strategy/TradingDecisionEngineTest.kt`**

**Test Cases:**

```kotlin
@Test
fun `DEFENSE - price below SMA200 returns Defense decision instantly`()

@Test
fun `DEFENSE - resets hysteresis counters when entering defense`()

@Test
fun `TREND - requires 3 consecutive candles with ADX above 25`()

@Test
fun `TREND - counter resets if ADX drops below threshold`()

@Test
fun `TREND - calculates stop loss and take profit using ATR multiples`()

@Test
fun `RANGE - requires 3 consecutive candles with ADX below 25`()

@Test
fun `RANGE - grid spacing is max of 1_5% or ATR`()

@Test
fun `RANGE - counter resets if ADX rises above threshold`()

@Test
fun `WAIT - returns wait during hysteresis confirmation period`()

@Test
fun `validates minimum 200 candles required`()
```

**Mock Data:**
- Create helper functions to generate candle lists
- Test with known SMA/ADX/ATR values

---

## Phase 5: Build & Verify

1. Run local build: `./gradlew :core:domain:build`
2. Verify no Android dependencies
3. Check unit test coverage (aim for 100%)
4. Ensure no compilation errors

---

## Acceptance Criteria Checklist

- [ ] DecisionEngine interface created
- [ ] StrategyConfig with all parameters
- [ ] SMACalculator using ta4j
- [ ] ADXCalculator using ta4j
- [ ] ATRCalculator using ta4j
- [ ] TradingDecisionEngine with hysteresis logic
- [ ] All 4 decision types correctly returned
- [ ] 3-candle hysteresis works for TREND/RANGE
- [ ] DEFENSE mode has no hysteresis (instant)
- [ ] Grid spacing >= 1.5% enforced
- [ ] Unit tests with 100% coverage
- [ ] No Android dependencies (pure Kotlin)
- [ ] Build passes locally

---

## File Structure

```
core/domain/src/main/kotlin/com/tradeflow/core/domain/
├── strategy/
│   ├── DecisionEngine.kt          [NEW]
│   ├── TradingDecisionEngine.kt   [NEW]
│   └── StrategyConfig.kt          [NEW]
└── indicator/
    ├── SMACalculator.kt           [NEW]
    ├── ADXCalculator.kt           [NEW]
    └── ATRCalculator.kt           [NEW]

core/domain/src/test/kotlin/com/tradeflow/core/domain/
└── strategy/
    └── TradingDecisionEngineTest.kt [NEW]
```

---

## Dependencies

**Already in project:**
- ✅ ta4j-core 0.16 (in libs.versions.toml)
- ✅ Domain models (Candle, Decision, OrderSide)

**To verify:**
- Check ta4j is added to :core:domain module

---

## Implementation Order

1. StrategyConfig (simple data class)
2. Indicator calculators (ta4j wrappers)
3. DecisionEngine interface
4. TradingDecisionEngine (main logic)
5. Unit tests
6. Build verification

---

## Notes

- Keep code simple and readable
- No premature optimization
- Focus on correctness first
- Pure Kotlin - no Android imports
- Use BigDecimal for prices (precision)
- Use Double for ADX (0-100 scale is fine)

---

## Code Review Preparation

After implementation:
1. Self-review all code
2. Run all tests
3. Check test coverage report
4. Build passes
5. Create PR with:
   - Clear description
   - Test results
   - Coverage report
   - Screenshots if applicable
