# TradeFlow Trading Strategy & Architecture

**Parent:** [../reference.md](../reference.md)

Strategy specification and Android architecture for TradeFlow.

---

## Strategy Specification (Phase 1)

### What's IN Phase 1

| Component | Implementation |
|-----------|----------------|
| Trend detection | SMA(200) on H4 candles |
| Trend strength | ADX(14) on H4 candles |
| Volatility sizing | ATR(14) on H4 candles |
| Mode switching | 3-candle hysteresis (except DEFENSE) |
| Position sizing | Fixed 5% per trade |
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

## Risk Limits (Hardcoded)

| Limit | Value | Enforcement |
|-------|-------|-------------|
| Max position/trade | 5% portfolio | Reject if exceeded |
| Max total exposure | 10% portfolio | No new orders |
| Max correlated assets | 1 | Block second asset |
| Portfolio drawdown | 15% from HWM | Emergency liquidate + stop |
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
