# 📦 Domain Models - Candle & Decision

Effort level: Small
Priority: High

## Objective

Create core domain models used throughout the app.

## Files to Create

- `domain/model/Candle.kt`
- `domain/model/Decision.kt`

## Candle.kt

```kotlin
data class Candle(
    val timestamp: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)
```

## Decision.kt (Sealed Class)

```kotlin
sealed class Decision {
    data class Wait(val reason: String) : Decision()
    data class Defense(val reason: String) : Decision()
    data class Trend(
        val stopLossPrice: Double,
        val takeProfitPrice: Double,
        val atr: Double
    ) : Decision()
    data class Range(
        val gridSpacing: Double,
        val atr: Double
    ) : Decision()
}
```

## Acceptance Criteria

- Models compile without errors
- Decision sealed class covers all 4 modes
- Candle uses java.time.Instant for timestamps