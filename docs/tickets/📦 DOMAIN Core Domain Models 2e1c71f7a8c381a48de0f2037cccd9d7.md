# 📦 DOMAIN: Core Domain Models

Effort level: Small
Priority: High
Status: Not started
Blocked by: MODULE: Project Modularization Setup
Module: :core:domain

## Objective

Define exchange-agnostic domain models in pure Kotlin.

## Module

`:core:domain` (NO Android dependencies)

## Models

### Market Data

```kotlin
data class Candle(
    val timestamp: Instant,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal
)

enum class Granularity {
    ONE_MINUTE, FIVE_MINUTE, FIFTEEN_MINUTE,
    THIRTY_MINUTE, ONE_HOUR, TWO_HOUR,
    FOUR_HOUR, SIX_HOUR, ONE_DAY
}

data class Ticker(
    val productId: String,
    val price: BigDecimal,
    val bid: BigDecimal,
    val ask: BigDecimal,
    val volume24h: BigDecimal,
    val timestamp: Instant
)
```

### Account

```kotlin
data class Balance(
    val currency: String,
    val available: BigDecimal,
    val hold: BigDecimal
) {
    val total: BigDecimal get() = available + hold
}

data class Portfolio(
    val balances: List<Balance>,
    val totalEquityUsd: BigDecimal,
    val timestamp: Instant
)
```

### Orders

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
```

### Strategy

```kotlin
sealed class Decision {
    data class Wait(val reason: String) : Decision()
    data class Defense(val reason: String) : Decision()
    data class Trend(
        val direction: OrderSide,
        val entryPrice: BigDecimal,
        val stopLoss: BigDecimal,
        val takeProfit: BigDecimal,
        val positionSize: BigDecimal
    ) : Decision()
    data class Range(
        val gridSpacing: BigDecimal,
        val levels: Int,
        val positionSizePerLevel: BigDecimal
    ) : Decision()
}
```

## File Structure

```
core/domain/src/main/kotlin/com/tradeflow/core/domain/
├── model/
│   ├── Candle.kt
│   ├── Ticker.kt
│   ├── Balance.kt
│   ├── Portfolio.kt
│   ├── Order.kt
│   └── Decision.kt
└── extension/
    └── BigDecimalExt.kt
```

## Acceptance Criteria

- [ ]  All models are data classes with `BigDecimal` for money
- [ ]  No Android imports in this module
- [ ]  Unit tests for any extension functions