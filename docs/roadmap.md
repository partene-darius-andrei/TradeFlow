# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-09
**Current Phase:** Phase 2 Complete - Core Trading Logic (v1.10.0)  
**Current Build:** #31 SUCCESS (Version 1.10.0)
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

**Phases 1 & 2 COMPLETE - Enhanced Coinbase Integration + Core Trading Logic (v1.10.0):**

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

✅ COMPLETE: Domain Layer Foundation with Enhanced Decision Engine (v1.10.0)
└── core/domain/                 ✅ Complete domain layer with use cases
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
    ├── strategy/                ✅ Decision engine (Ticket 15 - COMPLETE)
    │   ├── DecisionEngine.kt    ✅ Decision engine interface
    │   ├── TradingDecisionEngine.kt ✅ Complete regime-switching implementation with hysteresis + DI
    │   └── StrategyConfig.kt    ✅ Comprehensive strategy parameters
    ├── indicator/               ✅ Technical indicators (COMPLETE with DI)
    │   ├── SMACalculator.kt     ✅ Simple Moving Average with ta4j integration + @Inject
    │   ├── ADXCalculator.kt     ✅ Average Directional Index with ta4j integration + @Inject
    │   └── ATRCalculator.kt     ✅ Average True Range with ta4j integration + @Inject
    ├── risk/
    │   └── RiskManager.kt       ✅ Risk management interface with enhanced types + @Inject
    └── usecase/                 ✅ Complete Use Case Layer Implementation (v1.10.0)
        ├── ExecuteDecisionUseCase.kt   ✅ Trading decision execution orchestrator
        ├── ExecuteTradingCycleUseCase.kt ✅ Complete trading cycle with risk management (UPDATED v1.10.0)
        ├── HandleEmergencyUseCase.kt   ✅ Emergency liquidation handler
        ├── HandleGridFillsUseCase.kt   ✅ Grid fill detection and profit taking
        ├── ManageGridOrdersUseCase.kt  ✅ Grid order management for range trading
        ├── ManageOrdersUseCase.kt      ✅ Order lifecycle and reconciliation
        ├── UpdatePortfolioUseCase.kt   ✅ Portfolio state updates
        └── model/
            ├── ExecutionResult.kt      ✅ Use case result types
            └── TradingContext.kt       ✅ Trading context data model

✅ COMPLETE: Data Layer Implementation (Enhanced v1.10.0)
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
    │   ├── TradingDataRepositoryImpl.kt ✅ Local data repository implementation
    │   └── PortfolioRepositoryImpl.kt   ✅ Portfolio data repository (ENHANCED v1.10.0)
    └── di/
        ├── SecurityModule.kt        ✅ Hilt DI for credential store
        ├── DatabaseModule.kt        ✅ Hilt DI for Room database
        └── RepositoryModule.kt      ✅ Repository DI bindings (ENHANCED v1.10.0)

✅ COMPLETE: Enhanced Coinbase API Integration (v1.5.5)
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
    │   └── CoinbaseRepository.kt    ✅ Implementation (getBalances working, others TODO for Phase 3)
    └── di/
        ├── AuthModule.kt            ✅ Hilt DI for JWT provider
        └── ExchangeModule.kt        ✅ Repository DI binding

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

✅ COMPLETE: Enhanced Live Portfolio Data Integration (v1.5.5)
├── App now displays real Coinbase account balances with COMPREHENSIVE error handling
├── ViewModel with complete state management (loading, error, success states) 
├── ENHANCED error handling with better retry functionality for network failures
├── Loading indicators during API calls with better UX
├── Portfolio card shows BTC/USD balances with "Live Data" indicator + enhanced formatting
├── Navigation OPTIMIZED - resolved duplicate TopAppBar issue for cleaner UI
├── COMPREHENSIVE authentication with advanced PEM key parsing supporting all formats
└── Professional UX flow with robust state management and error recovery

✅ COMPLETE: Intelligent CI/CD Pipeline (v1.10.0)
├── Claude API integration for build failure analysis
├── Automated fix recommendations and commit-back workflow
├── Intelligent version management with semantic versioning
├── Release notes generation based on commit analysis
├── Infinite loop prevention with [claude-fix] markers
└── Professional deployment process with Firebase App Distribution
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

---

## 🎉 Major Achievement: Complete Trading Engine (v1.10.0)

**Phase 2 has been successfully completed with enhanced use case implementation and intelligent CI/CD integration:**

### ⚡ Intelligent CI/CD Pipeline (v1.10.0)

**Revolutionary development workflow with Claude API integration:**

- **🤖 Build Failure Analysis:** Automatic error analysis with Claude API providing specific fix recommendations
- **📋 Version Management:** Intelligent semantic versioning based on commit analysis
- **📝 Release Notes:** Contextual release notes generated automatically
- **🔄 Auto-Fix Workflow:** Commit-back pattern with fix instructions and retrigger capability
- **🛡️ Safety Features:** Infinite loop prevention and manual intervention detection

### 🔧 Enhanced Use Cases (v1.10.0)

- **ExecuteTradingCycleUseCase** - UPDATED with improved error handling and portfolio state management
- **PortfolioRepositoryImpl** - ENHANCED with high water mark tracking and comprehensive portfolio calculations
- **RepositoryModule** - ENHANCED with additional repository bindings and improved DI structure

### 🏗️ Core Trading Logic (Complete)

- **TradingDecisionEngine** - Complete regime-switching logic with SMA(200), ADX(14), ATR(14)
- **Technical Indicators** - Full ta4j integration with proper dependency injection
- **Use Case Orchestration** - 7 use cases covering complete trading workflow
- **Risk Management Integration** - Drawdown monitoring, position sizing, emergency liquidation
- **Comprehensive Testing** - Unit tests with MockK for all critical components

---

## ❌ What's Missing (Phase 3)

### Critical Path to Live Trading

| Component | Status | Blocker Level | Description |
|-----------|--------|---------------|-------------|
| **REST API Client** | ❌ Not Started | 🔴 CRITICAL | Order placement, candle fetching, product queries |
| **WebSocket Client** | ❌ Not Started | 🔴 CRITICAL | Real-time price feeds, order status updates |
| **Risk Manager Implementation** | ❌ Not Started | 🟡 HIGH | Position sizing, drawdown monitoring |
| **Trading Service** | ❌ Not Started | 🟡 HIGH | 24/7 foreground service orchestration |
| **Integration Testing** | ❌ Not Started | 🟠 MEDIUM | End-to-end validation with real API |

**Estimated Timeline:** 3-4 weeks to first live trade capability

---

## 📊 Implementation Progress

### Overall Progress: 8/12 tickets complete (67%)

```
Phase 1:  ████████████████████ 100% (4/4) ✅ COMPLETE
Phase 2:  ████████████████████ 100% (4/4) ✅ COMPLETE  
Phase 3:  ░░░░░░░░░░░░░░░░░░░░   0% (0/4) ← NEXT PHASE
Total:    ███████████████░░░░░  67% (8/12)
```

### Phase Progress Detail

**✅ Phase 1: Foundation & API Integration (100% - COMPLETE)**
- [x] Domain models (Candle, Order, Decision, Portfolio) + enums
- [x] Room database (4 entities + 4 DAOs + mappers)
- [x] JWT generator (ES256 signing with comprehensive BouncyCastle)
- [x] Dashboard with live Coinbase data + robust error handling

**✅ Phase 2: Core Trading Logic (100% - COMPLETE)** 
- [x] **Decision Engine** (regime-switching with SMA/ADX/ATR + hysteresis)
- [x] **Technical Indicators** (SMACalculator, ADXCalculator, ATRCalculator + DI)
- [x] **Strategy Configuration** (StrategyConfig with comprehensive defaults)
- [x] **Complete Use Case Layer** (7 use cases with full trading orchestration)

**❌ Phase 3: API Integration & Service (0% - IN PROGRESS)**
- [ ] **Full REST API Client** (order placement, candles, products)
- [ ] **WebSocket Client** (real-time feeds, order updates)
- [ ] **Risk Manager** (position sizing, drawdown monitoring)
- [ ] **Trading Service** (24/7 foreground service with orchestration)

---

## 🎯 Next Immediate Actions

### 1. Ticket 13: Full REST API Client (CRITICAL BLOCKER)

**Priority:** 🔴 HIGHEST
**Effort:** Large (3-5 days)
**Dependencies:** ✅ JWT auth working, ✅ Use cases ready

**Goal:** Complete CoinbaseRepository implementation for live trading

**Key methods to implement:**
```kotlin
// Order placement (use cases ready to consume)
suspend fun placeBracketOrder(...): Result<Order>  // TREND mode
suspend fun placeLimitOrder(...): Result<Order>    // RANGE mode  
suspend fun placeMarketOrder(...): Result<Order>   // Emergency liquidation

// Market data
suspend fun getCandles(...): Result<List<Candle>>   // Decision engine needs this
suspend fun getCurrentPrice(...): Result<Ticker>   // Portfolio updates

// Order management
suspend fun cancelOrders(...): Result<Unit>        // Risk management
suspend fun getOpenOrders(...): Result<List<Order>> // Order reconciliation
```

**Integration points:**
- ExecuteDecisionUseCase ready to place bracket/limit orders
- TradingDecisionEngine needs historical candles
- ManageOrdersUseCase ready for order lifecycle management
- Portfolio calculation needs current BTC price

### 2. Ticket 14: WebSocket Client (CRITICAL BLOCKER)

**Priority:** 🔴 HIGHEST  
**Effort:** Large (3-5 days)
**Dependencies:** ✅ JWT auth working

**Goal:** Real-time market data and order status updates

**Key features:**
```kotlin
// Real-time price feeds
fun subscribeTicker(productIds: List<String>): Flow<Ticker>

// Order status updates  
fun subscribeOrderUpdates(): Flow<Order>

// Connection management
val connectionState: StateFlow<ConnectionState>
fun connect()
fun disconnect()
```

**Integration points:**
- UpdatePortfolioUseCase needs real-time BTC price
- ManageOrdersUseCase needs order status updates
- Trading service needs connection state monitoring

### 3. Ticket 16: Risk Manager (HIGH PRIORITY)

**Priority:** 🟡 HIGH
**Effort:** Medium (2-3 days)
**Dependencies:** REST API for portfolio queries

**Goal:** Position sizing and drawdown monitoring

**Key features:**
```kotlin
fun calculatePositionSize(portfolio: Portfolio, decision: Decision): BigDecimal
fun checkDrawdown(currentEquity: BigDecimal): DrawdownStatus
fun validateOrder(order: Order, portfolio: Portfolio): OrderValidation
```

**Integration points:**
- ExecuteTradingCycleUseCase ready to use drawdown checking
- All use cases ready for risk validation
- Emergency liquidation triggered at 15% drawdown

### 4. Ticket 17: Trading Service (HIGH PRIORITY)

**Priority:** 🟡 HIGH
**Effort:** Large (4-6 days)  
**Dependencies:** REST API + WebSocket + Risk Manager

**Goal:** 24/7 autonomous trading orchestration

**Key features:**
```kotlin
class TradingService : Service() {
    // Strategy evaluation every 15 minutes
    private suspend fun runTradingCycle()
    
    // Real-time price monitoring
    private fun startPriceMonitor()
    
    // Foreground service with notification
    override fun onStartCommand(): Int
}
```

**Integration points:**
- ExecuteTradingCycleUseCase ready for complete orchestration
- All use cases ready for service consumption
- UI service controls already implemented

---

## 🛠️ Development Strategy

### Critical Path Dependencies

```
REST API (13) ──┐
                ├─→ Risk Manager (16) ──┐
WebSocket (14)──┘                      ├─→ Trading Service (17) → MVP
                                       │
Decision Engine (15) ──────────────────┘
     ↑ COMPLETE ✅
```

### Parallel Development Opportunities

**Can work simultaneously:**
- REST API Client (Ticket 13) 
- WebSocket Client (Ticket 14)
- Risk Manager foundation (Ticket 16)

**Must be sequential:**
- Risk Manager needs REST API for portfolio queries
- Trading Service needs all other components complete
- Integration testing needs Trading Service working

### Success Criteria for Phase 3

**Minimum Viable Product (MVP) requirements:**
- [ ] Can place bracket orders for TREND mode
- [ ] Can place limit orders for RANGE mode  
- [ ] Can cancel orders for DEFENSE mode
- [ ] Real-time BTC price updates in UI
- [ ] Order status updates flow through system
- [ ] Portfolio value calculated correctly
- [ ] Drawdown monitoring triggers at 15%
- [ ] Service survives device sleep (24-hour test)

**Integration testing requirements:**
- [ ] Small real trades ($10-20) execute correctly
- [ ] WebSocket reconnects after network interruption
- [ ] Order reconciliation handles edge cases
- [ ] Emergency liquidation works end-to-end
- [ ] No memory leaks in 24-hour service test

---

## 💡 Key Learnings & Architecture Decisions

### ✅ What's Working Well

**1. Multi-module architecture** - Clean separation enables parallel development
**2. Use case pattern** - Business logic isolated and testable
**3. Dependency injection** - Easy to swap implementations and test
**4. Build-time credentials** - Secure and deployment-friendly
**5. Claude CI/CD integration** - Revolutionary development workflow
**6. Domain-first design** - Interfaces enable clean testing and swapping

### 🔧 Technical Debt & Improvements

**1. Error handling consistency** - Need standardized Result patterns
**2. Logging strategy** - Structured logging for debugging
**3. Configuration management** - Runtime strategy parameter tuning
**4. Performance monitoring** - Track use case execution times
**5. Integration test framework** - Automated testing with real API

### 🎯 Architecture Validation

**The modular architecture has proven its value:**
- Domain layer: 100% pure Kotlin, easily testable
- Data layer: Clean abstraction over Room and network
- Exchange layer: Coinbase implementation isolated and swappable
- UI layer: Reactive state management with proper separation
- CI/CD layer: Intelligent automation with Claude API

**Next phase will validate:**
- Network resilience (WebSocket reconnection)
- Background service reliability (24/7 operation)  
- Risk management effectiveness (real money protection)
- System integration (all components working together)

---

## 🚀 Phase 3 Success Metrics

### Technical Milestones
- [ ] **REST API:** All CRUD operations working with real Coinbase API
- [ ] **WebSocket:** 24-hour connection stability test passed
- [ ] **Risk Management:** Drawdown detection triggers correctly
- [ ] **Service:** Survives device sleep/wake cycles
- [ ] **Integration:** End-to-end trade execution (small amounts)

### Business Validation
- [ ] **First live trade:** Successfully execute bracket order
- [ ] **Portfolio tracking:** Real-time value updates working
- [ ] **Risk enforcement:** Position sizing limits respected  
- [ ] **Emergency protection:** 15% drawdown triggers liquidation
- [ ] **System reliability:** 7-day continuous operation

### Quality Gates
- [ ] **Unit test coverage:** >80% for all new components
- [ ] **Integration tests:** All critical paths covered
- [ ] **Performance:** Use case execution <500ms average
- [ ] **Memory:** No leaks in 24-hour service operation
- [ ] **Security:** API credentials never logged or exposed

---

## 📋 Implementation Checklist

### Before Starting Phase 3

- [x] ✅ Phase 1 & 2 complete and tested
- [x] ✅ All dependencies configured and active
- [x] ✅ Use cases implemented and tested
- [x] ✅ CI/CD pipeline with Claude integration working
- [x] ✅ Domain interfaces defined and stable

### Ready to Begin

**Ticket 13 (REST API Client) can start immediately:**
- JWT authentication working and tested
- DTO/mapper patterns established  
- Error handling patterns defined
- Use cases ready to consume ExchangeRepository
- Integration points clearly defined

**Estimated Timeline:**
- **Week 1:** REST API Client (Ticket 13)
- **Week 2:** WebSocket Client (Ticket 14) 
- **Week 3:** Risk Manager + Trading Service (Tickets 16, 17)
- **Week 4:** Integration testing + MVP validation (Tickets 19, 20)

**Total Phase 3 Duration:** 4 weeks to live trading capability

