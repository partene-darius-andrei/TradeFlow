# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-08
**Project Status:** Phase 1 Complete - Live Portfolio Integration
**Current Build:** #30 (SUCCESS)
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

### ✅ What Exists (Jan 2026)

**Codebase:**
```
app/src/main/java/com/dpart/tradeflow/
├── MainActivity.kt              ✅ Simplified (no auth check)
├── TradeFlowApp.kt              ✅ Initializes Timber logging + Hilt
├── presentation/dashboard/
│   ├── DashboardScreen.kt       ✅ Complete implementation with real data integration
│   ├── DashboardViewModel.kt    ✅ Full state management + error handling + live data
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
    ├── AppNavHost.kt            ✅ Complete navigation with TopAppBar ("TradeFlow" title)
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
    └── repository/
        ├── BracketOrderRepository.kt ✅ Bracket order support
        ├── ExchangeRepository.kt     ✅ Core operations (12 methods)
        └── ExchangeWebSocket.kt      ✅ Real-time streams

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

✅ COMPLETE: Coinbase API Integration
└── exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt  ✅ ES256 JWT with BouncyCastle PEM parsing + escape handling
    ├── api/
    │   └── CoinbaseApiClient.kt     ✅ Complete Ktor-based API client (accounts)
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

✅ COMPLETE: Live Portfolio Data Integration
├── App now displays real Coinbase account balances with proper error handling
├── ViewModel with complete state management (loading, error, success states)
├── Error handling with retry functionality for network failures
├── Loading indicators during API calls
├── Portfolio card shows BTC/USD balances with "Live Data" indicator
├── Navigation restructured with TopAppBar moved to AppNavHost (fixes duplicate navbar)
└── Professional UX flow with proper state management
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
- ✅ **BouncyCastle 1.78** ✅ ACTIVE (for PEM key parsing - bcprov-jdk18on, bcpkix-jdk18on)
- ✅ **ta4j-core 0.16** (for technical indicators - pending decision engine)
- ✅ **security-crypto 1.1.0-alpha06** (replaced by build-time injection)
- ✅ **work-runtime-ktx 2.10.0** (for background tasks)
- ✅ **datastore-preferences 1.1.1** (for settings)
- ✅ **material-icons-extended** ✅ ACTIVE (for ModeIndicator icons)
- ✅ Firebase Analytics + Crashlytics (BOM 34.7.0)

**CI/CD:**
- ✅ GitHub Actions: Enhanced build workflow with credential injection and PEM key escaping
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

### 🆕 MAJOR MILESTONE: Complete Live Portfolio Integration

**Live Coinbase account balances now display in the app with professional error handling!**

**What was accomplished:**

✅ **Enhanced Authentication (v1.5.0):**
- Advanced PEM key parsing with BouncyCastle libraries
- Proper escape sequence handling for environment variable injection
- Comprehensive error handling in JWT generation
- Build-time credential injection with enhanced security

✅ **Complete API Integration:**
- CoinbaseApiClient with Ktor HTTP client 
- AccountsResponseDto with full Coinbase API response structure
- Domain mapping from DTOs to Balance models
- Dependency injection binding ExchangeRepository → CoinbaseRepository

✅ **Professional UI/UX:**
- DashboardViewModel with complete state management (loading, error, success)
- Error states with retry functionality
- Loading indicators during API calls
- PortfolioCard updated to show real BTC/USD balances
- "Live Data" indicator to show real-time information
- Navigation structure improved (TopAppBar moved to AppNavHost)

✅ **Architecture Validation:**
- Clean Architecture working end-to-end (Domain ← Data ← External)
- Repository pattern enables easy exchange swapping
- Dependency injection working properly with Hilt
- State management with StateFlow and Compose integration

**Technical Improvements:**
- Fixed duplicate navigation bar issue (TopAppBar centralized in AppNavHost)
- Enhanced PEM key parsing for CI/CD environment variable injection  
- Comprehensive error handling for network failures
- Professional loading states throughout the app
- Version bump to 1.5.0 reflecting major functionality additions

**This milestone proves the entire authentication and data flow architecture works with real Coinbase data.**

### 🎯 What's NEXT (Current Priorities)

**Ready to start immediately (no blockers):**

```
❌ Phase 1B: Strategy Implementation (NEXT PHASE)
├── Ticket 15: Decision Engine (SMA, ADX, ATR + regime switching logic)
├── Ticket 16: Risk Manager (position sizing, stop-loss, drawdown monitoring)
└── Backtesting Framework (historical validation)

❌ Phase 2: Trading Infrastructure  
├── Ticket 14: WebSocket Client (real-time price streams)
├── Full REST API (orders, market data, candles - extending current implementation)
├── Ticket 11: Settings Screen (credential management, trading parameters)
└── Order placement + management

❌ Phase 3: Live Trading Service
├── Ticket 17: Trading Service (foreground service, 24/7 loops)
├── Ticket 18: Battery optimization (Doze exemption, wake locks)
└── Integration testing with small real trades

❌ Phase 4: Production Readiness
├── Comprehensive backtesting with historical data
├── Paper trading validation
├── Live testing with $50-100 capital
└── Performance monitoring and optimization
```

**Next Immediate Focus: Decision Engine (Ticket 15)**

This is now the critical path blocker. We have:
- ✅ Live data from Coinbase (prices, balances)
- ✅ UI to display trading mode
- ✅ Database to store decisions

We need:
- ❌ Logic to analyze data and make decisions (DEFENSE/TREND/RANGE)
- ❌ Integration with ta4j for technical indicators (SMA, ADX, ATR)
- ❌ Regime switching with hysteresis (prevent whipsawing)

---

## Phase Breakdown

### Phase 0: Foundation (✅ COMPLETE)

**Goal:** Core infrastructure and live data integration

| Ticket | Component | Status |
|--------|-----------|--------|
| 00 | Project modularization | ✅ Complete |
| 01 | Domain models | ✅ Complete |  
| 02 | Repository interfaces | ✅ Complete |
| 03 | Room database | ✅ Complete |
| 04 | Credential storage | ✅ Complete (build-time injection) |
| 07 | JWT generator | ✅ Complete |
| - | UI components | ✅ Complete |
| 10A | Dashboard + ViewModel with live data | ✅ Complete |

**Outcome:** ✅ App displays real Coinbase portfolio data with proper error handling

---

### Phase 1A: Strategy Logic (🎯 CURRENT FOCUS)

**Goal:** Implement decision-making brain of the trading system

| Ticket | Component | Priority | Estimated Days |
|--------|-----------|----------|----------------|
| 15 | **Decision Engine** | CRITICAL | 3-4 days |
| 16 | Risk Manager | HIGH | 2-3 days |
| - | Backtesting Framework | HIGH | 2-3 days |

**Decision Engine Requirements:**
```kotlin
// What we need to implement
interface DecisionEngine {
    fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision
}

sealed class Decision {
    data class Wait(val reason: String) : Decision()
    data class Defense(val reason: String) : Decision()  // Price < SMA(200) 
    data class Trend(val direction: OrderSide, val stopLoss: BigDecimal, val takeProfit: BigDecimal) : Decision()
    data class Range(val gridSpacing: BigDecimal, val levels: Int) : Decision()
}

// Implementation with ta4j
class EngineDecisionEngine {
    fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        val sma200 = calculateSMA(candles, 200)
        val adx14 = calculateADX(candles, 14) 
        val atr14 = calculateATR(candles, 14)
        
        // Decision tree logic:
        // 1. Price < SMA(200) → DEFENSE (instant)
        // 2. Price > SMA(200) AND ADX > 25 for 3 candles → TREND  
        // 3. Price > SMA(200) AND ADX < 25 for 3 candles → RANGE
        // 4. Otherwise → WAIT
    }
}
```

**Risk Manager Requirements:**
```kotlin
interface RiskManager {
    fun validateOrder(order: OrderRequest, portfolio: Portfolio): OrderValidation
    fun calculatePositionSize(portfolioValue: BigDecimal, riskPercent: BigDecimal): BigDecimal
    fun checkDrawdown(currentEquity: BigDecimal, highWaterMark: BigDecimal): DrawdownStatus
}

// Risk limits (hardcoded for safety)
object RiskLimits {
    const val MAX_POSITION_PERCENT = 0.10        // 10% of portfolio per trade
    const val MAX_RISK_PER_TRADE = 0.02          // 2% risk per trade
    const val EMERGENCY_DRAWDOWN_LIMIT = 0.15    // 15% drawdown stops everything
}
```

**Deliverable:** Dashboard shows current mode (DEFENSE/TREND/RANGE) based on real market analysis

---

### Phase 1B: Strategy Validation (After Phase 1A)

**Goal:** Prove strategy works on historical data before risking real money

| Component | Description | Success Criteria |
|-----------|-------------|------------------|
| Historical data fetching | Get 2+ years of BTC H4 candles | 4,000+ candles stored |
| Backtest framework | Run strategy on historical data | Win rate, Sharpe ratio, max drawdown |
| Performance analysis | Statistical validation | 52%+ win rate, 1.0+ Sharpe, <20% max drawdown |

**Key Questions to Answer:**
- Does the strategy actually work on historical data?
- What's the realistic win rate and risk-adjusted return?
- How often does it trade? (too much = fees kill profits)
- What's the maximum historical drawdown?

**Go/No-Go Decision Point:** If backtest shows <50% win rate or excessive drawdown, stop here and redesign strategy.

---

### Phase 2: Trading Infrastructure (After validation)

**Goal:** Complete Coinbase integration for live trading

| Ticket | Component | Dependencies |
|--------|-----------|--------------|
| 14 | WebSocket client | Phase 1A complete |
| - | Full REST API (candles, orders) | Ticket 14 |
| 11 | Settings screen | UI foundation |
| - | Order placement logic | All above |

**Deliverable:** Can place and manage real orders (paper trading level)

---

### Phase 3: Live Trading Service (Final phase)

**Goal:** 24/7 autonomous operation

| Ticket | Component | Risk Level |
|--------|-----------|------------|
| 17 | Trading Service (foreground) | High - handles real money |
| 18 | Battery optimization | Medium - reliability |
| - | Performance monitoring | Medium - observability |

**Deliverable:** System trades autonomously with $50-100 test capital

---

## 🎯 Success Criteria by Phase

### Phase 1A Success (Current Goal)
- [ ] Decision engine correctly identifies DEFENSE when BTC < SMA(200)  
- [ ] Decision engine correctly identifies TREND when ADX > 25 for 3+ H4 candles
- [ ] Decision engine correctly identifies RANGE when ADX < 25 for 3+ H4 candles
- [ ] Risk manager enforces 10% position limit and 2% risk per trade
- [ ] Dashboard displays current mode based on real-time analysis
- [ ] Unit tests cover all decision paths with >90% coverage

### Phase 1B Success (Validation Gate)
- [ ] Backtest shows 52%+ win rate over 2+ years historical data
- [ ] Sharpe ratio > 1.0 (risk-adjusted returns)
- [ ] Maximum historical drawdown < 20%
- [ ] Strategy trades 1-4 times per week (not overtrading)
- [ ] Positive expectancy after 0.25% trading fees

### Phase 2 Success (Trading Ready)
- [ ] Can place and cancel orders on Coinbase successfully
- [ ] WebSocket provides real-time price updates with <1s latency
- [ ] Order status updates tracked in real-time
- [ ] Settings screen allows parameter adjustments
- [ ] Paper trading shows positive results over 30 days

### Phase 3 Success (Production)
- [ ] Service runs for 48+ hours without crashes
- [ ] Survives device sleep/doze mode
- [ ] Places orders according to decision engine
- [ ] Respects risk limits in live trading
- [ ] Emergency stop triggers at 15% drawdown

---

## 🚨 Risk Management Throughout

### Code Quality Gates
- **No commit without tests** for decision engine logic
- **Code review required** for order placement logic  
- **Staging environment** required for live trading features
- **Rollback plan** for every production deployment

### Financial Risk Controls
- **Hardcoded limits** in code (no user configuration for critical limits)
- **Circuit breakers** - stop trading after consecutive losses
- **Position sizing** - never risk >2% per trade, >10% per position
- **Drawdown stops** - emergency liquidation at 15% loss

### Development Risk Controls  
- **Small iteration cycles** - test each component thoroughly
- **Progressive deployment** - $50 → $100 → $250 → $500 test amounts
- **Kill switches** - multiple ways to stop trading immediately
- **Comprehensive logging** - audit trail for every decision and trade

---

## 📈 Long-term Vision (Years 2-5)

### Year 2: Strategy Refinement
- Add multiple timeframe analysis (H1 + H4 confirmation)
- Implement volatility-based position sizing
- Add mean reversion component for ranging markets
- Optimize parameters based on live trading data

### Year 3: Multi-Exchange Support  
- Add Kraken integration (via existing ExchangeRepository interface)
- Cross-exchange arbitrage opportunities
- Portfolio allocation across exchanges
- Advanced risk management (correlation, VaR)

### Year 4: Advanced Features
- Options/futures integration for hedging
- DeFi yield farming for idle cash
- Tax optimization (wash sale rules, FIFO/LIFO)
- Performance analytics dashboard

### Year 5: Scaling & Automation
- Multi-asset support (ETH, SOL, major altcoins)  
- Machine learning for regime detection
- Cloud deployment for 24/7 reliability
- Multiple strategy portfolio (trend + mean reversion + arbitrage)

**Reality Check:** Most traders never make it past Year 1. Focus on building something that works consistently with BTC before expanding.

---

## 📚 Key Resources & References

### Technical Documentation
- [docs/reference.md](reference.md) - Complete implementation blueprint
- [docs/api/coinbase.md](api/coinbase.md) - Coinbase API integration guide  
- [docs/strategy/overview.md](strategy/overview.md) - Trading strategy specification
- [docs/ci.md](ci.md) - CI/CD pipeline documentation

### Market Analysis  
- [docs/strategy/bitcoin-first-strategy.md](strategy/bitcoin-first-strategy.md) - Why BTC-only initially
- [docs/case-studies/polymarket-bot-analysis.md](case-studies/polymarket-bot-analysis.md) - Lessons from successful trading bot

### Implementation Examples
- [docs/implementation/](implementation/) - Complete code examples organized by layer
- [docs/tickets/](tickets/) - Detailed requirements for each component

---

## 🎯 Current Action Items

### Immediate (This Week)
1. **Implement Decision Engine (Ticket 15)**
   - Create `EngineDecisionEngine.kt` class
   - Integrate ta4j for SMA(200), ADX(14), ATR(14) calculations
   - Implement decision tree logic with hysteresis
   - Add unit tests for all decision paths
   - Integrate with Dashboard to show current mode

2. **Implement Risk Manager (Ticket 16)**  
   - Create `TradingRiskManager.kt` class
   - Implement position sizing calculator
   - Add drawdown monitoring logic
   - Create risk validation for order requests
   - Add comprehensive unit tests

### Next Week
3. **Strategy Validation**
   - Fetch historical BTC H4 data (2+ years)
   - Build backtesting framework
   - Run strategy against historical data
   - Analyze win rate, Sharpe ratio, maximum drawdown
   - Make go/no-go decision on strategy viability

### Following Weeks (If validation successful)
4. **Complete Coinbase Integration**
   - Extend REST API for order placement and market data
   - Implement WebSocket client for real-time data
   - Build order management system
   - Add comprehensive error handling and reconnection logic

---

## 🔄 Review & Update Schedule

- **Weekly:** Update progress on current phase tickets  
- **Monthly:** Review overall roadmap and timelines
- **Quarterly:** Analyze live trading performance and strategy adjustments
- **Annually:** Strategic review of goals, risk tolerance, and technology stack

**Last Review:** 2026-01-08 - Updated to reflect Phase 1 completion and live portfolio integration
**Next Review:** 2026-01-15 - After Decision Engine implementation
