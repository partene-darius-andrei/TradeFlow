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
    │   └── AppNavHost.kt               ✅ Complete navigation with TopAppBar
    └── presentation/dashboard/
        ├── DashboardScreen.kt          ✅ Full implementation with real data integration
        ├── DashboardViewModel.kt       ✅ Complete state management + error handling
        └── components/
            ├── PortfolioCard.kt        ✅ Live balance data with "Live Data" indicator
            ├── ModeCard.kt             ✅ Trading mode + current price
            ├── ServiceCard.kt          ✅ Service status + start/stop button
            └── OrdersList.kt           ✅ Recent orders + empty state

🆕 APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants (white/black)
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

🆕 CREDENTIALS SYSTEM COMPLETE:
✅ app/build.gradle.kts                 ✅ Enhanced build-time credential injection with PEM escape handling
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig

🆕 COINBASE API DEPENDENCIES:
✅ BouncyCastle PEM key parsing libraries (bcprov-jdk18on, bcpkix-jdk18on)
✅ Advanced PEM key parsing with proper escape sequence handling for environment variables
✅ JWT token generation with ES256 and comprehensive error handling
✅ Authentication flow fully implemented and tested
```

### Major Milestone: Live Portfolio Integration Complete

**Dashboard now displays real Coinbase account balances with full error handling!**

✅ **Authentication working** - JWT tokens generated with ES256 and BouncyCastle
✅ **API integration working** - Successfully fetching account data with proper error handling
✅ **Loading states** - Proper loading indicators during API calls
✅ **Error handling** - Error display with retry functionality for network issues
✅ **Real-time data** - Portfolio card shows actual BTC/USD balances with live data indicator
✅ **Clean architecture** - Repository pattern with dependency injection enables easy exchange swapping
✅ **Advanced PEM parsing** - Enhanced PEM key handling with escape sequence support for CI/CD

This represents a complete working connection to live Coinbase data with professional error handling.

### Current App Version

**Version:** 1.5.0 (updated from 1.4.0)
**Key Features:**
- Improved authentication reliability with enhanced security key parsing
- Fixed app crashes related to JWT token generation  
- Resolved duplicate navigation bar display issue
- Enhanced error handling for better user experience when API calls fail
- Strengthened connection stability with Coinbase API integration
- Professional loading states and error recovery

### What DOESN'T Exist Yet (Next Up)

```
✅ Domain models - Ticket 01 ✅ DONE
✅ Room database - Ticket 03 ✅ DONE
✅ UI Foundation - Tickets 05-09 ✅ DONE
✅ Dashboard screen - Ticket 10 ✅ DONE (with real data + error handling)
✅ Dashboard ViewModel - Ticket 12 ✅ DONE (complete state management)
✅ Coinbase REST API client - Ticket 13 ✅ DONE (accounts endpoint complete)
❌ Decision engine (regime switching logic) - Ticket 15
❌ Risk manager - Ticket 16
❌ Backtest validation - Phase 1B
❌ Coinbase WebSocket - Ticket 14 (full REST API comes first)
❌ Full REST API implementation (candles, orders, market data) - Phase 2
❌ Settings screen - Ticket 11 (refined, ready to implement)
❌ Trading service (foreground service) - Tickets 17-18
```

---

## 🏗️ Tech Stack

### Core Platform
- **Android API 29+** (Android 10+) - Modern device support
- **Kotlin 2.3.0** - Latest stable with coroutines
- **Jetpack Compose BOM 2025.12.01** ✅ ACTIVE - Modern declarative UI
- **Hilt 2.57.2** ✅ ACTIVE - Dependency injection

### Persistence & Data
- **Room 2.8.4** ✅ ACTIVE - 4 entities + 4 DAOs implemented
- **DataStore Preferences 1.1.1** ⚠️ READY - Settings storage (pending settings screen)

### Networking & Auth
- **Ktor 3.3.3** ✅ ACTIVE - HTTP client with OkHttp engine, used for Coinbase API
- **nimbus-jose-jwt 9.47** ✅ ACTIVE - ES256 JWT signing for Coinbase authentication
- **BouncyCastle (bcprov-jdk18on, bcpkix-jdk18on) 1.78** ✅ ACTIVE - PEM key parsing with escape handling

### UI & User Experience
- **Material 3** ✅ ACTIVE - Design system implemented
- **Navigation Compose** ✅ ACTIVE - App navigation with TopAppBar
- **Vico 2.4.0** ⚠️ READY - Charts (pending decision engine visualization)
- **Timber 5.0.1** ✅ ACTIVE - Logging throughout app

### Trading & Analysis
- **ta4j-core 0.16** ⚠️ READY - Technical indicators (pending decision engine)
- **WorkManager 2.10.0** ⚠️ READY - Background tasks (pending trading service)

### Security & Credentials
- **Security-Crypto 1.1.0-alpha06** ⚠️ REPLACED - Replaced by build-time credential injection
- **Build-time credential injection** ✅ ACTIVE - GitHub secrets → environment variables → BuildConfig

### Observability
- **Firebase BOM 34.7.0** ✅ ACTIVE - Analytics + Crashlytics
- **CI/CD: GitHub Actions** ✅ ACTIVE - Auto-build + Firebase App Distribution

**Legend:**
- ✅ ACTIVE - Currently used in implemented code
- ⚠️ READY - Configured but pending implementation
- ❌ REMOVED - No longer needed

---

## 🔄 Development Workflow

### Working with Live Credentials

TradeFlow uses **build-time credential injection** (no runtime credential entry UI).

**For GitHub Actions (Automated):**
```yaml
# Credentials automatically injected from repo secrets
env:
  COINBASE_API_KEY: ${{ secrets.COINBASE_API_KEY }}
  COINBASE_API_SECRET: ${{ secrets.COINBASE_API_SECRET }}
```

**For Local Development:**
```properties
# local.properties (NOT committed)
coinbase.api.key=organizations/your-org/apiKeys/your-key
coinbase.api.secret=-----BEGIN EC PRIVATE KEY-----
MHcCAQEEI...
-----END EC PRIVATE KEY-----
```

### Build & Test Cycle

```bash
# 1. Implement feature locally
git add -A && git commit -m "Implement X"

# 2. Push to trigger CI/CD
git push origin claude/feature-name

# 3. Check build status
cat .build-status  # SUCCESS or FAILURE
cat build-log.txt  # If build failed

# 4. APK automatically uploaded to Firebase App Distribution
# Check phone for app update notification
```

### Key Commands

```bash
# Quick status check
git status && cat .build-status

# View last build log
cat build-log.txt

# Trigger manual build
gh workflow run build.yml

# Download APK locally  
gh run download --name debug-apk
```

---

## 📁 Project Structure

```
TradeFlow/
├── app/                              # Main app module
│   ├── src/main/java/com/dpart/tradeflow/
│   │   ├── MainActivity.kt           ✅ Simple host activity
│   │   ├── TradeFlowApp.kt           ✅ Application class
│   │   ├── di/CredentialsModule.kt   ✅ Build-time credential injection
│   │   ├── navigation/AppNavHost.kt  ✅ Compose navigation with TopAppBar
│   │   └── presentation/
│   │       └── dashboard/            ✅ Complete dashboard with real data
│   └── build.gradle.kts              ✅ Enhanced credential injection + escape handling
│
├── core/
│   ├── domain/                       ✅ Pure Kotlin domain layer
│   ├── data/                         ✅ Room database + static credentials
│   └── ui/                           ✅ Shared UI components
│
├── exchange/
│   └── coinbase/                     ✅ Complete Coinbase integration
│       ├── auth/CoinbaseJwtGenerator.kt     ✅ ES256 JWT with BouncyCastle
│       ├── api/CoinbaseApiClient.kt         ✅ Ktor HTTP client
│       ├── repository/CoinbaseRepository.kt ✅ Domain interface implementation
│       └── dto/AccountDto.kt                ✅ API response DTOs
│
└── docs/                             ✅ Complete documentation
    ├── roadmap.md                    # Implementation phases
    ├── reference.md                  # Technical blueprint
    └── tickets/                      # All requirements
```

---

## 🎯 Current Development Focus

### NEXT: Trading Logic Implementation

**Priority Order:**
1. **Decision Engine (Ticket 15)** - SMA/ADX/ATR regime switching
2. **Risk Manager (Ticket 16)** - Position sizing + stop losses
3. **Strategy Backtesting** - Historical validation
4. **Full REST API** - Orders, candles, market data
5. **Trading Service** - 24/7 background execution

**Why This Order:**
- Logic layer foundational (can unit test without API)
- Risk management critical before live trading
- Backtesting proves strategy before real money
- Service layer last (orchestrates everything)

### Key Files to Implement Next

```kotlin
// Ticket 15: Decision Engine
core/domain/src/main/kotlin/com/tradeflow/core/domain/strategy/
├── DecisionEngine.kt           # Interface
├── EngineDecisionEngine.kt     # Implementation with ta4j
└── StrategyConfig.kt           # Risk parameters

// Ticket 16: Risk Manager  
core/domain/src/main/kotlin/com/tradeflow/core/domain/risk/
├── RiskManager.kt              # Interface
├── TradingRiskManager.kt       # Implementation
└── RiskConfig.kt               # Limits (15% drawdown, 5% position)
```

### Success Criteria for Next Phase

- [ ] Decision engine correctly detects DEFENSE/TREND/RANGE modes
- [ ] Risk manager enforces 15% drawdown limit
- [ ] Backtesting shows 52%+ win rate on historical data  
- [ ] Unit tests cover all decision logic paths
- [ ] Integration with existing dashboard (show current mode)

---

## 🚨 Risk Management Philosophy

**Core Principle:** Assume 97% failure rate. Build for the 3% who succeed.

**Account Size Strategy:**
- **$500 → $2,500:** BTC/USDT only (altcoins killed by fees)
- **Risk per trade:** 1-2% ($5-10 max loss)
- **Position size:** 10% of account max
- **Drawdown limit:** 15% emergency stop
- **Timeline:** 5-10 years to meaningful income

**What Kills Small Accounts:**
- Trading small-cap altcoins (3-15% round-trip costs)
- Overleveraging (>5% risk per trade)
- No stop losses (letting losers run)
- Overtrading (fees compound quickly)
- Emotional decisions (why we automate)

**The Math That Matters:**
- Need 60%+ win rate at 1:1 R:R to overcome 0.25% fees
- OR 50% win rate at 2:1 R:R
- Anything less = slow account decay
- Hence focus on Bitcoin (tightest spreads, best data)

---

## 📞 Emergency Contacts & Resources

### If Something Breaks

**Build Failures:**
1. Check `.build-status` and `build-log.txt`  
2. Review recent commits for syntax errors
3. Check [docs/ci.md](docs/ci.md) for troubleshooting

**API Issues:**
1. Verify Coinbase API status: https://status.coinbase.com
2. Check rate limits (10,000/hour REST)
3. Validate JWT tokens haven't expired

**Strategy Issues:**
1. Review [docs/strategy/bitcoin-first-strategy.md](docs/strategy/bitcoin-first-strategy.md)
2. Check backtesting results vs. live performance
3. Consider market regime changes

### Key Resources

- **Coinbase API Docs:** https://docs.cdp.coinbase.com/advanced-trade/docs/welcome
- **ta4j Documentation:** https://ta4j.github.io/ta4j-wiki/
- **Material 3 Guidelines:** https://m3.material.io/
- **Repository:** https://github.com/partene-darius-andrei/TradeFlow

---

## 🎓 Learning & Improvement

### Code Quality Standards

- **Architecture:** Clean Architecture with clear layer separation
- **Testing:** Unit tests for domain logic, integration tests for API
- **Documentation:** Every public interface documented
- **Logging:** Timber for debug info, never log credentials
- **Security:** All sensitive data encrypted or injected at build time

### Performance Targets

- **UI:** 60 FPS on mid-range devices (Compose best practices)
- **API:** <2 second response times for critical calls
- **Battery:** Minimal impact when not actively trading
- **Memory:** <100MB steady state (Room query optimization)

### Trading Performance Targets

- **Win Rate:** 52-58% (realistic for retail algo trading)
- **Sharpe Ratio:** 1.0+ (risk-adjusted returns)
- **Max Drawdown:** <15% (hard stop)
- **Capital Efficiency:** 5-10% portfolio risk per trade max

Remember: The goal isn't to get rich quick. It's to build a system that compounds small edges consistently over years.
