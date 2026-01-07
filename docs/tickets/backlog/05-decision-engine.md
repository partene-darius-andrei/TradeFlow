# 🧠 DOMAIN: Decision Engine

Effort level: Large
Priority: High
Blocked by: DOMAIN: Core Domain Models
Module: :core:domain

## Objective

Implement the regime-switching decision engine (pure domain logic, no exchange dependencies).

## Module

`:core:domain` (NO Android dependencies)

## Interface

```kotlin
interface DecisionEngine {
    fun evaluate(
        candles: List<Candle>,
        currentPrice: BigDecimal
    ): Decision
}
```

## Implementation

```kotlin
class TradingDecisionEngine(
    private val config: StrategyConfig = StrategyConfig()
) : DecisionEngine {
    
    private var consecutiveTrendCandles = 0
    private var consecutiveRangeCandles = 0
    
    override fun evaluate(
        candles: List<Candle>,
        currentPrice: BigDecimal
    ): Decision {
        require(candles.size >= 200) { "Need 200+ candles for SMA" }
        
        val sma200 = calculateSMA(candles, 200)
        val adx14 = calculateADX(candles, 14)
        val atr14 = calculateATR(candles, 14)
        
        // Rule 1: Defense (instant, no hysteresis)
        if (currentPrice < sma200) {
            resetCounters()
            return Decision.Defense("Price below SMA200")
        }
        
        // Rule 2: Trend (3 candles hysteresis)
        if (adx14 > config.adxTrendThreshold) {
            consecutiveTrendCandles++
            consecutiveRangeCandles = 0
            if (consecutiveTrendCandles >= 3) {
                return Decision.Trend(
                    direction = [OrderSide.BUY](http://OrderSide.BUY),
                    entryPrice = currentPrice,
                    stopLoss = currentPrice - (atr14 * config.stopLossAtrMultiplier),
                    takeProfit = currentPrice + (atr14 * config.takeProfitAtrMultiplier),
                    positionSize = calculateTrendSize(currentPrice)
                )
            }
        }
        
        // Rule 3: Range (3 candles hysteresis)
        if (adx14 < config.adxRangeThreshold) {
            consecutiveRangeCandles++
            consecutiveTrendCandles = 0
            if (consecutiveRangeCandles >= 3) {
                val spacing = maxOf(
                    currentPrice * config.minGridSpacing,
                    atr14
                )
                return Decision.Range(
                    gridSpacing = spacing,
                    levels = 5,
                    positionSizePerLevel = calculateGridSize(currentPrice)
                )
            }
        }
        
        return Decision.Wait("Waiting for confirmation")
    }
}
```

## Strategy Config

```kotlin
data class StrategyConfig(
    val smaPeriod: Int = 200,
    val adxPeriod: Int = 14,
    val atrPeriod: Int = 14,
    val adxTrendThreshold: Double = 25.0,
    val adxRangeThreshold: Double = 25.0,
    val stopLossAtrMultiplier: BigDecimal = 3.toBigDecimal(),
    val takeProfitAtrMultiplier: BigDecimal = 6.toBigDecimal(),
    val minGridSpacing: BigDecimal = "0.015".toBigDecimal(),  // 1.5%
    val trendPositionPercent: BigDecimal = "0.05".toBigDecimal(),  // 5%
    val gridPositionPercentPerLevel: BigDecimal = "0.02".toBigDecimal()  // 2%
)
```

## Indicator Calculations

Use **ta4j-core** library for indicators:

```kotlin
private fun calculateSMA(candles: List<Candle>, period: Int): BigDecimal {
    val series = BaseBarSeriesBuilder().build()
    candles.forEach { series.addBar(...) }
    return SMAIndicator(ClosePriceIndicator(series), period)
        .getValue(series.endIndex)
        .bigDecimalValue()
}
```

## File Structure

```
core/domain/src/main/kotlin/com/tradeflow/core/domain/
├── strategy/
│   ├── DecisionEngine.kt  (interface)
│   ├── TradingDecisionEngine.kt
│   └── StrategyConfig.kt
└── indicator/
    ├── SMACalculator.kt
    ├── ADXCalculator.kt
    └── ATRCalculator.kt
```

## Acceptance Criteria

- [ ]  Correctly identifies all 4 modes
- [ ]  Hysteresis prevents whipsawing
- [ ]  Grid spacing never below 1.5%
- [ ]  100% unit test coverage with mock candles
- [ ]  No Android dependencies