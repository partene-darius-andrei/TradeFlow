# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-09
**Project Status:** Phase 2 Complete - Core Trading Logic (v1.8.0)
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

### What EXISTS (Phases 1 & 2 COMPLETE - v1.8.0)

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

✅ DOMAIN LAYER COMPLETE (v1.8.0):
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
    │   └── TradingDataRepository.kt    ✅ Local trading data queries (NEW in v1.8.0)
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
    └── usecase/ ← COMPLETE IMPLEMENTATION (v1.8.0)
        ├── ExecuteDecisionUseCase.kt   ✅ Trading decision execution orchestrator
        ├── ExecuteTradingCycleUseCase.kt ✅ Complete trading cycle with risk management
        ├── HandleEmergencyUseCase.kt   ✅ Emergency liquidation handler
        ├── HandleGridFillsUseCase.kt   ✅ Grid order fill detection and profit taking (NEW)
        ├── ManageGridOrdersUseCase.kt  ✅ Grid order management for range trading
        ├── ManageOrdersUseCase.kt      ✅ Order lifecycle and reconciliation
        ├── UpdatePortfolioUseCase.kt   ✅ Portfolio state updates
        └── model/
            ├── ExecutionResult.kt      ✅ Use case result types (Success/Skipped/Failed)
            └── TradingContext.kt       ✅ Trading context data model

✅ DATA LAYER COMPLETE (v1.8.0):
✅ core/data/src/main/kotlin/com/tradeflow/core/data/
    ├── local/ ← COMPLETE
    │   ├── database/
    │   │   └── EngineDatabase.kt       ✅ Room database with 4 tables
    │   ├── entity/
    │   │   ├── CandleEntity.kt        ✅ Room entity for candles
    │   │   ├── OrderEntity.kt         ✅ Room entity for orders + getRecentFilledOrders()
    │   │   ├── DecisionEntity.kt      ✅ Room entity for decisions
    │   │   └── PortfolioSnapshotEntity.kt ✅ Room entity for portfolio
    │   └── dao/
    │       ├── CandleDao.kt           ✅ CRUD + delete old candles
    │       ├── OrderDao.kt            ✅ CRUD + query by status/product + filled orders
    │       ├── DecisionDao.kt         ✅ CRUD + latest decision query
    │       └── PortfolioDao.kt        ✅ CRUD + snapshot history + high water mark queries
    ├── security/
    │   └── StaticCredentialStore.kt    ✅ Static credential injection (replaces UI input)
    ├── mapper/
    │   └── OrderMapper.kt              ✅ OrderEntity ↔ Order domain model mapping (NEW)
    ├── repository/
    │   ├── TradingDataRepositoryImpl.kt ✅ Implementation for local data queries (NEW)
    │   └── PortfolioRepositoryImpl.kt   ✅ Portfolio data repository (NEW)
    └── di/
        ├── SecurityModule.kt           ✅ Static credential DI binding
        └── RepositoryModule.kt         ✅ Repository DI bindings (ENHANCED)

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
    ├── presentation/dashboard/
    │   ├── DashboardScreen.kt          ✅ Complete implementation with ENHANCED real data integration
    │   ├── DashboardViewModel.kt       ✅ Full state management + ROBUST error handling + loading states
    │   └── components/
    │       ├── PortfolioCard.kt        ✅ Live data, BTC/USD balances with "Live Data" indicator + enhanced formatting
    │       ├── ModeCard.kt             ✅ Trading mode + current price
    │       ├── ServiceCard.kt          ✅ Service status + start/stop button
    │       └── OrdersList.kt           ✅ Recent orders + empty state
    └── di/
        └── DomainModule.kt             ✅ DecisionEngine DI binding (NEW in v1.8.0)

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

### 🎉 Major Milestone: Complete Trading Engine Implementation (v1.8.0)

**Phase 2 has been successfully completed with comprehensive trading engine and use case implementation:**

🆕 **Complete Use Case Layer Implementation (v1.8.0):**
- ✅ **ExecuteDecisionUseCase** - Complete decision execution orchestrator
  - Handles all 4 trading modes (DEFENSE/TREND/RANGE/WAIT)
  - Risk validation before order placement
  - Bracket order support for trend trading
  - Grid order management for range trading
  - Comprehensive duplicate prevention

- ✅ **ExecuteTradingCycleUseCase** - Master trading orchestrator (UPDATED in v1.8.0)
  - Portfolio updates with high water mark tracking
  - Drawdown monitoring with emergency liquidation at 15%
  - Complete trading cycle with safety-first design
  - Grid fill handling integration
  - Order reconciliation and management
  - **REMOVED** direct ExchangeRepository dependency (uses individual use cases now)

- ✅ **HandleEmergencyUseCase** - Emergency liquidation handler
  - Order cancellation before liquidation
  - Market sell all BTC positions
  - Comprehensive error handling and recovery
  - Safety-first emergency protocols

- ✅ **ManageGridOrdersUseCase** - Advanced grid trading engine
  - Dynamic grid spacing with ATR integration
  - Risk-based position sizing
  - Duplicate order prevention
  - Market impact analysis

- ✅ **ManageOrdersUseCase** - Order lifecycle management
  - Order reconciliation between local DB and exchange
  - Status synchronization
  - Fill detection and processing
  - Order expiration handling

- ✅ **UpdatePortfolioUseCase** - Portfolio state management
  - Multi-currency balance updates
  - High water mark tracking
  - Drawdown calculation
  - Portfolio snapshot persistence

- ✅ **HandleGridFillsUseCase** - Grid profit optimization (NEW in v1.8.0)
  - Filled order detection
  - Profit-taking logic
  - Grid level management
  - Risk-adjusted position sizing

🆕 **Enhanced Data Layer (v1.8.0):**
- ✅ **PortfolioRepositoryImpl** - Portfolio data operations
  - High water mark management
  - Portfolio snapshot operations
  - Drawdown calculations

- ✅ **Enhanced RepositoryModule** - Complete DI configuration
  - All repository implementations bound
  - Proper dependency injection setup
  - Singleton scope management

🆕 **Comprehensive Unit Testing (v1.8.0):**
- ✅ **MockK Integration** - Professional testing framework
- ✅ **TradingDecisionEngineTest** - Complete decision engine testing
- ✅ **Use Case Testing** - Individual and integration tests
- ✅ **Edge Case Coverage** - Error conditions and boundary testing

### What's MISSING (Phase 3 Ready to Start)

```
❌ PHASE 3: Full API Integration & Service (Ready to Start)
❌ exchange/coinbase/
    ├── CoinbaseRepository.kt           ❌ Full REST API implementation (orders, candles, products)
    ├── CoinbaseWebSocket.kt            ❌ Real-time price feeds + order updates
    └── error/
        └── CoinbaseErrorHandler.kt     ❌ API error mapping and retry logic

❌ core/domain/risk/
    └── RiskManagerImpl.kt              ❌ Position sizing + drawdown monitoring

❌ service/
    └── TradingService.kt               ❌ 24/7 foreground service with wake locks

❌ INTEGRATION TESTING:
❌ Small real trades ($10-20) to validate system
❌ 24-hour service stability testing
❌ Emergency liquidation testing
```

---

## 🛠️ Tech Stack

| Layer | Technology | Status | Notes |
|-------|------------|---------|-------|
| **Language** | Kotlin 2.3.0 | ✅ Active | Latest stable |
| **UI Framework** | Jetpack Compose BOM 2025.12.01 | ✅ Active | Material 3 design |
| **Architecture** | Multi-module (8 modules) | ✅ Active | Clean Architecture + Hilt DI |
| **Database** | Room 2.8.4 | ✅ Active | 4 entities + 4 DAOs |
| **HTTP Client** | Ktor 3.3.3 (OkHttp engine) | ✅ Active | JWT authentication working |
| **Dependency Injection** | Hilt 2.57.2 | ✅ Active | Complete DI configuration |
| **JSON** | kotlinx.serialization 2.1.0 | ✅ Active | Type-safe serialization |
| **Logging** | Timber 5.0.1 | ✅ Active | Debug/release configurations |
| **Analytics** | Firebase BOM 33.8.0 | ✅ Active | Crashlytics + Analytics |

### Trading-Specific Dependencies

| Dependency | Version | Status | Purpose |
|------------|---------|---------|---------|
| **ta4j-core** | 0.16 | ✅ Active | Technical indicators (SMA/ADX/ATR) - INTEGRATED |
| **nimbus-jose-jwt** | 9.47 | ✅ Active | ES256 JWT signing for Coinbase API - WORKING |
| **BouncyCastle** | 1.78 (bcprov-jdk18on, bcpkix-jdk18on) | ✅ Active | Advanced PEM key parsing - COMPREHENSIVE |
| **mockk** | 1.14.7 | ✅ Active | Unit testing with mocks - INTEGRATED |
| **kotlin-test** | 2.1.0 | ✅ Active | Testing framework - ACTIVE |
| **javax.inject** | 1 | ✅ Active | Dependency injection annotations - USED |
| **security-crypto** | 1.1.0-alpha06 | ✅ Inactive | Replaced by build-time injection |
| **work-runtime-ktx** | 2.11.0 | ⚠️ Ready | Background tasks (Phase 3) |
| **datastore-preferences** | 1.2.0 | ⚠️ Ready | Settings persistence (Phase 3) |

**Legend:**
- ✅ Active: Currently used in implemented features
- ⚠️ Ready: Configured but awaiting implementation
- ❌ Missing: Needed for upcoming features

---

## 📂 Project Structure

```
TradeFlow/
├── app/                                    ✅ Application module
│   ├── src/main/java/com/dpart/tradeflow/
│   │   ├── MainActivity.kt                 ✅ Navigation host
│   │   ├── TradeFlowApp.kt                ✅ Application class
│   │   ├── navigation/AppNavHost.kt        ✅ Compose navigation
│   │   ├── presentation/dashboard/         ✅ Dashboard feature
│   │   └── di/                            ✅ App-level DI modules
│   └── build.gradle.kts                   ✅ Depends on all features
│
├── core/                                   ✅ Core shared modules
│   ├── domain/                            ✅ Pure Kotlin (NO Android deps)
│   ├── data/                              ✅ Room + repositories
│   └── ui/                                ✅ Shared Compose components
│
├── exchange/                              ✅ Exchange implementations
│   └── coinbase/                          🟡 Partial (auth ✅, full API ❌)
│
├── feature/                               ⚠️ Future feature modules
│   ├── dashboard/                         ⚠️ (Currently in :app)
│   ├── settings/                          ⚠️ (Currently in :app)
│   └── trading/                           ⚠️ (Planned)
│
└── gradle/libs.versions.toml               ✅ Centralized dependency management
```

**Module Dependencies:**
```
:app → :core:ui, :core:domain, :exchange:coinbase
:core:data → :core:domain
:core:ui → :core:domain  
:exchange:coinbase → :core:domain
```

---

## 🚀 Development Workflow

### 1. GitHub Actions CI/CD

**Build Status:** #31 SUCCESS ✅

**Pipeline Features:**
- ✅ Automated credential injection from GitHub Secrets
- ✅ APK build and Firebase App Distribution  
- ✅ Auto-documentation updates via Claude API
- ✅ Build status commit-back to branch
- ✅ APK artifact upload (7-day retention)

**For Code Changes:**
```bash
git add . && git commit -m "feat: implement X"
git push origin main  # or claude/branch-name
# → Triggers build automatically
# → Check .build-status file for result
```

### 2. Testing Strategy

**Unit Tests:**
```bash
./gradlew :core:domain:test          # Pure Kotlin tests (fast)
./gradlew :core:data:testDebug       # Database tests
./gradlew :exchange:coinbase:testDebug  # API client tests
```

**Integration Tests:**
```bash
./gradlew :exchange:coinbase:connectedDebugAndroidTest  # Real API tests
```

### 3. Build & Debug

**Local Development:**
```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Credential Configuration:**
- Production: GitHub Secrets → Environment Variables → BuildConfig
- Local: `local.properties` file (not committed)

---

## 🎯 Current Phase: Ready for Phase 3

### What's Next: Full API Integration

**Priority Order:**
1. **Ticket 13:** Full REST API Client (orders, candles, products) 
2. **Ticket 14:** WebSocket Client (real-time feeds)
3. **Ticket 16:** Risk Manager implementation
4. **Ticket 17:** Trading Service (24/7 foreground service)

**Dependencies Ready:**
- ✅ Domain interfaces defined (ExchangeRepository, etc.)
- ✅ Use cases implemented and tested
- ✅ Decision engine fully functional
- ✅ Data layer complete
- ✅ Authentication working
- ✅ UI showing live data

**Estimated Timeline:** 3-4 weeks to MVP

**See:** [docs/roadmap.md](docs/roadmap.md) for detailed implementation plan

---

## 🆘 Troubleshooting

### Common Issues

**Build Failures:**
1. Check `.build-status` file for latest status
2. Review `build-log.txt` if present
3. Verify GitHub Secrets are set correctly
4. Check Gradle wrapper version compatibility

**Credential Issues:**
1. Verify PEM key format (proper newlines)
2. Check API key format: `organizations/{org}/apiKeys/{key}`
3. Ensure GitHub Secrets match expected format
4. Test locally with `local.properties`

**Git Workflow:**
```bash
git pull origin main          # Get latest including docs updates
cat .build-status            # Check build result
git log --oneline -5         # See recent commits including CI commits
```

### Getting Help

1. **Check existing documentation:** [docs/README.md](docs/README.md)
2. **Review error logs:** `.build-status` and `build-log.txt`
3. **Verify dependencies:** All trading deps are configured
4. **Test locally:** Use `local.properties` for development

---

## 📈 Success Metrics

### Technical Milestones

- [x] **Phase 1:** Foundation complete (domain + data + auth)
- [x] **Phase 2:** Core trading logic complete (decision engine + use cases)
- [ ] **Phase 3:** API integration complete (REST + WebSocket)
- [ ] **Phase 4:** Service running 24/7 (stability testing)
- [ ] **Phase 5:** First profitable week (real trading)

### Business Metrics (Future)

- [ ] Break-even on $500 account (Year 1 goal)
- [ ] Consistent 3% monthly returns (Year 2-3 goal) 
- [ ] $10k+ account size (Year 5-10 goal)
- [ ] $500-1k monthly passive income (Ultimate goal)

**Remember:** 97% of retail algo traders fail. Expectations are intentionally conservative.

