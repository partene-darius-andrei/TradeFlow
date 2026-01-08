# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-08
**Project Status:** Phase 1 Complete - Coinbase Integration  
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
    │   └── CoinbaseJwtGenerator.kt     ✅ ES256 JWT with ENHANCED BouncyCastle PEM parsing + advanced escape handling
    ├── api/
    │   └── CoinbaseApiClient.kt        ✅ Complete Ktor-based API client with robust error handling
    ├── dto/
    │   └── AccountDto.kt               ✅ Account DTOs with complete response structure
    ├── mapper/
    │   └── AccountMapper.kt            ✅ DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt       ✅ Complete implementation for account balances
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
    │   └── AppNavHost.kt               ✅ Complete navigation with FIXED TopAppBar (no duplicate issue)
    └── presentation/dashboard/
        ├── DashboardScreen.kt          ✅ Full implementation with ENHANCED real data integration + improved error handling
        ├── DashboardViewModel.kt       ✅ Complete state management + ROBUST error handling + loading states
        └── components/
            ├── PortfolioCard.kt        ✅ Live balance data with "Live Data" indicator + better formatting
            ├── ModeCard.kt             ✅ Trading mode + current price
            ├── ServiceCard.kt          ✅ Service status + start/stop button
            └── OrdersList.kt           ✅ Recent orders + empty state

🆕 APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants (white/black)
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

🆕 CREDENTIALS SYSTEM COMPLETE:
✅ app/build.gradle.kts                 ✅ ENHANCED build-time credential injection with ADVANCED PEM escape handling
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig

🆕 COINBASE API DEPENDENCIES:
✅ BouncyCastle PEM key parsing libraries (bcprov-jdk18on, bcpkix-jdk18on) 1.78
✅ ADVANCED PEM key parsing with enhanced escape sequence handling for CI/CD environment variables
✅ JWT token generation with ES256 and COMPREHENSIVE error handling + debugging capabilities
✅ Authentication flow fully implemented and HARDENED with multiple key format support
```

### Major Milestone: Enhanced Security & Stability (v1.5.1)

**Latest improvements focused on reliability and robustness:**

✅ **Enhanced Security Key Parsing** - Advanced PEM key parsing with BouncyCastle libraries
✅ **Improved Error Handling** - Better stability when connecting to Coinbase API with comprehensive fallback mechanisms
✅ **Fixed Navigation Issues** - Resolved duplicate TopAppBar display for cleaner UI
✅ **Strengthened JWT Generation** - More reliable token generation with better debugging
✅ **Enhanced Credential Injection** - Improved build-time credential handling with proper escape sequences
✅ **Professional Error Recovery** - Better user experience with retry mechanisms and clear error messages

### Current App Version

**Version:** 1.5.1 (latest stable)
**Key Improvements:**
- Enhanced security key parsing for more robust authentication
- Improved error handling and stability when connecting to Coinbase API
- Fixed navigation display issues for cleaner user interface
- Strengthened JWT token generation reliability
- Better debugging capabilities for troubleshooting connection issues

### What DOESN'T Exist Yet (Next Up)

```
✅ Domain models - Ticket 01 ✅ DONE
✅ Room database - Ticket 03 ✅ DONE
✅ UI Foundation - Tickets 05-09 ✅ DONE
✅ Dashboard screen - Ticket 10 ✅ DONE (with real data + enhanced error handling)
✅ Dashboard ViewModel - Ticket 12 ✅ DONE (complete state management + robust error handling)
✅ Coinbase REST API client - Ticket 13 ✅ DONE (accounts endpoint complete with enhanced security)
❌ Decision engine (regime switching logic) - Ticket 15
❌ Risk manager - Ticket 16
❌ Backtest validation - Phase 1B
❌ Coinbase WebSocket - Ticket 14 (full REST API comes first)
❌ Full REST API implementation (candles, orders, market data) - Phase 2
❌ Settings screen - Ticket 11 (refined, ready to implement)
❌ Trading service (foreground service) - Tickets 17-18

## Next Priorities (Post-v1.5.1)

1. **Decision Engine Implementation** (Ticket 15)
   - SMA(200), ADX(14), ATR(14) indicators with ta4j
   - Regime switching logic (DEFENSE/TREND/RANGE/WAIT)
   - Hysteresis to prevent whipsawing

2. **Risk Management** (Ticket 16)
   - Position sizing (max 2% risk per trade)
   - Drawdown monitoring (15% emergency stop)
   - Portfolio value tracking

3. **Strategy Validation**
   - Backtesting with historical data
   - Paper trading validation
   - Performance metrics tracking

4. **Full Trading Capability**
   - Complete REST API (orders, candles, market data)
   - WebSocket real-time updates
   - Automated trading service

## Dependencies Status

| Library | Version | Status | Usage |
|---------|---------|--------|-------|
| BouncyCastle bcprov-jdk18on | 1.78 | ✅ ACTIVE | PEM key parsing - crypto operations |
| BouncyCastle bcpkix-jdk18on | 1.78 | ✅ ACTIVE | PEM key parsing - certificate/key utilities |
| nimbus-jose-jwt | 9.47 | ✅ ACTIVE | ES256 JWT signing for Coinbase auth |
| ta4j-core | 0.16 | ⏳ Ready | Technical indicators (pending decision engine) |
| Ktor | 3.3.3 | ✅ ACTIVE | HTTP client with OkHttp engine |
| Room | 2.8.4 | ✅ ACTIVE | Database (4 entities + 4 DAOs) |
| Hilt | 2.57.2 | ✅ ACTIVE | Dependency injection |
| Compose BOM | 2025.12.01 | ✅ ACTIVE | UI framework |
| Kotlin | 2.3.0 | ✅ ACTIVE | Language |
| Timber | 5.0.1 | ✅ ACTIVE | Logging |
| Firebase BOM | 34.7.0 | ✅ ACTIVE | Analytics + Crashlytics |
| WorkManager | 2.10.0 | ⏳ Ready | Background tasks |
| Vico | 2.4.0 | ⏳ Ready | Charts (pending dashboard enhancement) |

**Legend:**
- ✅ ACTIVE (currently used in code and working)
- ⏳ Ready (configured, awaiting implementation)

## Architecture Status

**Clean Architecture Implementation:** ✅ COMPLETE
- Domain layer (pure Kotlin, no Android dependencies)
- Data layer with Repository pattern
- Presentation layer with MVVM + Compose
- Dependency injection with Hilt
- Exchange-specific implementations isolated

**Security Implementation:** ✅ HARDENED
- Build-time credential injection (no runtime credential entry needed)
- ES256 JWT with BouncyCastle cryptographic libraries
- Enhanced PEM key parsing with multiple format support
- Secure credential storage abstraction

**API Integration:** ✅ WORKING
- Coinbase Advanced Trade API authenticated connection
- Account balance fetching with error handling
- JWT token generation with comprehensive error recovery
- Repository pattern enables easy exchange swapping

**UI/UX:** ✅ PROFESSIONAL
- Material 3 design with dark theme
- Loading states and error recovery
- Real-time data display
- Clean navigation (fixed duplicate TopAppBar issue)

## Build & CI Status

**Latest Build:** #31 SUCCESS
**CI Pipeline:** ✅ Fully automated GitHub Actions
- Build-time credential injection from secrets
- Enhanced PEM key escape handling for environment variables
- Firebase App Distribution for testing
- Automated APK artifact uploads
- Auto-documentation updates with Claude API integration
- Commit-back pattern for build status tracking

---

## 🎯 Development Context

This app is **NOT intended for publication** - it's a personal trading tool. Focus on:

1. **Reliability over features** - Better to have fewer features that work perfectly
2. **Security first** - Handle API credentials and trading operations with maximum care  
3. **Conservative trading** - Small positions, strict risk management, treat as education
4. **Simple implementation** - Avoid over-engineering, keep codebase maintainable
5. **Thorough testing** - Test extensively before live trading with real money

**Critical Success Factors:**
- ✅ **Authentication robustness** - Enhanced security key parsing ensures reliable API connection
- ✅ **Error handling** - Comprehensive error recovery prevents crashes during trading operations
- ✅ **Clean architecture** - Repository pattern enables easy exchange integration and testing
- ❌ **Strategy validation** - Need backtesting before live deployment
- ❌ **Risk management** - Need position sizing and stop-loss logic before real trading

The app now has a solid foundation with live Coinbase data integration and enhanced security. Next priority is implementing the trading decision engine and risk management systems to enable actual automated trading capability.
