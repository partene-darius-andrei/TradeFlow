# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-09
**Project Status:** Phase 2 Complete - Core Trading Logic (v1.6.0)
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

### What EXISTS (Phases 1 & 2 COMPLETE - v1.6.0)

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

✅ DOMAIN LAYER COMPLETE:
✅ core/domain/src/main/kotlin/com/tradeflow/core/domain/
    ├── auth/
    │   ├── AuthTokenProvider.kt        ✅ Token generation interface
    │   └── CredentialStore.kt          ✅ Secure credential storage interface
    ├── error/
    │   └── ExchangeError.kt           ✅ Exchange error types (6 variants)
    ├── model/ ← Ticket 01 COMPLETE
    │   ├── Candle.kt                  ✅ OHLCV + Granularity enum (9 timeframes)
    │   ├── Order.kt                   ✅ Order model + Side/Type/Status enums
    │   ├── Decision.kt                ✅ Enhanced sealed class with technical indicators (Wait/Defense/Trend/Range)
    │   ├── Portfolio.kt               ✅ Portfolio snapshot model
    │   ├── Balance.kt                 ✅ Account balance model
    │   └── Ticker.kt                  ✅ Real-time price ticker
    ├── repository/
    │   ├── BracketOrderRepository.kt   ✅ Bracket order support interface
    │   ├── ExchangeRepository.kt       ✅ Core exchange operations (12 methods)
    │   └── ExchangeWebSocket.kt        ✅ Real-time data streams
    └── strategy/ ← Ticket 15 COMPLETE ✅
        ├── DecisionEngine.kt           ✅ Decision engine interface
        ├── TradingDecisionEngine.kt    ✅ Complete regime-switching implementation with hysteresis
        └── StrategyConfig.kt           ✅ Comprehensive strategy parameters

🆕 TECHNICAL INDICATORS COMPLETE (v1.6.0):
✅ core/domain/src/main/kotlin/com/tradeflow/core/domain/indicator/
    ├── SMACalculator.kt                ✅ Simple Moving Average with ta4j integration
    ├── ADXCalculator.kt                ✅ Average Directional Index with ta4j integration
    └── ATRCalculator.kt                ✅ Average True Range with ta4j integration

✅ DATA LAYER COMPLETE:
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

✅ COINBASE INTEGRATION COMPLETE (v1.5.5):
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
    │   └── CoinbaseRepository.kt       ✅ Implementation (getBalances working, others TODO for Phase 3)
    └── di/
        ├── AuthModule.kt               ✅ JWT generator DI binding
        └── ExchangeModule.kt           ✅ Repository DI binding

✅ UI COMPONENTS COMPLETE:
✅ core/ui/src/main/kotlin/com/tradeflow/core/ui/
    ├── component/
    │   ├── ErrorDisplay.kt             ✅ Error state with retry button
    │   ├── LoadingButton.kt            ✅ Button with loading spinner
    │   ├── ModeIndicator.kt            ✅ Trading mode badges (DEFENSE/TREND/RANGE)
    │   ├── PriceDisplay.kt             ✅ Price with +/- color coding
    │   └── StatusCard.kt               ✅ Reusable card container
    └── extension/
        └── BigDecimalExt.kt           ✅ Currency/percentage formatting

✅ PRESENTATION LAYER WITH ENHANCED LIVE DATA (v1.5.5):
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

✅ APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants (white/black)
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

✅ ENHANCED CREDENTIALS SYSTEM (v1.5.5):
✅ app/build.gradle.kts                 ✅ ADVANCED build-time credential injection with ENHANCED PEM key escaping + security improvements
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig
```

### 🎉 Major Milestone: Core Trading Engine Complete (v1.6.0)

**Phase 2 has been successfully completed with the implementation of the complete decision engine:**

🆕 **Decision Engine Implementation (Ticket 15 - v1.6.0):**
- ✅ Complete regime-switching decision engine with hysteresis logic
- ✅ SMA(200) trend filter for bull/bear market detection
- ✅ ADX(14) trend strength analysis (>25 = trending, <25 = ranging)
- ✅ ATR(14) volatility-based position sizing and stop placement
- ✅ 3-candle confirmation for mode switches (prevents whipsaws)
- ✅ Support for all 4 trading modes: DEFENSE/TREND/RANGE/WAIT

🆕 **Technical Indicators (NEW):**
- ✅ SMACalculator with ta4j BaseBarSeries integration
- ✅ ADXCalculator for trend strength measurement
- ✅ ATRCalculator for volatility-based risk management
- ✅ Complete error handling and validation

🆕 **Strategy Configuration:**
- ✅ Configurable parameters (SMA period: 200, ADX period: 14, ATR period: 14)
- ✅ Risk management controls (stop-loss: 3x ATR, take-profit: 6x ATR)
- ✅ Position sizing (trend: 5%, grid: 2% per level)
- ✅ Grid spacing controls (minimum 1.5% for fee break-even)

🆕 **Comprehensive Unit Testing:**
- ✅ MockK integration for fast, isolated testing
- ✅ Edge case coverage (insufficient candles, mode switching, hysteresis)
- ✅ Validation testing (grid spacing, stop-loss placement)
- ✅ All decision paths tested with synthetic candle data

### Current App Version

**Version:** 1.6.0 (latest stable with complete decision engine)
**Key Features:**
- ✅ Complete decision engine with regime switching
- ✅ Technical indicator calculations using ta4j library
- ✅ Hysteresis logic to prevent false signals and overtrading
- ✅ Support for all 4 trading modes with proper validation
- ✅ Enhanced testing capabilities with MockK and Kotlin Test
- ✅ Comprehensive strategy configuration system

### Tech Stack Status

| Component | Version | Status | Usage |
|-----------|---------|--------|-------|
| **Kotlin** | 2.3.0 | ✅ Active | Language |
| **Compose BOM** | 2025.12.01 | ✅ Active | UI framework |
| **Hilt** | 2.57.2 | ✅ Active | DI |
| **Room** | 2.8.4 | ✅ Active | Database (4 entities + 4 DAOs) |
| **Ktor** | 3.3.3 | ✅ Active | HTTP client (auth complete) |
| **Timber** | 5.0.1 | ✅ Active | Logging |
| **ta4j-core** | 0.16 | ✅ Active | Technical indicators (SMA/ADX/ATR) |
| **nimbus-jose-jwt** | 9.47 | ✅ Active | ES256 JWT signing |
| **BouncyCastle** | 1.78 | ✅ Active | Advanced PEM key parsing |
| **mockk** | 1.14.7 | ✅ Active | Unit testing with mocks |
| **kotlin-test** | 2.1.0 | ✅ Active | Testing framework |
| **security-crypto** | 1.1.0 | ✅ Active | Credential encryption |
| **work-runtime-ktx** | 2.11.0 | ✅ Ready | Background tasks |
| **datastore-preferences** | 1.2.0 | ✅ Ready | Settings storage |
| **Firebase BOM** | 34.7.0 | ✅ Active | Analytics + Crashlytics |

### Dependencies Status

| Status | Count | Description |
|--------|-------|-------------|
| ✅ **Active** | 12 | Currently used in implemented code |
| ⏳ **Ready** | 2 | Configured, awaiting implementation |

---

## 🚦 What's NEXT (Phase 3)

### Phase 3: API Integration & Service Implementation

**Goal:** Connect decision engine to live trading

| Priority | Ticket | Component | Description |
|----------|--------|-----------|-------------|
| **HIGH** | 13 | **Full REST API Client** | Order placement, candle fetching, product queries |
| **HIGH** | 14 | **WebSocket Client** | Real-time price feeds, order status updates |
| **HIGH** | 16 | **Risk Manager** | Position sizing, drawdown monitoring, emergency stops |
| **MEDIUM** | 17 | **Trading Service** | 24/7 foreground service with trading loop |
| **LOW** | 19 | **Integration Tests** | End-to-end testing with small real trades |

### Critical Path to First Live Trade

1. ✅ ~~Decision Engine~~ - COMPLETE (v1.6.0)
2. **REST API Client** (Ticket 13) ← IMMEDIATE NEXT
3. **WebSocket Client** (Ticket 14)
4. **Risk Manager** (Ticket 16)
5. **Trading Service** (Ticket 17)

**Estimated completion:** 3-4 weeks at current pace

---

## 🏗️ Module Status

| Module | Purpose | Status | Completion |
|--------|---------|--------|-----------|
| `:app` | DI wiring + credential injection | ✅ Complete | 100% |
| `:core:domain` | Pure Kotlin interfaces + models + **strategy** | ✅ Complete | 100% |
| `:core:data` | Room database + security | ✅ Complete | 100% |
| `:core:ui` | Shared Compose components | ✅ Complete | 100% |
| `:exchange:coinbase` | Coinbase API integration | 🟡 Partial | 40% (auth ✅, REST ❌, WS ❌) |

**Legend:**
- ✅ Complete (90-100%)
- 🟡 In Progress (25-89%) 
- ❌ Not Started (0-24%)

---

## 📈 Progress Summary

### Overall Progress: 60% Complete

```
Phase 1:  ████████████████████ 100% ✅ COMPLETE
Phase 2:  ████████████████████ 100% ✅ COMPLETE  
Phase 3:  ████░░░░░░░░░░░░░░░░  20% ← YOU ARE HERE

Total Tickets: 11/18 complete (61%)
```

### Phase Breakdown

| Phase | Focus | Status | Tickets |
|-------|-------|--------|---------|
| **Phase 1** | Foundation & Auth | ✅ Complete | 4/4 |
| **Phase 2** | Trading Logic | ✅ Complete | 3/3 |
| **Phase 3** | API & Service | 🟡 In Progress | 1/5 |

---

## 🔧 Development Workflow

### Working with GitHub Actions CI

**Pattern optimized for Claude Code:**

```bash
# 1. Implement feature
git add .
git commit -m "Implement feature X"
git push

# 2. GitHub Actions automatically:
# - Injects Coinbase credentials from secrets
# - Builds APK with embedded credentials  
# - Runs unit tests
# - Updates documentation via Claude API
# - Commits results back to branch

# 3. Check results
git pull
cat .build-status  # SUCCESS or FAILURE
```

**No local Gradle execution needed** - perfect for mobile development with Claude Code.

### Quick Commands

```bash
# Check build status
cat .build-status

# View build log if failure
cat build-log.txt

# Run specific tests (when testing locally)
./gradlew :core:domain:test --tests="*TradingDecisionEngineTest*"

# Clean build
./gradlew clean
```

### Architecture Principles

1. **Domain-first:** Core logic in pure Kotlin (no Android deps)
2. **Interface-driven:** Repository pattern isolates Coinbase code
3. **Testable:** Unit tests run without Android SDK
4. **Simple:** No complex ML or over-engineering
5. **Safe:** Multiple safety layers (risk manager, emergency stops)

---

## 💡 Key Implementation Notes

### Trading Strategy (Implemented in v1.6.0)

```kotlin
// Core decision logic
when {
    currentPrice < sma200 -> Decision.Defense()      // Safety first
    adx > 25.0 && confirmed -> Decision.Trend()     // Strong trend  
    adx < 25.0 && confirmed -> Decision.Range()     // Weak trend (grid)
    else -> Decision.Wait()                          // Need confirmation
}
```

### Risk Management (Next Phase)

- **Position sizing:** 2% risk per trade (max $10 loss on $500 account)
- **Drawdown limit:** 15% from high-water mark (emergency liquidation)
- **Grid spacing:** Minimum 1.5% (fee break-even at intro tier)
- **Stop-losses:** 3x ATR distance (volatility-adjusted)

### Testing Strategy

- **Unit tests:** Fast, isolated testing with MockK
- **Integration tests:** Small real trades ($10-20) to verify API
- **Paper trading:** Not possible (Coinbase sandbox returns static data)
- **Monitoring:** Comprehensive logging + Firebase Crashlytics

---

This document serves as the single source of truth for TradeFlow's current state and next steps. All major architectural decisions, implementation status, and development workflows are captured here for efficient Claude Code development.

