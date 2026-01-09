# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-09
**Project Status:** Phase 2 Complete - Core Trading Logic (v1.10.0)
**Current Build:** #31 SUCCESS

This is the entry point for Claude Code when working with TradeFlow. All essential context, navigation, and workflows are documented here.

---

## 🎯 Quick Navigation

| Document | Purpose | Use When |
|----------|---------|----------|
| **[docs/roadmap.md](docs/roadmap.md)** | Implementation roadmap organized in phases | Planning what to build next |
| **[docs/README.md](docs/README.md)** | Complete documentation index and ticket mapping | Finding specific documentation |
| **[docs/reference.md](docs/reference.md)** | Implementation blueprint with code examples | Implementing features |
| **[docs/ci-claude-integration.md](docs/ci-claude-integration.md)** | CI/CD workflows with Claude API integration | Understanding build pipeline |
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

### What EXISTS (Phases 1 & 2 COMPLETE - v1.10.0)

```
✅ Modern Android app structure
✅ Hilt dependency injection configured
✅ Room database with complete schema (4 entities + 4 DAOs)
✅ Ktor HTTP client configured (OkHttp engine)
✅ Timber logging initialized
✅ Firebase Analytics + Crashlytics
✅ All trading dependencies added (ta4j, nimbus-jose-jwt, security-crypto)
✅ GitHub Actions CI/CD pipeline with INTELLIGENT CLAUDE INTEGRATION
✅ Adaptive app icon with trading chart design (day/night variants)

✅ ENHANCED CI/CD WITH CLAUDE API (v1.10.0):
✅ Intelligent build failure analysis with Claude API
✅ Automatic fix recommendations on build failures
✅ Automated version management with semantic versioning
✅ Release notes generation based on commit analysis
✅ No infinite loops protection with [claude-fix] markers
✅ Manual intervention detection for persistent failures

✅ DOMAIN LAYER COMPLETE (v1.10.0):
✅ core/domain/src/main/kotlin/com/tradeflow/core/domain/
    ├── auth/
    │   ├── AuthTokenProvider.kt        ✅ Token generation interface
    │   └── CredentialStore.kt          ✅ Secure credential storage interface
    ├── error/
    │   └── ExchangeError.kt           ✅ Exchange error types (6 variants)
    ├── model/ ← COMPLETE
    │   ├── Candle.kt                  ✅ OHLCV + Granularity enum (9 timeframes)
    │   ├── Order.kt                   ✅ Order model + Side/Type/Status enums
    │   ├── Decision.kt                ✅ Enhanced sealed class with technical indicators (Wait/Defense/Trend/Range)
    │   ├── Portfolio.kt               ✅ Portfolio snapshot model + utility extensions
    │   ├── Balance.kt                 ✅ Account balance model
    │   └── Ticker.kt                  ✅ Real-time price ticker
    ├── repository/
    │   ├── BracketOrderRepository.kt   ✅ Bracket order support interface
    │   ├── ExchangeRepository.kt       ✅ Core exchange operations (12 methods)
    │   ├── ExchangeWebSocket.kt        ✅ Real-time data streams
    │   └── TradingDataRepository.kt    ✅ Local trading data queries
    ├── strategy/ ← COMPLETE
    │   ├── DecisionEngine.kt           ✅ Decision engine interface
    │   ├── TradingDecisionEngine.kt    ✅ Complete regime-switching implementation with hysteresis + DI
    │   └── StrategyConfig.kt           ✅ Comprehensive strategy parameters
    ├── indicator/ ← COMPLETE with DI
    │   ├── SMACalculator.kt            ✅ Simple Moving Average with ta4j integration + @Inject
    │   ├── ADXCalculator.kt            ✅ Average Directional Index with ta4j integration + @Inject
    │   └── ATRCalculator.kt            ✅ Average True Range with ta4j integration + @Inject
    ├── risk/
    │   └── RiskManager.kt              ✅ Risk management interface with enhanced types + @Inject
    └── usecase/ ← COMPLETE IMPLEMENTATION
        ├── ExecuteDecisionUseCase.kt   ✅ Trading decision execution orchestrator
        ├── ExecuteTradingCycleUseCase.kt ✅ Complete trading cycle with risk management (UPDATED v1.10.0)
        ├── HandleEmergencyUseCase.kt   ✅ Emergency liquidation handler
        ├── HandleGridFillsUseCase.kt   ✅ Grid order fill detection and profit taking
        ├── ManageGridOrdersUseCase.kt  ✅ Grid order management for range trading
        ├── ManageOrdersUseCase.kt      ✅ Order lifecycle and reconciliation
        ├── UpdatePortfolioUseCase.kt   ✅ Portfolio state updates
        └── model/
            ├── ExecutionResult.kt      ✅ Use case result types (Success/Skipped/Failed)
            └── TradingContext.kt       ✅ Trading context data model

✅ DATA LAYER COMPLETE (v1.10.0):
✅ core/data/src/main/kotlin/com/tradeflow/core/data/
    ├── local/ ← COMPLETE
    │   ├── database/
    │   │   └── EngineDatabase.kt       ✅ Room database with 4 tables
    │   ├── entity/
    │   │   ├── CandleEntity.kt        ✅ Room entity for candles
    │   │   ├── OrderEntity.kt         ✅ Room entity for orders
    │   │   ├── DecisionEntity.kt      ✅ Room entity for decisions
    │   │   └── PortfolioSnapshotEntity.kt ✅ Room entity for portfolio
    │   └── dao/
    │       ├── CandleDao.kt           ✅ CRUD + delete old candles
    │       ├── OrderDao.kt            ✅ CRUD + query by status/product + filled orders
    │       ├── DecisionDao.kt         ✅ CRUD + latest decision query
    │       └── PortfolioDao.kt        ✅ CRUD + snapshot history + high water mark queries
    ├── security/
    │   └── StaticCredentialStore.kt    ✅ Static credential injection
    ├── mapper/
    │   └── OrderMapper.kt              ✅ OrderEntity ↔ Order domain model mapping
    ├── repository/
    │   ├── TradingDataRepositoryImpl.kt ✅ Implementation for local data queries
    │   └── PortfolioRepositoryImpl.kt   ✅ Portfolio data repository (ENHANCED v1.10.0)
    └── di/
        ├── SecurityModule.kt           ✅ Static credential DI binding
        └── RepositoryModule.kt         ✅ Repository DI bindings (ENHANCED v1.10.0)

✅ COINBASE INTEGRATION COMPLETE:
✅ exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt     ✅ ES256 JWT with comprehensive BouncyCastle PEM parsing
    ├── api/
    │   └── CoinbaseApiClient.kt        ✅ Complete Ktor-based API client (accounts)
    ├── dto/
    │   └── AccountDto.kt               ✅ Account DTOs for API responses  
    ├── mapper/
    │   └── AccountMapper.kt            ✅ DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt       ✅ Implementation (getBalances working)
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

✅ PRESENTATION LAYER WITH ENHANCED LIVE DATA:
✅ app/src/main/java/com/dpart/tradeflow/
    ├── navigation/
    │   └── AppNavHost.kt               ✅ Complete navigation with centralized TopAppBar
    ├── presentation/dashboard/
    │   ├── DashboardScreen.kt          ✅ Complete implementation with real data integration
    │   ├── DashboardViewModel.kt       ✅ Full state management + robust error handling
    │   └── components/
    │       ├── PortfolioCard.kt        ✅ Live data, BTC/USD balances
    │       ├── ModeCard.kt             ✅ Trading mode + current price
    │       ├── ServiceCard.kt          ✅ Service status + start/stop button
    │       └── OrdersList.kt           ✅ Recent orders + empty state
    └── di/
        └── DomainModule.kt             ✅ DecisionEngine DI binding

✅ APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

✅ ENHANCED CREDENTIALS SYSTEM:
✅ app/build.gradle.kts                 ✅ Advanced build-time credential injection
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig
```

### What DOESN'T EXIST YET (Phase 3)

```
❌ Full Coinbase REST API client (orders, candles, products)
❌ Coinbase WebSocket client (real-time feeds)
❌ Risk manager implementation (position sizing, drawdown)
❌ Trading service (24/7 background execution)
❌ Integration testing with real API
❌ Live trading capability

Estimated: 3-4 weeks to first live trade
```

---

## 🏗️ Tech Stack & Dependencies

### Core Framework
- **Kotlin:** 2.3.0
- **Android:** minSdk 29, targetSdk 36
- **Compose:** BOM 2025.12.01 (Material 3)
- **Gradle:** 8.13

### Architecture & DI
- **Hilt:** 2.57.2 ✅ ACTIVE (dependency injection)
- **Room:** 2.8.4 ✅ ACTIVE (local database with 4 entities)
- **Coroutines:** 1.10.2 ✅ ACTIVE (async operations)

### Network & Security
- **Ktor:** 3.3.3 ✅ ACTIVE (HTTP client with OkHttp engine)
- **nimbus-jose-jwt:** 9.47 ✅ ACTIVE (ES256 JWT signing)
- **BouncyCastle:** 1.78 ✅ ACTIVE (advanced PEM parsing)
- **security-crypto:** 1.1.0-alpha06 ⚠️ CONFIGURED

### Trading & Analytics
- **ta4j-core:** 0.16 ✅ ACTIVE (technical indicators - SMA/ADX/ATR)
- **work-runtime-ktx:** 2.11.0 ⚠️ CONFIGURED (background tasks)

### Testing
- **mockk:** 1.14.7 ✅ ACTIVE (unit testing with mocks)
- **kotlin-test:** 2.1.0 ✅ ACTIVE (testing framework)

### Utilities
- **Timber:** 5.0.1 ✅ ACTIVE (logging)
- **Vico:** 2.4.0 ⚠️ CONFIGURED (charts - future use)

**Legend:**
- ✅ ACTIVE = Currently used in implemented code
- ⚠️ CONFIGURED = Added to build but not yet used
- ❌ MISSING = Needed but not added

---

## ⚡ CI/CD Integration

### Intelligent Build Pipeline (v1.10.0)

**TradeFlow features advanced CI/CD with Claude API integration for intelligent build failure analysis and automated version management.**

#### Key Features:

**🤖 Intelligent Build Failure Analysis:**
- Automatically analyzes build failures with Claude API
- Provides specific fix recommendations
- Commits analysis files for developer review
- Prevents infinite loops with `[claude-fix]` markers

**📋 Automated Version Management:**
- Analyzes recent commits to determine version bump type
- Updates `versionName` and `versionCode` automatically
- Generates contextual release notes
- Follows semantic versioning (major.minor.patch)

**🔄 Workflow Process:**
```
Push Code → Run Tests + Build → 
  ├─ SUCCESS: Update version + release notes → Build APK → Upload
  └─ FAILURE: Claude analysis → Commit fix instructions → Retrigger
```

#### Required Secrets:

| Secret | Purpose | Required For |
|--------|---------|--------------|
| `ANTHROPIC_API_KEY` | Claude API access | Build analysis & version management |
| `COINBASE_API_KEY` | Embedded in APK | Trading functionality |
| `COINBASE_API_SECRET` | Embedded in APK | Authentication |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | App distribution | APK deployment |

**See:** [docs/ci-claude-integration.md](docs/ci-claude-integration.md) for complete pipeline documentation.

---

## 📁 Project Structure

### Modular Architecture
```
TradeFlow/
├── app/                    # Main application + DI wiring
├── core/
│   ├── domain/            # ✅ Pure Kotlin (business logic)
│   ├── data/              # ✅ Room + repositories  
│   └── ui/                # ✅ Shared Compose components
├── exchange/
│   └── coinbase/          # ✅ Coinbase API integration
├── feature/
│   ├── dashboard/         # ✅ Portfolio + trading status
│   ├── trading/           # ❌ TODO: Trading execution
│   └── settings/          # ✅ App configuration
└── build-logic/           # ✅ Gradle convention plugins
```

### Key Directories
- **`docs/`** - Complete documentation (roadmap, tickets, guides)
- **`.github/workflows/`** - CI/CD with Claude integration
- **`app/src/main/res/`** - Adaptive icon + resources

---

## 🎯 Development Workflow

### For Implementation (Claude Code)

1. **Check current status:** Read [docs/roadmap.md](docs/roadmap.md) for next tickets
2. **Read ticket details:** Find specific ticket in [docs/tickets/backlog/](docs/tickets/backlog/)
3. **Reference implementation:** Use [docs/reference.md](docs/reference.md) for code patterns
4. **Push changes:** CI/CD handles build + analysis automatically
5. **Review results:** Check `.build-status` and `build-log.txt` if needed

### For Testing
- **Local:** `./gradlew testDebugUnitTest` for unit tests
- **Integration:** Small real trades with live API (when ready)
- **APK:** Download from Firebase App Distribution

### For Troubleshooting
- **Build failures:** Check Claude's analysis in `fix-instructions.txt`
- **API issues:** Review [docs/api/coinbase.md](docs/api/coinbase.md)
- **Dependencies:** Verify versions in [docs/roadmap.md](docs/roadmap.md)

---

## 🚨 Critical Constraints

| Constraint | Value | Impact |
|------------|-------|--------|
| **Coinbase fees** | 0.60% maker (intro tier) | Grid spacing ≥ 1.5% |
| **JWT expiry** | 2 minutes | Regenerate per API call |
| **Max candles/request** | 350 | Multiple requests for H4 data |
| **WebSocket timeout** | 60-90 seconds | Must use heartbeats |
| **Rate limits** | 10,000 requests/hour | Use WebSocket for real-time |
| **Position size** | 2% portfolio max | Risk management critical |
| **Drawdown limit** | 15% → emergency stop | Hardcoded safety |

---

## 📈 Success Metrics

### Technical Milestones
- [x] **Phase 1:** Foundation + API integration (COMPLETE)
- [x] **Phase 2:** Trading logic + decision engine (COMPLETE) 
- [ ] **Phase 3:** Full API + service implementation (IN PROGRESS)
- [ ] **Phase 4:** Live trading validation (PENDING)

### Performance Targets
- **Win rate:** 52-58% (realistic for directional trading)
- **Risk per trade:** 1-2% of portfolio
- **Monthly returns:** 3-5% (exceptional skill required)
- **Drawdown:** Never exceed 15% (automatic stop)

**Remember:** 97% of day traders fail. Treat initial $500 as education cost, not expected profit.

