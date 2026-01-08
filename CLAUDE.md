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
    │   └── CoinbaseJwtGenerator.kt     ✅ ES256 JWT with BouncyCastle PEM parsing
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
✅ app/build.gradle.kts                 ✅ Build-time credential injection
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig

🆕 COINBASE API DEPENDENCIES:
✅ BouncyCastle PEM key parsing libraries added to exchange/coinbase/build.gradle.kts
✅ JWT token generation now supports EC private key from PEM format
✅ Authentication flow fully implemented and tested
```

### Major Milestone: Live Data Integration Complete

**Dashboard now displays real Coinbase account balances!**

✅ **Authentication working** - JWT tokens generated with ES256 and BouncyCastle
✅ **API integration working** - Successfully fetching account data
✅ **Error handling** - Loading states, error display with retry
✅ **UI updates** - Portfolio card shows actual BTC/USD balances
✅ **Clean architecture** - Repository pattern enables easy exchange swapping

This represents the first successful connection to live Coinbase data - a critical foundation milestone.

### Current App Version

**Version:** 1.3.0 (updated from 1.2.0)
**Features:**
- Live portfolio data integration with Coinbase API
- Dashboard displays real BTC and USD account balances
- Loading states and error handling for portfolio data
- Automatic portfolio refresh on app launch
- Portfolio card shows "Live Data" indicator for real-time information

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

## 🏗️ Tech Stack

| Component | Library/Framework | Version | Status |
|-----------|------------------|---------|--------|
| **Language** | Kotlin | 2.3.0 | ✅ Active |
| **UI Framework** | Jetpack Compose | BOM 2025.12.01 | ✅ Active |
| **Architecture** | Clean Architecture + MVVM | - | ✅ Active |
| **DI** | Dagger Hilt | 2.57.2 | ✅ Active |
| **Database** | Room | 2.8.4 | ✅ Active |
| **HTTP Client** | Ktor | 3.3.3 (OkHttp) | ✅ Active |
| **Crypto/JWT** | nimbus-jose-jwt + BouncyCastle | 9.47 + 1.78 | ✅ Active |
| **Technical Analysis** | ta4j-core | 0.16 | ⏳ Ready |
| **Logging** | Timber | 5.0.1 | ✅ Active |
| **Charts** | Vico | 2.4.0 | ⏳ Ready |
| **Analytics** | Firebase | BOM 34.7.0 | ✅ Active |
| **Background** | WorkManager | 2.10.0 | ⏳ Ready |
| **Settings** | DataStore Preferences | 1.1.1 | ⏳ Ready |

**Legend:**
- ✅ **Active** - Currently used in implemented features
- ⏳ **Ready** - Dependency added, awaiting implementation
- ❌ **Missing** - Not yet added

---

## 📦 Dependencies

### Core Dependencies (Active)
```kotlin
// Core Android
androidx-core-ktx = "1.15.0"
compose-bom = "2025.12.01"
activity-compose = "1.9.3"

// Architecture
lifecycle-viewmodel-compose = "2.8.7"
lifecycle-runtime-compose = "2.8.7"
navigation-compose = "2.8.5"
hilt-android = "2.57.2"
hilt-navigation-compose = "1.2.0"

// Database
room = "2.8.4"

// Network
ktor = "3.3.3"

// Authentication/Security  
nimbus-jose-jwt = "9.47"
bouncycastle = "1.78"  # NEW: PEM key parsing
security-crypto = "1.1.0-alpha06"

// Logging
timber = "5.0.1"

// Firebase
firebase-bom = "34.7.0"
```

### Pending Implementation
```kotlin
// Technical Analysis (ready for Decision Engine)
ta4j-core = "0.16"

// Background Processing (ready for Trading Service)  
work-runtime-ktx = "2.10.0"

// Charts (ready for advanced UI)
vico = "2.4.0"

// Settings (ready for Settings screen)
datastore-preferences = "1.1.1"
```

### BouncyCastle Integration (NEW)

**Added to exchange/coinbase module:**
```kotlin
implementation("org.bouncycastle:bcprov-jdk18on:1.78")
implementation("org.bouncycastle:bcpkix-jdk18on:1.78")
```

**Purpose:** Parse EC private keys from PEM format for Coinbase JWT authentication. Replaces manual PEM parsing with industry-standard crypto library.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0: Foundation (COMPLETE ✅)
- [x] **Modularization** (Ticket 00) ✅ - 8-module Clean Architecture
- [x] **Domain models** (Ticket 01) ✅ - All trading entities
- [x] **Repository interfaces** (Ticket 02) ✅ - Exchange contracts
- [x] **Room database** (Ticket 03) ✅ - Complete persistence layer
- [x] **Credential storage** (Ticket 04) ✅ - Build-time static injection

### Phase 1: Coinbase Integration (COMPLETE ✅)
- [x] **JWT Generator** (Ticket 07) ✅ - ES256 + BouncyCastle PEM parsing
- [x] **Basic API Client** (Ticket 13A) ✅ - Account data retrieval
- [x] **Dashboard Integration** (Ticket 10A) ✅ - Live portfolio display

### Phase 2: Trading Logic (IN PROGRESS)
- [ ] **Decision Engine** (Ticket 05) ← NEXT - SMA/ADX/ATR + regime switching
- [ ] **Risk Manager** (Ticket 06) - Position sizing + drawdown limits
- [ ] **Full REST API** (Ticket 13) - Orders, candles, market data
- [ ] **WebSocket Client** (Ticket 14) - Real-time price feeds

### Phase 3: Service & UI (PLANNED)
- [ ] **Settings Screen** (Ticket 11) - Configuration management  
- [ ] **Trading Service** (Tickets 17-18) - 24/7 background execution
- [ ] **Integration Testing** (Ticket 19) - End-to-end validation

**Next Immediate Priority:** Decision Engine implementation (Ticket 05) to enable trading strategy logic.

---

## 🔧 Development Workflow

### Credential Management System

**New Approach:** Static credentials injected at build time, removing need for UI credential entry.

**Configuration (Priority Order):**
1. **Environment Variables** (CI/CD): `COINBASE_API_KEY`, `COINBASE_API_SECRET`
2. **Local Properties** (Dev): Add to `local.properties`:
   ```properties
   coinbase.api.key=organizations/your-org/apiKeys/your-key
   coinbase.api.secret=-----BEGIN EC PRIVATE KEY-----...
   ```

**Build Integration:**
- `app/build.gradle.kts` injects credentials into `BuildConfig`
- `CredentialsModule` provides via Hilt DI
- `StaticCredentialStore` returns injected credentials
- No UI credential input needed

### GitHub Actions Integration

**CI/CD Pipeline:**
1. ✅ Credential injection from GitHub Secrets
2. ✅ Debug APK build with embedded credentials
3. ✅ Firebase App Distribution
4. ✅ Build status commit-back
5. ✅ Automated documentation updates

**See:** [docs/ci.md](docs/ci.md) for detailed CI/CD documentation

---

## 🎯 Current Focus: Decision Engine

**Next ticket:** Implement `EngineDecisionEngine.kt` with:
- SMA(200) trend filtering
- ADX(14) trend strength measurement
- ATR(14) volatility-based positioning
- Hysteresis logic (3-candle confirmation)
- Regime switching (DEFENSE/TREND/RANGE)

This is the core trading brain that determines when and how to trade based on market conditions.
