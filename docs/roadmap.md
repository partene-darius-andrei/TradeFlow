# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-09
**Current Phase:** Phase 2 Complete - Core Trading Logic (v1.8.0)  
**Current Build:** #31 SUCCESS (Version 1.8.0)
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

**Phases 1 & 2 COMPLETE - Enhanced Coinbase Integration + Core Trading Logic (v1.8.0):**

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
│   └── DomainModule.kt          ✅ Decision engine DI binding (NEW in v1.8.0)
├── data/local/
│   ├── AppDatabase.kt           ✅ Complete Room DB with 4 entities
│   └── PlaceholderEntity.kt     ✅ Removed (no longer needed)
└── navigation/
    ├── AppNavHost.kt            ✅ Complete navigation with UNIFIED TopAppBar ("TradeFlow" title)
    └── Screen.kt                ✅ Dashboard + Settings routes

✅ COMPLETE: Domain Layer Foundation with Enhanced Decision Engine (v1.8.0)
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
    │   └── TradingDataRepository.kt      ✅ Local data queries (NEW in v1.8.0)
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
    └── usecase/                 ✅ Complete Use Case Layer Implementation (v1.8.0)
        ├── ExecuteDecisionUseCase.kt   ✅ Trading decision execution orchestrator
        ├── ExecuteTradingCycleUseCase.kt ✅ Complete trading cycle with risk management (UPDATED)
        ├── HandleEmergencyUseCase.kt   ✅ Emergency liquidation handler
        ├── HandleGridFillsUseCase.kt   ✅ Grid fill detection and profit taking (NEW)
        ├── ManageGridOrdersUseCase.kt  ✅ Grid order management for range trading
        ├── ManageOrdersUseCase.kt      ✅ Order lifecycle and reconciliation
        ├── UpdatePortfolioUseCase.kt   ✅ Portfolio state updates
        └── model/
            ├── ExecutionResult.kt      ✅ Use case result types
            └── TradingContext.kt       ✅ Trading context data model

✅ COMPLETE: Data Layer Implementation (Enhanced v1.8.0)
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
    │       ├── OrderDao.kt          ✅ Order queries + getRecentFilledOrders() (NEW)
    │       ├── DecisionDao.kt       ✅ Decision queries
    │       └── PortfolioDao.kt      ✅ Portfolio queries
    ├── mapper/
    │   └── OrderMapper.kt           ✅ OrderEntity ↔ Order domain mapping (NEW)
    ├── repository/
    │   ├── TradingDataRepositoryImpl.kt ✅ Local data repository implementation (NEW)
    │   └── PortfolioRepositoryImpl.kt   ✅ Portfolio data repository (NEW)
    └── di/
        ├── SecurityModule.kt        ✅ Hilt DI for credential store
        ├── DatabaseModule.kt        ✅ Hilt DI for Room database
        └── RepositoryModule.kt      ✅ Repository DI bindings (ENHANCED)

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

### 🎉 Major Milestone: Complete Core Trading Logic (v1.8.0)

**Phase 2 has been successfully completed with comprehensive use case implementation:**

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
  - **UPDATED:** Removed direct ExchangeRepository dependency - now uses individual use cases for better separation of concerns

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

### ❌ What's MISSING (Phase 3 Ready to Start)

**Immediate Roadblock - Exchange Integration:**
```
❌ exchange/coinbase/
    ├── CoinbaseRepository.kt           ❌ Full REST API implementation
    │                                      - Order placement (bracket, limit, market)
    │                                      - Candle data fetching (handle 350-limit)
    │                                      - Product queries (min sizes, trading pairs)
    │                                      - Order management (cancel, status)
    ├── CoinbaseWebSocket.kt            ❌ Real-time feeds
    │                                      - Ticker subscription (price updates)
    │                                      - Order updates (fill notifications)
    │                                      - Connection management with heartbeats
    └── error/
        └── CoinbaseErrorHandler.kt     ❌ API error mapping

❌ core/domain/risk/
    └── RiskManagerImpl.kt              ❌ Risk validation implementation
                                           - Position sizing calculations
                                           - Drawdown monitoring
                                           - Order validation against limits

❌ service/trading/
    └── TradingService.kt               ❌ 24/7 foreground service
                                           - Use case orchestration
                                           - Wake lock management
                                           - Notification updates
                                           - Periodic strategy evaluation
```

---

## 🚀 Implementation Roadmap

### ✅ Phase 1: Foundation Complete (100%) 

**Goal:** Solid architecture foundation with live data integration

| Component | Status | Description |
|-----------|---------|-------------|
| **Modularization** | ✅ Complete | 8-module architecture with proper dependencies |
| **Domain Models** | ✅ Complete | Candle, Order, Portfolio, Decision, Balance, Ticker |
| **Room Database** | ✅ Complete | 4 entities + 4 DAOs with proper relationships |
| **JWT Authentication** | ✅ Complete | ES256 signing with BouncyCastle PEM parsing |
| **Basic API Integration** | ✅ Complete | getBalances() working with real Coinbase data |
| **Dashboard UI** | ✅ Complete | Live portfolio display with error handling |

**Deliverables:** ✅ All complete
- Multi-module architecture established
- Domain layer interfaces defined
- Room database with complete schema
- JWT authentication working with real API
- Dashboard showing live Coinbase balances
- Repository pattern with dependency injection

---

### ✅ Phase 2: Core Trading Logic Complete (100%)

**Goal:** Complete trading decision engine with comprehensive use case layer

| Component | Status | Description |
|-----------|---------|-------------|
| **Decision Engine** | ✅ Complete | SMA/ADX/ATR regime-switching with hysteresis |
| **Technical Indicators** | ✅ Complete | SMA, ADX, ATR calculators with ta4j integration |
| **Strategy Configuration** | ✅ Complete | Comprehensive parameters for all trading modes |
| **Use Case Layer** | ✅ Complete | 7 use cases handling complete trading lifecycle |
| **Unit Testing** | ✅ Complete | MockK integration with comprehensive test coverage |

**Deliverables:** ✅ All complete (v1.8.0)
- TradingDecisionEngine with 4 modes (DEFENSE/TREND/RANGE/WAIT)
- 3-candle hysteresis to prevent whipsawing
- Complete use case layer orchestrating trading operations
- ExecuteTradingCycleUseCase as master coordinator
- Comprehensive unit testing with MockK
- Technical indicators fully integrated and tested

---

### ❌ Phase 3: API Integration & Service Implementation (0% - NEXT PHASE)

**Goal:** Complete Coinbase API integration and 24/7 service implementation

**Priority Order:**

#### 3A: Full REST API Client (HIGH PRIORITY)
| Ticket | Component | Status | Description |
|--------|-----------|---------|-------------|
| **13** | **Full REST API Client** | ❌ Not Started | Order placement, candle fetching, product queries |

**Critical Implementation:**
- Complete CoinbaseRepository with all 12 ExchangeRepository methods
- Order placement: bracket orders (TREND), limit orders (RANGE), market orders (emergency)
- Candle data: Handle 350-candle limit, TWO_HOUR aggregation to H4
- Product queries: Trading pairs, minimum order sizes, fee structures
- Error handling: Rate limits, authentication failures, order rejections

#### 3B: Real-Time Data Feeds (HIGH PRIORITY)
| Ticket | Component | Status | Description |
|--------|-----------|---------|-------------|
| **14** | **WebSocket Client** | ❌ Not Started | Real-time price feeds, order status updates |

**Critical Implementation:**
- CoinbaseWebSocket implementing ExchangeWebSocket interface
- Ticker subscription for portfolio value updates
- Order status updates for fill detection
- Connection management with auto-reconnect and heartbeats
- Health monitoring to detect stale connections

#### 3C: Risk Management (MEDIUM PRIORITY)
| Ticket | Component | Status | Description |
|--------|-----------|---------|-------------|
| **16** | **Risk Manager Implementation** | ❌ Not Started | Position sizing, drawdown monitoring |

**Critical Implementation:**
- RiskManagerImpl with position sizing calculations
- Drawdown monitoring with 15% emergency liquidation
- Order validation against risk limits
- High water mark tracking and updates

#### 3D: Trading Service (HIGH PRIORITY)
| Ticket | Component | Status | Description |
|--------|-----------|---------|-------------|
| **17** | **Trading Service** | ❌ Not Started | 24/7 foreground service orchestration |

**Critical Implementation:**
- Foreground service with proper notifications
- Use case orchestration (ExecuteTradingCycleUseCase every 15 minutes)
- Wake lock management for 24/7 operation
- Battery optimization handling
- Service lifecycle management

**Phase 3 Success Criteria:**
- [ ] Can place bracket orders for TREND mode via ExecuteDecisionUseCase
- [ ] Can place limit orders for RANGE mode via ManageGridOrdersUseCase
- [ ] Real-time price updates feeding into UpdatePortfolioUseCase
- [ ] Order fill detection triggering HandleGridFillsUseCase
- [ ] Emergency liquidation via HandleEmergencyUseCase working end-to-end
- [ ] Service runs 24/7 without crashes
- [ ] All use cases integrated with real exchange APIs

**Estimated Timeline:** 3-4 weeks

---

### ❌ Phase 4: Testing & Validation (0%)

**Goal:** Comprehensive testing and system validation

| Ticket | Component | Status | Priority |
|--------|-----------|---------|----------|
| **19** | **Integration Tests** | ❌ Not Started | MEDIUM | End-to-end testing with small real trades |
| **20** | **MVP Milestone** | ❌ Not Started | HIGH | 24-hour live system validation |

**Success Criteria:**
- [ ] Small real trades ($10-20) execute successfully
- [ ] System survives 24-hour continuous operation
- [ ] Emergency liquidation tested and working
- [ ] All trading modes demonstrate correct behavior
- [ ] Risk limits enforced properly
- [ ] No memory leaks or performance degradation

**Estimated Timeline:** 1-2 weeks

---

## 📊 Progress Tracking

### Overall Progress: 8/14 core tickets complete (57%)

```
Phase 1:  ████████████████████ 100% (4/4) ✅ COMPLETE
Phase 2:  ████████████████████ 100% (4/4) ✅ COMPLETE  
Phase 3:  ░░░░░░░░░░░░░░░░░░░░   0% (0/4) ← YOU ARE HERE
Phase 4:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
```

### Current Sprint: Phase 3 Ready to Start

**Critical Path to MVP:**

1. ✅ ~~Complete trading engine~~ (v1.8.0 - DONE)
2. **Ticket 13: Full REST API Client** ← IMMEDIATE PRIORITY
3. **Ticket 14: WebSocket Client**
4. **Ticket 16: Risk Manager Implementation**
5. **Ticket 17: Trading Service**
6. **Integration Testing & MVP Validation**

**Estimated Time to MVP:** 4-6 weeks total

---

## 🔍 Architecture Status

### Module Dependency Health

```
:app ──────────────────────────┐
  ├─ :core:ui ─────────────────┼─ :core:domain
  ├─ :core:data ───────────────┘
  └─ :exchange:coinbase ───────── :core:domain

Status: ✅ Clean dependencies, no circular references
```

### Key Architecture Decisions

**✅ Successful Patterns:**
- Multi-module architecture prevents cross-layer dependencies
- Repository pattern with interfaces enables clean testing
- Use case layer provides clear business logic separation
- Hilt dependency injection simplifies testing and configuration
- Build-time credential injection eliminates UI complexity

**🎯 Ready for Scale:**
- Domain layer is completely exchange-agnostic
- Use cases are ready to orchestrate real trading operations
- Decision engine is fully functional and tested
- Database schema supports all trading operations

---

## 🚨 Risk Assessment

### Technical Risks

**LOW RISK ✅:**
- Domain logic (decision engine, use cases) - Fully implemented and tested
- Database schema - Complete and stable
- Authentication - JWT working with real API
- Architecture - Clean separation, no technical debt

**MEDIUM RISK ⚠️:**
- Real-time WebSocket reliability - Needs robust error handling and reconnection
- Order execution latency - API response times impact strategy effectiveness  
- Battery optimization - Android may kill service despite foreground status

**HIGH RISK 🚨:**
- Exchange API changes - Coinbase could modify endpoints without notice
- Market conditions - Strategy assumes normal market behavior
- Regulatory changes - Crypto trading regulations could impact operations

### Financial Risks

**CONTROLLED ✅:**
- Starting capital: $500 (education budget, not investment)
- Position sizing: 1-2% risk per trade ($5-10 maximum loss)
- Emergency stops: 15% drawdown triggers complete liquidation
- Realistic expectations: 3-5% monthly returns (exceptional skill required)

**See:** [docs/strategy/bitcoin-first-strategy.md](strategy/bitcoin-first-strategy.md) for complete risk analysis

---

## 🎯 Success Metrics

### Phase 3 Targets
- [ ] All REST API endpoints working (orders, candles, products)
- [ ] WebSocket stable for 8+ hours continuous operation
- [ ] Trading service survives overnight with screen off
- [ ] Emergency liquidation tested with small real trade

### MVP Targets (End of Phase 4)
- [ ] System runs 24/7 for one week without crashes
- [ ] Executes at least one successful trade per trading mode
- [ ] Risk management prevents any single loss > 2% of capital
- [ ] Complete audit trail of all decisions and trades

### Business Targets (Post-MVP)
- [ ] Break-even after 6 months (covers all trading fees)
- [ ] 3% average monthly returns after 12 months
- [ ] Scale to $2,500+ account size after 18-24 months

---

## 🔮 Future Roadmap (Post-MVP)

### Phase 5: Strategy Enhancement
- Multi-timeframe analysis (H1 + H4 + D1)
- Additional indicators (RSI, MACD, Bollinger Bands)
- Market regime detection improvements
- Dynamic position sizing based on market volatility

### Phase 6: Multi-Exchange Support
- Kraken integration (lower fees for larger accounts)
- Binance.US support (if regulatory environment permits)
- Cross-exchange arbitrage opportunities
- Exchange-specific optimization

### Phase 7: Advanced Features
- Backtesting framework with historical data
- Paper trading mode for strategy validation
- Advanced analytics and performance reporting
- Tax reporting integration

---

## 🛠️ Development Guidelines

### Code Quality Standards
- ✅ All use cases have comprehensive unit tests
- ✅ Domain layer remains exchange-agnostic
- ✅ Repository pattern enforced with interfaces
- ✅ Dependency injection used throughout
- ✅ Error handling covers all failure scenarios

### Testing Strategy
- **Unit Tests:** MockK for isolated component testing
- **Integration Tests:** Small real trades to validate API integration
- **System Tests:** 24-hour continuous operation validation
- **Performance Tests:** Memory usage and battery consumption monitoring

### Deployment Strategy
- **Development:** Local builds with `local.properties` credentials
- **Testing:** GitHub Actions with injected credentials
- **Production:** Direct APK install on dedicated Android device
- **Monitoring:** Timber logging with Firebase Crashlytics

---

This roadmap is a living document updated as development progresses. The focus remains on building a simple, reliable system that can trade profitably with minimal complexity.

