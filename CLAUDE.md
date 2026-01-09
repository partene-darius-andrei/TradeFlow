# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-09
**Project Status:** Phase 1 Complete - Enhanced Coinbase Integration (v1.5.5)
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

### What EXISTS (Phases 0A, 0B, & 1: COMPLETE - v1.5.5)

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

🆕 COINBASE INTEGRATION COMPLETE (v1.5.5):
✅ exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt     ✅ ES256 JWT with ADVANCED BouncyCastle PEM parsing + enhanced escape handling + comprehensive error recovery
    ├── api/
    │   └── CoinbaseApiClient.kt        ✅ Complete Ktor-based API client (accounts) with robust error handling
    ├── dto/
    │   └── AccountDto.kt               ✅ Account DTOs for API responses  
    ├── mapper/
    │   └── AccountMapper.kt            ✅ DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt       ✅ Implementation (getBalances working, others TODO for Phase 2)
    └── di/
        ├── AuthModule.kt               ✅ JWT generator DI binding
        └── ExchangeModule.kt           ✅ Repository DI binding

🆕 UI COMPONENTS COMPLETE:
✅ core/ui/src/main/kotlin/com/tradeflow/core/ui/
    ├── component/
    │   ├── ErrorDisplay.kt             ✅ Error state with retry button
    │   ├── LoadingButton.kt            ✅ Button with loading spinner
    │   ├── ModeIndicator.kt            ✅ Trading mode badges (DEFENSE/TREND/RANGE)
    │   ├── PriceDisplay.kt             ✅ Price with +/- color coding
    │   └── StatusCard.kt               ✅ Reusable card container
    └── extension/
        └── BigDecimalExt.kt           ✅ Currency/percentage formatting

🆕 PRESENTATION LAYER WITH ENHANCED LIVE DATA (v1.5.5):
✅ app/src/main/java/com/dpart/tradeflow/
    ├── navigation/
    │   └── AppNavHost.kt               ✅ Complete navigation with CENTRALIZED TopAppBar ("TradeFlow" title)
    └── presentation/dashboard/
        ├── DashboardScreen.kt          ✅ Complete implementation with ENHANCED real data integration
        ├── DashboardViewModel.kt       ✅ Full state management + ROBUST error handling + loading states
        └── components/
            ├── PortfolioCard.kt        ✅ Live data, BTC/USD balances with "Live Data" indicator + enhanced formatting
            ├── ModeCard.kt             ✅ Trading mode + current price
            ├── ServiceCard.kt          ✅ Service status + start/stop button
            └── OrdersList.kt           ✅ Recent orders + empty state

🆕 APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants (white/black)
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

🆕 ENHANCED CREDENTIALS SYSTEM (v1.5.5):
✅ app/build.gradle.kts                 ✅ ADVANCED build-time credential injection with ENHANCED PEM key escaping + security improvements
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig

🆕 COINBASE API DEPENDENCIES (v1.5.5):
✅ BouncyCastle PEM key parsing libraries (bcprov-jdk18on, bcpkix-jdk18on) 1.78 ✅ ACTIVE
✅ ENHANCED security key parsing with improved support for Coinbase CDP private key formats
✅ JWT authentication issues RESOLVED with better connection failure handling
✅ STRENGTHENED PEM key processing with better escape sequence handling
✅ Authentication flow fully hardened for production stability
✅ Enhanced error handling and debugging capabilities for API connections
```

### Major Milestone: Enhanced Security & Reliability (v1.5.5)

**Latest stability and security enhancements now live in the app:**

✅ **Enhanced Security Key Parsing (v1.5.5):**
- STRENGTHENED support for Coinbase CDP private key formats
- JWT authentication issues RESOLVED that could cause connection failures
- Improved error handling and debugging capabilities for API connections
- Enhanced PEM key processing with better escape sequence handling

✅ **UI & Navigation Improvements:**
- RESOLVED duplicate navigation bar display issue for cleaner interface
- Centralized TopAppBar configuration with "TradeFlow" title
- Enhanced dashboard with complete state management and loading indicators
- Portfolio card shows BTC/USD balances with "Live Data" indicator and enhanced formatting

✅ **API Integration Enhancements:**
- App now displays real Coinbase account balances with ENHANCED error handling
- ViewModel with complete state management (loading, error, success states)
- IMPROVED error handling with better retry functionality for network failures
- Professional UX flow with robust state management and error recovery

### Current App Version

**Version:** 1.5.5 (latest stable)
**Key Improvements:**
- Enhanced security key parsing with improved support for Coinbase CDP private key formats
- Fixed JWT authentication issues that could cause connection failures
- Improved error handling and debugging capabilities for API connections
- Resolved duplicate navigation bar display issue for cleaner interface
- Strengthened PEM key processing with better escape sequence handling

### What DOESN'T Exist Yet (Next Up)

```
✅ Domain models - Ticket 01 ✅ DONE
✅ Room database - Ticket 03 ✅ DONE
✅ UI Foundation - Tickets 05-09 ✅ DONE
✅ Dashboard screen - Ticket 10 ✅ DONE (with enhanced real data + professional error handling)
✅ Coinbase API Integration - Phase 1 ✅ COMPLETE (accounts endpoint working)

❌ NEXT PRIORITIES:
13. Full REST API Client - Complete Coinbase implementation (candles, orders, market data)
15. Decision Engine - SMA/ADX/ATR indicators + regime switching logic
16. Risk Manager - Position sizing, drawdown limits, emergency stop
17. Trading Service - 24/7 background execution with foreground service
```

---

## 🏗️ Tech Stack

### Core Dependencies

| Component | Library | Version | Status |
|-----------|---------|---------|---------|
| **Language** | Kotlin | 2.3.0 | ✅ Active |
| **UI Framework** | Compose BOM | 2025.12.01 | ✅ Active |
| **DI Framework** | Hilt | 2.57.2 | ✅ Active |
| **Database** | Room | 2.8.4 | ✅ Active (4 entities + 4 DAOs) |
| **HTTP Client** | Ktor | 3.3.3 | ✅ Active (OkHttp engine) |
| **JSON** | kotlinx.serialization | 1.7.3 | ✅ Active |
| **Logging** | Timber | 5.0.1 | ✅ Active |

### Coinbase API Integration

| Component | Library | Version | Status |
|-----------|---------|---------|--------|
| **JWT Signing** | nimbus-jose-jwt | 9.47 | ✅ Active (ES256 JWT generation) |
| **PEM Parsing** | BouncyCastle bcprov-jdk18on | 1.78 | ✅ Active (enhanced key parsing) |
| **PEM Parsing** | BouncyCastle bcpkix-jdk18on | 1.78 | ✅ Active (enhanced key parsing) |

### Trading & Technical Analysis

| Component | Library | Version | Status |
|-----------|---------|---------|---------|
| **Technical Indicators** | ta4j-core | 0.16 | ⚠️ Ready (pending decision engine) |
| **Security** | security-crypto | 1.1.0-alpha06 | ⚠️ Replaced (using build-time injection) |

### Background Services & Utilities

| Component | Library | Version | Status |
|-----------|---------|---------|---------|
| **Background Work** | work-runtime-ktx | 2.10.0 | ⚠️ Ready (pending trading service) |
| **Settings Storage** | datastore-preferences | 1.1.1 | ⚠️ Ready (pending settings) |
| **Charts** | Vico | 2.4.0 | ⚠️ Ready (pending advanced UI) |

### Development & CI/CD

| Component | Library | Version | Status |
|-----------|---------|---------|---------|
| **Icons** | material-icons-extended | Compose BOM | ✅ Active (ModeIndicator icons) |
| **Analytics** | Firebase BOM | 34.7.0 | ✅ Active (Analytics + Crashlytics) |
| **CI/CD** | GitHub Actions | - | ✅ Active (auto-build + Firebase distribution) |

**Legend:**
- ✅ Active = Currently used in code
- ⚠️ Ready = Configured, awaiting feature implementation
- ❌ Missing = Not yet added/configured

---

## 🔗 Key File Locations

### Application Layer

```
app/src/main/java/com/dpart/tradeflow/
├── MainActivity.kt                     # Compose entry point
├── TradeFlowApp.kt                     # Application class (Timber, Hilt)
├── navigation/
│   └── AppNavHost.kt                   # Navigation setup with centralized TopAppBar
├── presentation/dashboard/
│   ├── DashboardScreen.kt              # Main dashboard UI with enhanced live data
│   ├── DashboardViewModel.kt           # Complete state management with robust error handling
│   └── components/                     # Dashboard UI components
└── di/
    └── CredentialsModule.kt            # Build-time credential injection
```

### Domain Layer (Pure Kotlin)

```
core/domain/src/main/kotlin/com/tradeflow/core/domain/
├── model/                              # All domain models (Candle, Order, etc.)
├── repository/                         # Repository interfaces
└── auth/                               # Authentication interfaces
```

### Data Layer

```
core/data/src/main/kotlin/com/tradeflow/core/data/
├── local/                              # Room database (4 entities + 4 DAOs)
└── security/                           # Static credential store
```

### UI Layer

```
core/ui/src/main/kotlin/com/tradeflow/core/ui/
└── component/                          # Reusable components (5 components complete)
```

### Coinbase Integration

```
exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
├── auth/CoinbaseJwtGenerator.kt        # Enhanced ES256 JWT with BouncyCastle
├── api/CoinbaseApiClient.kt            # Ktor HTTP client wrapper  
├── repository/CoinbaseRepository.kt    # ExchangeRepository implementation
├── dto/AccountDto.kt                   # Account DTOs
└── mapper/AccountMapper.kt             # DTO to domain mapping
```

---

## 🚀 Development Workflow

### Using Claude Code with TradeFlow

1. **Check current status:**
   ```bash
   cat .build-status  # SUCCESS or FAILURE
   ```

2. **Read roadmap for next priority:**
   ```bash
   head -50 docs/roadmap.md
   ```

3. **Implement features following docs/reference.md patterns**

4. **Push to trigger build:**
   ```bash
   git push origin claude/feature-name
   ```

5. **Check build result:**
   ```bash
   cat .build-status
   # If FAILURE: cat build-log.txt
   ```

6. **Test APK distributed to Firebase automatically**

### Key Commands

```bash
# Project overview
find . -name "*.kt" | grep -v test | wc -l  # Count source files

# Check module structure  
find . -name build.gradle.kts | head -10

# See recent changes
git log --oneline -10

# Check dependencies
grep -r "implementation(" */build.gradle.kts | head -10
```

---

## 📋 Current Priorities

### Next Implementation (Phase 2)

1. **Full REST API Client (Ticket 13)** - Complete CoinbaseRepository:
   - Implement getCandles(), getCurrentPrice()
   - Implement order placement (placeLimitOrder, placeMarketOrder, placeBracketOrder)
   - Implement order management (cancelOrder, getOpenOrders)

2. **Decision Engine (Ticket 05)** - SMA/ADX/ATR indicators:
   - Integrate ta4j library for technical indicators
   - Implement regime switching logic (DEFENSE/TREND/RANGE)
   - Add hysteresis to prevent mode switching whipsaws

3. **Risk Manager (Ticket 06)** - Position sizing and limits:
   - 1-2% risk per trade calculation
   - 15% drawdown emergency stop
   - Position size validation

### Quality Gates

- **No crashes** in 8-hour device test
- **Authentication works** with real Coinbase API
- **Database persists** data across app restarts
- **UI responsive** on phone + tablet
- **Build succeeds** in CI/CD pipeline

---

## 🔍 Troubleshooting

### Build Issues

- Check `.build-status` file for SUCCESS/FAILURE
- Read `build-log.txt` for error details (last 200 lines)
- Verify environment secrets in GitHub repo settings

### Common Issues

1. **Build fails with credential issues:**
   - Check GitHub Secrets: `COINBASE_API_KEY`, `COINBASE_API_SECRET`, `ANTHROPIC_API_KEY`
   - Verify PEM key format (must include headers and newlines)

2. **App crashes on launch:**
   - Check Timber logs in logcat
   - Verify Hilt modules are properly configured
   - Check Room database schema

3. **Navigation issues:**
   - AppNavHost is centralized in navigation/AppNavHost.kt
   - Bottom navigation handled in same file
   - TopAppBar centralized to prevent duplicates

For more details, see [docs/ci.md](docs/ci.md) for CI/CD troubleshooting.
