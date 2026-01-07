# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (5/6 Complete) + Login Screen
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
├── data/local/
│   ├── AppDatabase.kt           ✅ Empty Room DB
│   └── PlaceholderEntity.kt     ✅ Dummy entity
└── presentation/
    └── login/                   ✅ COMPLETE Login Screen
        ├── LoginScreen.kt       ✅ Credential entry UI with validation
        ├── LoginViewModel.kt    ✅ Form logic + secure storage integration
        └── LoginUiState.kt      ✅ UI state management

🆕 COMPLETE: Domain Layer Foundation
└── core/domain/                 ✅ Complete domain interfaces
    ├── auth/
    │   ├── AuthTokenProvider.kt ✅ Token generation interface
    │   └── CredentialStore.kt   ✅ Secure storage interface
    ├── error/
    │   └── ExchangeError.kt     ✅ Exchange error types (6 variants)
    └── repository/
        ├── BracketOrderRepository.kt ✅ Bracket order support
        ├── ExchangeRepository.kt     ✅ Core operations (12 methods)
        └── ExchangeWebSocket.kt      ✅ Real-time streams

🆕 COMPLETE: Data Layer Implementation  
└── core/data/
    ├── security/
    │   └── SecureCredentialStore.kt ✅ EncryptedSharedPreferences impl
    └── di/
        └── SecurityModule.kt        ✅ Hilt DI for credential store

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
- ✅ **security-crypto 1.1.0-alpha06** ✅ ACTIVE (for encrypted credentials)
- ✅ **work-runtime-ktx 2.10.0** (for background tasks)
- ✅ **datastore-preferences 1.1.1** (for settings)
- ✅ **material-icons-extended** ✅ ACTIVE (for ModeIndicator icons and login UI)
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

### 🎯 Phase 0A Progress: Authentication Infrastructure + Login Screen (5/6 Complete)

**✅ COMPLETE:**
- [x] **Ticket 02:** Repository Interfaces - All exchange contracts defined
- [x] **Ticket 04:** Secure Credential Store - EncryptedSharedPreferences with AES-256
- [x] **Ticket 07:** JWT Generator - ES256 token generation for Coinbase
- [x] **UI Foundation (Bonus):** ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard, BigDecimalExt
- [x] **Login Screen (Bonus):** Complete credential entry UI with validation and secure storage integration

**⚠️ IN PROGRESS:**
- [ ] **Ticket 01:** Domain Models - Basic domain types (Candle, Order, Decision, Portfolio)
- [ ] **Ticket 03:** Room Database - Entities, DAOs, database setup  
- [ ] **Ticket 08:** REST API Client - JWT ✅ complete, REST API methods next

### ❌ What DOESN'T Exist (Everything Else)

**No business logic has been implemented yet beyond interfaces, credential storage, UI components, JWT generation, and login screen.**

```
❌ domain/model/             # No domain models yet
❌ domain/usecase/           # No use cases
❌ domain/strategy/          # No decision engine
❌ domain/risk/              # No risk manager
❌ data/exchange/coinbase/   # No REST API methods (JWT only)
❌ data/repository/          # No repository implementations
❌ presentation/             # Only login screen, no dashboard/settings
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
│   ├── repository/         # ✅ COMPLETE: ExchangeRepository interface
│   ├── strategy/           # DecisionEngine
│   └── risk/               # RiskManager
│
├── data/
│   ├── local/              # Room database
│   ├── security/           # ✅ COMPLETE: Credential storage
│   └── exchange/
│       └── coinbase/       # Coinbase-specific implementation (ISOLATED)
│           ├── auth/       # ✅ COMPLETE: JWT generator
│           ├── api/        # REST client (pending)
│           ├── websocket/  # WebSocket client (pending)
│           ├── dto/        # Coinbase DTOs
│           └── mapper/     # DTO → domain model mappers
│
├── presentation/
│   ├── login/              # ✅ COMPLETE: Login screen + ViewModel
│   ├── dashboard/          # Dashboard screen + ViewModel
│   ├── settings/           # Settings screen + ViewModel
│   └── components/         # ✅ COMPLETE: Shared UI components
│
├── service/
│   └── trading/            # TradingService (foreground service)
│
└── di/                     # Hilt modules
    ├── AppModule.kt
    ├── DatabaseModule.kt
    ├── DomainModule.kt
    ├── SecurityModule.kt   # ✅ COMPLETE: Credential store binding
    └── ExchangeModule.kt   # ONLY place that knows about Coinbase
```

### Critical Rules

1. **Domain layer**: NO Android imports, NO Coinbase imports
2. **Presentation layer**: Imports ONLY `domain/repository/ExchangeRepository` interface
3. **Service layer**: Imports ONLY domain interfaces
4. **All Coinbase code** lives in `data/exchange/coinbase/`
5. **DI binds implementation to interface** in `ExchangeModule.kt`
6. **Never hardcode API keys** - EncryptedSharedPreferences only ✅ IMPLEMENTED
7. **Account for fees** in all calculations (0.25-0.60% at Coinbase)

### Why This Architecture?

**To swap Coinbase for Kraken:**
1. Create new `:exchange:kraken` module
2. Implement same domain interfaces
3. Change ONE line in `ExchangeModule.kt` DI binding
4. **Zero changes to domain, presentation, or service layers**

**This is only possible because features use interfaces, not Coinbase directly.**

---

## Phase-by-Phase Execution Plan

### ✅ Phase 0A: Foundation (CURRENT - 5/6 Complete + Login Screen)

**Status:** 83% complete + bonus login screen
**Goal:** Core architecture, secure credentials, and user authentication

| Ticket | Title | Status | Notes |
|--------|-------|--------|-------|
| **01** | Domain Models | ❌ **NEXT** | Candle, Order, Portfolio, Decision |
| **02** | Repository Interfaces | ✅ **DONE** | 6 interfaces: ExchangeRepository, WebSocket, Auth, etc. |
| **03** | Room Database | ❌ In Progress | Need entities + DAOs based on domain models |
| **04** | Secure Credential Store | ✅ **DONE** | AES-256 encrypted with Android Keystore |
| **05** | Decision Engine | ❌ Future | SMA + ADX indicators |
| **06** | Risk Manager | ❌ Future | Position sizing + drawdown limits |
| **07** | JWT Generator | ✅ **DONE** | ES256 signing for Coinbase auth |
| **Bonus** | UI Components | ✅ **DONE** | 5 reusable components + formatting |
| **Bonus** | Login Screen | ✅ **DONE** | Complete credential entry flow |

**Latest Achievement:** 
- ✅ **Login Screen Complete** - Full credential entry UI with form validation, secure storage integration, and proper state management
- ✅ **material-icons-extended** dependency active for UI icons (visibility toggle, mode indicators)

**Next Up:** Domain Models (Ticket 01) - Define Candle, Order, Portfolio, Decision data classes

**ETA:** 1-2 more commits to complete Phase 0A

---

### 🎯 Phase 0B: Core Trading Logic (Week 2)

**Goal:** Business rules and domain logic implementation

| Ticket | Title | Priority | Dependencies |
|--------|-------|----------|-------------|
| **05** | Decision Engine | HIGH | Domain Models (01) |
| **06** | Risk Manager | HIGH | Domain Models (01) |
| **08** | REST API Client | HIGH | JWT Generator (07) ✅ |
| **09** | WebSocket Client | MEDIUM | JWT Generator (07) ✅ |

**Deliverables:**
- ✅ SMA(200) + ADX(14) regime detection
- ✅ Position sizing and risk limits
- ✅ Complete Coinbase API integration
- ✅ Real-time price feeds

**ETA:** 4-6 commits

---

### 🎨 Phase 1: Presentation Layer (Week 3)

**Goal:** User interface to monitor and control the system

| Ticket | Title | Priority | Dependencies |
|--------|-------|----------|-------------|
| **10** | Core UI Theme | HIGH | None |
| **11** | Dashboard Screen | HIGH | Core UI (10), Login (✅) |
| **12** | Dashboard ViewModel | HIGH | REST API (08), Decision Engine (05) |
| **13** | Settings Screen | MEDIUM | Core UI (10) |
| **14** | Settings ViewModel | MEDIUM | Credential Store (04) ✅ |
| **15** | App Navigation | HIGH | All screens (10-14) |

**Deliverables:**
- ✅ Professional dark theme optimized for trading
- ✅ Dashboard showing portfolio, mode, orders, service status
- ✅ Settings for credentials and configuration
- ✅ Complete navigation flow

**ETA:** 6-8 commits

---

### ⚙️ Phase 2: Trading Service (Week 4)

**Goal:** 24/7 autonomous operation

| Ticket | Title | Priority | Dependencies |
|--------|-------|----------|-------------|
| **16** | Trading Service | HIGH | All core logic (05-09) |
| **17** | Battery Optimization | HIGH | Trading Service (16) |

**Deliverables:**
- ✅ Android foreground service with trading loops
- ✅ Survives device sleep and aggressive power management
- ✅ Emergency liquidation on 15% drawdown

**ETA:** 3-4 commits

---

### 🧪 Phase 3: Testing & Validation (Week 5)

**Goal:** Verify system works end-to-end

| Ticket | Title | Priority | Dependencies |
|--------|-------|----------|-------------|
| **18** | Integration Tests | MEDIUM | Complete system |
| **19** | MVP Milestone | HIGH | All features |

**Deliverables:**
- ✅ Unit tests for decision engine
- ✅ Integration tests with real Coinbase API
- ✅ 24-hour stability test
- ✅ Ready for small real money testing

**ETA:** 2-3 commits

---

## 📊 Progress Tracking

### Overall Progress: 5/19 tickets complete (26%) + Login Screen

```
Phase 0A: ████████████████░░░░ 83% (5/6) + Login ✅ ← YOU ARE HERE
Phase 0B: ░░░░░░░░░░░░░░░░░░░░   0% (0/4)
Phase 1:  ░░░░░░░░░░░░░░░░░░░░   0% (0/6)
Phase 2:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
Phase 3:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
```

### Current Sprint: Complete Phase 0A Foundation

**Next 3 Tickets:**
1. **Domain Models (Ticket 01)** - Data structures for Candle, Order, Portfolio, Decision
2. **Room Database (Ticket 03)** - Local persistence with entities and DAOs
3. **REST API Client (Ticket 08)** - Complete Coinbase integration (JWT ✅, add REST methods)

**Goal:** Foundation 100% complete, ready for trading logic implementation

---

## 🎯 Success Criteria by Phase

### Phase 0A Complete When:
- [x] All domain interfaces defined and documented
- [x] Credentials stored securely with AES-256 encryption
- [x] JWT tokens generated correctly for Coinbase API
- [x] UI components library established
- [x] Login screen allows credential entry and validation
- [ ] **Domain models defined** (Ticket 01 - NEXT)
- [ ] **Database schema implemented** (Ticket 03)
- [ ] **Basic REST API methods working** (Ticket 08)

### Phase 0B Complete When:
- [ ] Decision engine correctly identifies DEFENSE/TREND/RANGE modes
- [ ] Risk manager enforces position size and drawdown limits
- [ ] Complete Coinbase REST API integration working
- [ ] WebSocket provides real-time price updates

### Phase 1 Complete When:
- [ ] Professional trading-focused UI theme
- [ ] Dashboard displays live portfolio, trading mode, and orders
- [ ] Settings screen manages credentials and preferences
- [ ] Navigation flows work end-to-end

### Phase 2 Complete When:
- [ ] Android service runs 24/7 without battery killing it
- [ ] Trading loops execute every 15 minutes
- [ ] Emergency stop triggers at 15% drawdown
- [ ] All orders placed and cancelled correctly

### Phase 3 Complete When:
- [ ] Decision engine unit tests pass with various market scenarios
- [ ] Integration tests work with real Coinbase API (small trades)
- [ ] 24-hour stability test completes successfully
- [ ] Ready for real money testing with $100-500

---

## 🚀 Getting Started

**Current Status:** Phase 0A foundation nearly complete + Login screen ready

**Next Actions:**
1. **Domain Models (Ticket 01)** - Define core data structures in `:core:domain`
2. **Room Database (Ticket 03)** - Create entities and DAOs for persistence
3. **REST API Methods (Ticket 08)** - Add order placement, market data, account endpoints

**Development Pattern:**
1. Push changes → GitHub Actions builds
2. `git pull` to get build status and doc updates  
3. `cat .build-status` to verify SUCCESS/FAILURE
4. Iterate based on feedback

**Key Files:**
- `docs/reference.md` - Implementation blueprint with code examples
- `docs/tickets/refined/` - Detailed requirements for each ticket
- `CLAUDE.md` - This file (project context)

**Architecture Ready:** Clean separation, secure by default, exchange-agnostic contracts, complete login flow. Foundation is solid for building trading features.
