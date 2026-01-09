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

### 3. DecisionEngine (The "Strategy")
Stateless engine that converts indicators into a `Decision` (Wait, Defense, Trend, Range).

---

## 🚦 Current Status

### ✅ Streamlined Domain Layer (v2.0)
- **Merged Use Cases:** 7 small use cases collapsed into `TradeOrchestrator`.
- **Stateless Engine:** Removed hysteresis state from the decision logic for predictability.
- **Rich Models:** `Portfolio` and other models now encapsulate their own utility logic.
- **Unified Models:** All domain-level interfaces and errors moved to `com.tradeflow.core.domain.model`.

### 🛠️ Coinbase Integration (Next Up)
- **Ticket 13:** Full REST API (Order placement, candles).
- **Ticket 14:** WebSocket client for real-time updates.

---

## 🔄 Development Workflow

1. **Verify Build:** `gradlew assembleDebug`
2. **Implement API:** Extend `CoinbaseApiClient` and `CoinbaseRepository`.
3. **Test:** Use simulated exchange for strategy validation.
