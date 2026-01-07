# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (4/6 Complete)
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

### 🎯 Phase 0A Progress: Authentication Infrastructure (4/6 Complete)

**✅ COMPLETE:**
- [x] **Ticket 02:** Repository Interfaces - All exchange contracts defined
- [x] **Ticket 04:** Secure Credential Store - EncryptedSharedPreferences with AES-256
- [x] **Ticket 07:** JWT Generator - ES256 token generation for Coinbase
- [x] **Authentication Pipeline:** Complete secure credential storage → JWT generation → API auth ready

**⚠️ IN PROGRESS:**
- [ ] **Ticket 01:** Domain Models - Basic domain types (Candle, Order, Decision, Portfolio)
- [ ] **Ticket 03:** Room Database - Entities, DAOs, database setup  
- [ ] **Ticket 08:** REST API Client - JWT ✅ complete, REST API methods next

### ❌ What DOESN'T Exist (Everything Else)

**No business logic has been implemented yet beyond interfaces, credential storage, and JWT generation.**

```
❌ domain/model/             # No domain models yet
❌ domain/usecase/           # No use cases
❌ domain/strategy/          # No decision engine
❌ domain/risk/              # No risk manager
❌ data/exchange/coinbase/   # No REST API methods (JWT only)
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
1. Implement `ExchangeRepository` in `data/exchange/kraken/`
2. Update `ExchangeModule.kt` DI bindings
3. **Zero changes** to domain, presentation, or service layers

---

## Implementation Roadmap

### Phase 0A: Authentication Infrastructure [CURRENT PHASE] (4/6 Complete)

**Goal:** Get foundation ready for Coinbase API authentication

**Strategy:** Build authentication infrastructure before implementing business logic

**Tickets:** 01, 02, 03, 04, 07, 08

**Status:**
- [x] **Ticket 02:** Repository Interfaces ✅ COMPLETE - ExchangeRepository, BracketOrderRepository, ExchangeWebSocket, AuthTokenProvider, CredentialStore all defined
- [x] **Ticket 04:** Secure Credential Store ✅ COMPLETE - SecureCredentialStore with AES-256-GCM encryption 
- [x] **Ticket 07:** JWT Generator ✅ COMPLETE - CoinbaseJwtGenerator with ES256 signing, nonce generation, proper URI formatting
- [ ] **Ticket 01:** Domain Models - Core domain types (Candle, Order, Portfolio, Decision)
- [ ] **Ticket 03:** Room Database - Database entities, DAOs, migrations
- [ ] **Ticket 08:** REST API Client - JWT generation done ✅, REST API methods pending

**Completion:** 4/6 (67%) - **Authentication pipeline fully functional**

---

### Phase 0B: Trading Logic Foundation [NEXT PHASE] (0/2 Complete)

**Goal:** Core trading decision engine and risk management

**Strategy:** Pure domain logic with no external dependencies

**Tickets:** 05, 06

**Status:**
- [ ] **Ticket 05:** Decision Engine - SMA(200), ADX(14), ATR(14) indicators with hysteresis logic
- [ ] **Ticket 06:** Risk Manager - Position sizing, drawdown limits, portfolio protection

**Prerequisites:** Domain models (Ticket 01) must be complete

**Completion:** 0/2 (0%) - **Waiting for Phase 0A completion**

---

### Phase 1: Coinbase Integration [FUTURE] (1/3 Complete)

**Goal:** Complete Coinbase Advanced Trade API integration

**Strategy:** Implement all repository interfaces for Coinbase

**Tickets:** 07, 08, 09

**Status:**
- [x] **Ticket 07:** JWT Generator ✅ COMPLETE - ES256 authentication tokens
- [ ] **Ticket 08:** REST API Client - Order placement, market data, account info (JWT portion complete)
- [ ] **Ticket 09:** WebSocket Client - Real-time price feeds, order updates

**Completion:** 1/3 (33%) - **Authentication ready, API methods pending**

---

### Phase 2: Presentation Layer [FUTURE] (0/6 Complete)

**Goal:** User interface for monitoring and configuration

**Strategy:** Clean MVVM with Compose UI, ViewModels use repository interfaces only

**Tickets:** 10, 11, 12, 13, 14, 15

**Status:**
- [ ] **Ticket 10:** Core UI Components - Theme, shared composables
- [ ] **Ticket 11:** Dashboard Screen - Trading status, portfolio, active orders  
- [ ] **Ticket 12:** Dashboard ViewModel - Business logic, state management
- [ ] **Ticket 13:** Settings Screen - Credential input, configuration
- [ ] **Ticket 14:** Settings ViewModel - Credential management logic
- [ ] **Ticket 15:** App Navigation - Navigation between screens

**Prerequisites:** Core domain logic (Phases 0A-1) must be complete

**Completion:** 0/6 (0%) - **Waiting for backend completion**

---

### Phase 3: Trading Service [FUTURE] (0/2 Complete)

**Goal:** 24/7 background trading execution

**Strategy:** Android foreground service with battery optimization

**Tickets:** 16, 17

**Status:**
- [ ] **Ticket 16:** Trading Service - Foreground service with strategy loops
- [ ] **Ticket 17:** Battery Optimization - Doze survival, wake lock management

**Prerequisites:** Decision engine, risk manager, API clients must be complete

**Completion:** 0/2 (0%) - **Final implementation phase**

---

### Phase 4: Testing & Validation [FUTURE] (0/2 Complete)

**Goal:** Verify system works with real trading

**Strategy:** Integration tests, small live trades

**Tickets:** 18, 19

**Status:**
- [ ] **Ticket 18:** Integration Tests - Real API calls with small amounts
- [ ] **Ticket 19:** MVP Milestone - System ready for live trading

**Prerequisites:** All previous phases complete

**Completion:** 0/2 (0%) - **Validation and launch**

---

## Overall Progress

### Completion Summary

| Phase | Status | Progress | Key Deliverable |
|-------|--------|----------|----------------|
| **Phase 0A** | 🚀 CURRENT | **4/6 (67%)** | **Authentication Infrastructure** |
| **Phase 0B** | ⏳ NEXT | 0/2 (0%) | Trading Logic Foundation |
| **Phase 1** | ⏳ FUTURE | 1/3 (33%) | Coinbase Integration |
| **Phase 2** | ⏳ FUTURE | 0/6 (0%) | Presentation Layer |
| **Phase 3** | ⏳ FUTURE | 0/2 (0%) | Trading Service |
| **Phase 4** | ⏳ FUTURE | 0/2 (0%) | Testing & Validation |
| **TOTAL** | | **5/21 (24%)** | **MVP Trading Bot** |

### Latest Achievements ✅

**December 2026 - Authentication Infrastructure:**
- ✅ **Repository Interfaces:** Complete exchange abstraction layer
- ✅ **Secure Credential Store:** AES-256-GCM encrypted storage
- ✅ **JWT Generator:** ES256 tokens for Coinbase API authentication
- ✅ **Dependency Injection:** Clean separation with Hilt modules

### Critical Path Forward

**Immediate (Next 2 weeks):**
1. **Ticket 01:** Domain Models (Candle, Order, Portfolio, Decision) 
2. **Ticket 03:** Room Database (entities, DAOs, schema)
3. **Ticket 08:** Complete REST API Client (order placement, market data)

**Medium term (Weeks 3-4):**  
4. **Ticket 05:** Decision Engine (SMA/ADX/ATR with hysteresis)
5. **Ticket 06:** Risk Manager (position sizing, drawdown limits)

**Goal:** Phase 0A+0B complete by end of January 2026 = **Full backend ready for UI development**

---

## Quality Gates

### Phase 0A Exit Criteria (Current Phase)
- [x] Repository interfaces defined and documented ✅
- [x] Secure credential storage working ✅  
- [x] JWT token generation functional ✅
- [ ] Domain models complete and tested
- [ ] Room database schema implemented
- [ ] Basic REST API methods working (accounts, orders, market data)

**Progress:** 4/6 complete ✅ **Ready to finish Phase 0A**

### Phase 0B Exit Criteria (Next Phase)
- [ ] Decision engine correctly identifies all 4 modes (WAIT/DEFENSE/TREND/RANGE)
- [ ] Hysteresis prevents mode whipsawing (3-candle confirmation)
- [ ] Risk manager enforces position limits and drawdown protection
- [ ] All domain logic unit tested
- [ ] No Android dependencies in domain layer

### Phase 1 Exit Criteria
- [ ] Can place and cancel orders via Coinbase API
- [ ] Real-time price updates via WebSocket
- [ ] Order status updates flow through system
- [ ] Rate limiting handled correctly
- [ ] Integration tests pass with real API

### Phase 2 Exit Criteria
- [ ] Settings screen for credential input
- [ ] Dashboard shows trading status and portfolio
- [ ] Can start/stop trading service from UI
- [ ] All screens responsive and follow Material Design

### Phase 3 Exit Criteria
- [ ] Service runs 24/7 without intervention
- [ ] Survives device sleep and battery optimization
- [ ] Emergency stop works correctly
- [ ] Performance monitoring and alerting

### Phase 4 Exit Criteria (MVP)
- [ ] System places profitable trades over 1-week period
- [ ] Risk limits enforced (no trades exceed 5% position size)
- [ ] Drawdown limit triggers emergency stop
- [ ] All trades logged for tax reporting

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Probability | Mitigation |
|------|---------|-------------|------------|
| **Coinbase API changes** | High | Medium | Use interfaces, build adapters |
| **Android doze kills service** | High | High | **✅ Battery optimization handling planned** |
| **Strategy loses money** | High | High | **Small position sizes, strict risk limits** |
| **API rate limits** | Medium | Low | Implement exponential backoff |
| **Database corruption** | Medium | Low | Regular backups, migrations |

### Business Risks

| Risk | Impact | Probability | Mitigation |
|------|---------|-------------|------------|
| **Fees eat profits** | High | High | **Grid spacing > 1.5% minimum** |
| **Tax complexity** | Medium | High | **Complete trade logging** |
| **Regulatory changes** | High | Low | Monitor crypto regulations |
| **Exchange bankruptcy** | High | Very Low | Never leave large amounts on exchange |

### Current Risk Status
**LOW** - Building authentication foundation only, no trading yet. All high-impact risks are mitigated by design (small positions, strict limits, local data storage).

---

## Success Metrics

### Technical Metrics
- **Uptime:** >99% (service running 24/7)
- **Performance:** Strategy evaluation <30 seconds
- **Reliability:** <1 crash per week
- **Security:** Zero credential leaks or unauthorized access

### Trading Metrics  
- **Win Rate:** Target >60% (grid strategy should achieve this)
- **Sharpe Ratio:** Target >1.0 (risk-adjusted returns)
- **Max Drawdown:** <15% (hard limit, service stops)
- **Return:** Target >10% annually (after fees)

### Development Metrics
- **Test Coverage:** >80% for domain logic
- **Build Time:** <3 minutes for full rebuild
- **Documentation:** All interfaces and decisions documented

**Current Status:** Infrastructure complete ✅ Ready to build trading logic on solid foundation.
