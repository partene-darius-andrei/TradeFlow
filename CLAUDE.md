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
    │   ├── entity/
    │   │   ├── CandleEntity.kt        ✅ Room entity for candles
    │   │   ├── OrderEntity.kt         ✅ Room entity for orders + getRecentFilledOrders()
    │   │   ├── DecisionEntity.kt      ✅ Room entity for decisions
    │   │   └── PortfolioSnapshotEntity.kt ✅ Room entity for portfolio
    │   └── dao/
    │       ├── CandleDao.kt           ✅ CRUD + delete old candles
    │       ├── OrderDao.kt            ✅ CRUD + query by status/product + filled orders
    │       ├── DecisionDao.kt         ✅ CRUD + latest decision query
    │       └── PortfolioDao.kt        ✅ CRUD + snapshot history
    ├── security/
    │   └── StaticCredentialStore.kt    ✅ Static credential injection (replaces UI input)
    ├── mapper/
    │   └── OrderMapper.kt              ✅ OrderEntity ↔ Order domain model mapping (NEW)
    ├── repository/
    │   └── TradingDataRepositoryImpl.kt ✅ Implementation for local data queries (NEW)
    └── di/
        ├── SecurityModule.kt           ✅ Static credential DI binding
        └── RepositoryModule.kt         ✅ TradingDataRepository DI binding (NEW)

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

- ✅ **ExecuteTradingCycleUseCase** - Master trading orchestrator
  - Portfolio updates with high water mark tracking
  - Drawdown monitoring with emergency liquidation at 15%
  - Complete trading cycle with safety-first design
  - Grid fill handling integration
  - Order reconciliation and management

- ✅ **HandleEmergencyUseCase** - Emergency liquidation handler
  - Cancel all open orders first
  - Market sell all BTC positions
  - Complete portfolio liquidation with error handling

- ✅ **ManageGridOrdersUseCase** - Advanced grid trading
  - Dynamic grid spacing calculation (max of 1.5% or ATR)
  - Risk-validated position sizing per level
  - Post-only orders for maker fees (0.60% vs 1.20%)
  - Partial success handling

- ✅ **HandleGridFillsUseCase** - Grid profit realization (NEW)
  - Detects filled grid BUY orders
  - Places corresponding SELL orders for profit
  - Risk validation and position sizing
  - Completes the grid trading cycle

- ✅ **ManageOrdersUseCase** - Order lifecycle management
  - Stale order cancellation (48-hour timeout)
  - Order reconciliation between local and exchange
  - Status tracking and updates

- ✅ **UpdatePortfolioUseCase** - Portfolio state management
  - Multi-currency balance updates
  - Total equity calculation with current prices
  - High water mark tracking for drawdown calculation

🆕 **Enhanced Domain Infrastructure (v1.8.0):**
- ✅ **TradingDataRepository** - Local data query abstraction
- ✅ **OrderMapper** - Clean entity ↔ domain model mapping
- ✅ **Complete Dependency Injection** - All components with @Inject
- ✅ **Comprehensive Unit Testing** - 20+ tests with MockK
- ✅ **Enhanced Error Handling** - Result<T> pattern throughout
- ✅ **Safety-First Design** - Emergency liquidation as first-class citizen

### What's MISSING (Ready for Phase 3)

```
❌ Phase 3: API Integration (0% - Ready to Start)
❌ exchange/coinbase/ - Full REST API implementation
    ├── Complete CoinbaseRepository (only getBalances working)
    ├── Order placement methods (bracket, limit, market)
    ├── Candle data fetching (TWO_HOUR + H4 aggregation)
    ├── Real-time WebSocket client
    ├── Order status tracking and updates
    └── Error handling and retry logic

❌ Phase 4: Service & Testing (0%)
❌ Foreground trading service (TradingService)
❌ 24/7 background execution
❌ Integration testing with small real trades
❌ MVP validation milestone
```

---

## 🏗️ Tech Stack

| Component | Library/Version | Status | Usage |
|-----------|----------------|--------|-------|
| **Language** | Kotlin 2.3.0 | ✅ Active | Core language |
| **UI** | Compose BOM 2025.12.01 | ✅ Active | Modern Android UI |
| **DI** | Hilt 2.57.2 | ✅ Active | Dependency injection |
| **Database** | Room 2.8.4 | ✅ Active | Local persistence (4 entities) |
| **HTTP** | Ktor 3.3.3 | ⚠️ Partial | REST API client (auth only) |
| **Async** | Coroutines 1.10.2 | ✅ Active | Concurrency |
| **Serialization** | kotlinx.serialization | ✅ Active | JSON parsing |
| **Logging** | Timber 5.0.1 | ✅ Active | Structured logging |
| **Charts** | Vico 2.4.0 | ⏳ Ready | Trading charts (future) |
| **Analytics** | Firebase BOM 34.7.0 | ✅ Active | Crashlytics + Analytics |

### Trading-Specific Dependencies

| Library | Version | Status | Purpose |
|---------|---------|--------|---------|
| **nimbus-jose-jwt** | 9.47 | ✅ Active | ES256 JWT signing for Coinbase |
| **BouncyCastle** | 1.78 | ✅ Active | Advanced PEM key parsing |
| **ta4j-core** | 0.16 | ✅ Active | Technical indicators (SMA/ADX/ATR) |
| **mockk** | 1.14.7 | ✅ Active | Unit testing with mocks |
| **kotlin-test** | 2.1.0 | ✅ Active | Testing framework |
| **security-crypto** | 1.1.0-alpha06 | ⏳ Ready | Encrypted preferences (if needed) |
| **work-runtime-ktx** | 2.11.0 | ⏳ Ready | Background work scheduling |
| **datastore-preferences** | 1.2.0 | ⏳ Ready | Settings persistence |
| **material-icons-extended** | ✅ Active | UI icons for ModeIndicator |

**Legend:**
- ✅ Active (currently used in implemented code)
- ⚠️ Partial (configured but incomplete implementation) 
- ⏳ Ready (configured, awaiting implementation)

---

## 🎯 Next Steps (Phase 3 - Critical Path)

### Immediate Priority: Complete Coinbase API Integration

**Goal:** Transform existing use cases from pure domain logic to working trading system

**Blocking Path to MVP:**
1. **Complete CoinbaseRepository** - Order placement, candle fetching, product queries
2. **WebSocket Client** - Real-time price feeds, order status updates  
3. **Trading Service** - 24/7 foreground service orchestrating use cases
4. **Integration Testing** - Small real trades to verify system works end-to-end

**Estimated Timeline:** 3-4 weeks to first live trade capability

### Critical Implementation Notes

**For REST API Client (Phase 3A):**
- Use cases are **ready to consume** - just implement ExchangeRepository methods
- JWT authentication **already working** - CoinbaseJwtGenerator complete
- Error handling patterns established - use Result<T> consistently
- Order placement: Bracket orders for TREND, limit orders with post_only for RANGE
- Candle fetching: Handle 350-candle limit, TWO_HOUR → H4 aggregation

**For WebSocket Client (Phase 3B):**
- ExecuteTradingCycleUseCase needs current BTC price for TradingContext
- HandleGridFillsUseCase needs order fill notifications 
- Connection health monitoring with auto-reconnect
- Heartbeat subscription required (60-90 second timeout)

**For Trading Service (Phase 3C):**
- ExecuteTradingCycleUseCase is the main orchestrator
- 15-minute evaluation cycles (not continuous)
- Foreground service with wake lock for 24/7 operation
- Emergency liquidation at 15% drawdown (hardcoded safety limit)

---

## 🔥 Critical Context for Claude Code

### Build System (Mobile Optimized)

**The project uses GitHub Actions for builds** - optimized for mobile development with Claude Code:

1. **No local Gradle execution needed** - Push → Actions builds → Firebase distribution
2. **Real credentials injected at build time** - No credential entry UI needed  
3. **Commit-back pattern** - Build results in `.build-status` and `build-log.txt`
4. **Enhanced PEM key handling** - Supports complex JWT private keys with newlines

**Workflow:**
```bash
# Make changes in Claude Code
git add . && git commit -m "Implement X feature"
git push origin claude/your-branch

# GitHub Actions:
# 1. Injects real Coinbase credentials from secrets
# 2. Builds APK with embedded credentials  
# 3. Uploads to Firebase App Distribution
# 4. Commits build status back to branch

# Check result
git pull
cat .build-status  # SUCCESS or FAILURE
```

### Dependency Injection Architecture

**Everything uses Hilt DI** - no manual object creation:

```kotlin
// Domain classes with @Inject constructor
class TradingDecisionEngine @Inject constructor(
    private val smaCalculator: SMACalculator,
    private val adxCalculator: ADXCalculator,
    private val atrCalculator: ATRCalculator
) : DecisionEngine

// Use cases with @Inject constructor  
class ExecuteTradingCycleUseCase @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val decisionEngine: DecisionEngine,
    private val riskManager: RiskManager,
    // ... all dependencies injected
)

// Modules bind interfaces to implementations
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    @Binds
    abstract fun bindDecisionEngine(impl: TradingDecisionEngine): DecisionEngine
}
```

### Testing Strategy

**Comprehensive unit testing with MockK:**

```kotlin
class ExecuteDecisionUseCaseTest {
    private val exchangeRepository: ExchangeRepository = mockk()
    private val riskManager: RiskManager = mockk()
    
    @Test
    fun `execute places bracket order for Trend decision`() = runTest {
        // Given
        val decision = Decision.Trend(...)
        every { riskManager.validateOrder(any(), any(), any()) } returns RiskCheck.Approved
        coEvery { exchangeRepository.placeBracketOrder(...) } returns Result.success(mockOrder)
        
        // When
        val result = useCase.execute(decision, portfolio, currentPrice, productId)
        
        // Then
        assertTrue(result is ExecutionResult.Success)
        coVerify { exchangeRepository.placeBracketOrder(...) }
    }
}
```

**20+ tests covering:**
- Happy paths and edge cases
- Error conditions and recovery
- Risk validation and rejection
- Partial successes and failures
- Emergency scenarios

### Architecture Principles

**Clean Architecture with Clear Boundaries:**

```
┌─────────────────────────────────────┐
│           Presentation              │ ← ViewModels, Compose UI
│        (app module)                 │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│            Use Cases                │ ← Business logic orchestration  
│         (core:domain)               │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│          Domain Models              │ ← Pure Kotlin, no Android deps
│       Repositories (interfaces)    │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│       Infrastructure               │ ← Coinbase API, Room database
│      (exchange:coinbase,            │
│       core:data)                   │
└─────────────────────────────────────┘
```

**Key Rules:**
- Domain layer is **pure Kotlin** (no Android dependencies)
- Use cases orchestrate business logic (not ViewModels)
- Repository interfaces define contracts (implementations are swappable)
- Always use Result<T> for operations that can fail
- All async operations are suspending functions
- Dependency injection throughout (no manual instantiation)

---

## 📚 Documentation Quick Access

**When implementing features:**
- Read the ticket file in `docs/tickets/backlog/` first
- Check `docs/reference.md` for code examples  
- Update `docs/roadmap.md` when tickets complete
- Use `docs/ci.md` for build troubleshooting

**When debugging:**
- Check `.build-status` for build results
- Read `build-log.txt` if build failed
- Use `cat app/build/reports/lint-results-debug.html` for lint issues
- Firebase Crashlytics for runtime errors

**Key insight:** The core trading logic is **completely implemented** in v1.8.0. What remains is connecting it to the real Coinbase API and wrapping it in a foreground service. The hard part (strategy, risk management, order logic) is done.
