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

### Phase 0A: Authentication Infrastructure [CURRENT PHASE]

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
| 08 | REST API Client (Partial) | Not Started | High | Medium | CoinbaseRepository.getAccounts() only |

**Dependencies Added:** ✅ All dependencies already in build.gradle.kts

**Blockers:** None - ready to continue with domain models

**Success Criteria (Phase 0A):**
- [x] Repository interfaces define clean contracts ✅
- [x] Credential store securely saves/loads API keys ✅
- [ ] Domain models compile with NO Android imports
- [ ] Room database schema ready for persistence
- [ ] JWT tokens generate correctly (ES256)
- [ ] Can call `GET /accounts` and retrieve balances
- [ ] ✅ **MILESTONE: Can authenticate and see real Coinbase data**

**Progress: 2/6 Complete (33%)**

---

### Phase 0B: Trading Logic [DEFERRED]

**Goal:** Implement decision engine and risk management

**When:** After Phase 0A milestone is reached

**Tickets:** 05, 06

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 05 | Decision Engine | Not Started | High | Large | TradingDecisionEngine with ta4j indicators |
| 06 | Risk Manager | Not Started | High | Medium | RiskManager for position sizing & drawdown |

---

### Phase 1: Coinbase Integration

**Goal:** Complete REST API and WebSocket clients

**Tickets:** 09

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 09 | WebSocket Client | Not Started | High | Large | CoinbaseWebSocket for real-time data |

---

### Phase 2: Presentation Layer

**Goal:** Build UI for monitoring and control

**Tickets:** 10, 11, 12, 13, 14, 15

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 10 | Core UI Components | Not Started | Medium | Medium | Theme, shared components |
| 11 | Dashboard Screen | Not Started | High | Medium | Dashboard UI (pure Compose) |
| 12 | Dashboard ViewModel | Not Started | High | Medium | Dashboard business logic |
| 13 | Settings Screen | Not Started | Medium | Medium | Settings UI (pure Compose) |
| 14 | Settings ViewModel | Not Started | Medium | Small | Settings business logic |
| 15 | App Navigation | Not Started | High | Medium | Main app & Hilt modules |

---

### Phase 3: Trading Service

**Goal:** Background service for 24/7 operation

**Tickets:** 16, 17

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 16 | Trading Service | Not Started | High | Large | Foreground service orchestrator |
| 17 | Battery Optimization | Not Started | Medium | Small | Doze mode survival |

---

### Phase 4: Testing & MVP Validation

**Goal:** Verify system works end-to-end

**Tickets:** 18, 19

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 18 | Integration Tests | Not Started | Medium | Medium | Real API tests with small trades |
| 19 | MVP Milestone | Not Started | High | Small | End-to-end validation |

---

## Quality Gates

### Phase 0A Gate: Authentication Ready
- [ ] Can authenticate with Coinbase API
- [ ] Credentials stored securely 
- [ ] Domain contracts fully defined
- [ ] Database ready for order/portfolio tracking

### Phase 1 Gate: API Integration Complete  
- [ ] Can place and cancel all order types
- [ ] Real-time price updates via WebSocket
- [ ] Order status updates flow correctly

### Phase 2 Gate: UI Complete
- [ ] Can monitor trading status visually
- [ ] Can start/stop service from UI
- [ ] Settings screen for credentials

### Phase 3 Gate: Service Ready
- [ ] Service survives 24 hours unattended
- [ ] Battery optimization disabled
- [ ] Emergency stop works

### MVP Gate: Ready for Testing
- [ ] No crashes in 24-hour test
- [ ] Correct regime detection
- [ ] Orders placed at correct prices
- [ ] Drawdown limit enforced

**Current Status:** Working toward Phase 0A Gate
**Progress:** 2/6 Phase 0A tickets complete (Repository Interfaces ✅, Credential Store ✅)

---

## Next Actions

**Immediate Priority:** Complete Phase 0A - Authentication Infrastructure

**Next Ticket:** 01 - Domain Models
- Create Candle, Order, Portfolio, Decision types
- Pure Kotlin (no Android dependencies)  
- Use BigDecimal for all money values
- Location: `core/domain/src/main/kotlin/com/tradeflow/core/domain/model/`

**After Domain Models:** Tickets 03, 07, 08 can be done in parallel
- 03: Room database (depends on domain models)
- 07: JWT generator (uses credential store ✅)  
- 08: REST API client partial (depends on JWT generator)

**Blocked Until:** None - ready to proceed with domain models
