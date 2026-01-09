# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-09
**Project Status:** Phase 2 Complete - Core Trading Logic (v1.8.1)
**Current Build:** #31 SUCCESS

This is the entry point for Claude Code when working with TradeFlow. All essential context, navigation, and workflows are documented here.

---

## 🎯 Quick Navigation

| Document | Purpose | Use When |
|----------|---------|----------|
| **[docs/roadmap.md](docs/roadmap.md)** | Implementation roadmap organized in phases | Planning what to build next |
| **[docs/README.md](docs/README.md)** | Complete documentation index and ticket mapping | Finding specific documentation |
| **[docs/reference.md](docs/reference.md)** | Implementation blueprint with code examples | Implementing features |
| **[docs/ci-claude-integration.md](docs/ci-claude-integration.md)** | CI/CD with Claude API integration | Understanding build pipeline |
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

### What EXISTS (Phases 1 & 2 COMPLETE - v1.8.1)

```
✅ Modern Android app structure
✅ Hilt dependency injection configured
✅ Room database with complete schema (4 entities + 4 DAOs)
✅ Ktor HTTP client configured (OkHttp engine)
✅ Timber logging initialized
✅ Firebase Analytics + Crashlytics
✅ All trading dependencies added (ta4j, nimbus-jose-jwt, security-crypto)
✅ GitHub Actions CI/CD pipeline with Claude API integration
✅ Adaptive app icon with trading chart design (day/night variants)

✅ ENHANCED DOMAIN LAYER (v1.8.1):
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
    ├── strategy/ ← ENHANCED with thread-safety (v1.8.1)
    │   ├── DecisionEngine.kt           ✅ Decision engine interface
    │   ├── TradingDecisionEngine.kt    ✅ Complete regime-switching implementation with THREAD-SAFE hysteresis + @Synchronized
    │   └── StrategyConfig.kt           ✅ Comprehensive strategy parameters
    ├── indicator/ ← COMPLETE with DI
    │   ├── SMACalculator.kt            ✅ Simple Moving Average with ta4j integration + @Inject
    │   ├── ADXCalculator.kt            ✅ Average Directional Index with ta4j integration + @Inject
    │   └── ATRCalculator.kt            ✅ Average True Range with ta4j integration + @Inject
    ├── risk/
    │   └── RiskManager.kt              ✅ ENHANCED risk management with zero equity guards + @Inject
    └── usecase/ ← ENHANCED with total balance calculations (v1.8.1)
        ├── ExecuteDecisionUseCase.kt   ✅ Trading decision execution orchestrator
        ├── ExecuteTradingCycleUseCase.kt ✅ Complete trading cycle with risk management
        ├── HandleEmergencyUseCase.kt   ✅ Emergency liquidation handler
        ├── HandleGridFillsUseCase.kt   ✅ Grid order fill detection and profit taking
        ├── ManageGridOrdersUseCase.kt  ✅ Grid order management for range trading
        ├── ManageOrdersUseCase.kt      ✅ Order lifecycle and reconciliation
        ├── UpdatePortfolioUseCase.kt   ✅ ENHANCED portfolio updates with total balance support (v1.8.1)
        └── model/
            ├── ExecutionResult.kt      ✅ Use case result types (Success/Skipped/Failed)
            └── TradingContext.kt       ✅ Trading context data model

✅ DATA LAYER COMPLETE:
✅ core/data/src/main/kotlin/com/tradeflow/core/data/
    ├── local/ ← COMPLETE
    │   ├── entity/
    │   │   ├── CandleEntity.kt        ✅ Room entity for candles
    │   │   ├── OrderEntity.kt         ✅ Room entity for orders + getRecentFilledOrders()
    │   │   ├── DecisionEntity.kt      ✅ Room entity for decisions
    │   │   └── PortfolioSnapshotEntity.kt ✅ Room entity for portfolio
    │   ├── dao/
    │   │   ├── CandleDao.kt           ✅ CRUD + delete old candles
    │   │   ├── OrderDao.kt            ✅ CRUD + query by status/product + filled orders
    │   │   ├── DecisionDao.kt         ✅ CRUD + latest decision query
    │   │   └── PortfolioDao.kt        ✅ CRUD + snapshot history
    │   └── database/
    │       └── EngineDatabase.kt       ✅ Room database with all 4 entities
    ├── security/
    │   └── StaticCredentialStore.kt    ✅ Static credential injection (replaces UI input)
    ├── mapper/
    │   └── OrderMapper.kt              ✅ OrderEntity ↔ Order domain model mapping
    ├── repository/
    │   └── TradingDataRepositoryImpl.kt ✅ Implementation for local data queries
    └── di/
        ├── SecurityModule.kt           ✅ Static credential DI binding
        ├── DatabaseModule.kt           ✅ Room database DI
        └── RepositoryModule.kt         ✅ TradingDataRepository DI binding

✅ ENHANCED COINBASE INTEGRATION (v1.8.1):
✅ exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt     ✅ ES256 JWT with ADVANCED BouncyCastle PEM parsing + enhanced escape handling
    ├── api/
    │   └── CoinbaseApiClient.kt        ✅ Complete Ktor-based API client (accounts) with robust error handling
    ├── dto/
    │   └── AccountDto.kt               ✅ Account DTOs for API responses  
    ├── mapper/
    │   └── AccountMapper.kt            ✅ DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt       ✅ ENHANCED implementation with BracketOrderRepository support
    └── di/
        ├── AuthModule.kt               ✅ JWT generator DI binding
        └── ExchangeModule.kt           ✅ EXPANDED DI with dual repository bindings (ExchangeRepository + BracketOrderRepository)

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
    │   └── AppNavHost.kt               ✅ Complete navigation with CENTRALIZED TopAppBar ("TradeFlow" title)
    ├── presentation/dashboard/
    │   ├── DashboardScreen.kt          ✅ Complete implementation with ENHANCED real data integration
    │   ├── DashboardViewModel.kt       ✅ Full state management + ROBUST error handling + loading states
    │   └── components/
    │       ├── PortfolioCard.kt        ✅ Live data, BTC/USD balances with "Live Data" indicator
    │       ├── ModeCard.kt             ✅ Trading mode + current price
    │       ├── ServiceCard.kt          ✅ Service status + start/stop button
    │       └── OrdersList.kt           ✅ Recent orders + empty state
    └── di/
        └── DomainModule.kt             ✅ DecisionEngine DI binding

✅ APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants (white/black)
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

✅ ENHANCED CREDENTIALS SYSTEM:
✅ app/build.gradle.kts                 ✅ ADVANCED build-time credential injection with ENHANCED PEM key escaping
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig
```

### 🎉 Latest Enhancements (v1.8.1): Enhanced Risk Management & Robustness

🆕 **Risk Management Improvements:**
- ✅ **Zero Equity Protection** - RiskManager now guards against division by zero when portfolio equity is zero or negative
- ✅ **Thread-Safe Decision Engine** - TradingDecisionEngine uses @Synchronized and @Volatile for thread-safe state management
- ✅ **Total Balance Calculations** - UpdatePortfolioUseCase now uses total (available + hold) to include funds locked in pending orders
- ✅ **Enhanced DI Architecture** - ExchangeModule provides both ExchangeRepository and BracketOrderRepository interfaces

🔧 **Technical Enhancements:**
- **Enhanced error handling** in RiskManager prevents crashes with invalid portfolio states
- **Improved fund accounting** considers all funds including those in pending orders
- **Thread-safety** ensures concurrent access to decision engine state doesn't cause race conditions
- **Expanded interface support** allows Coinbase repository to serve multiple roles

### What's PENDING (Phase 3)

```
❌ Full REST API implementation (order placement, candles, product queries)
❌ WebSocket client for real-time data
❌ Trading service orchestration (24/7 background execution)
❌ Integration testing with real API
❌ Live system validation
```

**Critical Path:** REST API → WebSocket → Trading Service → Testing

---

## 🏗️ Architecture Overview

### Module Structure

```
TradeFlow/
├── app/                              ✅ Application (DI wiring, credentials, navigation)
├── core/
│   ├── domain/                       ✅ Pure Kotlin (business logic, interfaces) - ENHANCED v1.8.1
│   ├── data/                         ✅ Data layer (Room, security, repositories)
│   └── ui/                           ✅ Shared UI components
├── exchange/
│   └── coinbase/                     ✅ Coinbase API integration - ENHANCED DI v1.8.1
└── [features not yet implemented]
```

### Key Architectural Principles

1. **Clean Architecture** - Domain layer has zero Android dependencies
2. **Interface Segregation** - Exchange implementations hidden behind interfaces
3. **Dependency Inversion** - Core depends on abstractions, not concretions
4. **Single Responsibility** - Each module has one clear purpose
5. **Thread Safety** - Decision engine and risk management handle concurrent access

---

## 🧪 Tech Stack

| Component | Library | Version | Status | Usage |
|-----------|---------|---------|--------|-------|
| **Language** | Kotlin | 2.3.0 | ✅ Active | App language |
| **UI** | Compose BOM | 2025.12.01 | ✅ Active | Modern declarative UI |
| **DI** | Hilt | 2.57.2 | ✅ Active | Dependency injection |
| **Database** | Room | 2.8.4 | ✅ Active | Local persistence |
| **HTTP** | Ktor | 3.3.3 | ✅ Active | REST API client |
| **Async** | Coroutines | 1.10.2 | ✅ Active | Async programming |
| **Serialization** | kotlinx-serialization | 1.8.0 | ✅ Active | JSON parsing |
| **Logging** | Timber | 5.0.1 | ✅ Active | Structured logging |
| **Testing** | MockK | 1.14.7 | ✅ Active | Unit testing with mocks |
| **JWT** | nimbus-jose-jwt | 9.47 | ✅ Active | ES256 signing |
| **Crypto** | BouncyCastle | 1.78 | ✅ Active | Advanced cryptography |
| **Technical Analysis** | ta4j-core | 0.16 | ✅ Active | SMA/ADX/ATR indicators |
| **Security** | security-crypto | 1.1.0-alpha06 | ✅ Active | Encrypted preferences |
| **Charts** | Vico | 2.4.0 | ⚠️ Ready | Chart visualization (future) |
| **Background** | WorkManager | 2.11.0 | ⚠️ Ready | Background tasks |

**Legend:**
- ✅ Active (currently implemented and used)
- ⚠️ Ready (configured but not yet implemented)

---

## 📦 Dependencies Status

**Core Dependencies (Phase 1-2 Complete):**
```kotlin
// Essential - All working ✅
kotlin("jvm")                           // ✅ 2.3.0 - Core language
hilt-android                            // ✅ 2.57.2 - DI framework  
room-runtime + room-ktx                 // ✅ 2.8.4 - Local database
ktor-client-core + ktor-client-okhttp   // ✅ 3.3.3 - HTTP client
kotlinx-coroutines-android              // ✅ 1.10.2 - Async programming
timber                                  // ✅ 5.0.1 - Logging

// Trading - All integrated ✅
ta4j-core                              // ✅ 0.16 - Technical indicators (SMA/ADX/ATR)
nimbus-jose-jwt                        // ✅ 9.47 - JWT ES256 signing  
bcprov-jdk18on + bcpkix-jdk18on       // ✅ 1.78 - Advanced PEM parsing

// Testing - All working ✅
mockk                                  // ✅ 1.14.7 - Mocking framework
kotlin-test                            // ✅ 2.1.0 - Test assertions
```

**Phase 3 Dependencies (Configured, awaiting implementation):**
```kotlin
// Background execution
work-runtime-ktx                       // ⚠️ 2.11.0 - Background tasks
security-crypto                        // ⚠️ 1.1.0 - Secure storage

// Future enhancements  
vico-compose-m3                        // ⚠️ 2.4.0 - Charts
datastore-preferences                  // ⚠️ 1.2.0 - Settings storage
```

---

## 🔄 Development Workflow

### For Claude Code Sessions

**1. Check Status**
```bash
cat .build-status              # SUCCESS or FAILURE
cat docs/roadmap.md | head -20 # Current phase progress
```

**2. Plan Work**
```bash
# Read next ticket
ls docs/tickets/backlog/       # See available tickets
cat docs/tickets/backlog/13-rest-api-client.md  # Next high priority
```

**3. Implement**
```bash
# Make changes
git add .
git commit -m "Implement [feature]: [description]"
git push origin claude/[branch-name]
```

**4. Verify Build**
```bash
# GitHub Actions builds automatically
# Wait ~3 minutes, then:
git pull                       # Get CI results
cat .build-status              # Check build outcome
```

**5. Test on Device** (if build successful)
- Download APK from Firebase App Distribution
- Install and test on physical device
- Contains embedded credentials for testing

### CI/CD Integration

**GitHub Actions automatically:**
1. ✅ Injects Coinbase credentials from secrets
2. ✅ Builds APK with embedded credentials
3. ✅ Uploads to Firebase App Distribution  
4. ✅ Updates documentation via Claude API
5. ✅ Commits build status back to branch

**See:** [docs/ci-claude-integration.md](docs/ci-claude-integration.md) for complete CI/CD workflow

---

## 📋 Next Immediate Actions

### 1. Ticket 13: Full REST API Client (HIGH PRIORITY)

**Goal:** Complete CoinbaseRepository implementation with all order operations

**Files to implement:**
- Order placement (bracket, limit, market orders)
- Candle data fetching (TWO_HOUR aggregation to H4)
- Product queries and validation
- Rate limiting and error handling

**Integration:**
- Use cases ready via ExchangeRepository interface
- JWT authentication working
- Enhanced DI modules support dual bindings

### 2. Ticket 14: WebSocket Client (HIGH PRIORITY)  

**Goal:** Real-time market data and order updates

**Integration:**
- Thread-safe decision engine ready for concurrent data
- Enhanced portfolio calculations support real-time updates

### 3. Ticket 17: Trading Service (MEDIUM PRIORITY)

**Goal:** 24/7 orchestration service

**Integration:**
- Enhanced risk management with zero equity protection
- Use case layer provides complete trading orchestration
- Total balance calculations for accurate fund tracking

---

## 📞 Communication Patterns

**Status Updates:** Always update version numbers in commits
**Error Handling:** Check .build-status before proceeding  
**Documentation:** Auto-updated via CI/CD, verify changes in git log
**Testing:** Build success = APK ready for device testing

**Last Verified:** v1.8.1 - Enhanced risk management and thread safety complete
