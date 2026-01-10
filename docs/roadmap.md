# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-09
**Current Phase:** Phase 2 Complete - Core Trading Logic (v1.8.1)  
**Current Build:** #31 SUCCESS (Version 1.8.1)
**Architecture:** Multi-module app (8 modules: app, core:domain, core:data, core:ui, exchange:coinbase, feature:dashboard, feature:trading, feature:settings)

---

## Project Vision

**Personal automated crypto trading bot for Coinbase Advanced Trade API.**

### Goals
- Remove human emotions from trading decisions
- Run 24/7 unattended on physical Android device
- Simple UI, simple implementation, easy to maintain
- Backtest → Paper trade → Live (small amounts) → Scale

### Reality Constraints
- Fees matter: ~0.25-0.5% per trade on Coinbase Advanced Trade
- Most retail algo traders lose money - respect this fact
- Simple strategies often beat complex ML approaches
- Every trade is a taxable event (complete records required)

### Realistic Performance Expectations

**Starting Capital:** $500 (treat as education, not investment)

**Phase 1 Strategy:**
- **Trade BTC/USDT exclusively** (altcoins have 3-15% round-trip costs at this capital level)
- **Risk 1-2% per trade** ($5-10 max risk per position)
- **Target 3-5% monthly returns** (requires exceptional skill and discipline)
- **Win rate: 52-58%** realistic ceiling (not 70-98% like arbitrage bots)

**Timeline to Meaningful Income:**
```
Year 1:  $500 → $580-600   (Break even = success, learning phase)
Year 2:  $600 → $900       (Consistent 3-5% monthly)
Year 3:  $900 → $1,600     (Edge confirmed, scaling)
Year 5:  $3,000+           (Compound growth)
Year 10: $10,000-20,000    (Passive income: $500-1,000/month)
```

**Hard Truth:** Only 1-3% of day traders achieve consistent profitability. Expect 6-12 months of learning before positive results.

**See:** [docs/strategy/bitcoin-first-strategy.md](strategy/bitcoin-first-strategy.md) for complete analysis and math.

---

## Current State Analysis

### ✅ What Exists (January 2026)

**Phases 1 & 2 COMPLETE - Enhanced Coinbase Integration + Core Trading Logic (v1.8.1):**

```
app/src/main/java/com/dpart/tradeflow/
├── MainActivity.kt              ✅ Simplified (no auth check needed)
├── TradeFlowApp.kt              ✅ Initializes Timber logging + Hilt
├── presentation/dashboard/
│   ├── DashboardScreen.kt       ✅ Complete implementation with ENHANCED real data integration + ROBUST error handling + loading states
│   ├── DashboardViewModel.kt    ✅ Full state management + ENHANCED error handling + robust loading states
│   └── components/              ✅ PortfolioCard (live data), ModeCard, ServiceCard, OrdersList
├── di/
│   ├── AppModule.kt             ✅ Empty Hilt module
│   ├── DatabaseModule.kt        ✅ Provides Room database
│   ├── NetworkModule.kt         ✅ Provides Ktor HttpClient (OkHttp engine)
│   ├── CredentialsModule.kt     ✅ Provides build-injected credentials
│   └── DomainModule.kt          ✅ Decision engine DI binding
├── data/local/
│   ├── AppDatabase.kt           ✅ Complete Room DB with 4 entities
│   └── PlaceholderEntity.kt     ✅ Removed (no longer needed)
└── navigation/
    ├── AppNavHost.kt            ✅ Complete navigation with UNIFIED TopAppBar ("TradeFlow" title)
    └── Screen.kt                ✅ Dashboard + Settings routes

✅ COMPLETE: Enhanced Domain Layer Foundation with Improved Risk Management
└── core/domain/                 ✅ Complete domain layer with simplified use cases
    ├── auth/
    │   ├── AuthTokenProvider.kt ✅ Token generation interface
    │   └── CredentialStore.kt   ✅ Secure storage interface
    ├── error/
    │   └── ExchangeError.kt     ✅ Exchange error types (6 variants)
    ├── model/                   ✅ Domain models (Ticket 01)
    │   ├── Candle.kt            ✅ OHLCV data with granularity enums
    │   ├── Order.kt             ✅ Order types, sides, status
    │   ├── Decision.kt          ✅ Enhanced with ADX/ATR data (Wait, Defense, Trend, Range)
    │   ├── Portfolio.kt         ✅ Account balances with utility extensions
    │   ├── Balance.kt           ✅ Currency holdings
    │   └── Ticker.kt            ✅ Real-time price data
    ├── repository/
    │   ├── BracketOrderRepository.kt     ✅ Bracket order support
    │   ├── ExchangeRepository.kt         ✅ Core operations (12 methods)
    │   ├── ExchangeWebSocket.kt          ✅ Real-time streams
    │   └── TradingDataRepository.kt      ✅ Local data queries
    ├── strategy/                ✅ Enhanced decision engine with thread safety (v1.8.1)
    │   ├── DecisionEngine.kt    ✅ Decision engine interface
    │   ├── TradingDecisionEngine.kt ✅ Complete regime-switching implementation with THREAD-SAFE hysteresis + @Synchronized + @Volatile
    │   └── StrategyConfig.kt    ✅ Comprehensive strategy parameters
    ├── indicator/               ✅ Technical indicators (COMPLETE with DI)
    │   ├── SMACalculator.kt     ✅ Simple Moving Average with ta4j integration + @Inject
    │   ├── ADXCalculator.kt     ✅ Average Directional Index with ta4j integration + @Inject
    │   └── ATRCalculator.kt     ✅ Average True Range with ta4j integration + @Inject
    ├── risk/
    │   └── RiskManager.kt       ✅ Enhanced risk management with ZERO EQUITY PROTECTION + @Inject (v1.8.1)
    └── usecase/                 ✅ Simplified Use Case Layer (2 core use cases)
        ├── TradeOrchestrator.kt        ✅ Main orchestrator - handles trading cycle, risk checks, order execution
        ├── UpdatePortfolioUseCase.kt   ✅ Portfolio state updates and balance aggregation
        └── model/
            ├── ExecutionResult.kt      ✅ Use case result types
            └── TradingContext.kt       ✅ Trading context data model

✅ COMPLETE: Data Layer Implementation (Enhanced v1.8.1)
└── core/data/
    ├── security/
    │   └── StaticCredentialStore.kt ✅ Build-time credential injection
    ├── local/                       ✅ Room database (Ticket 03)
    │   ├── database/
    │   │   └── EngineDatabase.kt    ✅ Room DB with 4 tables
    │   ├── entity/
    │   │   ├── CandleEntity.kt      ✅ Candle storage
    │   │   ├── OrderEntity.kt       ✅ Order history
    │   │   ├── DecisionEntity.kt    ✅ Decision tracking
    │   │   └── PortfolioSnapshotEntity.kt ✅ Portfolio snapshots
    │   └── dao/
    │       ├── CandleDao.kt         ✅ Candle queries
    │       ├── OrderDao.kt          ✅ Order queries + getRecentFilledOrders()
    │       ├── DecisionDao.kt       ✅ Decision queries
    │       └── PortfolioDao.kt      ✅ Portfolio queries
    ├── mapper/
    │   └── OrderMapper.kt           ✅ OrderEntity ↔ Order domain mapping
    ├── repository/
    │   └── TradingDataRepositoryImpl.kt ✅ Local data repository implementation
    └── di/
        ├── SecurityModule.kt        ✅ Hilt DI for credential store
        ├── DatabaseModule.kt        ✅ Hilt DI for Room database
        └── RepositoryModule.kt      ✅ TradingDataRepository DI binding

✅ COMPLETE: Enhanced Coinbase API Integration with Expanded DI (v1.8.1)
└── exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt  ✅ ES256 JWT with COMPREHENSIVE BouncyCastle PEM parsing + enhanced escape handling + robust error recovery + format detection
    ├── api/
    │   └── CoinbaseApiClient.kt     ✅ Complete Ktor-based API client (accounts) with robust error handling
    ├── dto/
    │   └── AccountDto.kt            ✅ Account DTOs for API responses  
    ├── mapper/
    │   └── AccountMapper.kt         ✅ DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt    ✅ Enhanced implementation with BracketOrderRepository support
    └── di/
        ├── AuthModule.kt            ✅ Hilt DI for JWT provider
        └── ExchangeModule.kt        ✅ EXPANDED DI with dual repository bindings + TODO placeholders (v1.8.1)

✅ COMPLETE: UI Foundation
└── core/ui/
    ├── component/
    │   ├── ErrorDisplay.kt          ✅ Error state with retry button
    │   ├── LoadingButton.kt         ✅ Button with loading spinner
    │   ├── ModeIndicator.kt         ✅ Trading mode badges (DEFENSE/TREND/RANGE)
    │   ├── PriceDisplay.kt          ✅ Price with +/- color coding
    │   └── StatusCard.kt            ✅ Reusable card container
    └── extension/
        └── BigDecimalExt.kt        ✅ Currency/percentage formatting

✅ COMPLETE: Enhanced Live Portfolio Data Integration
├── App now displays real Coinbase account balances with COMPREHENSIVE error handling
├── ViewModel with complete state management (loading, error, success states) 
├── ENHANCED error handling with better retry functionality for network failures
├── Loading indicators during API calls with better UX
├── Portfolio card shows BTC/USD balances with "Live Data" indicator + enhanced formatting
├── Navigation OPTIMIZED - resolved duplicate TopAppBar issue for cleaner UI
├── COMPREHENSIVE authentication with advanced PEM key parsing supporting all formats
└── Professional UX flow with robust state management and error recovery
```

**Dependencies (ALL already added in build.gradle.kts):**
- ✅ Kotlin 2.3.0
- ✅ Compose BOM 2025.12.01
- ✅ Hilt 2.57.2
- ✅ Room 2.8.4
- ✅ Ktor 3.3.3 (with OkHttp engine)
- ✅ Timber 5.0.1
- ✅ Vico 2.4.0 (charts)
- ✅ Coroutines 1.10.2
- ✅ **nimbus-jose-jwt 9.47** ✅ ACTIVE (for JWT ES256 signing)
- ✅ **BouncyCastle 1.78** ✅ ACTIVE (for COMPREHENSIVE PEM key parsing - bcprov-jdk18on, bcpkix-jdk18on)
- ✅ **ta4j-core 0.16** ✅ ACTIVE (for technical indicators - SMA/ADX/ATR implemented)
- ✅ **mockk 1.14.7** ✅ ACTIVE (for unit testing with mocks)
- ✅ **kotlin-test 2.1.0** ✅ ACTIVE (for testing framework)
- ✅ **javax.inject 1** ✅ ACTIVE (for dependency injection annotations)
- ✅ **security-crypto 1.1.0-alpha06** (replaced by build-time injection)
- ✅ **work-runtime-ktx 2.11.0** (for background tasks)
- ✅ **datastore-preferences 1.2.0** (for settings)

---

## 🎉 Latest Milestone: Enhanced Risk Management & Thread Safety (v1.8.1)

**Critical improvements completed in this version:**

### 🔒 Enhanced Risk Management
- **Zero Equity Protection**: RiskManager now guards against division by zero when portfolio equity is zero or negative
- **Improved Error Handling**: Prevents crashes with "Cannot validate order: portfolio equity is zero or negative" messages
- **Better Fund Validation**: Ensures trading operations can only proceed with valid portfolio states

### 🧵 Thread-Safe Decision Engine  
- **@Synchronized Methods**: TradingDecisionEngine.evaluate() method is now thread-safe for concurrent access
- **@Volatile State Variables**: Hysteresis counters use @Volatile for proper memory visibility across threads
- **Concurrent Safety**: Supports real-time WebSocket updates without race conditions

### 💰 Enhanced Portfolio Calculations
- **Total Balance Support**: UpdatePortfolioUseCase now uses total (available + hold) balance calculations
- **Locked Fund Inclusion**: Accounts for funds tied up in pending orders for accurate portfolio valuation  
- **Comprehensive Fund Tracking**: Better visibility into actual available trading capital

### 🏗️ Expanded DI Architecture
- **Dual Interface Support**: ExchangeModule now provides both ExchangeRepository and BracketOrderRepository bindings
- **Scalable Design**: CoinbaseRepository serves multiple interface roles through single implementation
- **Future-Ready**: Architecture supports advanced trading features like bracket orders

**Technical Quality:**
- ✅ Zero division errors eliminated
- ✅ Thread safety ensured for concurrent access
- ✅ Accurate fund accounting including locked capital
- ✅ Clean separation of interface responsibilities
- ✅ Production-ready error handling

---

## ⭐ Core Achievements So Far

### Phase 1: Foundation (100% Complete)
- ✅ **8-module architecture** with clean boundaries
- ✅ **Complete domain models** (6 models: Candle, Decision, Order, Balance, Portfolio, Ticker)
- ✅ **Repository interfaces** (ExchangeRepository, AuthTokenProvider, ExchangeWebSocket)
- ✅ **Room database** (4 entities + 4 DAOs with proper BigDecimal handling)
- ✅ **Secure credential management** (build-time injection with enhanced PEM parsing)

### Phase 2: Core Trading Logic (100% Complete)
- ✅ **Complete decision engine** (SMA/ADX/ATR with 3-candle hysteresis)
- ✅ **2 simplified use cases** (TradeOrchestrator + UpdatePortfolioUseCase)
- ✅ **Complete risk management** (RiskManager with position sizing, drawdown monitoring, validation)
- ✅ **Technical indicators** (ta4j integration: SMACalculator, ADXCalculator, ATRCalculator)
- ✅ **Robust unit testing** (MockK integration with comprehensive test coverage)
- ✅ **Thread-safe architecture** ready for concurrent real-time data processing

### Coinbase Integration Status
- ✅ **JWT authentication** working (ES256 with advanced BouncyCastle parsing)
- ✅ **Account balance fetching** implemented and tested with live data
- ✅ **Enhanced DI architecture** supporting both basic and advanced trading features
- ⚠️ **Full REST API** pending (order placement, candle data, product queries)
- ⚠️ **WebSocket integration** pending (real-time data streams)

---

## 📋 What's PENDING (Phase 3)

### Next Phase: API Integration & Live Trading (0% Complete)

| Ticket | Title | Priority | Estimated Effort | Status | Description |
|--------|-------|----------|------------------|--------|-------------|
| **13** | **Full REST API Client** | 🔥 CRITICAL | Large (3-4 days) | ❌ Not Started | Order placement, candle fetching, product queries |
| **14** | **WebSocket Client** | 🔥 HIGH | Large (3-4 days) | ❌ Not Started | Real-time price feeds, order status updates |
| ~~**16**~~ | ~~**Risk Manager**~~ | - | - | ✅ **COMPLETE** | ✅ Fully implemented with 22 unit tests |
| **17** | **Trading Service** | 🔥 HIGH | Large (4-5 days) | ❌ Not Started | 24/7 foreground service orchestration |

### Phase 4: Testing & Validation

| Ticket | Title | Priority | Estimated Effort |
|--------|-------|----------|------------------|
| **19** | **Integration Tests** | 🟡 MEDIUM | Medium (2-3 days) |
| **20** | **MVP Milestone** | 🔥 CRITICAL | Small (1 day) |

---

## 🛣️ Detailed Implementation Roadmap

### 🎯 Phase 3A: Complete API Integration (3-4 weeks)

**Goal:** Full Coinbase API integration for live trading capabilities

#### Ticket 13: Full REST API Client (HIGHEST PRIORITY)
**Blocked by:** None (ready to start)
**Blocks:** Tickets 14, 17, 19, 20

**Implementation:**
```kotlin
// exchange/coinbase/api/CoinbaseApiClient.kt - EXTEND
class CoinbaseApiClient {
    // ✅ Already working: getAccounts()
    
    // NEW methods to implement:
    suspend fun getCandles(productId: String, granularity: String, limit: Int): Result<List<CandleDto>>
    suspend fun createOrder(orderRequest: CreateOrderRequest): Result<OrderResponseDto>  
    suspend fun cancelOrders(orderIds: List<String>): Result<CancelOrdersResponse>
    suspend fun getOrder(orderId: String): Result<OrderDto>
    suspend fun getProducts(): Result<List<ProductDto>>
}
```

**Key Integration Points:**
- ✅ JWT authentication already working
- ✅ Use cases ready to consume via ExchangeRepository interface
- ✅ Enhanced DI architecture supports bracket orders
- ✅ Thread-safe decision engine ready for real market data

**Acceptance Criteria:**
- Can place bracket orders for TREND mode via ExecuteDecisionUseCase
- Can place limit orders with post_only=true for RANGE mode
- Can fetch TWO_HOUR candles and aggregate to H4 for TradingDecisionEngine
- Proper error handling for rate limits, insufficient funds, invalid orders

#### Ticket 14: WebSocket Client (HIGH PRIORITY)
**Blocked by:** None (can develop in parallel with Ticket 13)
**Blocks:** Ticket 17 (Trading Service)

**Implementation:**
```kotlin
// exchange/coinbase/websocket/CoinbaseWebSocket.kt - NEW
class CoinbaseWebSocket : ExchangeWebSocket {
    // Market data streams (no auth)
    override fun subscribeTicker(productIds: List<String>): Flow<Ticker>
    
    // User order streams (requires auth)  
    override fun subscribeOrderUpdates(): Flow<Order>
    
    // Connection management with auto-reconnect
    override val connectionState: StateFlow<ConnectionState>
}
```

**Integration with Enhanced Architecture:**
- ✅ Thread-safe decision engine ready for concurrent real-time updates
- ✅ Enhanced portfolio calculations can incorporate real-time price data
- ✅ Use cases support real-time order status updates

#### ~~Ticket 16: Risk Manager~~ - ✅ COMPLETE
**Status:** FULLY IMPLEMENTED

**What Exists:**
```kotlin
// core/domain/risk/RiskManager.kt - COMPLETE
class RiskManager @Inject constructor(
    private val config: RiskConfig
) {
    ✅ validateOrder() - Position size validation with zero equity protection
    ✅ calculatePositionSize() - Risk-based position sizing
    ✅ checkDrawdownStatus() - Drawdown monitoring with thresholds
    ✅ shouldLiquidate() - Emergency liquidation triggers
}
```

**Test Coverage:** 22 unit tests in `RiskManagerTest.kt`
**Location:** `core/domain/src/main/kotlin/com/tradeflow/core/domain/risk/RiskManager.kt`

#### Ticket 17: Trading Service (HIGH PRIORITY - FINAL ORCHESTRATOR)
**Blocked by:** Tickets 13, 14 (needs REST API + WebSocket)

**Architecture:**
```kotlin
// service/TradingService.kt - NEW
class TradingService : Service() {
    @Inject lateinit var executeTradingCycleUseCase: ExecuteTradingCycleUseCase
    @Inject lateinit var handleEmergencyUseCase: HandleEmergencyUseCase
    
    // Main trading loop (every 15 minutes)
    private suspend fun runTradingCycle() {
        val context = buildTradingContext()  // Current price, candles, portfolio
        val result = executeTradingCycleUseCase.execute(context)
        
        when (result) {
            is TradingCycleResult.Emergency -> {
                stopSelf() // Emergency liquidation triggered
            }
            // ... handle other results
        }
    }
}
```

**Key Advantages:**
- ✅ Use case layer already provides complete trading orchestration
- ✅ Enhanced risk management with zero equity protection ready
- ✅ Thread-safe decision engine handles concurrent data updates
- ✅ Comprehensive error handling and recovery patterns established

---

### 📈 Success Metrics & Milestones

#### Phase 3A Complete When:
- [ ] Can place live bracket orders on Coinbase
- [ ] Real-time BTC price updates in dashboard  
- [ ] Complete trading cycle runs end-to-end
- [ ] Emergency liquidation triggers at 15% drawdown
- [ ] Service survives 8+ hours with device screen off

#### MVP Ready When (Phase 4):
- [ ] Successfully executes 10+ trades with $100 test capital
- [ ] No crashes or data corruption over 24-hour run
- [ ] All order states properly tracked and reconciled
- [ ] Performance metrics: <5% drawdown, 52%+ win rate

---

## 🔍 Critical Path Analysis

**Longest pole to MVP:** Ticket 13 (Full REST API) → Ticket 17 (Trading Service)

**Parallel development possible:**
- Ticket 14 (WebSocket) can develop alongside Ticket 13
- Ticket 16 (Risk Manager) can develop independently
- Integration testing (Ticket 19) can begin once Ticket 13 complete

**Estimated Timeline:**
- **Phase 3A (API Integration):** 3-4 weeks
- **Phase 3B (Service Implementation):** 1-2 weeks  
- **Phase 4 (Testing & Validation):** 1-2 weeks
- **Total to MVP:** 5-8 weeks

---

## 🚀 Getting Started with Phase 3

### Immediate Next Action: Ticket 13 Implementation

**Step 1: Extend CoinbaseApiClient**
- Add order placement endpoints
- Add candle fetching with TWO_HOUR→H4 aggregation
- Add product queries for trading pair validation

**Step 2: Complete CoinbaseRepository**
- Implement remaining ExchangeRepository methods  
- Add bracket order support
- Integrate with existing JWT authentication

**Step 3: Integration Testing**
- Test with small real orders ($10-20)
- Validate decision engine with live market data
- Ensure enhanced risk management prevents invalid trades

**Architecture Benefits:**
- ✅ Clean interfaces mean UI and use cases don't change
- ✅ Enhanced DI architecture supports advanced features
- ✅ Thread-safe implementation ready for real-time data
- ✅ Comprehensive error handling already established

**Risk Mitigation:**
- Start with small position sizes ($10-50)  
- Test extensively with non-production capital
- Enhanced risk management prevents catastrophic losses
- Emergency liquidation provides final safety net

---

## 📊 Progress Tracking

### Overall Completion: 57% (8/14 tickets)

```
✅ Phase 1: ████████████████████ 100% (4/4) COMPLETE  
✅ Phase 2: ████████████████████ 100% (4/4) COMPLETE - Enhanced v1.8.1
❌ Phase 3: ░░░░░░░░░░░░░░░░░░░░   0% (0/4) ← YOU ARE HERE
❌ Phase 4: ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
```

### Key Quality Metrics
- ✅ **Architecture Quality**: Clean separation, zero circular dependencies
- ✅ **Code Quality**: 100% Kotlin, comprehensive error handling, thread safety
- ✅ **Test Coverage**: Core business logic unit tested with MockK
- ✅ **Security**: Build-time credential injection, encrypted storage
- ✅ **Performance**: Efficient Room queries, optimized indicator calculations

**Next Milestone Target:** Phase 3A complete (Full API Integration) - Target: End January 2026

**Ready for next phase implementation! 🚀**
