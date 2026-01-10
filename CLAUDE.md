# TradeFlow - Core Trading Logic (Streamlined v2.0)

**Last Updated:** 2026-01-09
**Project Status:** Phase 2 Complete - Refactored for Simplicity
**Current Build:** SUCCESS

## 🎯 Architecture (Simplified)

TradeFlow follows a streamlined Clean Architecture to avoid "AI Slop" and redundant boilerplate.

| Layer | Responsibility | Key Component |
|-------|----------------|---------------|
| **App** | Presentation & DI | `DashboardViewModel`, `AppNavHost` |
| **Domain** | Pure Logic & Models | `TradeOrchestrator`, `TechnicalAnalysisService`, `DecisionEngine` |
| **Data** | Persistence & Remote | `CoinbaseRepository`, `EngineDatabase`, `StaticCredentialStore` |

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

### ✅ Streamlined Domain Layer (v2.0)
- **Simplified Use Cases:** 2 core use cases: `TradeOrchestrator` (orchestration) + `UpdatePortfolioUseCase` (data aggregation).
- **Stateful Decision Engine:** `TradingDecisionEngine` uses 3-candle hysteresis to prevent mode-switching noise.
- **Complete Risk Management:** `RiskManager` fully implemented with position sizing, drawdown monitoring, and validation.
- **Rich Models:** `Portfolio` and other models now encapsulate their own utility logic.
- **Unified Models:** All domain-level interfaces and errors moved to `com.tradeflow.core.domain.model`.

### 🛠️ Coinbase Integration (Next Up)
- **Ticket 13:** Full REST API (Order placement, candles) - Auth works, remaining endpoints TODO.
- **Ticket 14:** WebSocket client for real-time updates.
- ~~**Ticket 16:** Risk Manager~~ - ✅ COMPLETE (fully implemented with 22 unit tests)

---

## 🔄 Development Workflow

1. **Verify Build:** `gradlew assembleDebug`
2. **Implement API:** Extend `CoinbaseApiClient` and `CoinbaseRepository`.
3. **Test:** Use simulated exchange for strategy validation.
