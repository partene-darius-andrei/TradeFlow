# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-08
**Project Status:** Phase 1 In Progress - Coinbase Integration
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
├── presentation/dashboard/
│   ├── DashboardScreen.kt       🆕 ✅ Main screen with real data integration
│   ├── DashboardViewModel.kt    🆕 ✅ State management + API integration
│   └── components/              ✅ PortfolioCard, ModeCard, ServiceCard, OrdersList
├── di/
│   ├── AppModule.kt             ✅ Empty Hilt module
│   ├── DatabaseModule.kt        ✅ Provides Room database
│   ├── NetworkModule.kt         ✅ Provides Ktor HttpClient (OkHttp engine)
│   └── CredentialsModule.kt     ✅ Provides build-injected credentials
├── data/local/
│   ├── AppDatabase.kt           ✅ Empty Room DB
│   └── PlaceholderEntity.kt     ✅ Dummy entity
└── navigation/
    ├── AppNavHost.kt            ✅ Simplified navigation (no login)
    └── Screen.kt                ✅ Dashboard + Settings routes only

🆕 COMPLETE: Domain Layer Foundation
└── core/domain/                 ✅ Complete domain layer
    ├── auth/
    │   ├── AuthTokenProvider.kt ✅ Token generation interface
    │   └── CredentialStore.kt   ✅ Secure storage interface
    ├── error/
    │   └── ExchangeError.kt     ✅ Exchange error types (6 variants)
    ├── model/                   ✅ Domain models (Ticket 01)
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
    │   └── StaticCredentialStore.kt ✅ Build-time credential injection
    ├── local/                       ✅ Room database (Ticket 03)
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

🆕 COMPLETE: Coinbase API Integration (Partial)
└── exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt  ✅ ES256 JWT token generation
    ├── api/
    │   └── CoinbaseApiClient.kt     🆕 ✅ Ktor-based API client (accounts endpoint)
    ├── dto/
    │   └── AccountDto.kt            🆕 ✅ Account DTOs for API responses  
    ├── mapper/
    │   └── AccountMapper.kt         🆕 ✅ DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt    🆕 ✅ Partial implementation (getBalances)
    └── di/
        ├── AuthModule.kt            ✅ Hilt DI for JWT provider
        └── ExchangeModule.kt        🆕 ✅ Repository DI binding

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

### 🆕 MAJOR MILESTONE: Real Data Integration

**Dashboard now displays real Coinbase account balances!**

✅ **DashboardViewModel** - Complete state management with API integration
✅ **CoinbaseApiClient** - Working Ktor client with JWT authentication
✅ **Error handling** - Loading, error states with retry functionality
✅ **Repository pattern** - Clean interface separation (can swap exchanges)

This represents the first successful connection to live Coinbase data - a critical foundation milestone.

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
- **Better security:** Credentials never stored in device storage  
- **Easier testing:** Credentials injected via CI/CD pipeline
- **Faster development:** No login flow to maintain

### 🎯 Phase 0B Progress: UI Foundation (COMPLETE ✅)

**✅ COMPLETE:**
- [x] **Ticket 05:** UI Design Overview - Complete design system specification
- [x] **Ticket 06:** Core UI Theme - Material 3 theme with trading colors
- [x] **Ticket 07-UI:** Base Components - All reusable components implemented
- [x] **Ticket 09:** App Navigation - Bottom nav between Dashboard + Settings
- [x] **Ticket 10:** Dashboard Screen - Complete UI with mock data
- [x] **App Branding:** Adaptive icon with trading chart design

**Key UI Components Implemented:**
```
✅ StatusCard - Reusable card container
✅ LoadingButton - Button with loading spinner
✅ PriceDisplay - Price with +/- color coding
✅ ModeIndicator - Trading mode badges (DEFENSE/TREND/RANGE)
✅ ErrorDisplay - Error state with retry button
✅ PortfolioCard - Portfolio display with real data
✅ ModeCard - Trading mode display
✅ ServiceCard - Service controls
✅ OrdersList - Recent orders list
```

### 🎯 Phase 1 Progress: Coinbase Integration (25% COMPLETE)

**✅ COMPLETE:**
- [x] **Ticket 12:** Dashboard ViewModel - State management + API integration
- [x] **Partial Ticket 13:** REST API Client - getBalances endpoint working

**🟡 IN PROGRESS:**
- [🟡] **Ticket 13:** REST API Client - Need full implementation (market data, orders)

**❌ PENDING:**
- [ ] **Ticket 14:** WebSocket Client - Real-time data streams
- [ ] **Ticket 11:** Settings Screen - Configuration UI

**Current Coinbase Implementation:**
```
✅ CoinbaseJwtGenerator - ES256 JWT signing with proper nonce
✅ CoinbaseApiClient - Ktor HTTP client with authentication
✅ AccountDto + AccountMapper - Proper DTO to domain mapping
✅ CoinbaseRepository - Implements ExchangeRepository (getBalances)
✅ ExchangeModule - Hilt DI binding
✅ Dashboard shows real account balances from API
```

---

## 📊 Progress Tracking

### Overall Progress: 12/20 tickets complete (60%)

```
Phase 0A: ████████████████████ 100% (6/6) ✅ COMPLETE
Phase 0B: ████████████████████ 100% (5/5) ✅ COMPLETE  
Phase 1:  ████████░░░░░░░░░░░░  25% (1/4) 🟡 IN PROGRESS
Phase 2:  ░░░░░░░░░░░░░░░░░░░░   0% (0/3) ← NEXT
Phase 3:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
```

### Current Sprint: Phase 1 - Coinbase Integration

**Recently Completed:**
- ✅ **Ticket 12:** Dashboard ViewModel - Real API integration working!
- ✅ **Partial Ticket 13:** getBalances API call working

**Currently Working On:**
- 🟡 **Complete Ticket 13:** Full REST API client implementation

**Next Up:**
- **Ticket 14:** WebSocket client for real-time data
- **Ticket 11:** Settings screen implementation

---

## 🚀 Next Immediate Steps

### 1. Complete REST API Client (Ticket 13)
**Goal:** Full Coinbase API integration beyond just account balances

**Remaining work:**
```kotlin
// Need to implement in CoinbaseRepository:
suspend fun getCandles(productId: String, granularity: Granularity, limit: Int): Result<List<Candle>>
suspend fun getCurrentPrice(productId: String): Result<Ticker>
suspend fun placeMarketOrder(...): Result<Order>
suspend fun placeLimitOrder(...): Result<Order>
suspend fun placeBracketOrder(...): Result<Order>  // For trend mode
suspend fun cancelOrder(orderId: String): Result<Unit>
suspend fun getOpenOrders(productId: String): Result<List<Order>>
```

**API endpoints to implement:**
```
GET  /api/v3/brokerage/products/BTC-USD/candles
GET  /api/v3/brokerage/products/BTC-USD/ticker
POST /api/v3/brokerage/orders              # Order placement
POST /api/v3/brokerage/orders/batch_cancel # Cancel orders
GET  /api/v3/brokerage/orders/historical/batch # List orders
```

### 2. WebSocket Integration (Ticket 14)
**Goal:** Real-time data for Dashboard

**Implementation:**
```
wss://advanced-trade-ws.coinbase.com        # Market data
wss://advanced-trade-ws-user.coinbase.com   # User orders (auth)

Channels needed:
- heartbeats (keep-alive)
- ticker (real-time BTC price)
- user (order status updates)
```

### 3. Settings Screen (Ticket 11)
**Goal:** Configuration UI and app information

**Sections:**
- Trading parameters display (readonly initially)
- Notification preferences
- About section (version, logs, help)
- Future: Credential management when needed

---

## 🎯 Phase Definitions

### ✅ Phase 0A: Authentication Infrastructure (COMPLETE)
**Goal:** Secure credential management and API authentication
- [x] Repository interfaces
- [x] Static credential injection
- [x] JWT token generation  
- [x] Room database setup
- [x] Domain models

### ✅ Phase 0B: UI Foundation (COMPLETE) 
**Goal:** Professional UI with all screens and components
- [x] Design system and theme
- [x] Core UI components
- [x] Dashboard screen with real data
- [x] App navigation
- [x] App branding

### 🟡 Phase 1: Coinbase Integration (25% COMPLETE)
**Goal:** Complete API integration for live trading
- [x] Dashboard ViewModel ✅
- [🟡] REST API Client (partial)
- [ ] WebSocket Client
- [ ] Settings Screen

### Phase 2: Trading Logic (NEXT)
**Goal:** Decision engine and risk management
- [ ] Decision Engine (SMA, ADX, ATR + regime switching)
- [ ] Risk Manager (position sizing, stop-loss, drawdown)
- [ ] Backtesting framework

### Phase 3: Trading Service
**Goal:** 24/7 autonomous operation
- [ ] Foreground Service (strategy loop)
- [ ] Battery optimization

### Phase 4: Testing & Production
**Goal:** Validate system works reliably
- [ ] Integration tests
- [ ] Paper trading validation
- [ ] Live trading (small amounts)

---

## 🎯 Getting Started with Current Work

**Phase 1 is 25% complete** - Dashboard shows real Coinbase data! 

**Next Priority:** Complete REST API Client (Ticket 13)

**Implementation approach:**
1. Add remaining endpoints to CoinbaseApiClient
2. Create DTOs for candle, ticker, order responses  
3. Implement mappers from DTOs to domain models
4. Complete CoinbaseRepository implementation
5. Test with small real API calls

**Then:** WebSocket for real-time data and Settings screen

The foundation is solid - time to complete the API integration and build the trading intelligence!

---

## 🔄 Development Workflow

### For New Features
1. **Read ticket** in docs/tickets/ for detailed requirements
2. **Check reference.md** for implementation examples  
3. **Implement** following Clean Architecture patterns
4. **Test** with real API calls (small amounts)
5. **Update** CLAUDE.md and roadmap.md when complete

### For Current Phase 1 Work
- **Focus:** Complete Coinbase API integration
- **Test:** Use real API with small amounts ($10-20 max)
- **Validate:** Ensure error handling works properly
- **Document:** Update progress in roadmap.md

**The app now successfully connects to Coinbase and shows real data - major milestone achieved!**
