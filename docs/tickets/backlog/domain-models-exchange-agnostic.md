# 📦 DOMAIN - Models (Exchange-Agnostic)

Effort level: Small
Priority: High

## Objective

Define all domain models that are **completely exchange-agnostic**. These are used by domain logic and UI.

## Files

```
domain/model/
├── Candle.kt
├── Order.kt
├── Account.kt
├── Decision.kt
└── Product.kt
```

## Model Definitions

### Candle.kt

```kotlin
data class Candle(
    val timestamp: Instant,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal
)

enum class CandleGranularity {
    ONE_MINUTE,
    FIVE_MINUTE,
    FIFTEEN_MINUTE,
    THIRTY_MINUTE,
    ONE_HOUR,
    TWO_HOUR,
    FOUR_HOUR,  // Aggregated from TWO_HOUR
    SIX_HOUR,
    ONE_DAY
}
```

### Order.kt

```kotlin
data class Order(
    val id: String,
    val clientOrderId: String,
    val productId: String,
    val side: OrderSide,
    val type: OrderType,
    val status: OrderStatus,
    val size: BigDecimal,
    val price: BigDecimal?,
    val filledSize: BigDecimal,
    val avgFilledPrice: BigDecimal?,
    val createdAt: Instant
)

enum class OrderSide { BUY, SELL }
enum class OrderType { MARKET, LIMIT, BRACKET }
enum class OrderStatus { PENDING, OPEN, FILLED, CANCELLED, FAILED }

data class OrderRequest(
    val productId: String,
    val side: OrderSide,
    val type: OrderType,
    val size: BigDecimal,
    val price: BigDecimal? = null,
    val clientOrderId: String = UUID.randomUUID().toString()
)
```

### Account.kt

```kotlin
data class Account(
    val id: String,
    val currency: String,
    val available: BigDecimal,
    val hold: BigDecimal
) {
    val total: BigDecimal get() = available + hold
}
```

### Decision.kt

```kotlin
sealed class Decision {
    data class Wait(val reason: String) : Decision()
    data class Defense(val reason: String) : Decision()
    data class Trend(
        val direction: TrendDirection,
        val entryPrice: BigDecimal,
        val takeProfitPrice: BigDecimal,
        val stopLossPrice: BigDecimal,
        val positionSize: BigDecimal
    ) : Decision()
    data class Range(
        val gridSpacing: BigDecimal,
        val gridLevels: Int,
        val positionSizePerLevel: BigDecimal
    ) : Decision()
}

enum class TrendDirection { LONG, SHORT }
```

## Critical Rule

**NO Coinbase/Kraken/Binance imports allowed in this package.**

## Acceptance Criteria

- [ ]  All models use BigDecimal for money (not Double)
- [ ]  All timestamps use java.time.Instant
- [ ]  No exchange-specific fields
- [ ]  Serializable for Room storage