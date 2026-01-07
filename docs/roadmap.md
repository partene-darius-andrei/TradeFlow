# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (2/6 Complete)
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

🆕 NEW: Secure Credential Storage Complete
└── core/data/
    ├── security/
    │   └── SecureCredentialStore.kt ✅ EncryptedSharedPreferences impl
    └── di/
        └── SecurityModule.kt        ✅ Hilt DI for credential store
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

### 🎯 Phase 0A Progress: Authentication Infrastructure (2/6 Complete)

**✅ COMPLETE:**
- [x] **Ticket 02:** Repository Interfaces - All exchange contracts defined
- [x] **Ticket 04:** Secure Credential Store - EncryptedSharedPreferences with AES-256

**⚠️ IN PROGRESS:**
- [ ] **Ticket 01:** Domain Models - Basic domain types (Candle, Order, Decision, Portfolio)
- [ ] **Ticket 03:** Room Database - Entities, DAOs, database setup  
- [ ] **Ticket 07:** JWT Generator - ES256 token generation for Coinbase
- [ ] **Ticket 08:** REST API Client (Partial) - getAccounts() method only

### ❌ What DOESN'T Exist (Everything Else)

**No business logic has been implemented yet beyond interfaces and credential storage.**

```
❌ domain/model/             # No domain models yet
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
│   ├── repository/         # ✅ COMPLETE: ExchangeRepository interface
│   ├── strategy/           # DecisionEngine
│   └── risk/               # RiskManager
│
├── data/
│   ├── local/              # Room database
│   ├── security/           # ✅ COMPLETE: Credential storage
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

### Phase 0A: Authentication Infrastructure [CURRENT PHASE] (2/6 Complete)

**Goal:** Get foundation ready for Coinbase API authentication

**Strategy:** Build authentication infrastructure before implementing business logic

**Tickets:** 01, 02, 03, 04, 07, 08

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 01 | Domain Models | Not Started | **HIGH** | Small | Candle, Order, Portfolio, Ticker, Balance, Decision |
| 02 | Repository Interfaces | ✅ **COMPLETE** | **CRITICAL** | Medium | ExchangeRepository, ExchangeWebSocket, AuthTokenProvider |
| 03 | Room Database | Not Started | High | Medium | Database, Entities, DAOs |
| 04 | Credential Store | ✅ **COMPLETE** | High | Small | SecureCredentialStore (EncryptedSharedPreferences) |
| 07 | JWT Generator | Not Started | High | Medium | CoinbaseJwtGenerator (ES256 signing) |
| 08 | REST API Client | Not Started | High | Large | CoinbaseRepository (getAccounts only) |

**Latest Completion:** Secure Credential Store (Ticket 04) ✅ `SecureCredentialStore` implemented with AES-256-GCM encryption using Android Keystore, integrated with Hilt DI via `SecurityModule`.

**Next Up:** Domain Models (Ticket 01) - Define Candle, Order, Portfolio, Decision classes

### Phase 0B: Trading Logic (1 Week)

**Goal:** Implement core decision engine without external dependencies

**Tickets:** 05, 06

| # | Ticket | Status | Priority | Effort | Component |
|---|--------|--------|----------|--------|-----------|
| 05 | Decision Engine | Not Started | HIGH | Large | TradingDecisionEngine (SMA, ADX, ATR) |
| 06 | Risk Manager | Not Started | HIGH | Medium | TradingRiskManager (drawdown, limits) |

### Phase 1: Coinbase Integration (1 Week)

**Goal:** Complete Coinbase API integration

**Tickets:** 07-09

| # | Ticket | Status | Priority | Effort | Component |
|---|--------|--------|----------|--------|-----------|
| 07 | JWT Generator | Not Started | HIGH | Medium | CoinbaseJwtGenerator (ES256 signing) |
| 08 | REST API Client | Not Started | HIGH | Large | Complete CoinbaseRepository |
| 09 | WebSocket Client | Not Started | HIGH | Large | CoinbaseWebSocket (ticker + order updates) |

### Phase 2: Presentation Layer (1 Week)

**Goal:** Build UI that works with domain interfaces

**Tickets:** 10-15

| # | Ticket | Status | Priority | Effort | Component |
|---|--------|--------|----------|--------|-----------|
| 10 | Core UI Components | Not Started | Medium | Medium | Theme, shared composables |
| 11 | Dashboard Screen | Not Started | HIGH | Medium | DashboardScreen (UI only) |
| 12 | Dashboard ViewModel | Not Started | HIGH | Medium | DashboardViewModel (logic) |
| 13 | Settings Screen | Not Started | Medium | Medium | SettingsScreen (UI only) |
| 14 | Settings ViewModel | Not Started | Medium | Small | SettingsViewModel (logic) |
| 15 | App Navigation | Not Started | HIGH | Medium | NavGraph + main app setup |

### Phase 3: Trading Service (1 Week)

**Goal:** 24/7 background execution

**Tickets:** 16-17

| # | Ticket | Status | Priority | Effort | Component |
|---|--------|--------|----------|--------|-----------|
| 16 | Trading Service | Not Started | HIGH | Large | TradingService (foreground service) |
| 17 | Battery Optimization | Not Started | Medium | Small | Doze exemption, wake lock |

### Phase 4: Testing & Validation (1 Week)

**Goal:** Verify system works end-to-end

**Tickets:** 18-19

| # | Ticket | Status | Priority | Effort | Component |
|---|--------|--------|----------|--------|-----------|
| 18 | Integration Tests | Not Started | Medium | Medium | Real API tests with small trades |
| 19 | MVP Milestone | Not Started | HIGH | Small | System validation checklist |

---

## Quality Gates

### Phase 0A Exit Criteria (Current)

- [x] All repository interfaces defined ✅
- [x] Secure credential storage working ✅
- [ ] Domain models defined
- [ ] Room database schema created
- [ ] JWT tokens can be generated
- [ ] Can call getAccounts() successfully

### Phase 0B Exit Criteria

- [ ] Decision engine produces correct modes for test data
- [ ] Risk manager enforces limits
- [ ] All domain logic unit tested

### Phase 1 Exit Criteria

- [ ] Can place and cancel orders on Coinbase
- [ ] WebSocket receives price updates
- [ ] All error cases handled

### Phase 2 Exit Criteria

- [ ] UI shows real-time data
- [ ] Can start/stop service from UI
- [ ] Settings persist correctly

### Phase 3 Exit Criteria

- [ ] Service survives 8+ hours screen-off
- [ ] Emergency liquidation works
- [ ] Battery optimization configured

### Phase 4 Exit Criteria

- [ ] Integration tests pass
- [ ] Can run with real money safely
- [ ] All logs and monitoring working

---

## File Dependency Roadmap

### Phase 0A Files (Current)

```
✅ core/domain/auth/CredentialStore.kt
✅ core/domain/auth/AuthTokenProvider.kt  
✅ core/domain/repository/ExchangeRepository.kt
✅ core/domain/repository/BracketOrderRepository.kt
✅ core/domain/repository/ExchangeWebSocket.kt
✅ core/domain/error/ExchangeError.kt
✅ core/data/security/SecureCredentialStore.kt
✅ core/data/di/SecurityModule.kt
```

**Next: Domain Models (Ticket 01)**
```
□ domain/model/Candle.kt                 # Market data
□ domain/model/Order.kt                  # Order lifecycle  
□ domain/model/Portfolio.kt              # Account balances
□ domain/model/Decision.kt               # Strategy decisions
□ domain/model/Ticker.kt                 # Real-time price
□ domain/model/Balance.kt                # Account balance
```

### Dependency Chain

```
01 Domain Models
├── 03 Room Database (needs entities)
├── 05 Decision Engine (needs Decision, Candle)
└── 06 Risk Manager (needs Portfolio, Order)

02 Repository Interfaces ✅
├── 07 JWT Generator (needs AuthTokenProvider)
├── 08 REST API Client (needs ExchangeRepository)
└── 09 WebSocket Client (needs ExchangeWebSocket)

04 Credential Store ✅
├── 07 JWT Generator (needs credentials)
└── 13-14 Settings (needs to save/load credentials)
```

---

## Success Metrics

### Technical KPIs

- **Uptime:** Service runs 23+ hours/day
- **Latency:** Strategy decisions complete within 30 seconds
- **Reliability:** < 1 crash per 7 days
- **Security:** Zero credential leaks in logs

### Trading KPIs (Future)

- **Drawdown:** Never exceed 15% from high water mark
- **Fees:** Average < 0.4% per trade (maker rates)
- **Fill Rate:** > 95% of limit orders fill within 24h
- **Accuracy:** Mode detection correct > 80% of time

---

## Current Phase Focus

**Phase 0A Target:** Complete authentication infrastructure

**This Week:**
1. **Domain Models** (Ticket 01) - Define all core types
2. **Room Database** (Ticket 03) - Set up persistence layer
3. **JWT Generator** (Ticket 07) - Coinbase authentication

**Success Criteria:** Can authenticate with Coinbase API and store credentials securely

**Blocker Resolution:** Repository interfaces complete ✅, credential storage complete ✅

**Next Phase Preview:** Phase 0B will implement trading logic (decision engine + risk manager) without any external API dependencies.
