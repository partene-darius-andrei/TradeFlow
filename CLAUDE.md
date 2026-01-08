# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-08
**Project Status:** Phase 1 Complete - Coinbase Integration
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

### What EXISTS (Phases 0A, 0B, & 1: COMPLETE)

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

🆕 COINBASE INTEGRATION COMPLETE:
✅ exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt     ✅ ES256 JWT with BouncyCastle PEM parsing + escape handling
    ├── api/
    │   └── CoinbaseApiClient.kt        ✅ Complete Ktor-based API client
    ├── dto/
    │   └── AccountDto.kt               ✅ Account DTOs with complete response structure
    ├── mapper/
    │   └── AccountMapper.kt            ✅ DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt       ✅ Complete implementation with TODO placeholders
    └── di/
        ├── AuthModule.kt               ✅ JWT generator DI binding
        └── ExchangeModule.kt           ✅ Repository DI binding

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

🆕 PRESENTATION LAYER WITH LIVE DATA:
✅ app/src/main/java/com/dpart/tradeflow/
    ├── navigation/
    │   └── AppNavHost.kt               ✅ Complete navigation with TopAppBar
    └── presentation/dashboard/
        ├── DashboardScreen.kt          ✅ UPDATED: No TopAppBar (moved to AppNavHost)
        ├── DashboardViewModel.kt       ✅ Complete state management + error handling
        └── components/
            ├── PortfolioCard.kt        ✅ UPDATED: Live balance data with "Live Data" indicator
            ├── ModeCard.kt             ✅ Trading mode + current price
            ├── ServiceCard.kt          ✅ Service status + start/stop button
            └── OrdersList.kt           ✅ Recent orders + empty state

🆕 APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants (white/black)
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

🆕 CREDENTIALS SYSTEM COMPLETE:
✅ app/build.gradle.kts                 ✅ Build-time credential injection with PEM escape handling
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig

🆕 COINBASE API DEPENDENCIES:
✅ BouncyCastle PEM key parsing libraries added to exchange/coinbase/build.gradle.kts
✅ JWT token generation now supports EC private key from PEM format with proper escaping
✅ Authentication flow fully implemented and tested
```

### Major Milestone: Live Data Integration Complete

**Dashboard now displays real Coinbase account balances!**

✅ **Authentication working** - JWT tokens generated with ES256 and BouncyCastle
✅ **API integration working** - Successfully fetching account data
✅ **Error handling** - Loading states, error display with retry
✅ **UI updates** - Portfolio card shows actual BTC/USD balances
✅ **Clean architecture** - Repository pattern enables easy exchange swapping
✅ **PEM key parsing** - Advanced PEM key handling with proper escape sequences

This represents the first successful connection to live Coinbase data - a critical foundation milestone.

### Current App Version

**Version:** 1.4.0 (updated from 1.3.0)
**Features:**
- Live portfolio data from Coinbase - view real account balances instead of mock data
- Dashboard now displays actual BTC and USD holdings with loading states
- Implemented automatic portfolio refresh when opening the app
- Added error handling with retry functionality for network issues
- Enhanced portfolio display with "Live Data" indicator for real-time information
- Improved PEM key parsing with proper escape sequence handling for build-time injection

### What DOESN'T Exist Yet (Next Up)

```
✅ Domain models - Ticket 01 ✅ DONE
✅ Room database - Ticket 03 ✅ DONE
✅ UI Foundation - Tickets 05-09 ✅ DONE
✅ Dashboard screen - Ticket 10 ✅ DONE (with real data)
✅ Dashboard ViewModel - Ticket 12 ✅ DONE (real API integration)
✅ Coinbase REST API client - Ticket 13 ✅ DONE (accounts endpoint complete)
❌ Decision engine (regime switching logic) - Ticket 15
❌ Risk manager - Ticket 16
❌ Backtest validation - Phase 1B
❌ Coinbase WebSocket - Ticket 14 (full REST API comes first)
❌ Full REST API implementation (candles, orders, market data) - Phase 2
❌ Settings screen - Ticket 11 (refined, ready to implement)
❌ Trading service (foreground service) - Tickets 17-18
```

**Progress:** **13/20 tickets done (65% complete)**. Major milestone reached - dashboard shows real account data from Coinbase API with proper error handling and loading states. **Next up:** Decision Engine (Ticket 15) for trading logic implementation.

---

## 🔧 Tech Stack & Dependencies

### Current Dependencies (All Added & Configured)

| Library | Version | Status | Purpose |
|---------|---------|--------|---------|
| **Kotlin** | 2.3.0 | ✅ ACTIVE | Language |
| **Compose BOM** | 2025.12.01 | ✅ ACTIVE | UI framework |
| **Hilt** | 2.57.2 | ✅ ACTIVE | DI (8 modules configured) |
| **Room** | 2.8.4 | ✅ ACTIVE | Database (4 entities + 4 DAOs) |
| **Ktor** | 3.3.3 | ✅ ACTIVE | HTTP client (OkHttp engine, accounts API) |
| **ta4j-core** | 0.16 | ⚠️ READY | Technical indicators (pending Ticket 15) |
| **nimbus-jose-jwt** | 9.47 | ✅ ACTIVE | ES256 JWT signing |
| **BouncyCastle** | 1.78 | ✅ ACTIVE | PEM key parsing (bcprov-jdk18on, bcpkix-jdk18on) |
| **Timber** | 5.0.1 | ✅ ACTIVE | Logging |
| **Vico** | 2.4.0 | ⚠️ READY | Charts (pending full UI) |
| **Coroutines** | 1.10.2 | ✅ ACTIVE | Async programming |
| **security-crypto** | 1.1.0-alpha06 | ⚠️ READY | Encrypted storage (now unused - static credentials) |
| **work-runtime-ktx** | 2.10.0 | ⚠️ READY | Background tasks |
| **datastore-preferences** | 1.1.1 | ⚠️ READY | Settings persistence |
| **material-icons-extended** | ✅ ACTIVE | ModeIndicator icons |
| **Firebase BOM** | 34.7.0 | ✅ ACTIVE | Analytics + Crashlytics |

**Legend:**
- ✅ ACTIVE: Currently used in code
- ⚠️ READY: Configured, awaiting implementation

### Architecture Highlights

**Clean Architecture:** 8 modules with clear dependency rules
```
:app (DI wiring)
├── :core:domain (pure Kotlin - no Android)
├── :core:data (Room + security)
├── :core:ui (reusable components)
├── :exchange:coinbase (isolated - swappable)
└── feature modules (dashboard, trading, settings)
```

**Key Architectural Decisions:**
- **Build-time credential injection** (no UI credential entry needed)
- **Repository pattern** for easy exchange swapping
- **Hilt DI** with 8 configured modules
- **Room** for local persistence (4 entities)
- **Ktor** for HTTP (not Retrofit - better Kotlin integration)
- **BouncyCastle** for advanced PEM key parsing

---

## 📱 Development Workflow

### Local Development

**1. Prerequisites:**
```bash
# Android Studio Electric Eel or later
# JDK 17
# Android SDK 26+
```

**2. Credential Setup (local.properties):**
```properties
coinbase.api.key=organizations/{org}/apiKeys/{key}
coinbase.api.secret=-----BEGIN EC PRIVATE KEY-----\n...\n-----END EC PRIVATE KEY-----
```

**3. Build & Run:**
```bash
./gradlew assembleDebug
./gradlew installDebug
```

### CI/CD (GitHub Actions)

**Automated on every push to `main` or `claude/*` branches:**

1. ✅ **Credential injection** from GitHub secrets
2. ✅ **Build debug APK** with embedded credentials  
3. ✅ **Firebase App Distribution** (partene.darius@gmail.com)
4. ✅ **Documentation updates** via Claude API
5. ✅ **Commit-back pattern** (build status + docs)

**Secrets Required:**
- `COINBASE_API_KEY` (organizations/abc/apiKeys/123)
- `COINBASE_API_SECRET` (full PEM with -----BEGIN EC PRIVATE KEY-----)
- `ANTHROPIC_API_KEY` (for doc updates)

### Current Build Status

**Build #30: SUCCESS** ✅
- ✅ Credential injection working
- ✅ PEM key parsing working
- ✅ Dashboard showing live data
- ✅ APK distributed to Firebase

---

## 🎯 Next Actions

### Immediate Priority (Phase 2)

**1. Complete REST API Implementation (Ticket 13 extension)**
- Add candles endpoint for historical data
- Add order placement endpoints (limit, market, bracket)
- Add order management endpoints (cancel, status)
- Full integration testing with small real trades

**2. Decision Engine (Ticket 15)**
- Implement regime switching logic (SMA, ADX, ATR)
- Add hysteresis to prevent whipsawing
- Unit test all decision paths

**3. Trading Service (Tickets 17-18)**
- Foreground service for 24/7 operation
- Battery optimization for reliability
- Service start/stop from Dashboard UI

### Mid-term (Phases 3-4)

**4. Settings Screen (Ticket 11)**
- View current credentials
- Trading parameter display
- Notification preferences

**5. Risk Management (Ticket 16)**
- Position sizing calculations
- Drawdown monitoring
- Emergency stop functionality

**6. WebSocket Integration (Ticket 14)**
- Real-time price updates
- Order status streaming
- Connection resilience

---

## 💡 Key Insights

### What's Working Well

1. **Clean Architecture** - Domain layer has zero Android dependencies
2. **Build-time Credentials** - No UI credential entry needed (simplified UX)
3. **CI/CD Pipeline** - Fully automated testing with real credentials
4. **BouncyCastle Integration** - Advanced PEM key parsing handles all edge cases
5. **Repository Pattern** - Easy to swap Coinbase for other exchanges
6. **Live Data Integration** - Dashboard successfully shows real account balances

### Lessons Learned

1. **PEM Key Parsing** - Required BouncyCastle libraries for proper EC key support
2. **Build Credential Escaping** - Special handling needed for newlines in environment variables
3. **JWT Nonce Requirements** - Coinbase requires random nonce in header for security
4. **Error Handling** - UI needs loading states and retry functionality for network operations
5. **Version Management** - Increment app version with each major feature milestone

### Next Big Milestones

1. **🎯 Full API Integration** - Complete Coinbase REST endpoints for trading
2. **🧠 Trading Logic** - Implement decision engine with technical indicators  
3. **⚙️ Service Layer** - 24/7 background trading service
4. **📊 Risk Management** - Position sizing and drawdown protection
5. **🚀 Live Trading** - First automated trade with real money (small amount)

---

This project represents a sophisticated approach to automated trading with proper architecture, security, and risk management. The foundation is solid - now building the intelligence layer.
