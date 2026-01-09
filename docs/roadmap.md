# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-09
**Project Status:** Phase 2 Complete - Core Trading Logic (v1.6.0)  
**Current Build:** #31 SUCCESS (Version 1.6.0)
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

**Phases 1 & 2 COMPLETE - Enhanced Coinbase Integration + Core Trading Logic (v1.6.0):**

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
│   └── CredentialsModule.kt     ✅ Provides build-injected credentials
├── data/local/
│   ├── AppDatabase.kt           ✅ Complete Room DB with 4 entities
│   └── PlaceholderEntity.kt     ✅ Removed (no longer needed)
└── navigation/
    ├── AppNavHost.kt            ✅ Complete navigation with UNIFIED TopAppBar ("TradeFlow" title)
    └── Screen.kt                ✅ Dashboard + Settings routes

✅ COMPLETE: Domain Layer Foundation with Enhanced Decision Engine (v1.6.0)
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
    │   ├── BracketOrderRepository.kt ✅ Bracket order support
    │   ├── ExchangeRepository.kt     ✅ Core operations (12 methods)
    │   └── ExchangeWebSocket.kt      ✅ Real-time streams
    ├── strategy/                ✅ Decision engine (Ticket 15 - COMPLETE)
    │   ├── DecisionEngine.kt    ✅ Decision engine interface
    │   ├── TradingDecisionEngine.kt ✅ Complete regime-switching implementation with hysteresis
    │   └── StrategyConfig.kt    ✅ Comprehensive strategy parameters
    ├── indicator/               ✅ Technical indicators (COMPLETE)
    │   ├── SMACalculator.kt     ✅ Simple Moving Average with ta4j integration
    │   ├── ADXCalculator.kt     ✅ Average Directional Index with ta4j integration
    │   └── ATRCalculator.kt     ✅ Average True Range with ta4j integration
    ├── risk/
    │   └── RiskManager.kt       ✅ Risk management interface with enhanced types
    └── usecase/                 ✅ NEW - Use Case Layer Implementation (v1.6.0)
        ├── ExecuteDecisionUseCase.kt   ✅ Trading decision execution orchestrator
        ├── ExecuteTradingCycleUseCase.kt ✅ Complete trading cycle with risk management
        ├── HandleEmergencyUseCase.kt   ✅ Emergency liquidation handler
        ├── ManageGridOrdersUseCase.kt  ✅ Grid order management for range trading
        ├── ManageOrdersUseCase.kt      ✅ Order lifecycle and reconciliation
        ├── UpdatePortfolioUseCase.kt   ✅ Portfolio state updates
        └── model/
            ├── ExecutionResult.kt      ✅ Use case result types
            └── TradingContext.kt       ✅ Trading context data model

✅ COMPLETE: Data Layer Implementation
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
    │       ├── OrderDao.kt          ✅ Order queries
    │       ├── DecisionDao.kt       ✅ Decision queries
    │       └── PortfolioDao.kt      ✅ Portfolio queries
    └── di/
        ├── SecurityModule.kt        ✅ Hilt DI for credential store
        └── DatabaseModule.kt        ✅ Hilt DI for Room database

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
- ✅ **security-crypto 1.1.0-alpha06** (replaced by build-time injection)
- ✅ **work-runtime-ktx 2.11.0** (for background tasks)
- ✅ **datastore-preferences 1.2.0** (for settings)
- ✅ **material-icons-extended** ✅ ACTIVE (for ModeIndicator icons)
- ✅ Firebase Analytics + Crashlytics (BOM 34.7.0)

**CI/CD:**
- ✅ GitHub Actions: Enhanced build workflow with COMPREHENSIVE credential injection and PEM key escaping
- ✅ Auto-build + Firebase App Distribution on PR
- ✅ Commit-back pattern (`.build-status` + `build-log.txt`)
- ✅ Auto-documentation workflow (updates CLAUDE.md + docs/)

**Documentation:**
- ✅ CLAUDE.md (project context for AI)
- ✅ ~/.claude/CLAUDE.md (global AI preferences)
- ✅ docs/reference.md (implementation blueprint with hierarchical structure)
- ✅ docs/ci.md (CI/CD documentation with credential injection)
- ✅ docs/auto-docs.md (auto-doc workflow)
- ✅ docs/tickets/ (all Notion tickets organized by status)

### 🎉 MAJOR MILESTONE: Complete Core Trading Logic Implementation (v1.6.0)

**Phase 2 has been successfully completed with comprehensive trading engine implementation:**

### 🆕 Use Case Layer Implementation (NEW in v1.6.0)

**Complete domain use case layer with comprehensive trading logic:**

✅ **ExecuteDecisionUseCase** - Central decision execution orchestrator
- **Handles all 4 trading modes:** DEFENSE (cancel buy orders), TREND (bracket orders), RANGE (grid orders), WAIT (no action)
- **Risk validation:** All orders validated by RiskManager before placement
- **Bracket order support:** Complete trend trading with entry, stop-loss, and take-profit
- **Grid order management:** Dynamic grid spacing with fee break-even validation
- **Comprehensive error handling:** Graceful failure recovery with detailed error messages

✅ **ExecuteTradingCycleUseCase** - Complete trading cycle orchestrator
- **Portfolio management:** Real-time updates with high water mark tracking
- **Drawdown monitoring:** Automatic emergency liquidation at 15% drawdown
- **Strategy evaluation:** Integration with TradingDecisionEngine 
- **Order reconciliation:** Sync local and exchange order states
- **Emergency procedures:** Complete portfolio liquidation with service shutdown

✅ **HandleEmergencyUseCase** - Emergency liquidation handler
- **Order cancellation:** Cancel all open orders before liquidation
- **Portfolio liquidation:** Market sell all BTC positions
- **Error resilience:** Handles partial failures and continues liquidation
- **Safety prioritization:** Prioritizes capital preservation over optimization

✅ **ManageGridOrdersUseCase** - Advanced grid trading implementation
- **Dynamic spacing:** ATR-based grid calculation with 1.5% minimum (fee break-even)
- **Risk integration:** Position sizing via RiskManager
- **Partial success:** Handles mixed order placement results
- **Level management:** Prevents duplicate orders at same grid levels

✅ **ManageOrdersUseCase** - Order lifecycle management
- **Stale order cleanup:** 48-hour timeout with automatic cancellation
- **Order reconciliation:** Sync between local database and exchange
- **Status tracking:** Real-time order state updates
- **Orphaned order detection:** Identify and handle missing orders

✅ **UpdatePortfolioUseCase** - Portfolio state management
- **Multi-currency support:** BTC and USD balance tracking
- **Real-time calculation:** Dynamic total equity computation
- **Snapshot creation:** Historical tracking for drawdown analysis
- **Price integration:** Current market price for accurate valuations

### 🆕 Enhanced Domain Models (v1.6.0)

✅ **ExecutionResult sealed class** - Standardized use case results
- `Success(message)` - Successful operation with details
- `Skipped(reason)` - Operation skipped with rationale  
- `Failed(error)` - Operation failed with error details

✅ **TradingContext data model** - Complete trading state context
- Product ID, candles, current price, portfolio, high water mark
- Standardized input for trading cycle execution

✅ **Portfolio utility extensions** - Enhanced portfolio operations
- `getBtcBalance()` extension function for easy BTC balance access
- Simplified balance retrieval across use cases

### 🆕 Technical Indicators Complete (v1.6.0)

✅ **SMACalculator** - Simple Moving Average with ta4j integration
- BaseBarSeries conversion from Candle domain models
- Configurable period support (default: 200 for trend filter)
- BigDecimal precision for financial calculations

✅ **ADXCalculator** - Average Directional Index for trend strength  
- Complete ADX calculation using ta4j ADXIndicator
- Configurable period support (default: 14)
- Double precision for percentage-based strength measurement

✅ **ATRCalculator** - Average True Range for volatility measurement
- ATR calculation for stop-loss and take-profit placement
- Configurable period support (default: 14)
- BigDecimal precision for price-based calculations

### 🆕 Comprehensive Unit Testing (v1.6.0)

✅ **ExecuteDecisionUseCaseTest** - Complete decision execution testing
- All 4 trading modes tested (DEFENSE/TREND/RANGE/WAIT)
- Risk validation scenarios (approved/rejected)
- Order placement success/failure handling
- Mock integration with ExchangeRepository and RiskManager

✅ **HandleEmergencyUseCaseTest** - Emergency scenarios
- Complete liquidation flow testing
- Partial failure recovery (orders vs balances)
- Error handling with network failures
- Edge cases (no orders, zero balance)

✅ **ManageGridOrdersUseCaseTest** - Grid trading validation
- Grid spacing calculation and validation
- Risk check integration and rejection handling
- Partial success scenarios (some orders succeed, others fail)
- Fee break-even validation (1.5% minimum)

**Testing Infrastructure:**
- **MockK integration** for isolated unit testing
- **Kotlin-test framework** for assertions and test structure  
- **Synthetic test data** for candles, portfolios, and orders
- **Edge case coverage** for error conditions and boundary values

### What's MISSING (Phase 3 - API Integration)

❌ **Full REST API Client** (Ticket 13) - NEXT PRIORITY
- Complete CoinbaseRepository implementation
- Order placement methods (bracket, limit, market orders)
- Candle data fetching (handle 350-candle limit, TWO_HOUR aggregation)
- Product queries and order management

❌ **WebSocket Client** (Ticket 14)  
- Real-time price feeds and order status updates
- Connection management with auto-reconnect
- Integration with use cases for live data

❌ **Risk Manager Implementation** (Ticket 16)
- Concrete RiskManager implementation
- Position sizing calculations
- Drawdown monitoring with high water mark tracking

❌ **Trading Service** (Ticket 17)
- 24/7 foreground service implementation
- Use case orchestration in background
- Battery optimization and doze survival

---

## Implementation Status

### Overall Progress

```
Phase 1 (Foundation):          ████████████████████ 100% (4/4 tickets) ✅
Phase 2 (Core Logic):          ████████████████████ 100% (4/4 tickets) ✅
Phase 3 (API Integration):     ░░░░░░░░░░░░░░░░░░░░   0% (0/4 tickets) ← NEXT
Phase 4 (Testing & MVP):       ░░░░░░░░░░░░░░░░░░░░   0% (0/2 tickets)

Total Progress: 8/14 tickets (57%)
```

### Current Architecture Status

| Module | Purpose | Completion | Status |
|--------|---------|------------|--------|
| `:app` | DI wiring + credential injection | 100% | ✅ Complete |
| `:core:domain` | Business logic + **use cases** | 100% | ✅ Complete |
| `:core:data` | Room database + security | 100% | ✅ Complete |
| `:core:ui` | Shared Compose components | 100% | ✅ Complete |
| `:exchange:coinbase` | Coinbase API integration | 30% | 🟡 Auth only |

### Dependency Status

| Library | Status | Usage |
|---------|--------|-------|
| **ta4j-core 0.16** | ✅ ACTIVE | Technical indicators (SMA/ADX/ATR) |
| **mockk 1.14.7** | ✅ ACTIVE | Unit testing with mocks |
| **kotlin-test 2.1.0** | ✅ ACTIVE | Testing framework |
| **nimbus-jose-jwt 9.47** | ✅ ACTIVE | ES256 JWT signing |
| **BouncyCastle 1.78** | ✅ ACTIVE | Advanced PEM key parsing |
| **Room 2.8.4** | ✅ ACTIVE | Database (4 entities + 4 DAOs) |
| **Ktor 3.3.3** | 🟡 PARTIAL | HTTP client (auth ready, full client pending) |
| **Hilt 2.57.2** | ✅ ACTIVE | Dependency injection |

---

## Phase 3: API Integration & Service Implementation

### Critical Path to MVP

**Next 4 tickets required for live trading capability:**

#### Ticket 13: Full REST API Client ⭐ HIGH PRIORITY

**Goal:** Complete CoinbaseRepository implementation with all trading operations

**Files to implement:**
- Order placement methods (bracket, limit, market orders)
- Candle data fetching with TWO_HOUR aggregation to H4
- Product queries (trading pairs, minimum sizes)
- Order management (cancel, query status, fills)
- Rate limiting and error handling

**Integration points:**
- Use cases ready to consume via ExchangeRepository interface
- JWT authentication already working
- DTO/mapper layer established

**Acceptance criteria:**
- ExecuteDecisionUseCase can place bracket orders for TREND mode
- ManageGridOrdersUseCase can place limit orders for RANGE mode
- TradingDecisionEngine can fetch 350 candles for analysis
- All use cases work with real Coinbase API

#### Ticket 14: WebSocket Client ⭐ HIGH PRIORITY

**Goal:** Real-time market data and order updates

**Files to implement:**
- CoinbaseWebSocket implementing ExchangeWebSocket interface
- Real-time ticker feeds for current price
- Order status updates for ExecuteTradingCycleUseCase
- Connection management with auto-reconnect

**Integration points:**
- UpdatePortfolioUseCase needs current BTC price
- ExecuteTradingCycleUseCase needs real-time context
- Order reconciliation via ManageOrdersUseCase

#### Ticket 16: Risk Manager Implementation 🔸 MEDIUM PRIORITY

**Goal:** Concrete risk management implementation

**Files to implement:**
- Complete RiskManager interface implementation
- Position sizing calculations
- Drawdown monitoring with high water mark
- Emergency liquidation triggers

**Integration points:**
- All use cases ready to consume RiskManager interface
- Portfolio drawdown calculation for ExecuteTradingCycleUseCase
- Emergency triggers for HandleEmergencyUseCase

#### Ticket 17: Trading Service 🔸 HIGH PRIORITY

**Goal:** 24/7 background execution

**Files to implement:**
- Android foreground service
- Use case orchestration (ExecuteTradingCycleUseCase every 15 minutes)
- Battery optimization and wake lock management
- Service lifecycle and notification management

**Integration points:**
- Complete use case layer ready for orchestration
- All dependencies injectable via Hilt
- Error handling and recovery mechanisms

### Phase 4: Testing & Validation

#### Ticket 19: Integration Tests

**Goal:** End-to-end testing with small real trades

**Approach:**
- Small position sizes ($10-20 trades)
- Validate complete trading cycle
- API integration testing
- Error handling verification

#### Ticket 20: MVP Milestone

**Goal:** 24-hour live system validation

**Criteria:**
- Service runs continuously for 24 hours
- Makes at least 1 successful trade
- Proper risk management demonstrated
- Emergency procedures tested

---

## Success Metrics

### Phase 3 Success (API Integration Complete)
- ✅ Can place all order types on Coinbase
- ✅ Real-time price updates working
- ✅ Complete trading cycle executes without errors
- ✅ Risk management prevents excessive losses
- ✅ Service survives 8+ hours of background operation

### Phase 4 Success (MVP Ready)
- ✅ 24-hour continuous operation
- ✅ At least 1 profitable trade executed
- ✅ Risk limits enforced (max 2% per trade)
- ✅ Emergency liquidation tested and working
- ✅ Complete audit trail in database

### Long-term Success (6-12 months)
- ✅ Consistent monthly returns (3-5% target)
- ✅ Win rate above 52%
- ✅ Maximum drawdown below 15%
- ✅ Zero manual intervention required
- ✅ Complete tax reporting data

**Timeline Estimate:** 4-6 weeks to Phase 4 complete at current development pace.
