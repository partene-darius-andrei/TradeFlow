# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (3/6 Complete)
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

🆕 NEW: Complete Authentication Infrastructure
└── core/data/
    ├── security/
    │   └── SecureCredentialStore.kt ✅ EncryptedSharedPreferences impl
    └── di/
        └── SecurityModule.kt        ✅ Hilt DI for credential store

🆕 NEW: Coinbase JWT Authentication  
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

### 🎯 Phase 0A Progress: Authentication Infrastructure (3/6 Complete)

**✅ COMPLETE:**
- [x] **Ticket 02:** Repository Interfaces - All exchange contracts defined
- [x] **Ticket 04:** Secure Credential Store - EncryptedSharedPreferences with AES-256
- [x] **Ticket 07:** JWT Generator - ES256 token generation for Coinbase

**⚠️ IN PROGRESS:**
- [ ] **Ticket 01:** Domain Models - Basic domain types (Candle, Order, Decision, Portfolio)
- [ ] **Ticket 03:** Room Database - Entities, DAOs, database setup  
- [ ] **Ticket 08:** REST API Client (Partial) - JWT ✅ complete, REST methods next

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

### Phase 0A: Authentication Infrastructure [CURRENT PHASE] (3/6 Complete)

**Goal:** Get foundation ready for Coinbase API authentication

**Strategy:** Build authentication infrastructure before implementing business logic

**Tickets:** 01, 02, 03, 04, 07, 08

| # | Ticket | Status | Priority | Effort | Files Created |
|---|--------|--------|----------|--------|---------------|
| 01 | Domain Models | Not Started | **HIGH** | Small | Candle, Order, Portfolio, Ticker, Balance, Decision |
| 02 | Repository Interfaces | ✅ **COMPLETE** | **CRITICAL** | Medium | ExchangeRepository, ExchangeWebSocket, AuthTokenProvider |
| 03 | Room Database | Not Started | High | Medium | Database, Entities, DAOs |
| 04 | Credential Store | ✅ **COMPLETE** | High | Small | SecureCredentialStore (EncryptedSharedPreferences) |
| 07 | JWT Generator | ✅ **COMPLETE** | High | Medium | CoinbaseJwtGenerator (ES256 signing) |
| 08 | REST API Client | ⚠️ **PARTIAL** | High | Large | JWT ✅ complete, REST methods pending |

**Latest Completion:** JWT Generator (Ticket 07) ✅ - Implemented `CoinbaseJwtGenerator` with ES256 signing algorithm. Generates both REST API tokens (with URI claim) and WebSocket tokens. Uses secure nonce generation and 2-minute token expiry as required by Coinbase API.

**Success Criteria (3/6 ✅):**
- [x] All repository interfaces defined ✅
- [x] Credentials encrypted at rest ✅
- [x] JWT tokens generated correctly ✅
- [ ] Domain models defined
- [ ] Room database schema ready
- [ ] Can authenticate against Coinbase API

**Files Added This Phase:**
```
core/domain/auth/
├── AuthTokenProvider.kt        ✅ (Ticket 02)
└── CredentialStore.kt          ✅ (Ticket 02)

core/domain/repository/
├── ExchangeRepository.kt       ✅ (Ticket 02)
├── BracketOrderRepository.kt   ✅ (Ticket 02)
└── ExchangeWebSocket.kt        ✅ (Ticket 02)

core/domain/error/
└── ExchangeError.kt            ✅ (Ticket 02)

core/data/security/
└── SecureCredentialStore.kt    ✅ (Ticket 04)

core/data/di/
└── SecurityModule.kt           ✅ (Ticket 04)

exchange/coinbase/auth/
└── CoinbaseJwtGenerator.kt     ✅ (Ticket 07)

exchange/coinbase/di/
└── AuthModule.kt               ✅ (Ticket 07)
```

**Next Ticket:** Domain Models (Ticket 01) - Define basic domain types: Candle, Order, Portfolio, Ticker, Balance, Decision

---

### Phase 0B: Core Domain & Database (Week 2)

**Goal:** Complete foundation with domain models and data persistence

**Tickets:** Continue 01, 03, plus domain logic

| # | Ticket | Description | Effort | Depends On |
|---|--------|-------------|--------|------------|
| 01 | **Domain Models** | Candle, Order, Portfolio, Decision types | Small | None |
| 03 | **Room Database** | Entities, DAOs, migrations | Medium | Ticket 01 |
| 05 | **Decision Engine** | SMA/ADX/ATR regime detection | Large | Ticket 01 |
| 06 | **Risk Manager** | Position sizing, drawdown limits | Medium | Ticket 01 |

**Success Criteria:**
- [ ] All domain types defined (exchange-agnostic)
- [ ] Room database persists orders, portfolio snapshots
- [ ] Decision engine identifies DEFENSE/TREND/RANGE/WAIT modes
- [ ] Risk manager calculates position sizes and drawdown

---

### Phase 1: Coinbase Integration (Weeks 3-4)

**Goal:** Complete Coinbase Advanced Trade API integration

| # | Ticket | Description | Effort | Depends On |
|---|--------|-------------|--------|------------|
| 08 | **REST API Client** | Complete order placement, market data | Large | Ticket 07 ✅ |
| 09 | **WebSocket Client** | Real-time price feeds, order updates | Large | Ticket 07 ✅ |

**Success Criteria:**
- [ ] Can place/cancel orders on Coinbase
- [ ] Can fetch OHLCV candles
- [ ] Real-time price updates via WebSocket
- [ ] Order status updates via WebSocket

---

### Phase 2: Presentation Layer (Week 5)

**Goal:** Build minimal UI for monitoring and control

| # | Ticket | Description | Effort | Depends On |
|---|--------|-------------|--------|------------|
| 10 | **UI Components** | Theme, shared components | Medium | None |
| 11 | **Dashboard Screen** | Price, portfolio, orders display | Medium | Ticket 10 |
| 12 | **Dashboard ViewModel** | Business logic, state management | Medium | Phase 1 |
| 13 | **Settings Screen** | API credentials entry | Medium | Ticket 10 |
| 14 | **Settings ViewModel** | Credential validation, testing | Small | Ticket 04 ✅ |
| 15 | **Navigation** | App structure, routing | Medium | All screens |

**Success Criteria:**
- [ ] Can enter and save API credentials
- [ ] Dashboard shows real-time trading status
- [ ] Can start/stop trading service from UI
- [ ] Emergency stop button works

---

### Phase 3: Trading Service (Week 6)

**Goal:** 24/7 autonomous trading execution

| # | Ticket | Description | Effort | Depends On |
|---|--------|-------------|--------|------------|
| 16 | **Trading Service** | Foreground service, strategy loops | Large | Phase 1 + Phase 0B |
| 17 | **Battery Optimization** | Doze survival, wake locks | Small | Ticket 16 |

**Success Criteria:**
- [ ] Service runs strategy evaluation every 15 minutes
- [ ] Places orders based on regime detection
- [ ] Survives device sleep and Doze mode
- [ ] Emergency liquidation at 15% drawdown

---

### Phase 4: Testing & MVP (Week 7)

**Goal:** Production-ready system validation

| # | Ticket | Description | Effort | Depends On |
|---|--------|-------------|--------|------------|
| 18 | **Integration Tests** | Real API testing with small trades | Medium | Phase 1 |
| 19 | **MVP Milestone** | End-to-end system verification | Small | All phases |

**Success Criteria:**
- [ ] Can place/cancel real orders (small amounts)
- [ ] 24-hour stability test passes
- [ ] All edge cases handled (network loss, API errors)
- [ ] Ready for small-scale live testing

---

## Quality Gates

### Phase Gate Requirements

**Phase 0A → 0B:** Authentication infrastructure complete
- [x] Repository interfaces defined ✅
- [x] Secure credential storage working ✅  
- [x] JWT generation producing valid tokens ✅
- [ ] Domain models defined
- [ ] Room database schema ready

**Phase 0B → 1:** Domain foundation complete
- [ ] All domain types defined
- [ ] Room database functional
- [ ] Decision engine logic working
- [ ] Risk management rules implemented

**Phase 1 → 2:** API integration working
- [ ] Can authenticate with Coinbase API
- [ ] Can place and cancel orders
- [ ] WebSocket receiving real-time data
- [ ] Error handling robust

**Phase 2 → 3:** UI operational
- [ ] Can configure credentials via UI
- [ ] Dashboard shows system status
- [ ] Manual controls work (start/stop/emergency)

**Phase 3 → 4:** Service running autonomously
- [ ] Strategy loop executing every 15 minutes
- [ ] Orders placed based on market regime
- [ ] Risk limits enforced
- [ ] Survives overnight without intervention

**Phase 4 → Production:** System validated
- [ ] Small real trades successful
- [ ] No unexpected behavior in 24h test
- [ ] Emergency stops working correctly
- [ ] Ready to increase position sizes

---

## Risk Management

### Technical Risks

| Risk | Mitigation | Status |
|------|------------|---------|
| **API changes** | Use interface abstraction | ✅ Implemented |
| **Network failures** | Retry logic + offline mode | ⚠️ Planned |
| **Service killed** | Battery exemption + watchdog | ⚠️ Planned |
| **Data corruption** | Room transactions + backups | ⚠️ Planned |

### Financial Risks

| Risk | Mitigation | Status |
|------|------------|---------|
| **Drawdown spiral** | Hard 15% stop loss | ⚠️ Planned |
| **Fee erosion** | Min 1.5% grid spacing | ⚠️ Planned |
| **Flash crash** | Position size limits | ⚠️ Planned |
| **API errors** | Order reconciliation | ⚠️ Planned |

### Operational Risks

| Risk | Mitigation | Status |
|------|------------|---------|
| **Credential theft** | AES-256 encryption | ✅ Implemented |
| **Device failure** | Cloud backup + monitoring | ⚠️ Planned |
| **User error** | Confirmation dialogs | ⚠️ Planned |
| **Tax compliance** | Complete transaction logs | ⚠️ Planned |

---

## Current Work

### Active Development

**Current Focus:** Domain Models (Ticket 01)
**Priority:** Define Candle, Order, Portfolio, Ticker, Balance, Decision types
**Goal:** Enable Room database schema design and decision engine implementation

### Ready to Start

1. **Ticket 01 - Domain Models** 
   - No blockers
   - Small effort
   - Critical for database design

2. **Ticket 03 - Room Database**
   - Blocked by: Ticket 01
   - Medium effort  
   - Needed for persistence

### Completed Recently

**Ticket 07 - JWT Generator (2026-01-07):**
- ✅ ES256 algorithm implementation
- ✅ 2-minute token expiry
- ✅ Nonce generation (secure random)
- ✅ REST vs WebSocket token differentiation
- ✅ URI claim formatting for REST
- ✅ Hilt dependency injection setup

**Key implementation details:**
- Uses `nimbus-jose-jwt` library for ES256 signing
- Implements `AuthTokenProvider` interface from domain layer
- Generates tokens on-demand (no caching due to 2-minute expiry)
- Proper error handling with `ExchangeError.AuthenticationFailed`
- Clean separation: only depends on domain contracts

### Upcoming Priorities

1. **Week 2:** Complete Phase 0A (Domain Models + Room Database)
2. **Week 3:** Begin Coinbase REST API implementation  
3. **Week 4:** Add WebSocket real-time data
4. **Week 5:** Build minimal UI for monitoring
5. **Week 6:** Implement autonomous trading service
6. **Week 7:** Validate with small real trades

---

## Development Workflow

### Daily Development Process

1. **Read current ticket** from `docs/tickets/refined/`
2. **Implement feature** following architecture principles
3. **Push to `claude/feature-name` branch** 
4. **Wait for CI build** (automatic via GitHub Actions)
5. **Check `.build-status`** file for SUCCESS/FAILURE
6. **Test APK** delivered via Firebase App Distribution
7. **Create PR** when feature complete

### Quality Checklist

**Before each commit:**
- [ ] Follows clean architecture (domain → data → presentation)
- [ ] No hardcoded API keys or secrets
- [ ] Proper error handling with Result types
- [ ] Hilt dependency injection configured
- [ ] Unit tests for business logic (when applicable)

**Before each PR:**
- [ ] CI build passes
- [ ] APK installs and launches
- [ ] No regression in existing functionality
- [ ] Documentation updated (if needed)

**Before each phase:**
- [ ] All phase tickets completed
- [ ] Integration test passes
- [ ] Quality gate requirements met
- [ ] Ready for next phase

