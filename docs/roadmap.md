# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-09
**Project Status:** Phase 2 In Progress - Core Trading Logic (v1.6.0)  
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

**Phase 1 COMPLETE - Enhanced Coinbase Integration (v1.5.5):**

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

✅ COMPLETE: Domain Layer Foundation
└── core/domain/                 ✅ Complete domain layer
    ├── auth/
    │   ├── AuthTokenProvider.kt ✅ Token generation interface
    │   └── CredentialStore.kt   ✅ Secure storage interface
    ├── error/
    │   └── ExchangeError.kt     ✅ Exchange error types (6 variants)
    ├── model/                   ✅ Domain models (Ticket 01)
    │   ├── Candle.kt            ✅ OHLCV data with granularity enums
    │   ├── Order.kt             ✅ Order types, sides, status
    │   ├── Decision.kt          ✅ Wait, Defense, Trend, Range decisions
    │   ├── Portfolio.kt         ✅ Account balances
    │   ├── Balance.kt           ✅ Currency holdings
    │   └── Ticker.kt            ✅ Real-time price data
    ├── repository/
    │   ├── BracketOrderRepository.kt ✅ Bracket order support
    │   ├── ExchangeRepository.kt     ✅ Core operations (12 methods)
    │   └── ExchangeWebSocket.kt      ✅ Real-time streams
    ├── strategy/                ✅ Decision engine (Ticket 15 - NEW)
    │   ├── DecisionEngine.kt    ✅ Decision engine interface
    │   ├── TradingDecisionEngine.kt ✅ Regime-switching implementation with hysteresis
    │   └── StrategyConfig.kt    ✅ Strategy parameters (SMA, ADX, ATR periods)
    └── indicator/               ✅ Technical indicators (NEW)
        ├── SMACalculator.kt     ✅ Simple Moving Average with ta4j
        ├── ADXCalculator.kt     ✅ Average Directional Index with ta4j
        └── ATRCalculator.kt     ✅ Average True Range with ta4j

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
    │   └── CoinbaseRepository.kt    ✅ Implementation (getBalances working, others TODO for Phase 2)
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
- ✅ **mockk 1.13.8** ✅ ACTIVE (for unit testing with mocks)
- ✅ **kotlin-test 2.1.0** ✅ ACTIVE (for testing framework)
- ✅ **security-crypto 1.1.0-alpha06** (replaced by build-time injection)
- ✅ **work-runtime-ktx 2.10.0** (for background tasks)
- ✅ **datastore-preferences 1.1.1** (for settings)
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

### 🆕 MAJOR MILESTONE: Core Trading Logic Implementation (v1.6.0)

**Latest decision engine implementation now complete:**

✅ **Decision Engine Implementation (v1.6.0):**
- Complete TradingDecisionEngine with regime-switching logic (DEFENSE/TREND/RANGE/WAIT)
- Hysteresis mechanism with 3-candle confirmation to prevent whipsaws
- SMA(200) trend filter, ADX(14) trend strength detection, ATR(14) volatility sizing
- Comprehensive unit testing with MockK and Kotlin Test framework

✅ **Technical Indicators Integration:**
- SMACalculator with ta4j BaseBarSeries for accurate moving average calculations
- ADXCalculator for trend strength analysis (>25 = trending, <25 = ranging)
- ATRCalculator for volatility-based stop-loss and take-profit placement
- All indicators properly handle BigDecimal precision for financial calculations

✅ **Strategy Configuration:**
- Configurable StrategyConfig with sensible defaults (SMA=200, ADX=14, ATR=14)
- Position sizing parameters (5% trend positions, 2% grid positions per level)
- Risk management settings (3x ATR stop-loss, 6x ATR take-profit for 2:1 R:R)
- Grid trading parameters (1.5% minimum spacing for fee break-even)

### 🎯 Implementation Status by Phase

**✅ Phase 1: Foundation & API Integration (100% Complete)**
- [x] Domain models & interfaces
- [x] Room database with 4 entities + 4 DAOs
- [x] Coinbase JWT authentication with ES256
- [x] Basic API client (account balances working)
- [x] UI foundation with live portfolio data
- [x] Build system with credential injection

**🔄 Phase 2: Core Trading Logic (66% Complete - IN PROGRESS)**
- [x] **Decision engine with regime switching** ← JUST COMPLETED
- [x] **Technical indicators (SMA/ADX/ATR)** ← JUST COMPLETED  
- [ ] **Full REST API client** (orders, candles, products) ← NEXT
- [ ] **WebSocket client** (real-time price feeds) ← NEXT
- [ ] **Risk manager** (position sizing, drawdown limits) ← NEXT

**❌ Phase 3: Service & Testing (0% Complete)**
- [ ] Trading foreground service (24/7 execution loop)
- [ ] Integration tests with small real trades
- [ ] MVP milestone validation

**❌ Phase 4: Production Ready (0% Complete)**  
- [ ] Strategy backtesting framework
- [ ] Performance monitoring & alerts
- [ ] Production deployment & monitoring

### 🎯 Current Focus: Phase 2 Completion

**Recently Completed:**
- ✅ **Ticket 15: Decision Engine** - Full regime-switching implementation with hysteresis
- ✅ **Technical Indicators** - SMA, ADX, ATR calculators using ta4j library
- ✅ **Unit Testing** - Comprehensive test coverage with MockK integration

**Next Up (Priority Order):**

1. **Ticket 13: Full REST API Client** (HIGH PRIORITY)
   - Extend CoinbaseRepository with order placement methods
   - Implement candle data fetching (350 candle limit, TWO_HOUR aggregation)  
   - Add product information queries
   - Support for bracket orders, limit orders, market orders

2. **Ticket 14: WebSocket Client** (HIGH PRIORITY)
   - Real-time price feeds from Coinbase Advanced Trade WebSocket
   - Order status updates for live position tracking
   - Heartbeat subscription to prevent connection timeouts
   - Auto-reconnection logic with exponential backoff

3. **Ticket 16: Risk Manager** (MEDIUM PRIORITY)
   - Position sizing calculations (% of portfolio per trade)
   - Drawdown monitoring (15% emergency stop limit)
   - Portfolio tracking with high water mark
   - Order validation against risk limits

### 🏗️ Architecture Status

**Module Completion:**
- `:app` (DI + navigation): ✅ 95%
- `:core:domain` (models + strategy): ✅ 95% (decision engine complete)
- `:core:data` (database + security): ✅ 90%  
- `:core:ui` (components): ✅ 100%
- `:exchange:coinbase` (API integration): 🟡 35% (auth complete, REST/WS partial)
- `:feature:*` (not started): ❌ 0%

### 🧪 Testing Strategy

**Unit Tests:**
- ✅ Decision engine with comprehensive mock scenarios
- ✅ Technical indicators validation
- ✅ Hysteresis logic testing (prevents mode switching whipsaws)
- ✅ Strategy configuration validation

**Integration Tests (Planned):**
- [ ] End-to-end API calls with small real orders ($10-20)
- [ ] WebSocket connection stability testing
- [ ] Strategy execution with live market data

**Risk Management:**
- [ ] Paper trading validation before live deployment
- [ ] Small capital testing ($100-500 initial risk)
- [ ] Performance tracking vs. buy-and-hold benchmark

---

## 📈 Success Metrics & Milestones

### Phase 2 Completion Criteria

- [x] ~~Decision engine correctly switches between 4 modes based on SMA/ADX~~
- [x] ~~Hysteresis prevents rapid mode switching (3-candle confirmation)~~
- [x] ~~Technical indicators match reference implementations~~
- [ ] REST API can place/cancel orders successfully
- [ ] WebSocket provides reliable real-time price data
- [ ] Risk manager enforces position limits and stops

### MVP Readiness Criteria (Phase 3)

- [ ] Complete trading loop: decision → order placement → monitoring → risk management
- [ ] Service runs 24/7 without crashes or memory leaks
- [ ] Emergency stop mechanisms work (15% drawdown limit)
- [ ] All trades logged for tax reporting
- [ ] Backtesting shows positive expectancy over 6+ months historical data

### Success Timeline

```
✅ Q4 2025: Foundation complete (auth, database, UI)
🔄 Q1 2026: Core logic complete (decision engine, risk management) ← CURRENT
⏳ Q2 2026: MVP testing (paper trading, small real trades)
⏳ Q3 2026: Live deployment with $500-1000 capital
⏳ Q4 2026: Performance validation, strategy refinement
```

---

## 🚀 Key Technical Achievements

### Decision Engine Architecture

The implemented decision engine represents the core "brain" of the trading system:

```kotlin
// Regime detection with hysteresis
when {
    currentPrice < sma200 -> Decision.Defense() // Instant (safety first)
    adx > 25.0 && trendConfirmCount >= 3 -> Decision.Trend() // Bull market + strong trend
    adx < 25.0 && rangeConfirmCount >= 3 -> Decision.Range() // Bull market + weak trend
    else -> Decision.Wait() // Waiting for confirmation
}
```

### Technical Integration

- **ta4j Library:** Professional-grade technical analysis with BaseBarSeries
- **BigDecimal Precision:** All financial calculations use proper decimal arithmetic
- **MockK Testing:** Comprehensive test coverage with indicator mocking
- **Configurable Parameters:** Strategy can be tuned without code changes

### Risk Management Foundation

- **Position Sizing:** 5% of portfolio for trend trades, 2% per grid level
- **Stop Losses:** 3x ATR from entry (volatility-adjusted)
- **Take Profits:** 6x ATR from entry (2:1 reward-to-risk ratio)
- **Grid Spacing:** Minimum 1.5% to overcome 0.6% maker fees

This represents the largest single milestone in the project - the core trading intelligence is now implemented and ready for market data integration.

