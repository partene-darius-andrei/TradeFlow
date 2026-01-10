# TradeFlow - Core Trading Logic (Standalone v3.0)

**Last Updated:** 2026-01-10
**Project Status:** Phase 3 - Standalone JVM Architecture
**Current Build:** SUCCESS

## 🎯 Architecture (Standalone JVM)

TradeFlow has transitioned from Android app to standalone JVM application for reliability and deployment simplicity.

| Layer | Responsibility | Key Component |
|-------|----------------|---------------|
| **Standalone** | Main Loop & Execution | `Main.kt`, JWT Auth, HTTP Client |
| **Domain** | Pure Logic & Models | `TradeOrchestrator`, `TechnicalAnalysisService`, `DecisionEngine` |
| **Data** | Persistence & Remote | `CoinbaseRepository`, `EngineDatabase`, `StaticCredentialStore` |

### Why Standalone?
- **Reliability:** No Android lifecycle interruptions (screen off, battery optimization, app kills)
- **Simplicity:** Pure Kotlin JVM - easier to deploy, debug, and run on servers
- **Cloud-Ready:** Can run on any laptop, VPS, or cloud instance
- **95% Code Reuse:** All domain logic remains unchanged

---

## 🏗️ Core Components

### 1. TradeOrchestrator (The "Brain")
Single entry point for the trading cycle. It handles:
- Fetching fresh market data and portfolio state.
- Risk management (Drawdown circuit breaker).
- Triggering the decision engine and executing orders.

### 2. TechnicalAnalysisService (The "Eyes")
Unified service for technical indicators.
- Calculates SMA, ADX, and ATR in a **single pass** over candles.
- Uses `ta4j` internally but exposes clean `BigDecimal` results.

### 3. TradingDecisionEngine (The "Strategy")
**Stateful** engine with 3-candle hysteresis to prevent whipsaw mode switching.
- Converts indicators into a `Decision` (Wait, Defense, Trend, Range).
- Maintains internal state: `lastMode`, `confirmationCount`, `candidateMode`.
- Location: `core/domain/src/main/kotlin/com/tradeflow/core/domain/strategy/TradingDecisionEngine.kt`

---

## 🚦 Current Status

### ✅ Standalone JVM Application (v3.0)
- **Module:** `:standalone` - Self-contained Kotlin/JVM application
- **Authentication:** JWT generation using ES256 algorithm (ECDSA signing)
- **HTTP Client:** Ktor with OkHttp engine for Coinbase API
- **Credential Loading:** From `local.properties` or environment variables
- **Proof-of-Concept:** Successfully fetches Coinbase account balances
- **Next Step:** Expand to full trading loop (add domain logic integration)

### ✅ Streamlined Domain Layer (v2.0)
- **Simplified Use Cases:** 2 core use cases: `TradeOrchestrator` (orchestration) + `UpdatePortfolioUseCase` (data aggregation).
- **Stateful Decision Engine:** `TradingDecisionEngine` uses 3-candle hysteresis to prevent mode-switching noise.
- **Complete Risk Management:** `RiskManager` fully implemented with position sizing, drawdown monitoring, and validation.
- **Rich Models:** `Portfolio` and other models now encapsulate their own utility logic.
- **Unified Models:** All domain-level interfaces and errors moved to `com.tradeflow.core.domain.model`.

### 🛠️ Coinbase Integration (In Progress)
- ~~**Ticket 13:** Full REST API~~ - ✅ PARTIAL (Auth + balance fetching working in standalone)
- **Ticket 13:** Remaining endpoints (order placement, candles) - TODO
- **Ticket 14:** WebSocket client for real-time updates - TODO
- ~~**Ticket 16:** Risk Manager~~ - ✅ COMPLETE (fully implemented with 22 unit tests)

---

## 🔄 Development Workflow

### Running the Standalone App
```bash
./gradlew :standalone:run
```

This executes `Main.kt` which:
1. Loads credentials from `local.properties`
2. Generates JWT token for Coinbase API authentication
3. Fetches account balances and displays results

### Local Development
1. **Verify Build:** `./gradlew :standalone:build`
2. **Run Application:** `./gradlew :standalone:run`
3. **Add Features:** Extend `Main.kt` to include trading logic
4. **Test:** Use simulated exchange for strategy validation

### Credential Setup
Create `local.properties` in project root:
```properties
coinbase.api.key=your_coinbase_api_key_here
coinbase.api.secret=-----BEGIN EC PRIVATE KEY-----\n...\n-----END EC PRIVATE KEY-----
```

Or set environment variables:
```bash
export COINBASE_API_KEY="your_key"
export COINBASE_API_SECRET="your_secret"
```
