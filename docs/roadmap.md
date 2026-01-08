# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-08
**Project Status:** Phase 0B In Progress - Core Domain & Data (50% complete)
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

### ✅ Phase 0A: Authentication Infrastructure (COMPLETE)

**Status:** 100% Complete
**Duration:** 2 weeks (Dec 2025 - Jan 2026)

| Ticket | Status | Description |
|--------|--------|-------------|
| **01** | ✅ Done | Domain Models (Candle, Order, Decision, Portfolio) |
| **02** | ✅ Done | Repository Interfaces (ExchangeRepository, AuthTokenProvider, etc.) |
| **03** | ✅ Done | Room Database (entities, DAOs, database setup) |
| **04** | ✅ Done | Static Credential Store (build-time injection) |
| **07** | ✅ Done | JWT Generator (ES256 signing for Coinbase) |
| **UI** | ✅ Done | Core UI components (StatusCard, LoadingButton, etc.) |
| **NAV** | ✅ Done | Simplified navigation (Dashboard + Settings only) |

**Deliverables:**
- ✅ Complete domain interface layer
- ✅ Build-time credential injection system
- ✅ JWT token generation for Coinbase API
- ✅ Foundational UI components
- ✅ Simplified app navigation (no login screen)

**Key Achievement:** **Authentication without UI complexity** - credentials injected at build time, eliminating need for credential entry screens and runtime authentication flows.

---

### 🎯 Phase 0B: Core Domain & Data (CURRENT - 50% Complete)

**Goal:** Implement core business models and persistence layer

**Duration:** 1-2 weeks
**Priority:** Critical (blocks everything else)

| Ticket | Priority | Status | Description | Blocks |
|--------|----------|--------|-------------|--------|
| **01** | CRITICAL | ✅ Done | Domain Models (Candle, Order, Decision, Portfolio) | All business logic |
| **03** | CRITICAL | ✅ Done | Room Database (entities, DAOs) | Data persistence |
| **05** | HIGH | ⏭️ Next | Decision Engine (SMA, ADX, ATR logic) | Trading strategy |
| **06** | HIGH | Pending | Risk Manager (position sizing, drawdown limits) | Safety |

**Dependencies:** None for Ticket 05 (can start immediately)

**Deliverables:**
- ✅ Candle, Order, Decision, Portfolio data classes
- ✅ Room database with proper entities and DAOs
- ⏭️ Decision engine with regime switching (DEFENSE/TREND/RANGE)
- ⏭️ Risk manager with position limits and emergency stop

**Progress:** 2/4 tickets complete (50%)

**Critical Path:** ~~Domain models~~ ✅ → ~~Database~~ ✅ → **Decision engine** ⏭️ → Risk manager

---

### 📊 Phase 0C: Strategy Validation (NEW - Week 3)

**Goal:** Validate strategy works before building full API integration

**Duration:** 1 week
**Priority:** CRITICAL (prevents wasted work on broken strategy)

| Task | Priority | Description |
|------|----------|-------------|
| **Backtest** | CRITICAL | 7-year BTC/USDT backtest (2018-2025) with realistic fees |
| **Validation** | CRITICAL | Verify 52%+ win rate, 1.0+ Sharpe ratio |
| **Paper Trade** | HIGH | Small $10 live trades for 30 days validation |

**Dependencies:** Tickets 05 (Decision Engine), 06 (Risk Manager)

**Deliverables:**
- ✅ Historical data download (7+ years H4 candles)
- ✅ Backtest harness (replay candles → strategy decisions)
- ✅ Performance report (win rate, Sharpe, drawdown)
- ✅ Go/No-Go decision (if fails, fix strategy before Phase 1)

**Strategy Parameters (from analysis):**
```kotlin
// Hysteresis (lag reduction)
trendConfirmation = 1 candle    // 4 hours (catch momentum)
rangeConfirmation = 3 candles   // 12 hours (patience)
defenseConfirmation = 0 candles // Instant (safety)

// Volume confirmation (prevent fake pumps)
val isRealTrend = adx > 25 && volume > avgVolume * 1.2

// Position sizing (CLARIFIED)
positionSize = 10% of portfolio  // Amount in trade
riskPerTrade = 1-2% of portfolio // Max loss via stop-loss
// Example: $500 account → $50 position (10%) → $10 stop (2%)
```

**Critical:** Do NOT proceed to Phase 1 if backtest fails validation criteria.

---

### 🔌 Phase 1: Coinbase Integration (Week 4)

**Goal:** Connect to live Coinbase API for trading and market data

| Ticket | Priority | Description | Blocks |
|--------|----------|-------------|--------|
| **08** | HIGH | REST API Client (order placement, market data) | Live trading |
| **09** | HIGH | WebSocket Client (real-time price, order updates) | Live data |

**Dependencies:** Tickets 01 (domain models), 04 (credentials), 07 (JWT), **Phase 0C validation passed**

**Deliverables:**
- ✅ Complete CoinbaseRepository implementation
- ✅ Real-time WebSocket price and order feeds
- ✅ Order placement (market, limit, bracket orders)
- ✅ Account balance and market data fetching

---

### 🎨 Phase 2: User Interface (Week 4)

**Goal:** Build complete user interface for monitoring and control

| Ticket | Priority | Description | Blocks |
|--------|----------|-------------|--------|
| **10** | HIGH | Dashboard Screen (portfolio, mode, orders) | User monitoring |
| **11** | HIGH | Settings Screen (preferences, about) | User control |
| **12** | MEDIUM | Dashboard ViewModel (business logic) | Data binding |
| **13** | MEDIUM | Settings ViewModel (settings logic) | Settings persistence |

**Dependencies:** Tickets 01, 03 (data layer), UI components (done)

**Deliverables:**
- ✅ Professional dashboard showing portfolio, trading mode, orders
- ✅ Settings screen for preferences and app information
- ✅ Complete navigation between screens
- ✅ Real-time UI updates from WebSocket

---

### ⚙️ Phase 3: Trading Service (Week 5)

**Goal:** 24/7 autonomous background trading execution

| Ticket | Priority | Description | Blocks |
|--------|----------|-------------|--------|
| **16** | HIGH | Trading Service (foreground service orchestration) | Autonomous trading |
| **17** | HIGH | Battery Optimization (Doze exemption, wake locks) | 24/7 operation |

**Dependencies:** All previous phases (complete system needed)

**Deliverables:**
- ✅ Foreground service running strategy evaluation loop
- ✅ Order execution based on decision engine output
- ✅ Risk monitoring and emergency liquidation
- ✅ Battery optimization and Doze mode survival

---

### 🧪 Phase 4: Testing & Polish (Week 6)

**Goal:** Validate system works reliably end-to-end

| Ticket | Priority | Description |
|--------|----------|-------------|
| **18** | MEDIUM | Integration Tests (real API with small orders) |
| **19** | HIGH | MVP Milestone (complete system validation) |

**Deliverables:**
- ✅ Integration tests with real Coinbase API
- ✅ End-to-end system validation
- ✅ Production readiness assessment

---

## 📊 Progress Tracking

### Overall Progress: 8/20 tickets complete (40%)

```
Phase 0A: ████████████████████ 100% (6/6)  ✅ COMPLETE
Phase 0B: ██████████░░░░░░░░░░  50% (2/4)  ← YOU ARE HERE
Phase 0C: ░░░░░░░░░░░░░░░░░░░░   0% (0/1)  ← NEXT (Validation)
Phase 1:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
Phase 2:  ░░░░░░░░░░░░░░░░░░░░   0% (0/4)
Phase 3:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
Phase 4:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
```

### Current Sprint: Phase 0B - Core Domain & Data

**Completed:**
- ✅ Ticket 01: Domain Models (Candle, Order, Decision, Portfolio, Balance, Ticker)
- ✅ Ticket 03: Room Database (4 entities, 4 DAOs, EngineDatabase)

**Next Up:** Ticket 05 (Decision Engine) - Implement regime switching with SMA, ADX, ATR

**Ready to Start:** Ticket 05 has no blockers (domain models + database complete)

---

## 🚀 Getting Started with Phase 0B

**Build foundation for all business logic:**

1. **Ticket 01** - Domain Models (1 day)
   - Create Candle, Order, Decision, Portfolio data classes
   - Define all enums (OrderSide, OrderStatus, etc.)
   - Pure Kotlin, no Android dependencies

2. **Ticket 03** - Room Database (1-2 days)
   - Create entities for domain models
   - Implement DAOs with Flow support
   - Database setup and migrations

3. **Ticket 05** - Decision Engine (2-3 days)
   - SMA(200), ADX(14), ATR(14) calculations using ta4j
   - Regime switching logic (DEFENSE/TREND/RANGE)
   - Hysteresis to prevent mode whipsawing

4. **Ticket 06** - Risk Manager (1 day)
   - Position sizing (**10% position size** with **1-2% risk** via stop-loss)
   - Drawdown monitoring (15% emergency stop)
   - Validation before order placement

**Total Phase 0B Effort:** ~5-7 days

**After Phase 0B:** Proceed to **Phase 0C (Strategy Validation)** - backtest before building API client

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
