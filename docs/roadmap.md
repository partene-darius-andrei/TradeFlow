# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (4/6 Complete) + UI Foundation
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
- ✅ **material-icons-extended** ✅ ACTIVE (for ModeIndicator icons)
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

### 🎯 Phase 0A Progress: Authentication Infrastructure + UI Foundation (4/6 Complete + Bonus)

**✅ COMPLETE:**
- [x] **Ticket 02:** Repository Interfaces - All exchange contracts defined
- [x] **Ticket 04:** Secure Credential Store - EncryptedSharedPreferences with AES-256
- [x] **Ticket 07:** JWT Generator - ES256 token generation for Coinbase
- [x] **UI Foundation (Bonus):** ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard, BigDecimalExt
- [x] **Authentication Pipeline:** Complete secure credential storage → JWT generation → API auth ready

**⚠️ IN PROGRESS:**
- [ ] **Ticket 01:** Domain Models - Basic domain types (Candle, Order, Decision, Portfolio)
- [ ] **Ticket 03:** Room Database - Entities, DAOs, database setup  
- [ ] **Ticket 08:** REST API Client - JWT ✅ complete, REST API methods next

### ❌ What DOESN'T Exist (Everything Else)

**No business logic has been implemented yet beyond interfaces, credential storage, UI components, and JWT generation.**

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
1. Implement `ExchangeRepository` in `data/exchange/kraken/`
2. Update `ExchangeModule.kt` DI bindings
3. **Zero changes** to domain, presentation, or service layers

---

## Implementation Roadmap

### Phase 0A: Authentication Infrastructure + UI Foundation [CURRENT PHASE] (4/6 Complete + Bonus)

**Goal:** Get foundation ready for Coinbase API authentication + base UI components

**Strategy:** Build authentication infrastructure and UI foundation before implementing business logic

**✅ COMPLETE:**
- Repository Interfaces - All exchange operation contracts defined ✅
- Secure Credential Store - AES-256 encrypted storage for API credentials ✅  
- JWT Generator - ES256 token generation for Coinbase Advanced Trade API ✅
- UI Foundation Components - ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard ✅
- BigDecimal Extensions - Currency and percentage formatting utilities ✅
- Complete DI setup - SecurityModule, AuthModule binding implementations ✅

**📋 REMAINING (Week 1):**
- **Ticket 01: Domain Models** (1-2 days) - Define Candle, Order, Portfolio, Decision
- **Ticket 03: Room Database** (2-3 days) - OrderEntity, PortfolioEntity, DAOs with Flow support  
- **Ticket 08: REST API Client** (3-4 days) - Implement getCandles, getAccounts, placeOrder methods

**🎯 Phase 0A Success Criteria:**
- [x] JWT tokens generate correctly for Coinbase API ✅
- [x] Credentials stored securely with AES-256 encryption ✅
- [x] All repository interfaces defined for clean architecture ✅
- [x] UI components ready for dashboard and settings screens ✅
- [ ] Domain models support all trading operations
- [ ] Room database persists orders and portfolio snapshots
- [ ] Can call Coinbase REST endpoints with real data

**Estimated Time:** 6-10 days remaining

### Phase 0B: Trading Logic Foundation (Week 2)

**Goal:** Core decision-making and risk management

**📋 TICKETS:**
- **Ticket 05: Decision Engine** - SMA(200), ADX(14), ATR(14) with ta4j
- **Ticket 06: Risk Manager** - Position sizing, drawdown limits

**🎯 Success Criteria:**
- [ ] Engine detects DEFENSE/TREND/RANGE/WAIT modes correctly
- [ ] 3-candle hysteresis prevents rapid switching
- [ ] Grid spacing never below 1.5% (fee break-even)
- [ ] Risk manager enforces 15% drawdown limit

### Phase 1: Live Data Integration (Week 3)

**Goal:** Connect to Coinbase and show real portfolio data

**📋 TICKETS:**
- **Ticket 09: WebSocket Client** - Real-time price and order updates
- **UI Integration** - Wire ViewModels to real data

**🎯 Success Criteria:**
- [ ] Dashboard shows real portfolio balances
- [ ] Real-time BTC-USD price updates
- [ ] Order status updates via WebSocket
- [ ] Can place and cancel small test orders

### Phase 2: Background Service (Week 4)

**Goal:** 24/7 autonomous trading

**📋 TICKETS:**  
- **Ticket 16: Trading Service** - Foreground service with strategy loop
- **Ticket 17: Battery Optimization** - Wake locks, Doze exemption

**🎯 Success Criteria:**
- [ ] Service runs 24/7 without crashes
- [ ] Strategy evaluation every 15 minutes
- [ ] Survives device sleep and Doze mode
- [ ] Emergency liquidation at 15% drawdown

### Phase 3: User Interface (Week 5)

**Goal:** Production-ready app interface

**📋 TICKETS:**
- **Dashboard Screen** - Portfolio, mode, service controls, recent orders
- **Settings Screen** - Credentials, trading parameters, about
- **App Navigation** - Bottom nav, login flow

**🎯 Success Criteria:**
- [ ] Complete user journey from credential entry to trading
- [ ] Professional dark theme optimized for 24/7 monitoring
- [ ] Can start/stop service from UI
- [ ] Emergency stop with confirmation

### Phase 4: Testing & Validation (Week 6)

**Goal:** Verify system works end-to-end before real money

**📋 TICKETS:**
- **Integration Tests** - Test with small real trades ($10-20)
- **Strategy Backtesting** - Verify logic on historical data
- **MVP Milestone** - Ready for small-scale live trading

**🎯 Success Criteria:**
- [ ] No crashes during 24-hour test
- [ ] Correct mode detection and order placement
- [ ] Risk limits enforced properly
- [ ] Complete audit trail for tax reporting

---

## 📊 Progress Tracking

### Overall Progress: 4/19 tickets complete (21%) + UI Foundation

```
Phase 0A: ████████████░░░░░░░░ 67% (4/6 + UI bonus) ✅
Phase 0B: ░░░░░░░░░░░░░░░░░░░░  0% (0/2)
Phase 1:  ░░░░░░░░░░░░░░░░░░░░  0% (0/3) 
Phase 2:  ░░░░░░░░░░░░░░░░░░░░  0% (0/2)
Phase 3:  ░░░░░░░░░░░░░░░░░░░░  0% (0/3)
Phase 4:  ░░░░░░░░░░░░░░░░░░░░  0% (0/3)
```

### Current Sprint: Phase 0A - Foundation + UI

**✅ COMPLETED THIS SPRINT:**
- Repository Interfaces (Ticket 02) ✅ 
- Secure Credential Store (Ticket 04) ✅
- JWT Generator (Ticket 07) ✅
- UI Foundation Components (Bonus) ✅
  - ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard
  - BigDecimalExt with currency/percentage formatting
  - Material Icons Extended dependency integration

**🎯 NEXT UP:**
- Domain Models (Ticket 01) - Define core data types
- Room Database (Ticket 03) - Persistent storage schema
- REST API Client (Ticket 08) - Complete Coinbase integration

---

## 🎯 Why This Order Makes Sense

### 1. Foundation First (Tickets 01-08) ✅ 67% Complete
- **Repository interfaces** define contracts ✅
- **Credential storage** secures API keys ✅
- **JWT generation** enables API auth ✅  
- **UI components** ready for screens ✅
- **Domain models** define data structures (next)
- **Database** provides persistence (next)
- **REST client** connects to exchange (next)
- **Result:** Solid architectural base with working API integration

### 2. Core Logic Second (Tickets 09-11)
- **Decision engine** implements trading strategy
- **Risk manager** enforces safety limits
- **WebSocket client** provides real-time data
- **Result:** Smart decision-making brain with live data

### 3. Service Third (Tickets 12-13)  
- **Trading service** runs 24/7 in background
- **Battery optimization** survives Android power management
- **Result:** Autonomous trading execution

### 4. UI Fourth (Tickets 14-16)
- **Dashboard** shows status and controls
- **Settings** manages configuration
- **Navigation** ties app together
- **Result:** Professional user interface

### 5. Validation Fifth (Tickets 17-19)
- **Integration tests** verify API integration
- **Strategy backtesting** validates logic
- **MVP milestone** confirms readiness
- **Result:** Confidence to trade real money

---

## 🚀 Getting Started with Current Phase

**Current Focus:** Complete Phase 0A foundation

**Next Implementation Steps:**

1. **Domain Models (Ticket 01)** - 1-2 days
   ```kotlin
   data class Candle(timestamp, open, high, low, close, volume)
   data class Order(id, side, type, status, size, price, ...)
   sealed class Decision { Wait, Defense, Trend, Range }
   ```

2. **Room Database (Ticket 03)** - 2-3 days  
   ```kotlin
   @Entity OrderEntity, PortfolioSnapshotEntity
   @Dao OrderDao, PortfolioDao with Flow<List<T>>
   ```

3. **REST API Client (Ticket 08)** - 3-4 days
   ```kotlin
   // Use existing CoinbaseJwtGenerator ✅
   suspend fun getAccounts(): Result<List<Balance>>
   suspend fun getCandles(...): Result<List<Candle>>
   suspend fun placeOrder(...): Result<Order>
   ```

**Ready to implement:** All foundational pieces (auth, storage, UI components) complete. Focus on data models and API integration next.

