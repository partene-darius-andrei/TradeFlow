# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-08
**Project Status:** Phase 1 In Progress - Coinbase Integration
**Current Build:** #30 SUCCESS

This is the entry point for Claude Code when working with TradeFlow. All essential context, navigation, and workflows are documented here.

---

## 🎯 Quick Navigation

| Document | Purpose | Use When |
|----------|---------|----------|
| **[docs/roadmap.md](docs/roadmap.md)** | Implementation roadmap organized in phases | Planning what to build next |
| **[docs/README.md](docs/README.md)** | Complete documentation index and ticket mapping | Finding specific documentation |
| **[docs/reference.md](docs/reference.md)** | Implementation blueprint with code examples | Implementing features |
| **[docs/ci.md](docs/ci.md)** | CI/CD workflows and troubleshooting | Understanding build pipeline |
| **[docs/tickets/](docs/tickets/)** | All ticket files organized by status | Reading detailed requirements |

---

## 📊 Project Overview

**TradeFlow** - Personal automated crypto trading bot for Coinbase Advanced Trade API.

**Vision:**
- Remove human emotions from trading decisions
- Run 24/7 unattended on physical device (when proven)
- Simple UI, simple implementation, easy to maintain
- Backtest → Paper trade → Live (small) → Scale
- Never published - personal use only

**Reality Constraints:**
- Fees matter: ~0.25-0.5% per trade on Advanced Trade
- Most retail algo traders lose money - respect this
- Simple strategies often beat complex ML
- Every trade is a taxable event

**Trading Strategy (Bitcoin-First):**
- **BTC/USDT ONLY** until account reaches $2,500+ (fees kill small-cap altcoins)
- **Risk: 1-2% per trade** ($5-10 max on $500 account)
- **Expected returns: 5% monthly** (exceptional skill, realistic ceiling)
- **Timeline: 5-10 years** to meaningful passive income ($500-1k/month)
- **97% of day traders fail** - Treat first $500 as education, not income
- **See:** [docs/strategy/bitcoin-first-strategy.md](docs/strategy/bitcoin-first-strategy.md) for complete analysis

---

## 🚦 Current Status

### What EXISTS (Phases 0A & 0B: COMPLETE, Phase 1: IN PROGRESS)

```
✅ Modern Android app structure
✅ Hilt dependency injection configured
✅ Room database with complete schema (4 entities + 4 DAOs)
✅ Ktor HTTP client configured (OkHttp engine)
✅ Timber logging initialized
✅ Firebase Analytics + Crashlytics
✅ All trading dependencies added (ta4j, nimbus-jose-jwt, security-crypto)
✅ GitHub Actions CI/CD pipeline with environment credentials
✅ Adaptive app icon with trading chart design (day/night variants)

🆕 DOMAIN LAYER COMPLETE:
✅ core/domain/src/main/kotlin/com/tradeflow/core/domain/
    ├── auth/
    │   ├── AuthTokenProvider.kt        ✅ Token generation interface
    │   └── CredentialStore.kt          ✅ Secure credential storage interface
    ├── error/
    │   └── ExchangeError.kt           ✅ Exchange error types (6 variants)
    ├── model/ ← Ticket 01 COMPLETE
    │   ├── Candle.kt                  ✅ OHLCV + Granularity enum (9 timeframes)
    │   ├── Order.kt                   ✅ Order model + Side/Type/Status enums
    │   ├── Decision.kt                ✅ Sealed class (Wait/Defense/Trend/Range)
    │   ├── Portfolio.kt               ✅ Portfolio snapshot model
    │   ├── Balance.kt                 ✅ Account balance model
    │   └── Ticker.kt                  ✅ Real-time price ticker
    └── repository/
        ├── BracketOrderRepository.kt   ✅ Bracket order support interface
        ├── ExchangeRepository.kt       ✅ Core exchange operations (12 methods)
        └── ExchangeWebSocket.kt        ✅ Real-time data streams

🆕 DATA LAYER COMPLETE:
✅ core/data/src/main/kotlin/com/tradeflow/core/data/
    ├── local/ ← Ticket 03 COMPLETE
    │   ├── entity/
    │   │   ├── CandleEntity.kt        ✅ Room entity for candles
    │   │   ├── OrderEntity.kt         ✅ Room entity for orders
    │   │   ├── DecisionEntity.kt      ✅ Room entity for decisions
    │   │   └── PortfolioSnapshotEntity.kt ✅ Room entity for portfolio
    │   └── dao/
    │       ├── CandleDao.kt           ✅ CRUD + delete old candles
    │       ├── OrderDao.kt            ✅ CRUD + query by status/product
    │       ├── DecisionDao.kt         ✅ CRUD + latest decision query
    │       └── PortfolioDao.kt        ✅ CRUD + snapshot history
    ├── security/
    │   └── StaticCredentialStore.kt    ✅ Static credential injection (replaces UI input)
    └── di/
        └── SecurityModule.kt           ✅ Static credential DI binding

🆕 COINBASE INTEGRATION (PARTIAL - IN PROGRESS):
✅ exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt     ✅ ES256 JWT token generation
    ├── api/
    │   └── CoinbaseApiClient.kt        ✅ NEW: Ktor-based API client (accounts endpoint)
    ├── dto/
    │   └── AccountDto.kt               ✅ NEW: Account DTOs for API responses
    ├── mapper/
    │   └── AccountMapper.kt            ✅ NEW: DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt       ✅ NEW: Partial implementation (getBalances only)
    └── di/
        ├── AuthModule.kt               ✅ JWT generator DI binding
        └── ExchangeModule.kt           ✅ NEW: Repository DI binding

🆕 UI COMPONENTS COMPLETE:
✅ core/ui/src/main/kotlin/com/tradeflow/core/ui/
    ├── component/
    │   ├── ErrorDisplay.kt             ✅ Error state UI with retry button
    │   ├── LoadingButton.kt            ✅ Button with loading spinner
    │   ├── ModeIndicator.kt            ✅ Trading mode visual indicator
    │   ├── PriceDisplay.kt             ✅ Price with color coding (+/-)
    │   └── StatusCard.kt               ✅ Reusable card container
    └── extension/
        └── BigDecimalExt.kt           ✅ Currency/percentage formatting

🆕 DASHBOARD WITH REAL DATA:
✅ app/src/main/java/com/dpart/tradeflow/presentation/dashboard/
    ├── DashboardScreen.kt              ✅ UPDATED: Now connects to ViewModel with real data
    ├── DashboardViewModel.kt           ✅ NEW: State management + API integration
    ├── DashboardUiState.kt             ✅ NEW: UI state sealed class
    └── components/
        ├── PortfolioCard.kt            ✅ UPDATED: Accepts real balance parameters
        ├── ModeCard.kt                 ✅ Trading mode + current price
        ├── ServiceCard.kt              ✅ Service status + start/stop button
        └── OrdersList.kt               ✅ Recent orders + empty state

🆕 APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants (white/black)
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

🆕 CREDENTIALS SYSTEM COMPLETE:
✅ app/build.gradle.kts                 ✅ Build-time credential injection
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig
```

### Credential Management System

**New Approach:** Static credentials injected at build time, removing need for UI credential entry.

**Configuration (Priority Order):**
1. **Environment Variables** (CI/CD): `COINBASE_API_KEY`, `COINBASE_API_SECRET`
2. **Local Properties** (Dev): Add to `local.properties`:
   ```properties
   coinbase.api.key=organizations/your-org/apiKeys/your-key
   coinbase.api.secret=your-private-key-pem
   ```

**Build Integration:**
- `app/build.gradle.kts` injects credentials into `BuildConfig`
- `CredentialsModule` provides via Hilt DI
- `StaticCredentialStore` returns injected credentials
- No UI credential input needed

### What DOESN'T Exist Yet (Next Up)

```
✅ Domain models - Ticket 01 ✅ DONE
✅ Room database - Ticket 03 ✅ DONE
✅ UI Foundation - Tickets 05-09 ✅ DONE
✅ Dashboard screen - Ticket 10 ✅ DONE (with real data)
✅ Dashboard ViewModel - Ticket 12 ✅ DONE (real API integration)
🟡 Coinbase REST API client - Ticket 13 🟡 PARTIAL (accounts only, need full implementation)
❌ Decision engine (regime switching logic) - Ticket 15
❌ Risk manager - Ticket 16
❌ Backtest validation - Phase 1B
❌ Coinbase WebSocket - Ticket 14
❌ Settings screen - Ticket 11 (refined, ready to implement)
❌ Trading service (foreground service) - Tickets 17-18
```

**Progress:** **12/20 tickets done (60% complete)**. Dashboard now shows real portfolio data from Coinbase API! **Next up:** Complete REST API client (Ticket 13) for full market data and order placement.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0A: Domain Foundation (COMPLETE ✅)
- [x] **Modularization** (Ticket 00) ✅ COMPLETE - 8-module Clean Architecture
- [x] **Domain models** (Ticket 01) ✅ COMPLETE - Candle, Order, Decision, Portfolio, Balance, Ticker
- [x] **Repository interfaces** (Ticket 02) ✅ COMPLETE - Exchange contracts
- [x] **Room database** (Ticket 03) ✅ COMPLETE - 4 entities + 4 DAOs
- [x] **Credential storage** (Ticket 04) ✅ COMPLETE - Build-time static injection
- [x] **JWT generator** (Ticket 07-JWT) ✅ COMPLETE - ES256 signing with proper nonce

### Phase 0B: UI Foundation (COMPLETE ✅)
- [x] **UI Design Overview** (Ticket 05) ✅ COMPLETE - Complete visual redesign
- [x] **Core UI Theme** (Ticket 06) ✅ COMPLETE - Material 3 theme + colors
- [x] **Core UI Components** (Ticket 07-UI) ✅ COMPLETE - ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard
- [x] **Login Screen** (Ticket 08) ✅ COMPLETE (obsolete - removed after credential change)
- [x] **App Navigation** (Ticket 09) ✅ COMPLETE - Simplified routing (Dashboard + Settings)
- [x] **Dashboard Screen** (Ticket 10) ✅ COMPLETE - UI with real data integration
- [x] **Dashboard ViewModel** (Ticket 12) ✅ COMPLETE - State management + API calls

### Phase 1: Coinbase Integration (CURRENT - 25% Complete)
- [x] **Dashboard ViewModel** (Ticket 12) ✅ COMPLETE - Connects to real Coinbase data
- [🟡] **REST API Client** (Ticket 13) 🟡 **IN PROGRESS** - getBalances ✅, need full implementation
- [ ] **WebSocket Client** (Ticket 14) ❌ **NEXT** - Real-time data streams
- [ ] **Settings Screen** (Ticket 11) ❌ Credential management, preferences

### Phase 2: Trading Logic (NEXT)
- [ ] **Decision Engine** (Ticket 15) ❌ SMA, ADX, ATR + regime switching
- [ ] **Risk Manager** (Ticket 16) ❌ Position sizing + drawdown limits
- [ ] **Backtest Validation** ❌ Validate strategy with historical data

### Phase 3: Trading Service (FUTURE)
- [ ] **Trading Service** (Ticket 17) ❌ 24/7 foreground service
- [ ] **Battery Optimization** (Ticket 18) ❌ Doze mode survival

---

## 🎯 Recent Achievements (Last Update)

### 🆕 Dashboard Real Data Integration
- ✅ **DashboardViewModel** implemented with proper state management
- ✅ **Coinbase API integration** - Dashboard now shows real account balances
- ✅ **Error handling** - Loading, error states with retry functionality
- ✅ **Hilt integration** - Proper DI with ExchangeRepository interface
- ✅ **UI updates** - PortfolioCard accepts real balance parameters

### 🆕 Coinbase API Foundation
- ✅ **CoinbaseApiClient** - Ktor-based HTTP client with JWT auth
- ✅ **Account DTOs** - Proper serialization for Coinbase API responses
- ✅ **Repository pattern** - Clean separation with domain interfaces
- ✅ **DI integration** - ExchangeModule binds implementations

This represents a major milestone - the app can now authenticate with Coinbase and display real account data!

---

## 🚀 Next Steps

1. **Complete REST API Client** (Ticket 13)
   - Add market data endpoints (candles, current price)
   - Add order placement (market, limit, bracket orders)
   - Add order management (cancel, status, fills)

2. **WebSocket Integration** (Ticket 14)
   - Real-time price feeds for Dashboard
   - Live order status updates

3. **Settings Screen** (Ticket 11)
   - Trading parameters configuration
   - Notification settings
   - About/version info

The foundation is solid - time to build the trading brain!

---

## 🎯 Tech Stack & Dependencies

**Core Framework:**
- ✅ **Kotlin 2.3.0** - Language
- ✅ **Android Gradle Plugin 8.8.0** - Build system
- ✅ **Compose BOM 2025.12.01** - UI framework

**Architecture:**
- ✅ **Hilt 2.57.2** - Dependency injection
- ✅ **Room 2.8.4** - Local database (4 entities + 4 DAOs)
- ✅ **Ktor 3.3.3** - HTTP client + WebSocket (with OkHttp engine)
- ✅ **Navigation Compose 2.9.0** - Screen navigation

**Trading & Analysis:**
- ✅ **ta4j-core 0.16** - Technical indicators (SMA, ADX, ATR)
- ✅ **nimbus-jose-jwt 9.47** - ES256 JWT signing for Coinbase

**Background Processing:**
- ✅ **WorkManager 2.10.0** - Background tasks + dead-man-switch
- ✅ **Coroutines 1.10.2** - Async programming

**Storage & Preferences:**
- ✅ **DataStore Preferences 1.1.1** - Settings storage
- ✅ **Security Crypto 1.1.0-alpha06** - Credential encryption (unused after static injection)

**UI & Charts:**
- ✅ **Material Icons Extended** - Trading mode icons
- ✅ **Vico 2.4.0** - Charts for future analytics

**Monitoring:**
- ✅ **Firebase BOM 34.7.0** - Analytics + Crashlytics
- ✅ **Timber 5.0.1** - Logging

All dependencies are actively used or ready for upcoming features. No unused libraries.

---

## 📁 Key File Locations

**Current Implementation:**
```
app/src/main/java/com/dpart/tradeflow/
├── presentation/dashboard/
│   ├── DashboardScreen.kt              # Main screen UI
│   ├── DashboardViewModel.kt           # NEW: State management + API calls
│   └── components/PortfolioCard.kt     # UPDATED: Real balance display

exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
├── api/CoinbaseApiClient.kt            # NEW: Ktor API client
├── dto/AccountDto.kt                   # NEW: API response DTOs
├── mapper/AccountMapper.kt             # NEW: DTO -> Domain mapping
├── repository/CoinbaseRepository.kt    # NEW: Repository implementation
└── di/ExchangeModule.kt                # NEW: DI bindings

core/domain/src/main/kotlin/com/tradeflow/core/domain/
└── repository/ExchangeRepository.kt    # Interface implemented by Coinbase
```

**Ready for Implementation:**
- Decision engine logic
- Risk management rules
- WebSocket real-time data
- Complete REST API methods
- Settings screen UI

**The foundation is complete - time to build the trading intelligence!**
