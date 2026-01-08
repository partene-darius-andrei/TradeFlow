# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-08
**Project Status:** Phase 1 Ready to Start - Business Logic
**Current Build:** #30 (SUCCESS)
**Architecture:** Multi-module app (8 modules: app, core:domain, core:data, core:ui, exchange:coinbase, feature:dashboard, feature:trading, feature:settings)

---

## Project Vision

**Personal automated crypto trading bot for Coinbase Advanced Trade API.**

### Goals
- Remove human emotions from trading decisions
- Run 24/7 unattended on physical Android device
- Simple UI, simple implementation, easy to maintain
- Backtest → Paper trade → Live (small amounts) → Scale

### Reality Constraints
- Fees matter: ~0.25-0.5% per trade on Coinbase Advanced Trade
- Most retail algo traders lose money - respect this fact
- Simple strategies often beat complex ML approaches
- Every trade is a taxable event (complete records required)

### Realistic Performance Expectations

**Starting Capital:** $500 (treat as education, not investment)

**Phase 1 Strategy:**
- **Trade BTC/USDT exclusively** (altcoins have 3-15% round-trip costs at this capital level)
- **Risk 1-2% per trade** ($5-10 max risk per position)
- **Target 3-5% monthly returns** (requires exceptional skill and discipline)
- **Win rate: 52-58%** realistic ceiling (not 70-98% like arbitrage bots)

**Timeline to Meaningful Income:**
```
Year 1:  $500 → $580-600   (Break even = success, learning phase)
Year 2:  $600 → $900       (Consistent 3-5% monthly)
Year 3:  $900 → $1,600     (Edge confirmed, scaling)
Year 5:  $3,000+           (Compound growth)
Year 10: $10,000-20,000    (Passive income: $500-1,000/month)
```

**Hard Truth:** Only 1-3% of day traders achieve consistent profitability. Expect 6-12 months of learning before positive results.

**See:** [docs/strategy/bitcoin-first-strategy.md](strategy/bitcoin-first-strategy.md) for complete analysis and math.

---

## Current State Analysis

### ✅ What Exists (Jan 2026)

**Codebase:**
```
app/src/main/java/com/dpart/tradeflow/
├── MainActivity.kt              ✅ Simplified (no auth check)
├── TradeFlowApp.kt              ✅ Initializes Timber logging + Hilt
├── di/
│   ├── AppModule.kt             ✅ Empty Hilt module
│   ├── DatabaseModule.kt        ✅ Provides Room database (empty)
│   ├── NetworkModule.kt         ✅ Provides Ktor HttpClient (OkHttp engine)
│   └── CredentialsModule.kt     🆕 ✅ Provides build-injected credentials
├── data/local/
│   ├── AppDatabase.kt           ✅ Empty Room DB
│   └── PlaceholderEntity.kt     ✅ Dummy entity
├── navigation/
│   ├── AppNavHost.kt            🆕 ✅ Simplified navigation (no login)
│   └── Screen.kt                🆕 ✅ Dashboard + Settings routes only
└── build.gradle.kts             🆕 ✅ Credential injection system

🆕 COMPLETE: Domain Layer Foundation
└── core/domain/                 ✅ Complete domain layer
    ├── auth/
    │   ├── AuthTokenProvider.kt ✅ Token generation interface
    │   └── CredentialStore.kt   ✅ Secure storage interface
    ├── error/
    │   └── ExchangeError.kt     ✅ Exchange error types (6 variants)
    ├── model/                   🆕 ✅ Domain models (Ticket 01)
    │   ├── Candle.kt            ✅ OHLCV data with granularity enums
    │   ├── Order.kt             ✅ Order types, sides, status
    │   ├── Decision.kt          ✅ Wait, Defense, Trend, Range decisions
    │   ├── Portfolio.kt         ✅ Account balances
    │   ├── Balance.kt           ✅ Currency holdings
    │   └── Ticker.kt            ✅ Real-time price data
    └── repository/
        ├── BracketOrderRepository.kt ✅ Bracket order support
        ├── ExchangeRepository.kt     ✅ Core operations (12 methods)
        └── ExchangeWebSocket.kt      ✅ Real-time streams

🆕 COMPLETE: Data Layer Implementation
└── core/data/
    ├── security/
    │   └── StaticCredentialStore.kt 🆕 ✅ Build-time credential injection
    ├── local/                       🆕 ✅ Room database (Ticket 03)
    │   ├── database/
    │   │   └── EngineDatabase.kt    ✅ Room DB with 4 tables
    │   ├── entity/
    │   │   ├── CandleEntity.kt      ✅ Candle storage
    │   │   ├── OrderEntity.kt       ✅ Order history
    │   │   ├── DecisionEntity.kt    ✅ Decision tracking
    │   │   └── PortfolioSnapshotEntity.kt ✅ Portfolio snapshots
    │   └── dao/
    │       ├── CandleDao.kt         ✅ Candle queries
    │       ├── OrderDao.kt          ✅ Order queries
    │       ├── DecisionDao.kt       ✅ Decision queries
    │       └── PortfolioDao.kt      ✅ Portfolio queries
    └── di/
        ├── SecurityModule.kt        ✅ Hilt DI for credential store
        └── DatabaseModule.kt        ✅ Hilt DI for Room database

🆕 COMPLETE: Coinbase Authentication
└── exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt  ✅ ES256 JWT token generation
    └── di/
        └── AuthModule.kt            ✅ Hilt DI for JWT provider

🆕 COMPLETE: UI Foundation
└── core/ui/
    ├── component/
    │   ├── ErrorDisplay.kt          ✅ Error state with retry button
    │   ├── LoadingButton.kt         ✅ Button with loading spinner
    │   ├── ModeIndicator.kt         ✅ Trading mode badges (DEFENSE/TREND/RANGE)
    │   ├── PriceDisplay.kt          ✅ Price with +/- color coding
    │   └── StatusCard.kt            ✅ Reusable card container
    └── extension/
        └── BigDecimalExt.kt        ✅ Currency/percentage formatting

🆕 COMPLETE: Static Credential System
├── Build-time injection          ✅ Environment vars → BuildConfig → DI
├── Local development support     ✅ local.properties fallback
├── CI/CD integration            ✅ GitHub secrets → environment vars
└── No UI credential entry       ✅ Simplified UX flow
```

**Dependencies (ALL already added in build.gradle.kts):**
- ✅ Kotlin 2.3.0
- ✅ Compose BOM 2025.12.01
- ✅ Hilt 2.57.2
- ✅ Room 2.8.4
- ✅ Ktor 3.3.3 (with OkHttp engine)
- ✅ Timber 5.0.1
- ✅ Vico 2.4.0 (charts)
- ✅ Coroutines 1.10.2
- ✅ **nimbus-jose-jwt 9.47** ✅ ACTIVE (for JWT ES256 signing)
- ✅ **ta4j-core 0.16** (for technical indicators)
- ✅ **security-crypto 1.1.0-alpha06** (for encrypted storage - now unused)
- ✅ **work-runtime-ktx 2.10.0** (for background tasks)
- ✅ **datastore-preferences 1.1.1** (for settings)
- ✅ **material-icons-extended** ✅ ACTIVE (for ModeIndicator icons)
- ✅ Firebase Analytics + Crashlytics (BOM 34.7.0)

**CI/CD:**
- ✅ GitHub Actions: Build workflow on `claude/*` branches with credential injection
- ✅ Auto-build + Firebase App Distribution on PR
- ✅ Commit-back pattern (`.build-status` + `build-log.txt`)
- ✅ Auto-documentation workflow (updates CLAUDE.md + docs/)

**Documentation:**
- ✅ CLAUDE.md (project context for AI)
- ✅ ~/.claude/CLAUDE.md (global AI preferences)
- ✅ docs/reference.md (implementation blueprint with hierarchical structure)
- ✅ docs/ci.md (CI/CD documentation with credential injection)
- ✅ docs/auto-docs.md (auto-doc workflow)
- ✅ docs/tickets/ (all Notion tickets organized by status)

### 🎯 Phase 0A Progress: Authentication Infrastructure (COMPLETE ✅)

**✅ COMPLETE:**
- [x] **Ticket 02:** Repository Interfaces - All exchange contracts defined
- [x] **Ticket 04:** Secure Credential Store - Build-time static injection (replaces runtime input)
- [x] **Ticket 07:** JWT Generator - ES256 token generation for Coinbase
- [x] **UI Foundation:** ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard
- [x] **Credential System:** Build-time injection, environment variable support, CI/CD integration
- [x] **Navigation Simplified:** Removed login screen, direct to Dashboard

**🆕 MAJOR ARCHITECTURAL CHANGE:**
**Static Credentials** - Replaced entire login screen system with build-time credential injection:

**Before (Removed):**
```
❌ Login Screen UI (LoginScreen.kt, LoginViewModel.kt, LoginUiState.kt)
❌ User credential input and validation
❌ SecureCredentialStore with AES-256 encryption
❌ Runtime authentication flow
```

**After (Current):**
```
✅ Build-time credential injection (app/build.gradle.kts)
✅ Environment variable priority (CI/CD → local.properties → empty)
✅ StaticCredentialStore (no encryption needed)
✅ CredentialsModule providing via Hilt DI
✅ Simplified navigation (direct to Dashboard)
```

**Benefits:**
- **Simplified UX:** No credential entry needed - app just works
- **Better Security:** Credentials never typed on device keyboard
- **CI/CD Ready:** GitHub secrets automatically injected
- **Developer Friendly:** local.properties for development
- **Reduced Code:** ~500 lines of login UI code removed

### ❌ What DOESN'T Exist (Business Logic & Integration)

**Phase 0B is 50% complete. Remaining work:**

```
✅ domain/model/             # COMPLETE: All domain models implemented
✅ data/local/database/      # COMPLETE: Room database with entities + DAOs
❌ domain/strategy/          # No decision engine yet (Ticket 05 - NEXT)
❌ domain/risk/              # No risk manager yet (Ticket 06)
❌ data/exchange/coinbase/   # No REST API methods (JWT only, need Ticket 08)
❌ data/repository/          # No repository implementations
❌ presentation/             # No screens yet (skeleton only)
❌ service/trading/          # No trading service
❌ No unit tests
❌ No integration tests
❌ No backtesting
```

---

## Architecture Principles

### Clean Architecture (Single Module)

We're using **single-module architecture** with clear package separation:

```
app/src/main/java/com/dpart/tradeflow/
├── domain/                  # Pure Kotlin, zero Android dependencies
│   ├── model/              # Candle, Order, Portfolio, Decision
│   ├── repository/         # ✅ COMPLETE: ExchangeRepository interface
│   ├── strategy/           # DecisionEngine
│   └── risk/               # RiskManager
│
├── data/
│   ├── local/              # Room database
│   ├── security/           # ✅ COMPLETE: Static credential injection
│   └── exchange/
│       └── coinbase/       # Coinbase-specific implementation (ISOLATED)
│           ├── auth/       # ✅ COMPLETE: JWT generator
│           ├── api/        # REST client (pending)
│           ├── websocket/  # WebSocket client (pending)
│           ├── dto/        # Coinbase DTOs
│           └── mapper/     # DTO → domain model mappers
│
├── presentation/
│   ├── dashboard/          # Dashboard screen + ViewModel (pending)
│   ├── settings/           # Settings screen + ViewModel (pending)
│   └── components/         # ✅ COMPLETE: Shared UI components
│
├── service/
│   └── trading/            # TradingService (foreground service)
│
├── navigation/             # ✅ COMPLETE: Simplified routing
│   ├── AppNavHost.kt       # Direct Dashboard → Settings
│   └── Screen.kt           # Two routes only
│
└── di/                     # Hilt modules
    ├── AppModule.kt
    ├── DatabaseModule.kt
    ├── NetworkModule.kt
    ├── CredentialsModule.kt # ✅ COMPLETE: Build-time injection
    ├── SecurityModule.kt   # ✅ COMPLETE: Credential store binding
    └── ExchangeModule.kt   # ONLY place that knows about Coinbase
```

### Critical Rules

1. **Domain layer**: NO Android imports, NO Coinbase imports
2. **Presentation layer**: Imports ONLY `domain/repository/ExchangeRepository` interface
3. **Service layer**: Imports ONLY domain interfaces
4. **All Coinbase code** lives in `data/exchange/coinbase/`
5. **DI binds implementation to interface** in `ExchangeModule.kt`
6. **Credentials injected at build time** - no runtime credential management

---

## Phase-by-Phase Implementation Plan

### ✅ Phase 0A: Domain Foundation (COMPLETE)

**Status:** 100% Complete
**Duration:** 2 weeks (Dec 2025 - Jan 2026)

| Ticket | Status | Description |
|--------|--------|-------------|
| **00** | ✅ Done | Project Modularization (8-module Clean Architecture) |
| **01** | ✅ Done | Domain Models (Candle, Order, Decision, Portfolio, Balance, Ticker) |
| **02** | ✅ Done | Repository Interfaces (ExchangeRepository, AuthTokenProvider, CredentialStore, BracketOrderRepository, ExchangeWebSocket) |
| **03** | ✅ Done | Room Database (4 entities: Candle, Order, Decision, PortfolioSnapshot + 4 DAOs) |
| **04** | ✅ Done | Static Credential Store (build-time injection via BuildConfig) |
| **07-JWT** | ✅ Done | JWT Generator (ES256 signing for Coinbase with proper nonce generation) |

**Deliverables:**
- ✅ 8-module Clean Architecture (app, core:domain, core:data, core:ui, exchange:coinbase, feature:*)
- ✅ Complete domain model layer (pure Kotlin, zero Android dependencies)
- ✅ Repository interfaces defining exchange contracts
- ✅ Room database with entity + DAO layer
- ✅ Build-time credential injection (env vars → BuildConfig → DI)
- ✅ JWT token generation for Coinbase REST API

**Key Achievement:** **Solid domain foundation** - Clean separation of concerns with domain-first architecture, making exchange swapping trivial.

---

### ✅ Phase 0B: UI Foundation (COMPLETE)

**Status:** 100% Complete
**Duration:** 1 week (Jan 2026)

| Ticket | Status | Description |
|--------|--------|-------------|
| **05** | ✅ Done | UI Design Overview (complete visual redesign) |
| **06** | ✅ Done | Core UI Theme (Material 3 theme + color system) |
| **07-UI** | ✅ Done | Core UI Components (ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard, BigDecimalExt) |
| **08** | ✅ Done | Login Screen (obsolete - removed when credentials moved to build-time) |
| **09** | 🔄 Review | App Navigation (simplified: Dashboard + Settings only) |

**Deliverables:**
- ✅ Complete Material 3 theme with day/night mode support
- ✅ Reusable UI components for trading app (mode indicators, price displays, status cards)
- ✅ Adaptive app icon with trading chart design
- ✅ Simplified navigation (no login flow)
- 🔄 App navigation pending review

**Key Achievement:** **Professional UI foundation** - Modern Compose components ready for Dashboard and Settings implementation.

---

### 🎯 Phase 1: Business Logic (CURRENT - Ready to Start)

**Goal:** Implement trading strategy and risk management

**Duration:** 1-2 weeks
**Priority:** CRITICAL (blocks all trading functionality)

| Ticket | Priority | Status | Description | Blocks |
|--------|----------|--------|-------------|--------|
| **15** | CRITICAL | ⏭️ **NEXT** | Decision Engine (SMA, ADX, ATR + regime switching) | Trading strategy |
| **16** | CRITICAL | Pending | Risk Manager (position sizing, drawdown limits) | Safety |

**Dependencies:**
- ✅ Ticket 01 (domain models exist)
- ✅ Ticket 03 (database for persistence)
- ✅ ta4j-core library (already in dependencies)

**Deliverables:**
- ⏭️ DecisionEngine interface + TradingDecisionEngine implementation
- ⏭️ SMA(200), ADX(14), ATR(14) indicator calculations using ta4j
- ⏭️ Regime switching logic (DEFENSE/TREND/RANGE modes with hysteresis)
- ⏭️ RiskManager with position sizing, exposure limits, drawdown monitoring
- ⏭️ Unit tests for decision engine with mock candles

**Progress:** 0/2 tickets complete (0%)

**Critical Path:** **Decision Engine (15)** ⏭️ → Risk Manager (16) → Backtest Validation → REST API

---

### 📊 Phase 1B: Strategy Validation (After Business Logic)

**Goal:** Validate strategy works before building full API integration

**Duration:** 1 week
**Priority:** CRITICAL (prevents wasted work on broken strategy)

| Task | Priority | Description |
|------|----------|-------------|
| **Backtest** | CRITICAL | 7-year BTC/USDT backtest (2018-2025) with realistic fees |
| **Validation** | CRITICAL | Verify 52%+ win rate, 1.0+ Sharpe ratio |
| **Paper Trade** | HIGH | Small $10 live trades for 30 days validation |

**Dependencies:** Tickets 15 (Decision Engine), 16 (Risk Manager)

**Deliverables:**
- ⏭️ Historical data download (7+ years H4 candles)
- ⏭️ Backtest harness (replay candles → strategy decisions)
- ⏭️ Performance report (win rate, Sharpe, drawdown)
- ⏭️ Go/No-Go decision (if fails, fix strategy before Phase 2)

**Strategy Parameters (from analysis):**
```kotlin
// Hysteresis (lag reduction)
trendConfirmation = 3 candles   // 12 hours (patience)
rangeConfirmation = 3 candles   // 12 hours (patience)
defenseConfirmation = 0 candles // Instant (safety)

// ADX thresholds
adxTrendThreshold = 25.0   // Strong trend
adxRangeThreshold = 25.0   // Range-bound

// Position sizing (CLARIFIED)
positionSize = 10% of portfolio  // Amount in trade
riskPerTrade = 1-2% of portfolio // Max loss via stop-loss
// Example: $500 account → $50 position (10%) → $10 stop (2%)
```

**Critical:** Do NOT proceed to Phase 2 if backtest fails validation criteria.

---

### 🔌 Phase 2: Coinbase Integration

**Goal:** Connect to live Coinbase API for trading and market data

**Duration:** 1-2 weeks
**Priority:** HIGH (enables live trading)

| Ticket | Priority | Description | Blocks |
|--------|----------|-------------|--------|
| **13** | HIGH | REST API Client (order placement, market data, candles) | Live trading |
| **14** | HIGH | WebSocket Client (real-time price, order updates) | Live data |

**Dependencies:**
- ✅ Ticket 02 (repository interfaces)
- ✅ Ticket 04 (credentials)
- ✅ Ticket 07-JWT (JWT generator)
- ⏭️ **Phase 1B validation passed** (backtest proves strategy works)

**Deliverables:**
- ⏭️ CoinbaseRepository implementing ExchangeRepository + BracketOrderRepository
- ⏭️ REST API methods: getCandles, placeOrder, cancelOrder, getAccounts, listOrders
- ⏭️ CoinbaseWebSocket implementing ExchangeWebSocket
- ⏭️ Real-time price feeds and order updates
- ⏭️ Rate limiting with exponential backoff (30 req/sec private, 10 req/sec public)
- ⏭️ Integration test with real API (small $1 trades)

---

### 🎨 Phase 3: User Interface

**Goal:** Build complete user interface for monitoring and control

**Duration:** 1 week
**Priority:** MEDIUM (monitoring, not critical path)

| Ticket | Priority | Description | Status |
|--------|----------|-------------|--------|
| **10** | MEDIUM | Dashboard Screen (portfolio, mode, recent orders) | Refined |
| **11** | MEDIUM | Settings Screen (preferences, about) | Refined |

**Dependencies:**
- ✅ Tickets 05-07 (UI foundation complete)
- ✅ Ticket 09 (navigation)
- ⏭️ Ticket 13 (REST API for data)

**Deliverables:**
- ⏭️ Dashboard screen showing: portfolio value, current mode, open orders, recent decisions
- ⏭️ Settings screen: trading preferences, app info, credentials status
- ⏭️ Real-time UI updates from WebSocket
- ⏭️ ViewModels with proper state management

---

### ⚙️ Phase 4: Trading Service

**Goal:** 24/7 autonomous background trading execution

**Duration:** 1 week
**Priority:** CRITICAL (enables autonomous trading)

| Ticket | Priority | Description | Blocks |
|--------|----------|-------------|--------|
| **17** | CRITICAL | Trading Service (foreground service orchestration) | Autonomous trading |
| **18** | HIGH | Battery Optimization (Doze exemption, wake locks) | 24/7 operation |

**Dependencies:** All previous phases (complete system needed)

**Deliverables:**
- ⏭️ Foreground service with persistent notification
- ⏭️ Strategy evaluation loop (fetch candles → decide → execute)
- ⏭️ Order execution based on DecisionEngine output
- ⏭️ RiskManager validation before all trades
- ⏭️ Emergency stop on drawdown breach
- ⏭️ Battery optimization (Doze whitelist, wake locks)

---

### 🧪 Phase 5: Testing & Polish

**Goal:** Validate system works reliably end-to-end

**Duration:** 1 week
**Priority:** HIGH (production readiness)

| Ticket | Priority | Description |
|--------|----------|-------------|
| **19** | HIGH | Integration Tests (real API with $1-5 trades) |
| **20** | CRITICAL | MVP Milestone (complete system validation + 7-day live test) |

**Deliverables:**
- ⏭️ Integration tests with real Coinbase API
- ⏭️ 7-day unattended operation test
- ⏭️ Emergency stop verification
- ⏭️ Production readiness checklist

---

## 📊 Progress Tracking

### Overall Progress: 10/20 tickets complete (50%)

```
Phase 0A: ████████████████████ 100% (6/6)   ✅ COMPLETE
Phase 0B: ████████████████████ 100% (4/4)   ✅ COMPLETE
Phase 1:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)   ← YOU ARE HERE
Phase 1B: ░░░░░░░░░░░░░░░░░░░░   0% (0/1)   (Backtest validation)
Phase 2:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
Phase 3:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
Phase 4:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
Phase 5:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
```

### Current Sprint: Phase 1 - Business Logic

**Completed Phases:**
- ✅ Phase 0A: Domain Foundation (Tickets 00-04, 07-JWT) - 6 tickets
- ✅ Phase 0B: UI Foundation (Tickets 05-09) - 4 tickets *(Ticket 09 in review but UI complete)*

**Next Up:** Ticket 15 (Decision Engine) - Implement regime switching with SMA(200), ADX(14), ATR(14)

**Ready to Start:** Ticket 15 has zero blockers
- ✅ Domain models exist (Ticket 01)
- ✅ Database ready (Ticket 03)
- ✅ ta4j-core library in dependencies

---

## 🚀 Getting Started with Phase 1

**Implement core business logic (strategy + risk management):**

### Ticket 15: Decision Engine (2-3 days)
**Location:** `:core:domain/strategy/`

**Files to create:**
- `DecisionEngine.kt` (interface)
- `TradingDecisionEngine.kt` (implementation)
- `StrategyConfig.kt` (configuration data class)
- `indicator/SMACalculator.kt` (SMA using ta4j)
- `indicator/ADXCalculator.kt` (ADX using ta4j)
- `indicator/ATRCalculator.kt` (ATR using ta4j)

**Key Implementation:**
- SMA(200) for trend direction (price above/below)
- ADX(14) for trend strength (>25 = trend, <25 = range)
- ATR(14) for volatility-based stop-loss/take-profit
- 3-candle hysteresis for TREND and RANGE modes
- Instant (0-candle) switch to DEFENSE mode

**Acceptance Criteria:**
- ✅ Correctly identifies all 4 modes (Wait, Defense, Trend, Range)
- ✅ Hysteresis prevents whipsawing between modes
- ✅ Grid spacing never below 1.5% (fee break-even)
- ✅ 100% unit test coverage with mock candles
- ✅ Zero Android dependencies (pure Kotlin/JVM)

### Ticket 16: Risk Manager (1 day)
**Location:** `:core:domain/risk/`

**Files to create:**
- `RiskManager.kt` (risk validation + position sizing)
- `RiskConfig.kt` (risk limits configuration)

**Key Implementation:**
- Position sizing: 10% of portfolio per trade
- Risk per trade: 1-2% via stop-loss
- Total exposure limit: 10%
- Drawdown limit: 15% emergency stop
- Grid spacing validation (>1.5%)

**Acceptance Criteria:**
- ✅ Validates orders against risk limits
- ✅ Calculates position sizes for trend and grid modes
- ✅ Monitors drawdown vs high-water mark
- ✅ Zero exchange-specific code
- ✅ Unit tests for all scenarios

**Total Phase 1 Effort:** ~3-4 days

**After Phase 1:** Proceed to **Phase 1B (Strategy Validation)** - backtest with historical data before building API client

---

## 🔮 Future Enhancements (Post-MVP)

**Implementation Priority:** AFTER Phase 3 complete and Coinbase integration proven profitable

### Polymarket Prediction Market Integration

**Goal:** Add cross-market arbitrage strategy by comparing Coinbase spot prices with Polymarket prediction odds.

**Opportunity:** Similar to the 0x8dxd bot that turned $313 → $438k in 30 days exploiting price lag between spot markets and prediction markets.

**Status:** Fully documented integration plan available
**Documentation:** [docs/future-enhancements/polymarket-integration.md](future-enhancements/polymarket-integration.md)

**Key Benefits:**
- Lower risk than directional trading (arbitrage vs. prediction)
- Higher potential win rate (70-90% vs. 52-58%)
- Diversification (second exchange, different strategy type)

**Key Risks:**
- Legal restrictions (Polymarket banned in US)
- Arbitrage edge may already be closed
- Lower liquidity than Coinbase

**Prerequisites:**
- ✅ Phase 0A complete (domain models, auth)
- ✅ Phase 0B complete (decision engine, risk manager)
- ✅ Phase 1-3 complete (Coinbase proven profitable 30+ days)
- ⚠️ Phase 1 validation (research arbitrage opportunities for 7 days)

**Timeline:** ~3 months AFTER Coinbase integration proven

**Go/No-Go Decision:** Based on validation phase finding 10+ arbitrage opportunities per week with 10%+ edge

---

## 🎯 Why This Order Makes Sense

### 1. Authentication First (Phase 0A - COMPLETE ✅)
- **Foundation:** Can't trade without API access
- **Simplification:** Static credentials remove UI complexity
- **Security:** Build-time injection more secure than device storage
- **CI/CD Ready:** GitHub secrets integration
- **Result:** Solid authentication foundation

### 2. Domain & Data Next (Phase 0B - CURRENT)
- **Business Models:** Define what data looks like
- **Persistence:** Store trading state and history
- **Strategy Logic:** Implement regime-switching decision engine
- **Risk Management:** Safety limits and emergency stops
- **Result:** Complete business logic foundation

### 3. API Integration Third (Phase 1)
- **Live Data:** Connect to real Coinbase API
- **Real Trading:** Place and manage actual orders
- **WebSocket:** Real-time price and order updates
- **Result:** Working connection to exchange

### 4. UI Fourth (Phase 2)
- **Monitoring:** Visual dashboard for portfolio and trading state
- **Control:** Settings and manual overrides
- **Real-time Updates:** Live data flowing to UI
- **Result:** Complete user interface

### 5. Service Last (Phase 3)
- **Autonomous Operation:** 24/7 background trading
- **Battery Optimization:** Survive Android power management
- **Production Ready:** Reliable long-term operation
- **Result:** Fully autonomous trading bot

### 6. Testing & Polish (Phase 4)
- **Validation:** End-to-end system testing
- **Reliability:** Confirm 24/7 operation
- **Production:** Ready for live trading
- **Result:** Proven, reliable system

---

## 💡 Architectural Decisions

### Static Credentials (New Approach)

**Decision:** Replace login screen with build-time credential injection

**Rationale:**
- **Simpler UX:** No credential entry needed - app just works
- **More Secure:** Credentials never typed on device, not stored persistently
- **CI/CD Friendly:** GitHub secrets automatically available
- **Developer Friendly:** local.properties for development
- **Less Code:** Eliminated ~500 lines of login UI/ViewModel code

**Implementation:**
```kotlin
// app/build.gradle.kts - Injection
val coinbaseApiKey = System.getenv("COINBASE_API_KEY")
    ?: props.getProperty("coinbase.api.key", "")
buildConfigField("String", "COINBASE_API_KEY", "\"$coinbaseApiKey\"")

// CredentialsModule.kt - DI
@Provides @Named("coinbase_api_key")
fun provideCoinbaseApiKey(): String = BuildConfig.COINBASE_API_KEY

// StaticCredentialStore.kt - Access
override suspend fun getApiKey(): String? = apiKey.takeIf { it.isNotBlank() }
```

### Single Module Architecture

**Decision:** Keep everything in `:app` module initially, modularize later if needed

**Rationale:**
- **Simplicity:** Easier to refactor within single module
- **Speed:** No inter-module boundaries during rapid development
- **Clear Packages:** Package structure enforces separation
- **Future Ready:** Can extract modules when architecture stabilizes

### Domain-First Design

**Decision:** Define interfaces in domain layer, implement in data layer

**Rationale:**
- **Testability:** Mock interfaces for unit testing
- **Flexibility:** Swap Coinbase for other exchanges
- **Clean Dependencies:** Domain never depends on infrastructure
- **Future Ready:** Easy to add Kraken, Binance, etc.
