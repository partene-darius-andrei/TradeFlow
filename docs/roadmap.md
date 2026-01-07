# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-07
**Project Status:** Phase 0 - Foundation
**Current Build:** #30 (SUCCESS)
**Architecture:** Multi-module app (7 modules: app, core:domain, core:data, core:ui, feature:dashboard, feature:trading, feature:settings)

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

---

## Current State Analysis

### ✅ What Exists (Jan 2026)

**Codebase:**
```
app/src/main/java/com/dpart/tradeflow/
├── MainActivity.kt              ✅ Shows "TradeFlow" text only
├── TradeFlowApp.kt              ✅ Initializes Timber logging + Hilt
├── di/
│   ├── AppModule.kt             ✅ Empty Hilt module
│   ├── DatabaseModule.kt        ✅ Provides Room database (empty)
│   └── NetworkModule.kt         ✅ Provides Ktor HttpClient (OkHttp engine)
└── data/local/
    ├── AppDatabase.kt           ✅ Empty Room DB
    └── PlaceholderEntity.kt     ✅ Dummy entity
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
- ✅ **nimbus-jose-jwt 9.47** (for JWT ES256 signing)
- ✅ **ta4j-core 0.16** (for technical indicators)
- ✅ **security-crypto 1.1.0-alpha06** (for encrypted credentials)
- ✅ **work-runtime-ktx 2.10.0** (for background tasks)
- ✅ **datastore-preferences 1.1.1** (for settings)
- ✅ Firebase Analytics + Crashlytics (BOM 34.7.0)

**CI/CD:**
- ✅ GitHub Actions: Build workflow on `claude/*` branches
- ✅ Auto-build + Firebase App Distribution on PR
- ✅ Commit-back pattern (`.build-status` + `build-log.txt`)
- ✅ Auto-documentation workflow (updates CLAUDE.md + docs/)

**Documentation:**
- ✅ CLAUDE.md (project context for AI)
- ✅ ~/.claude/CLAUDE.md (global AI preferences)
- ✅ docs/reference.md (implementation blueprint with hierarchical structure)
- ✅ docs/ci.md (CI/CD documentation)
- ✅ docs/auto-docs.md (auto-doc workflow)
- ✅ docs/tickets/ (all Notion tickets organized by status)

### ❌ What DOESN'T Exist (Everything Else)

**No business logic has been implemented yet.** This is a greenfield project.

```
❌ domain/model/             # No domain models
❌ domain/repository/        # No repository interfaces
❌ domain/usecase/           # No use cases
❌ domain/strategy/          # No decision engine
❌ domain/risk/              # No risk manager
❌ data/exchange/coinbase/   # No Coinbase API client
❌ data/repository/          # No repository implementations
❌ presentation/             # No screens beyond MainActivity
❌ service/trading/          # No trading service
❌ No unit tests
❌ No integration tests
```

---

## Architecture Principles

### Clean Architecture (Single Module)

We're using **single-module architecture** with clear package separation:

```
app/src/main/java/com/dpart/tradeflow/
├── domain/                  # Pure Kotlin, zero Android dependencies
│   ├── model/              # Candle, Order, Portfolio, Decision
│   ├── repository/         # ExchangeRepository interface
│   ├── strategy/           # DecisionEngine
│   └── risk/               # RiskManager
│
├── data/
│   ├── local/              # Room database
│   ├── security/           # Credential storage
│   └── exchange/
│       └── coinbase/       # Coinbase-specific implementation (ISOLATED)
│           ├── auth/       # JWT generator
│           ├── api/        # REST client
│           ├── websocket/  # WebSocket client
│           ├── dto/        # Coinbase DTOs
│           └── mapper/     # DTO → domain model mappers
│
├── presentation/
│   ├── dashboard/          # Dashboard screen + ViewModel
│   ├── settings/           # Settings screen + ViewModel
│   └── components/         # Shared UI components
│
├── service/
│   └── trading/            # TradingService (foreground service)
│
└── di/                     # Hilt modules
    ├── AppModule.kt
    ├── DatabaseModule.kt
    ├── DomainModule.kt
    └── ExchangeModule.kt   # ONLY place that knows about Coinbase
```

### Critical Rules

1. **Domain layer**: NO Android imports, NO Coinbase imports
2. **Presentation layer**: Imports ONLY `domain/repository/ExchangeRepository` interface
3. **Service layer**: Imports ONLY domain interfaces
4. **All Coinbase code** lives in `data/exchange/coinbase/`
5. **DI binds implementation to interface** in `ExchangeModule.kt`
6. **Never hardcode API keys** - EncryptedSharedPreferences only
7. **Account for fees** in all calculations (0.25-0.60% at Coinbase)

### Why This Architecture?

**To swap Coinbase for Kraken:**
1. Implement `ExchangeRepository` in `data/exchange/kraken/`
2. Update `ExchangeModule.kt` DI bindings
3. **Zero changes** to domain, presentation, or service layers

---

## Implementation Roadmap

### Phase 0: Foundation [CURRENT PHASE]

**Goal:** Establish core architecture and interfaces

**Tickets:** 00-06

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 00 | **Project Modularization** | Not Started | **CRITICAL** | Medium | 7 modules, 7 build.gradle.kts files |
| 01 | Domain Models | Not Started | High | Small | Candle, Order, Portfolio, Ticker, Balance, Decision |
| 02 | Repository Interfaces | Not Started | High | Medium | ExchangeRepository, ExchangeWebSocket, AuthTokenProvider |
| 03 | Room Database | Not Started | High | Medium | Database, Entities, DAOs |
| 04 | Credential Store | Not Started | High | Small | SecureCredentialStore (EncryptedSharedPreferences) |
| 05 | Decision Engine | Not Started | High | Large | DecisionEngine, indicator calculators |
| 06 | Risk Manager | Not Started | High | Medium | RiskManager (position sizing, drawdown) |

**Dependencies Added:** ✅ All dependencies already in build.gradle.kts

**Blockers:** Must complete Ticket 00 (Modularization) FIRST before starting any other Phase 0 tickets

**Success Criteria:**
- [ ] Domain models compile with NO Android imports
- [ ] All interfaces defined with clear contracts
- [ ] Room database creates tables on first run
- [ ] Can save/load encrypted credentials
- [ ] Decision engine has 100% unit test coverage
- [ ] Risk manager validates all edge cases

---

### Phase 1: Coinbase Integration

**Goal:** Implement Coinbase-specific code in isolation

**Tickets:** 07-09

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 07 | JWT Generator | Not Started | High | Medium | CoinbaseJwtGenerator (ES256 signing) |
| 08 | REST API Client | Not Started | High | Large | CoinbaseRepository, DTOs, Mappers |
| 09 | WebSocket Client | Not Started | High | Large | CoinbaseWebSocket (market + user channels) |

**Blocked By:** Phase 0 (interfaces must exist first)

**Critical Details:**
- JWT must use ES256 algorithm (ECDSA P-256)
- 120-second token expiry
- Refresh every 2 minutes for WebSocket
- **Must subscribe to heartbeats** channel (connection dies after 60-90s)
- Rate limits: 30 req/sec private, 10 req/sec public
- Bracket order mapping is counterintuitive (limit_price = take profit!)

**Success Criteria:**
- [ ] JWT tokens validate on Coinbase API
- [ ] Can fetch 350 H4 candles for BTC-USD
- [ ] WebSocket receives ticker updates
- [ ] WebSocket receives order status updates
- [ ] Can place + cancel real orders (integration tests with $10 minimum)
- [ ] Exponential backoff reconnect (5s → 60s max)

---

### Phase 2: Presentation Layer

**Goal:** Build UI screens with ViewModels

**Tickets:** 10-15

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 10 | Core UI Components | Not Started | Medium | Medium | Theme, PriceDisplay, ModeIndicator, OrderItem |
| 11 | Dashboard Screen | Not Started | Medium | Medium | DashboardScreen.kt (pure Compose) |
| 12 | Dashboard ViewModel | Not Started | High | Medium | DashboardViewModel (business logic) |
| 13 | Settings Screen | Not Started | Low | Small | SettingsScreen.kt (pure Compose) |
| 14 | Settings ViewModel | Not Started | Medium | Small | SettingsViewModel |
| 15 | App Navigation | Not Started | High | Medium | TradeFlowNavGraph, Application setup |

**Blocked By:** Phase 1 (WebSocket for real-time price updates)

**UI Requirements:**
- Material 3 design
- Dark mode preferred (trading apps = dark UI)
- Price display with 24h change %
- Mode indicator: WAIT / DEFENSE / TREND / RANGE
- Portfolio card with drawdown warning
- Active orders list with cancel buttons
- Control buttons: Start / Stop / Emergency Stop

**Success Criteria:**
- [ ] Can navigate between Dashboard and Settings
- [ ] Dashboard shows real-time BTC-USD price from WebSocket
- [ ] Can start/stop trading service from UI
- [ ] Emergency stop cancels all orders + liquidates positions
- [ ] Settings screen validates credentials before saving
- [ ] Test connection button confirms API access

---

### Phase 3: Trading Service

**Goal:** Implement 24/7 background trading orchestration

**Tickets:** 16-17

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 16 | Trading Service | Not Started | High | Large | TradingService, PriceMonitor, StrategyLoop |
| 17 | Battery Optimization | Not Started | Medium | Small | Doze exemption, wake locks |

**Blocked By:** Phase 0 (Decision Engine, Risk Manager) + Phase 1 (REST + WebSocket)

**Service Architecture:**
```
TradingService (Foreground Service)
├── PriceMonitor (WebSocket ticker subscription)
├── StrategyLoop (every 15 minutes, evaluates H4 candles)
├── RiskMonitor (continuous drawdown check, 15% limit)
└── OrderReconciler (syncs local DB with exchange)
```

**Trading Algorithm:**
- **Timeframe:** H4 candles (4-hour)
- **Indicators:** SMA(200), ADX(14), ATR(14)
- **Regime Detection:** TREND / RANGE / DEFENSE with hysteresis
- **Position Sizing:** 5% of portfolio max
- **Stop Loss:** 2x ATR
- **Take Profit:** 4x ATR (2:1 R:R ratio)
- **Emergency Liquidation:** At 15% drawdown

**Success Criteria:**
- [ ] Service survives Doze mode
- [ ] Runs continuously for 24 hours without crash
- [ ] Correctly identifies regime from candles
- [ ] Places bracket orders with proper SL/TP
- [ ] Emergency stop triggers at 15% drawdown
- [ ] All trades logged to Room database
- [ ] Notification shows current state (price, mode, P&L)

---

### Phase 4: Testing & Validation

**Goal:** Validate end-to-end system with real money (small amounts)

**Tickets:** 18-19

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 18 | Integration Tests | Not Started | Medium | Medium | Coinbase API tests (androidTest) |
| 19 | MVP Validation | Not Started | High | Small | Final checklist (40+ items) |

**Blocked By:** All previous phases

**Test Strategy:**
- **Unit tests:** Decision engine, risk manager (100% coverage)
- **Integration tests:** Real Coinbase API calls (requires credentials)
- **Live testing:** $10-50 real trades on BTC-USD
- **24-hour soak test:** Service must run without crash

**Safety Measures:**
- Integration tests place orders far from market, always cancelled
- Live testing uses minimum trade sizes
- Drawdown limit enforced (15% = emergency liquidation)
- Kill switch always accessible

**Success Criteria:**
- [ ] All unit tests pass
- [ ] Integration tests validate JWT, orders, WebSocket
- [ ] Service runs 24 hours without crash
- [ ] Correct mode detection in backtests
- [ ] Drawdown limit enforced in live testing
- [ ] All trades recorded in database

---

## Ticket Reference Guide

### Canonical Tickets

All tickets are in `docs/tickets/` organized by status. Below is the mapping of canonical tickets to their original Notion export files:

#### Foundation (Phase 0)
- **00-modularization** → 🏗️ MODULE Project Modularization Setup (NEW - CRITICAL)
- **01-domain-models** → 📦 DOMAIN Core Domain Models
- **02-interfaces** → 🔌 EXCHANGE-API Repository Interfaces
- **03-room-db** → 🗄️ INFRA - Room Database (Updated)
- **04-credentials** → 🔐 CORE-DATA Secure Credential Store
- **05-decision-engine** → 🧠 DOMAIN Decision Engine
- **06-risk-manager** → 🚨 DOMAIN - Risk Manager

#### Coinbase (Phase 1)
- **07-jwt** → 🟡 COINBASE JWT Token Generator
- **08-rest-api** → 🟡 COINBASE REST API Client
- **09-websocket** → 🟡 COINBASE WebSocket Client

#### UI (Phase 2)
- **10-core-ui** → 🎨 CORE-UI Shared Components & Theme
- **11-dashboard-ui** → 📊 FEATURE Dashboard Screen (UI Only)
- **12-dashboard-vm** → 📊 FEATURE Dashboard ViewModel (Logic)
- **13-settings-ui** → ⚙️ FEATURE Settings Screen (UI Only)
- **14-settings-vm** → ⚙️ FEATURE Settings ViewModel (Logic)
- **15-app-nav** → 📱 APP Main Application & Navigation

#### Service (Phase 3)
- **16-trading-service** → ⚡ SERVICE Trading Foreground Service
- **17-battery** → 🔋 SERVICE Battery Optimization & Doze

#### Testing (Phase 4)
- **18-integration-tests** → 🧪 TEST Integration Tests
- **19-mvp-milestone** → 🚀 Milestone MVP Ready for Testing

### Superseded Tickets (in archived/ folder - IGNORE)

These have been replaced by updated versions and should not be implemented:
- ❌ [SUPERSEDED] TradingService - Foreground Service Core
- ❌ [SUPERSEDED] JwtGenerator - ES256 Token Generation
- ❌ [SUPERSEDED] CoinbaseWebSocket - Real-time Data
- ❌ [SUPERSEDED] CoinbaseRestApi - Market Data & Accounts
- ❌ [SUPERSEDED] TradingService - Decision Execution
- ❌ [SUPERSEDED] UI - Dashboard & Status Screen
- ❌ [SUPERSEDED] UI - Settings & Credentials Screen
- ❌ [SUPERSEDED] SecureKeyStore - Encrypted Credentials
- ❌ [SUPERSEDED] Battery Optimization & Doze Mode
- ❌ [SUPERSEDED] TradingService - Risk Management

### Duplicate Tickets

Some concepts have multiple similar tickets in the backlog (likely multiple iterations/versions):
- Room Database
- Credential Store
- JWT Generator
- REST API / WebSocket
- Dashboard (UI + ViewModel variations)
- Settings (UI + ViewModel variations)
- Risk Manager
- Integration Tests

**Note:** Use the canonical tickets listed above. Duplicate tickets exist in backlog but will not be implemented separately.

---

## Dependency Graph

### Critical Path (Longest Chain to MVP)

```
START
  ↓
00: Modularization (2 hours) ★ MUST DO FIRST
  ↓
01: Domain Models (3 days)
  ↓
02: Repository Interfaces (2 days) ★ CRITICAL BLOCKER
  ↓
03-06: Parallel Development (1 week)
  ├→ 03: Room Database
  ├→ 04: Credential Store
  ├→ 05: Decision Engine (LARGE - 3 days)
  └→ 06: Risk Manager
  ↓
07-09: Coinbase Implementation (1 week)
  ├→ 07: JWT Generator (2 days)
  ├→ 08: REST API (3 days)
  └→ 09: WebSocket (3 days)
  ↓
10-15: UI Layer (1 week)
  ├→ 10-11: Dashboard Screen
  ├→ 12: Dashboard ViewModel
  ├→ 13-14: Settings
  └→ 15: Navigation
  ↓
16-17: Trading Service (1 week)
  ├→ 16: Service Core (5 days)
  └→ 17: Battery Optimization (1 day)
  ↓
18-19: Testing & Validation (1 week)
  ├→ 18: Integration Tests
  └→ 19: MVP Validation
  ↓
MVP READY FOR LIVE TESTING
```

### Parallel Development Tracks

After **Ticket 02 (Interfaces)** is complete, these can proceed simultaneously:

**Track A - Domain Logic:**
- 05: Decision Engine
- 06: Risk Manager
- Unit tests

**Track B - Infrastructure:**
- 03: Room Database
- 04: Credential Store

**Track C - Coinbase:**
- 07: JWT Generator
- 08: REST API (needs JWT)
- 09: WebSocket (needs JWT)

**Track D - UI (with mocks):**
- 10: Core UI Components
- 11: Dashboard Screen (mock data)
- 13: Settings Screen

---

## File Roadmap

### Phase 0 - Expected Files

```
app/src/main/java/com/dpart/tradeflow/

domain/
├── model/
│   ├── Candle.kt                    # OHLCV + timestamp
│   ├── Ticker.kt                    # Real-time price
│   ├── Balance.kt                   # Asset balance
│   ├── Portfolio.kt                 # Total value + drawdown
│   ├── Order.kt                     # Order details (id, side, price, size)
│   └── Decision.kt                  # Engine decision (BUY/SELL/HOLD + reason)
│
├── repository/
│   ├── ExchangeRepository.kt        # REST operations (accounts, orders, candles)
│   ├── BracketOrderRepository.kt    # Advanced order types (SL/TP)
│   ├── ExchangeWebSocket.kt         # Real-time streams (ticker, orders)
│   └── AuthTokenProvider.kt         # Token generation interface
│
├── strategy/
│   ├── DecisionEngine.kt            # Interface
│   ├── TradingDecisionEngine.kt     # Implementation (regime-switching)
│   ├── StrategyConfig.kt            # Configuration data class
│   └── indicators/
│       ├── SmaCalculator.kt         # Simple Moving Average
│       ├── AdxCalculator.kt         # ADX (trend strength)
│       └── AtrCalculator.kt         # ATR (volatility)
│
├── risk/
│   └── RiskManager.kt               # Position sizing, drawdown limits
│
└── usecase/
    ├── GetPortfolioUseCase.kt       # Fetch current portfolio
    └── PlaceOrderUseCase.kt         # Validate + place order

data/
├── local/
│   ├── TradeFlowDatabase.kt         # Room database
│   ├── entity/
│   │   ├── CandleEntity.kt          # Historical candles (for indicators)
│   │   ├── OrderEntity.kt           # Order history (for taxes)
│   │   ├── PortfolioSnapshotEntity.kt # Portfolio over time
│   │   └── DecisionLogEntity.kt     # Engine decisions (for debugging)
│   └── dao/
│       ├── CandleDao.kt
│       ├── OrderDao.kt
│       ├── PortfolioDao.kt
│       └── DecisionLogDao.kt
│
└── security/
    └── SecureCredentialStore.kt     # EncryptedSharedPreferences wrapper
```

### Phase 1 - Coinbase Files

```
data/exchange/coinbase/
├── auth/
│   └── CoinbaseJwtGenerator.kt      # ES256 JWT signing
│
├── api/
│   ├── CoinbaseRepository.kt        # ExchangeRepository implementation
│   └── CoinbaseApiService.kt        # Ktor HTTP client wrapper
│
├── dto/
│   ├── CoinbaseOrderDto.kt          # API response models
│   ├── CoinbaseCandleDto.kt
│   ├── CoinbaseAccountDto.kt
│   └── CoinbaseProductDto.kt
│
├── mapper/
│   ├── OrderMapper.kt               # DTO → domain/Order
│   ├── CandleMapper.kt              # DTO → domain/Candle
│   └── PortfolioMapper.kt           # DTO → domain/Portfolio
│
└── websocket/
    ├── CoinbaseWebSocket.kt         # ExchangeWebSocket implementation
    └── CoinbaseWsMessage.kt         # WebSocket message models
```

### Phase 2 - UI Files

```
presentation/
├── theme/
│   ├── Theme.kt                     # Material 3 theme
│   ├── Color.kt                     # Color palette
│   └── Typography.kt                # Text styles
│
├── components/
│   ├── PriceDisplay.kt              # Large price with 24h change
│   ├── ModeIndicator.kt             # WAIT/DEFENSE/TREND/RANGE badge
│   ├── PortfolioCard.kt             # Value + drawdown
│   ├── OrderItem.kt                 # Order list item with cancel button
│   └── LoadingButton.kt             # Button with loading state
│
├── dashboard/
│   ├── DashboardScreen.kt           # Compose UI
│   ├── DashboardViewModel.kt        # State management
│   └── DashboardUiState.kt          # UI state model
│
└── settings/
    ├── SettingsScreen.kt            # Compose UI
    ├── SettingsViewModel.kt         # State management
    └── SettingsUiState.kt           # UI state model
```

### Phase 3 - Service Files

```
service/trading/
├── TradingService.kt                # Foreground service
├── TradingServiceController.kt      # Start/stop interface
├── TradingServiceState.kt           # Service state model
│
├── loop/
│   ├── PriceMonitor.kt              # WebSocket price subscription
│   ├── StrategyLoop.kt              # Every 15 min, check candles
│   ├── RiskMonitor.kt               # Continuous drawdown check
│   └── OrderReconciler.kt           # Sync DB with exchange
│
├── execution/
│   ├── DefenseExecutor.kt           # Liquidate positions
│   ├── TrendExecutor.kt             # Trend-following logic
│   └── RangeExecutor.kt             # Range-trading logic (future)
│
└── notification/
    └── TradingNotificationManager.kt # Foreground service notification
```

### DI Files

```
di/
├── AppModule.kt                     # Application-level dependencies
├── DatabaseModule.kt                # Room database
├── DomainModule.kt                  # Decision engine, risk manager
└── ExchangeModule.kt                # Binds CoinbaseRepository → ExchangeRepository
                                     # ★ ONLY place that knows about Coinbase
```

---

## Quality Gates

### Phase 0 Exit Criteria
- [ ] Multi-module architecture set up (7 modules)
- [ ] `:core:domain` has ZERO Android dependencies (verified in build.gradle.kts)
- [ ] All domain models compile with ZERO Android imports
- [ ] All interfaces defined with clear javadoc contracts
- [ ] Room database creates tables on app launch
- [ ] Can encrypt/decrypt credentials with Android Keystore
- [ ] Decision engine passes 20+ unit tests (pure JVM tests, no emulator)
- [ ] Risk manager rejects invalid position sizes

### Phase 1 Exit Criteria
- [ ] JWT tokens validate against Coinbase API
- [ ] Can fetch 350 candles for BTC-USD
- [ ] WebSocket receives 100+ consecutive ticker updates
- [ ] Can place real $10 order and cancel it
- [ ] Bracket orders correctly map (limit_price = TP)
- [ ] WebSocket reconnects after network drop

### Phase 2 Exit Criteria
- [ ] Dashboard shows real-time price updates
- [ ] Can navigate to Settings and back
- [ ] Settings validates credentials before saving
- [ ] Emergency stop button cancels all orders
- [ ] UI renders correctly in dark mode
- [ ] No memory leaks in ViewModel

### Phase 3 Exit Criteria
- [ ] Service runs 24 hours without crash
- [ ] Survives Doze mode (battery optimization)
- [ ] Correctly identifies trend vs range from candles
- [ ] Places bracket order with 2x ATR stop loss
- [ ] Emergency liquidation at 15% drawdown
- [ ] All trades logged with timestamps

### Phase 4 Exit Criteria (MVP)
- [ ] All integration tests pass on real Coinbase API
- [ ] Decision engine matches backtest expectations
- [ ] No crashes in 48-hour soak test
- [ ] Drawdown limit enforced (15%)
- [ ] Kill switch accessible within 2 seconds
- [ ] Complete trade log for tax purposes

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Android kills service** | High | Foreground service + battery exemption + START_STICKY |
| **WebSocket disconnects** | High | Heartbeat monitoring + exponential backoff reconnect |
| **Coinbase API rate limits** | Medium | Request throttling (30/sec max) |
| **JWT token expires** | Medium | Refresh every 2 minutes automatically |
| **Bracket order mapping wrong** | High | Integration tests with small real orders |
| **Drawdown limit not enforced** | Critical | Unit tests + live testing validation |
| **Fee calculation error** | High | Include 0.5% fee in all P&L calculations |

### Financial Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Strategy loses money** | High | Backtest first, paper trade, then small live amounts |
| **Flash crash liquidation** | Critical | Emergency stop at 15% drawdown (NOT 50%) |
| **Tax reporting incomplete** | High | Log every trade to database with timestamps |
| **Overtrading (fees eat profit)** | High | Minimum 1.5% grid spacing for fee break-even |
| **Position size too large** | Critical | 5% max per trade enforced by RiskManager |

### Operational Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Phone runs out of battery** | High | 24/7 charger + battery health monitoring |
| **No internet connection** | High | Service pauses, resumes on reconnect |
| **App crashes overnight** | Critical | Firebase Crashlytics + foreground service auto-restart |
| **Credentials leaked** | Critical | EncryptedSharedPreferences + never log keys |
| **Can't access phone to stop** | Critical | Remote kill switch via Firebase Remote Config (future) |

---

## Success Metrics (MVP)

### Technical Metrics
- ✅ Zero crashes in 48-hour soak test
- ✅ 100% uptime (service restarts after crash)
- ✅ <100ms latency for emergency stop
- ✅ WebSocket connection stability >99%
- ✅ All trades logged to database

### Trading Metrics (First Month Live)
- 🎯 Positive net P&L (fees included)
- 🎯 Max drawdown <15% (trigger = emergency stop)
- 🎯 Win rate >40% (with 2:1 R:R)
- 🎯 Average trade duration ~12 hours (H4 timeframe)
- 🎯 Slippage <0.1% on limit orders

### Definition of Done (MVP)
- [ ] Service runs 48 hours without manual intervention
- [ ] Dashboard shows real-time price + mode + P&L
- [ ] Can start/stop/emergency-stop from UI
- [ ] All orders have stop loss + take profit
- [ ] Drawdown limit enforced automatically
- [ ] Complete trade log for tax reporting
- [ ] Integration tests validate Coinbase API
- [ ] Code reviewed and documented

---

## Next Steps

### Immediate Actions (This Week)

1. **Verify Build:** ✅ `./gradlew assembleDebug` passes (BUILD SUCCESSFUL)
2. **Review This Plan:** Read through, validate assumptions, suggest changes
3. **Rename Tickets:** Move from Notion IDs to numbered format (01, 02, 03...)
4. **Archive Superseded:** Move superseded tickets to `docs/tickets/archived/`
5. **Consolidate Duplicates:** Merge similar tickets into canonical versions

### Phase 0 Start (Next Week)

1. Create `domain/model/` package
2. Implement 6 domain models (Candle, Order, Portfolio, etc.)
3. Write unit tests for domain models
4. Define repository interfaces with clear contracts
5. PR review + merge to main

### Developer Notes

- **Use TodoWrite tool** during implementation for tracking
- **Run local build** before pushing (Desktop only)
- **Update this plan** as you complete phases
- **All trades logged** - this is a tax requirement
- **Emergency stop always accessible** - safety first

---

## Appendix

### Coinbase Advanced Trade API Reference

**REST Base URL:** `https://api.coinbase.com/api/v3/brokerage/`
**WebSocket Market:** `wss://advanced-trade-ws.coinbase.com`
**WebSocket User:** `wss://advanced-trade-ws-user.coinbase.com`

**Key Endpoints:**
- GET `/accounts` - List accounts
- GET `/accounts/{id}` - Get account balance
- GET `/products/{product_id}/candles` - Historical candles
- POST `/orders` - Place order
- POST `/orders/batch_cancel` - Cancel orders
- GET `/orders/historical/batch` - Order history

**Key WebSocket Channels:**
- `heartbeats` - Keep-alive (REQUIRED every 60-90s)
- `ticker` - Real-time price updates
- `level2` - Order book
- `candles` - Candle updates
- `user` - Order status (authenticated)

### Trading Strategy (Regime-Switching)

**Indicators:**
- SMA(200) - Trend direction
- ADX(14) - Trend strength (>25 = trending)
- ATR(14) - Volatility (for stop loss sizing)

**Regimes:**
1. **TREND:** ADX >25, price far from SMA(200) → Follow trend
2. **RANGE:** ADX <20, price near SMA(200) → No trades (wait)
3. **DEFENSE:** Drawdown >10% → Liquidate positions, stop trading

**Position Sizing:**
- 5% of portfolio per trade (enforced by RiskManager)
- Stop Loss: 2x ATR from entry
- Take Profit: 4x ATR from entry (2:1 R:R)

**Hysteresis:**
- Regime change requires 3 consecutive candles (reduce whipsaws)
- Example: ADX crosses 25 → wait for 3 candles >25 before switching to TREND

### Development Resources

- **Coinbase API Docs:** https://docs.cdp.coinbase.com/advanced-trade/docs
- **ta4j Documentation:** https://github.com/ta4j/ta4j
- **Jetpack Compose Docs:** https://developer.android.com/compose
- **Hilt Docs:** https://developer.android.com/training/dependency-injection/hilt-android
- **Room Docs:** https://developer.android.com/training/data-storage/room

---

**End of Master Plan**

*This document is the single source of truth for TradeFlow implementation.*
*Update it as the project evolves.*
