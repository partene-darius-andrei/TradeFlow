# TradeFlow Trading Strategy & Architecture

**Parent:** [../reference.md](../reference.md)

Strategy specification and Android architecture for TradeFlow.

---

## Strategy Specification (Phase 1)

### 🚨 CRITICAL WARNING - Known Bug
**Issue:** TradingDecisionEngine ALWAYS goes LONG in Trend mode (hardcoded `OrderSide.BUY`).
- ADX measures trend *strength*, not *direction*.
- Current code will open LONG positions even in strong downtrends.
- This WILL cause losses in bear markets.

**Fix Required:** Add trend direction detection:
- **Option 1:** Use SMA slope (is price rising above SMA200?)
- **Option 2:** Add +DI/-DI from ADX calculation
- **Option 3:** Compare recent candle closes (bullish = close > open)

**Status:** Bug documented in CODE_REVIEW_DEEP_ANALYSIS.md (#5)
**Priority:** MUST FIX before live trading

---

### What's IN Phase 1

| Component | Implementation |
|-----------|----------------|
| Trend detection | SMA(200) on H4 candles |
| Trend strength | ADX(14) on H4 candles |
| Volatility sizing | ATR(14) on H4 candles |
| Mode switching | 3-candle hysteresis (except DEFENSE) |
| Position sizing | **Confidence-based scaling** (see below) |
| Grid spacing | max(1.5%, ATR-based) |
| Risk limit | 15% drawdown kills service |

### What's OUT of Phase 1

- ❌ Fear & Greed Index (add in Phase 2 if needed)
- ❌ RSI entry timing
- ❌ Multiple timeframes
- ❌ ML regime prediction
- ❌ On-chain metrics

---

## Decision Logic

```
Every H4 candle (4 hours):

┌─────────────────────────────────────────────┐
│ Is price BELOW SMA(200)?                    │
└─────────────────────────────────────────────┘
        │ YES                    │ NO
        ▼                        ▼
┌───────────────┐    ┌─────────────────────────────────┐
│ DEFENSE MODE  │    │ Has ADX been > 25 for 3 candles?│
│ (Instant)     │    └─────────────────────────────────┘
│ • Cancel buys │           │ YES              │ NO
│ • Set stops   │           ▼                  ▼
│ • Hold cash   │    ┌─────────────┐    ┌─────────────────────────────────┐
└───────────────┘    │ TREND MODE  │    │ Has ADX been < 25 for 3 candles?│
                     │ • Bracket   │    └─────────────────────────────────┘
                     │   order     │           │ YES              │ NO
                     │ • TP: +6ATR │           ▼                  ▼
                     │ • SL: -3ATR │    ┌─────────────┐    ┌───────────┐
                     └─────────────┘    │ RANGE MODE  │    │ WAIT      │
                                        │ • Grid buys │    │ (Keep     │
                                        │ • 1.5% min  │    │  current) │
                                        │ • post_only │    └───────────┘
                                        └─────────────┘
```

**See:** [../implementation/domain.md](../implementation/domain.md) for EngineDecisionEngine implementation

---

## Position Sizing & Risk Management

**CRITICAL DISTINCTION:**
- **Position Size** = Amount of capital allocated to the trade (% of portfolio in the trade)
- **Risk Per Trade** = Maximum loss allowed via stop-loss (% of portfolio at risk)

### Phase 1: Fixed Position Sizing (CURRENT)

**Simple, proven approach for initial deployment:**

```kotlin
val positionSize = 0.10  // 10% of portfolio in the trade
val riskPerTrade = 0.02  // 2% max loss via stop-loss

// Example: $500 account
// Position: $50 (10% of $500)
// Stop-loss distance: $10 max loss (2% of $500)
// Stop price: Entry - $10 (for $50 position)
```

**Why this works:**
- Position size (10%) gives meaningful exposure
- Risk per trade (2%) limits damage from bad trades
- 50 consecutive losses needed to blow up account (mathematically impossible)
- Aligns with bitcoin-first-strategy.md ($5-10 risk on $500 account)

### Phase 2: Confidence-Based Sizing (FUTURE - After Validation)

**Only enable after 50+ trades prove confidence scoring is accurate.**

```kotlin
val baseSize = 0.08   // 8% base position
val maxSize = 0.12    // 12% max position
val fixedRisk = 0.02  // Always 2% risk (never scales)
val minConfidence = 0.75

// Position size scales with confidence
val positionSize = when {
    confidence < minConfidence -> 0.0  // Don't trade
    else -> baseSize + (maxSize - baseSize) * ((confidence - minConfidence) / (1.0 - minConfidence))
}

// Risk NEVER scales (always 2%)
val riskPerTrade = 0.02
```

**Examples:**

| Confidence | Position Size | Risk Per Trade | Stop Distance | Notes |
|-----------|---------------|----------------|---------------|-------|
| 0.70 | 0% | 0% | N/A | Below threshold |
| 0.75 | 8.0% | 2.0% | 25% of position | Base size |
| 0.85 | 9.6% | 2.0% | 20.8% of position | Medium conviction |
| 0.95 | 11.2% | 2.0% | 17.9% of position | High conviction |
| 1.00 | 12.0% | 2.0% | 16.7% of position | Max conviction |

**Critical Assumption:**

Confidence-based sizing ONLY works if confidence scores correlate with win rates.

**Validation Required (Before Phase 2):**
- Backtest: Do 0.95 confidence trades win more than 0.75 trades?
- Live tracking: Confidence vs. actual win rate correlation
- Statistical test: Chi-square test for independence
- **If no correlation:** Stay with Phase 1 fixed sizing indefinitely

---

## Risk Limits (Hardcoded)

| Limit | Value | Enforcement |
|-------|-------|-------------|
| Risk per trade | 2% portfolio | Fixed - stop-loss distance calculated to limit loss |
| Max position/trade (Phase 1) | 10% portfolio | Fixed for initial deployment |
| Max position/trade (Phase 2) | 12% portfolio | Only after confidence validation |
| Max total exposure | 10% portfolio | No new orders if exceeded |
| Max correlated assets | 1 (BTC only initially) | Block second asset until $2,500+ account |
| Portfolio drawdown | 15% from HWM | Emergency liquidate + stop service |
| Daily loss limit | 5% portfolio | Stop trading for 24 hours |
| Unfilled order timeout | 48 hours | Cancel and re-evaluate |

---

## Android Architecture

### Tech Stack

| Component | Library | Rationale |
|-----------|---------|-----------|
| HTTP/WebSocket | **Ktor 3.x** | Already configured, good Kotlin integration |
| JSON | kotlinx.serialization | Type-safe, fast |
| Database | Room | SQLite with compile-time checks |
| Background | Foreground Service | Required for 24/7 |
| Security | EncryptedSharedPreferences | API key storage |
| Indicators | ta4j | Battle-tested TA library |
| JWT | nimbus-jose-jwt | ES256 signing |
| Scheduler | WorkManager | Dead-man-switch backup |

### Why Ktor for TradeFlow

**Decision: Use Ktor 3.x** (already configured in project)

**Rationale:**
- Already integrated with dependencies configured
- Better Kotlin idioms and coroutine integration
- Type-safe builders for HTTP requests
- Unified client for both REST and WebSocket
- Good enough battery performance for intermittent use

**Note:** While OkHttp has better battery life for 24/7 WebSocket connections, TradeFlow uses:
- Periodic strategy evaluation (every 15min) NOT continuous streaming
- Short-lived WebSocket sessions for order updates
- REST API for most operations (candles, orders, accounts)

For this use case, Ktor's developer experience outweighs OkHttp's battery advantage.

---

## Doze Mode Survival Strategy

| Layer | Tool | Purpose |
|-------|------|---------|
| Primary | Battery optimization exemption | Prevent OS from killing |
| WebSocket | Ktor WebSocket (heartbeat subscription) | Keep connection alive |
| Internal | Coroutine watchdog (45s) | Detect dead WebSocket |
| Backup | WorkManager (15-min periodic) | Restart service if killed |

**Critical for Xiaomi/Huawei/Samsung:** These vendors have aggressive battery optimization. Users MUST manually disable it for the app.

---

## Project Structure

```
com.dpart.tradeflow/
├── data/
│   ├── local/
│   │   ├── EngineDatabase.kt
│   │   ├── dao/OrderDao.kt
│   │   └── entity/
│   │       ├── OrderEntity.kt
│   │       └── PortfolioEntity.kt
│   ├── remote/
│   │   ├── CoinbaseRestApi.kt
│   │   ├── CoinbaseWebSocket.kt
│   │   └── dto/
│   │       ├── OrderRequest.kt
│   │       └── OrderResponse.kt
│   └── security/
│       ├── SecureKeyStore.kt
│       └── JwtGenerator.kt
├── domain/
│   ├── model/
│   │   ├── Candle.kt
│   │   └── Decision.kt
│   └── strategy/
│       └── EngineDecisionEngine.kt
├── service/
│   ├── TradingService.kt
│   └── ServiceWatchdog.kt
└── ui/
    ├── MainActivity.kt
    └── screens/
```

---

## Implementation Notes

**Technology Update:**

The code examples in implementation/*.md were originally written using OkHttp for HTTP/WebSocket communication. The TradeFlow project now uses **Ktor 3.x** instead (already configured in dependencies).

**What this means:**
- REST API code: Adapt OkHttp syntax to Ktor HttpClient
- WebSocket code: Adapt OkHttp WebSocket to Ktor WebSocket client
- Trading Service: Use Ktor clients instead of OkHttp

**Package names:** All examples use `com.dpart.tradeflow.*`

Treat these examples as **reference implementations** showing the logic patterns and API integration - adapt to Ktor syntax when implementing.

---

## Navigation

- **[Back to Technical Reference](../reference.md)** - Parent document
- **[Previous: Coinbase API](../api/coinbase.md)** - API reference
- **[Next: Core Domain](../implementation/domain.md)** - Domain models and engine
