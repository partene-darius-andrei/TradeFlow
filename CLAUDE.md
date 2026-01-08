# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-08
**Project Status:** Phase 1 Complete - Enhanced Coinbase Integration (v1.5.2)
**Current Build:** #31 SUCCESS

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

### What EXISTS (Phases 0A, 0B, & 1: COMPLETE - v1.5.2)

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

🆕 COINBASE INTEGRATION COMPLETE (v1.5.2):
✅ exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt     ✅ ES256 JWT with ADVANCED BouncyCastle PEM parsing + comprehensive error recovery + multiple key formats
    ├── api/
    │   └── CoinbaseApiClient.kt        ✅ Complete Ktor-based API client with enhanced error handling + improved stability
    ├── dto/
    │   └── AccountDto.kt               ✅ Account DTOs with complete response structure
    ├── mapper/
    │   └── AccountMapper.kt            ✅ DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt       ✅ Enhanced implementation with better error handling
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

🆕 PRESENTATION LAYER WITH ENHANCED LIVE DATA (v1.5.2):
✅ app/src/main/java/com/dpart/tradeflow/
    ├── navigation/
    │   └── AppNavHost.kt               ✅ Complete navigation with CENTRALIZED TopAppBar + cleaner UI architecture
    └── presentation/dashboard/
        ├── DashboardScreen.kt          ✅ Enhanced implementation with IMPROVED real data integration + professional error handling
        ├── DashboardViewModel.kt       ✅ Complete state management with ENHANCED error handling + robust loading states
        └── components/
            ├── PortfolioCard.kt        ✅ Live balance data with enhanced formatting + better "Live Data" indicators
            ├── ModeCard.kt             ✅ Trading mode + current price
            ├── ServiceCard.kt          ✅ Service status + start/stop button
            └── OrdersList.kt           ✅ Recent orders + empty state

🆕 APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants (white/black)
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

🆕 ENHANCED CREDENTIALS SYSTEM (v1.5.2):
✅ app/build.gradle.kts                 ✅ ADVANCED build-time credential injection with ENHANCED PEM escape handling + improved security
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig

🆕 COINBASE API DEPENDENCIES (v1.5.2):
✅ BouncyCastle PEM key parsing libraries (bcprov-jdk18on, bcpkix-jdk18on) 1.78 ✅ ACTIVE
✅ ENHANCED PEM key parsing with ADVANCED format detection (raw base64 + PEM) + comprehensive error recovery
✅ JWT token generation with ES256 and ROBUST error handling + enhanced debugging capabilities
✅ Authentication flow FULLY HARDENED with multiple key format support + fallback mechanisms
✅ Connection stability STRENGTHENED with improved retry logic and error recovery
```

### Major Milestone: Enhanced Security & Reliability (v1.5.2)

**Latest stability and security enhancements now live in the app:**

✅ **Advanced Authentication (v1.5.2):**
- ENHANCED PEM key parsing with BouncyCastle libraries (bcprov-jdk18on, bcpkix-jdk18on 1.78)
- Support for both raw base64 and PEM formats with automatic detection
- Comprehensive error handling in JWT generation with multiple fallback mechanisms
- Build-time credential injection with IMPROVED security key processing

✅ **Improved API Integration:**
- CoinbaseApiClient with enhanced Ktor HTTP client and robust error handling
- AccountsResponseDto with complete Coinbase API response structure  
- Domain mapping from DTOs to Balance models with better error recovery
- Professional UX flow with enhanced loading states and retry mechanisms

✅ **Navigation & UI Improvements:**
- Centralized TopAppBar configuration to resolve duplicate display issues
- Cleaner navigation architecture with better user experience
- Enhanced dashboard with improved real-time data display
- Better error state management with user-friendly retry options

### Current App Version

**Version:** 1.5.2 (latest stable)
**Key Improvements:**
- Enhanced security key parsing supporting multiple key formats (raw base64 + PEM)
- Improved connection stability and error recovery mechanisms
- Fixed navigation display issues with centralized TopAppBar architecture
- Strengthened JWT token generation with comprehensive error handling
- Enhanced debugging capabilities for better troubleshooting
- Professional error recovery with improved retry functionality

### What DOESN'T Exist Yet (Next Up)

```
✅ Domain models - Ticket 01 ✅ DONE
✅ Room database - Ticket 03 ✅ DONE
✅ UI Foundation - Tickets 05-09 ✅ DONE
✅ Dashboard screen - Ticket 10 ✅ DONE (with enhanced real data + professional error handling)
✅ Dashboard ViewModel - Ticket 12 ✅ DONE (complete state management + robust error handling)
✅ Coinbase REST API client - Ticket 13 ✅ DONE (accounts endpoint complete with enhanced security)
❌ Decision engine (regime switching logic) - Ticket 15
❌ Risk manager - Ticket 16
❌ Backtest validation - Phase 1B
❌ Coinbase WebSocket - Ticket 14 (full REST API comes first)
❌ Full REST API implementation (candles, orders, market data) - Phase 2
❌ Settings screen - Ticket 11 (refined, ready to implement)
❌ Trading service (foreground service) - Tickets 17-18

## Next Priorities (Post-v1.5.2)

1. **Full REST API Implementation** (remaining endpoints for candles, orders, market data)
2. **Decision Engine Implementation** (Ticket 15)
3. **Settings Screen Implementation** (Ticket 11)
```

---

## 🏗️ Tech Stack

### Core Dependencies (All Added ✅)

| Library | Version | Status | Usage |
|---------|---------|--------|-------|
| **Kotlin** | 2.3.0 | ✅ Active | Language |
| **Compose BOM** | 2025.12.01 | ✅ Active | UI framework |
| **Hilt** | 2.57.2 | ✅ Active | Dependency injection |
| **Room** | 2.8.4 | ✅ Active | Database (4 entities, 4 DAOs) |
| **Ktor** | 3.3.3 (OkHttp) | ✅ Active | HTTP client + WebSocket |
| **Timber** | 5.0.1 | ✅ Active | Logging |

### Trading-Specific Dependencies

| Library | Version | Status | Usage |
|---------|---------|--------|-------|
| **ta4j-core** | 0.16 | ⏳ Ready | Technical indicators (SMA, ADX, ATR) |
| **nimbus-jose-jwt** | 9.47 | ✅ Active | JWT ES256 signing |
| **BouncyCastle** | 1.78 | ✅ Active | PEM key parsing (bcprov-jdk18on, bcpkix-jdk18on) |
| **security-crypto** | 1.1.0-alpha06 | ⏳ Ready | EncryptedSharedPreferences |
| **work-runtime-ktx** | 2.10.0 | ⏳ Ready | Background tasks |
| **datastore-preferences** | 1.1.1 | ⏳ Ready | Settings persistence |

### Development & Analytics

| Library | Version | Status | Usage |
|---------|---------|--------|-------|
| **Vico** | 2.4.0 | ⏳ Ready | Charts (dashboard) |
| **Firebase BOM** | 34.7.0 | ✅ Active | Analytics + Crashlytics |
| **Coroutines** | 1.10.2 | ✅ Active | Async programming |
| **material-icons-extended** | - | ✅ Active | Icons for ModeIndicator |

**Legend:**
- ✅ Active = Currently used in code
- ⏳ Ready = Configured, awaiting implementation

---

## 🔧 Development Workflow

### For Claude Code Mobile

**Pattern:** Remote development with GitHub Actions building APKs

```
Claude Code → Push branch → GitHub Actions → Firebase Distribution → Test on device
```

**No local Gradle needed.** CI/CD handles all building with embedded credentials.

### Key Files to Watch

| File | Purpose | Change Frequency |
|------|---------|------------------|
| **`.build-status`** | Build success/failure | Every push |
| **`build-log.txt`** | Gradle errors (if build fails) | On failures |
| **`docs/roadmap.md`** | Implementation progress | Weekly |
| **`CLAUDE.md`** | This file - project context | As needed |

### After Every Push

```bash
git pull                    # Get CI results
cat .build-status          # Check build result
# If FAILURE:
cat build-log.txt          # Check error details
```

---

## 📋 File Structure (What Exists)

```
TradeFlow/
├── app/                                    ✅ Main app module
│   ├── build.gradle.kts                   ✅ Enhanced credential injection (v1.5.2)
│   ├── src/main/java/com/dpart/tradeflow/
│   │   ├── MainActivity.kt                ✅ Minimal (just hosts Compose)
│   │   ├── TradeFlowApp.kt               ✅ Application class
│   │   ├── di/
│   │   │   ├── AppModule.kt              ✅ Empty Hilt module
│   │   │   ├── DatabaseModule.kt         ✅ Room database DI
│   │   │   ├── NetworkModule.kt          ✅ Ktor HTTP client DI
│   │   │   └── CredentialsModule.kt      ✅ Static credential injection
│   │   ├── navigation/
│   │   │   └── AppNavHost.kt             ✅ Navigation with enhanced TopAppBar
│   │   └── presentation/dashboard/
│   │       ├── DashboardScreen.kt        ✅ Enhanced UI with real data
│   │       ├── DashboardViewModel.kt     ✅ Complete state management
│   │       └── components/               ✅ PortfolioCard, ModeCard, etc.
│   └── src/main/res/                     ✅ App icon, strings, themes
│
├── core/
│   ├── domain/                           ✅ Pure Kotlin domain layer
│   │   └── src/main/kotlin/.../domain/
│   │       ├── auth/                     ✅ AuthTokenProvider, CredentialStore
│   │       ├── error/                    ✅ ExchangeError types
│   │       ├── model/                    ✅ Candle, Order, Portfolio, etc.
│   │       └── repository/               ✅ ExchangeRepository, WebSocket
│   ├── data/                             ✅ Room + Security
│   │   └── src/main/kotlin/.../data/
│   │       ├── local/                    ✅ Room entities + DAOs
│   │       ├── security/                 ✅ StaticCredentialStore
│   │       └── di/                       ✅ Database + Security modules
│   └── ui/                               ✅ Shared UI components
│       └── src/main/kotlin/.../ui/
│           ├── component/                ✅ StatusCard, PriceDisplay, etc.
│           └── extension/                ✅ BigDecimal formatting
│
├── exchange/coinbase/                    ✅ Coinbase API implementation (v1.5.2)
│   └── src/main/kotlin/.../coinbase/
│       ├── auth/
│       │   └── CoinbaseJwtGenerator.kt   ✅ Enhanced ES256 JWT with BouncyCastle
│       ├── api/
│       │   └── CoinbaseApiClient.kt      ✅ Ktor HTTP client wrapper
│       ├── dto/
│       │   └── AccountDto.kt             ✅ API response DTOs
│       ├── mapper/
│       │   └── AccountMapper.kt          ✅ DTO → Domain mapping
│       ├── repository/
│       │   └── CoinbaseRepository.kt     ✅ Enhanced implementation
│       └── di/                           ✅ Auth + Exchange modules
│
└── docs/                                 ✅ All documentation
    ├── README.md                         ✅ Complete doc index
    ├── roadmap.md                        ✅ Implementation roadmap
    ├── reference.md                      ✅ Implementation blueprint
    ├── ci.md                             ✅ CI/CD documentation
    └── tickets/                          ✅ All ticket files
```

---

## 🚀 Current Development Phase

### Phase 1 COMPLETE: Enhanced Coinbase Integration (v1.5.2)

**Major achievements:**
- ✅ **Enhanced authentication** with advanced PEM key parsing and multiple format support
- ✅ **Improved API integration** with robust error handling and retry mechanisms
- ✅ **Professional user experience** with better loading states and error recovery
- ✅ **Centralized navigation** architecture resolving TopAppBar display issues
- ✅ **Live portfolio data** integration with Coinbase Advanced Trade API
- ✅ **Comprehensive error handling** throughout the authentication and API layers

**What this means:**
- App now displays real Coinbase account balances
- Authentication is hardened with multiple fallback mechanisms
- User interface provides professional error handling and recovery options
- Navigation is clean and consistent across the app
- Foundation is solid for implementing remaining trading features

### Next Phase: Full Trading Implementation

**Ready for:**
1. **Full REST API** - Complete Coinbase API client (candles, orders, market data)
2. **Decision Engine** - Implement the trading strategy logic
3. **Settings Screen** - Configuration and preferences
4. **Trading Service** - Background execution for autonomous trading

The enhanced v1.5.2 release provides a robust foundation for implementing the remaining trading features with confidence in the stability and security of the core systems.

