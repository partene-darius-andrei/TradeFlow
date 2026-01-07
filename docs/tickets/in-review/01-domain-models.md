# 📦 DOMAIN: Core Domain Models

Effort level: Small
Priority: High
Status: ✅ COMPLETE
Completed: 2026-01-07
PR: #6
Blocked by: Ticket 00 (Project Modularization Setup)
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

- [x]  All models are data classes with `BigDecimal` for money
- [x]  No Android imports in this module
- [ ]  Unit tests for any extension functions (none added yet)

---

## Post-Implementation Notes

**Completed:** 2026-01-07
**PR:** https://github.com/partene-darius-andrei/TradeFlow/pull/6

### Implementation Summary

All 6 domain model files created successfully in `:core:domain/model/` package. Pure Kotlin/JVM with zero Android dependencies.

### Files Created

1. **Candle.kt** - OHLCV candlestick data + Granularity enum with seconds field
2. **Ticker.kt** - Real-time price data (productId, price, bid, ask, volume, timestamp)
3. **Balance.kt** - Currency balance with computed `total` property
4. **Portfolio.kt** - Collection of balances + total equity USD
5. **Order.kt** - Order model + 3 enums (OrderSide, OrderType, OrderStatus)
6. **Decision.kt** - Sealed class for trading decisions (Wait, Defense, Trend, Range)

### Key Decisions

**Granularity Enum Enhancement:**
- Added `seconds: Long` field to each enum value
- Makes timeframe conversions easier (e.g., for ta4j or API params)
- Example: `Granularity.FIVE_MINUTE.seconds == 300`

**Money Precision:**
- All prices/amounts use `BigDecimal` (not Double/Float)
- Prevents floating-point errors in financial calculations
- Critical for order placement and P&L tracking

**Timestamp Format:**
- Using `java.time.Instant` (not Long epoch millis)
- Type-safe and easier to work with
- Coinbase DTOs will map epoch strings → Instant

### Build Verification

✅ `:core:domain:build` - SUCCESS
✅ `assembleDebug` - SUCCESS (full app)
✅ Zero Android dependencies confirmed

### Next Steps

With domain models complete:
- Ticket 02: Repository Interfaces (UNBLOCKED - can start immediately)
- Ticket 04: Credential Store
- Ticket 07: JWT Generator (needs these models for signing)