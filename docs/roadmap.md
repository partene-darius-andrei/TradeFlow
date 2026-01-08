# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-08  
**Project Status:** Phase 1 Complete - Enhanced Coinbase Integration  
**Current Build:** #31 SUCCESS (Version 1.5.1)
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

**Phase 1 COMPLETE - Enhanced Coinbase Integration (v1.5.1):**

```
app/src/main/java/com/dpart/tradeflow/
├── MainActivity.kt              ✅ Simplified (no auth check needed)
├── TradeFlowApp.kt              ✅ Initializes Timber logging + Hilt
├── presentation/dashboard/
│   ├── DashboardScreen.kt       ✅ Complete implementation with ENHANCED real data integration
│   ├── DashboardViewModel.kt    ✅ Full state management + ROBUST error handling + loading states
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
    ├── AppNavHost.kt            ✅ Complete navigation with FIXED TopAppBar ("TradeFlow" title)
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

✅ COMPLETE: Enhanced Coinbase API Integration (v1.5.1)
└── exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt  ✅ ES256 JWT with ENHANCED BouncyCastle PEM parsing + advanced escape handling + comprehensive error recovery
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

✅ COMPLETE: Enhanced Live Portfolio Data Integration (v1.5.1)
├── App now displays real Coinbase account balances with ENHANCED error handling
├── ViewModel with complete state management (loading, error, success states) 
├── IMPROVED error handling with better retry functionality for network failures
├── Loading indicators during API calls with better UX
├── Portfolio card shows BTC/USD balances with "Live Data" indicator + enhanced formatting
├── Navigation FIXED - resolved duplicate TopAppBar issue for cleaner UI
├── STRENGTHENED authentication with advanced PEM key parsing
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
- ✅ **BouncyCastle 1.78** ✅ ACTIVE (for ENHANCED PEM key parsing - bcprov-jdk18on, bcpkix-jdk18on)
- ✅ **ta4j-core 0.16** (for technical indicators - pending decision engine)
- ✅ **security-crypto 1.1.0-alpha06** (replaced by build-time injection)
- ✅ **work-runtime-ktx 2.10.0** (for background tasks)
- ✅ **datastore-preferences 1.1.1** (for settings)
- ✅ **material-icons-extended** ✅ ACTIVE (for ModeIndicator icons)
- ✅ Firebase Analytics + Crashlytics (BOM 34.7.0)

**CI/CD:**
- ✅ GitHub Actions: Enhanced build workflow with ADVANCED credential injection and PEM key escaping
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

### 🆕 MAJOR MILESTONE: Enhanced Security & Reliability (v1.5.1)

**Latest stability and security enhancements now live in the app:**

✅ **Enhanced Authentication (v1.5.1):**
- ADVANCED PEM key parsing with BouncyCastle libraries (bcprov-jdk18on, bcpkix-jdk18on 1.78)
- Enhanced escape sequence handling for environment variable injection
- Comprehensive error handling in JWT generation with multiple fallback mechanisms
- Build-time credential injection with IMPROVED security key processing

✅ **Improved API Integration:**
- CoinbaseApiClient with enhanced Ktor HTTP client and robust error handling
- AccountsResponseDto with complete Coinbase API response structure
- Domain mapping from DTOs to Balance models with better error recovery
- Fixed navigation issues - resolved duplicate TopAppBar for cleaner UI

✅ **Enhanced User Experience:**
- Professional loading states during API calls
- IMPROVED error display with better retry mechanisms  
- Real-time portfolio data with enhanced formatting
- Comprehensive state management in Dashboard ViewModel
- Better debugging capabilities for troubleshooting connection issues

✅ **Strengthened Build Process:**
- ADVANCED build-time credential injection with enhanced PEM key escape handling
- Improved CI/CD pipeline with better credential processing
- Enhanced error recovery during build process
- Better handling of complex private key formats from environment variables
```

### 🚧 What's Missing (Next Priorities)

**Phase 2: Trading Logic (Next Up)**
```
❌ Decision engine (SMA, ADX, ATR indicators + regime switching logic) - Ticket 15
❌ Risk manager (position sizing, drawdown limits, emergency stops) - Ticket 16  
❌ Backtest validation framework - Phase 2A
❌ Strategy performance metrics and validation - Phase 2A
❌ Paper trading capability for strategy testing - Phase 2A
```

**Phase 3: Full Trading Capability**
```
❌ Complete Coinbase REST API client (orders, candles, market data) - Ticket 13 (Phase 2)
❌ Coinbase WebSocket client (real-time price feeds, order updates) - Ticket 14
❌ Order placement and management system
❌ Market data fetching and storage
❌ Real-time price monitoring
```

**Phase 4: Automation & Service**
```
❌ Settings screen for configuration - Ticket 11 (refined, ready)
❌ Trading foreground service (24/7 automated execution) - Tickets 17-18
❌ Battery optimization and doze mode survival
❌ Background job scheduling and monitoring
❌ Service health checks and auto-restart capabilities
```

**Phase 5: Production Readiness**
```
❌ Integration testing with small real trades
❌ Performance monitoring and alerting
❌ Trade logging and tax reporting
❌ Risk monitoring dashboard
❌ Emergency stop and manual override capabilities
```

---

## 📈 Progress Tracking  

### Overall Progress: 11/20 core tickets complete (55%)

```
Phase 0A: ████████████████████ 100% (6/6) ✅ COMPLETE
Phase 1:  ████████████████████ 100% (5/5) ✅ COMPLETE - Enhanced Security & Live Data
Phase 2:  ░░░░░░░░░░░░░░░░░░░░   0% (0/4) ← NEXT UP (Trading Logic)
Phase 3:  ░░░░░░░░░░░░░░░░░░░░   0% (0/3)
Phase 4:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
```

### Current Sprint: Phase 2 - Trading Logic

**Next Up:** Ticket 15 (Decision Engine with SMA, ADX, ATR indicators)

---

## 🎯 Detailed Implementation Roadmap

### ✅ Phase 0A: Foundation (COMPLETE - 6/6 tickets)

**Goal:** Core domain models, data persistence, secure credential management

| Ticket | Title | Status | Implementation |
|--------|-------|--------|----------------|
| **01** | Domain Models | ✅ Done | Candle, Order, Decision, Portfolio, Balance, Ticker |
| **02** | Repository Interfaces | ✅ Done | ExchangeRepository, BracketOrderRepository, AuthTokenProvider |
| **03** | Room Database | ✅ Done | 4 entities (Candle, Order, Decision, Portfolio) + 4 DAOs |
| **04** | Secure Credential Storage | ✅ Done | StaticCredentialStore with build-time injection |
| **05** | Core UI Components | ✅ Done | StatusCard, PriceDisplay, LoadingButton, ErrorDisplay, ModeIndicator |
| **06** | App Navigation | ✅ Done | NavHost with Dashboard + Settings (fixed TopAppBar issue) |

### ✅ Phase 1: Enhanced Coinbase Integration (COMPLETE - 5/5 tickets)

**Goal:** Live connection to Coinbase API with robust authentication and error handling

| Ticket | Title | Status | Enhancement (v1.5.1) |
|--------|-------|--------|-----------------------|
| **07** | JWT Generator | ✅ Enhanced | ES256 JWT with ADVANCED BouncyCastle PEM parsing + comprehensive error recovery |
| **08** | REST API Client (Basic) | ✅ Enhanced | Account balances with ROBUST error handling + improved stability |
| **09** | Dashboard UI | ✅ Enhanced | Real portfolio data with IMPROVED loading states + better error recovery |
| **10** | Dashboard ViewModel | ✅ Enhanced | Complete state management + COMPREHENSIVE error handling |
| **11** | Build System | ✅ Enhanced | ADVANCED credential injection with enhanced PEM key escape handling |

**Key v1.5.1 Improvements:**
- Enhanced security key parsing for more robust authentication
- Improved error handling and stability when connecting to Coinbase API  
- Fixed navigation display issues for cleaner user interface
- Strengthened JWT token generation reliability
- Better debugging capabilities for troubleshooting connection issues

---

### 🚧 Phase 2: Trading Logic (NEXT - 0/4 tickets)

**Goal:** Implement decision engine and risk management for automated trading

| Ticket | Title | Priority | Effort | Blocked By | Description |
|--------|-------|----------|--------|------------|-------------|
| **15** | Decision Engine | HIGH | Large | None | SMA, ADX, ATR indicators + regime switching (DEFENSE/TREND/RANGE) |
| **16** | Risk Manager | HIGH | Medium | 15 | Position sizing, drawdown limits, emergency stops |
| **17** | Backtesting Framework | MEDIUM | Large | 15, 16 | Historical data testing with performance metrics |
| **18** | Strategy Validation | HIGH | Medium | 17 | Validate 52%+ win rate, 1.0+ Sharpe ratio before live trading |

**Estimated Time:** 4-6 weeks
**Success Criteria:** Profitable backtest results over 6+ months of historical data

---

### 🔮 Phase 3: Full Trading Capability (0/3 tickets)

**Goal:** Complete automated trading with real-time data and order management

| Ticket | Title | Priority | Dependencies |
|--------|-------|----------|-------------|
| **13** | Full REST API Client | HIGH | 15, 16 (strategy logic ready) |
| **14** | WebSocket Client | HIGH | 13 |
| **19** | Order Management System | HIGH | 13, 14 |

---

### 🏃 Phase 4: Automation Service (0/2 tickets)

**Goal:** 24/7 autonomous trading service

| Ticket | Title | Priority | Dependencies |
|--------|-------|----------|-------------|
| **17** | Trading Foreground Service | HIGH | Phase 3 complete |
| **18** | Battery Optimization | MEDIUM | 17 |

---

### 🚀 Phase 5: Production (0/2 tickets)

**Goal:** Live trading readiness with safety measures

| Ticket | Title | Priority | Dependencies |
|--------|-------|----------|-------------|
| **19** | Integration Tests | HIGH | Phase 4 complete |
| **20** | MVP Milestone | HIGH | All phases complete |

---

## 🎯 Next Immediate Actions (Post v1.5.1)

### 1. Ticket 15: Decision Engine Implementation

**Objective:** Implement regime-switching trading strategy with technical indicators

**Key Components:**
```kotlin
// Core interfaces
interface DecisionEngine {
    fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision
}

// Implementation with ta4j
class TradingDecisionEngine {
    private fun calculateSMA(candles: List<Candle>, period: Int): BigDecimal
    private fun calculateADX(candles: List<Candle>, period: Int): Double  
    private fun calculateATR(candles: List<Candle>, period: Int): BigDecimal
}

// Decision logic
sealed class Decision {
    data class Defense(val reason: String) : Decision()           // Price < SMA(200)
    data class Trend(val stopLoss: BigDecimal, val takeProfit: BigDecimal) : Decision()  // ADX > 25
    data class Range(val gridSpacing: BigDecimal) : Decision()   // ADX < 25
    data class Wait(val reason: String) : Decision()             // Transitioning
}
```

**Files to Create:**
- `core/domain/src/main/kotlin/com/tradeflow/core/domain/strategy/DecisionEngine.kt`
- `core/domain/src/main/kotlin/com/tradeflow/core/domain/strategy/TradingDecisionEngine.kt`
- Unit tests with mock candle data

### 2. Ticket 16: Risk Manager Implementation  

**Objective:** Position sizing and risk limits to prevent account blowup

**Key Components:**
```kotlin
interface RiskManager {
    fun calculatePositionSize(portfolioValue: BigDecimal, riskPercent: BigDecimal): BigDecimal
    fun validateOrder(order: Order, portfolio: Portfolio): ValidationResult
    fun checkDrawdown(currentEquity: BigDecimal, highWaterMark: BigDecimal): DrawdownStatus
}

data class RiskLimits(
    val maxPositionPercent: BigDecimal = "0.02".toBigDecimal(),  // 2% per trade
    val maxDrawdownPercent: BigDecimal = "0.15".toBigDecimal(),  // 15% emergency stop
    val maxDailyLossPercent: BigDecimal = "0.05".toBigDecimal()  // 5% daily limit
)
```

### 3. Backtesting & Validation (Phase 2A)

**Objective:** Prove strategy profitability before risking real money

**Success Criteria:**
- Win rate: 52%+ over 6 months of historical data
- Sharpe ratio: 1.0+ (risk-adjusted returns)
- Maximum drawdown: <20% during backtest period
- Monthly returns: 3-5% average with reasonable volatility

---

## 🏆 Success Metrics & Milestones

### Technical Milestones

| Milestone | Criteria | Status |
|-----------|----------|---------|
| **Authentication** | JWT tokens work with live Coinbase API | ✅ COMPLETE (Enhanced v1.5.1) |
| **Data Integration** | Real portfolio balances displayed | ✅ COMPLETE (Enhanced v1.5.1) |
| **UI Foundation** | Professional interface with error handling | ✅ COMPLETE (Enhanced v1.5.1) |
| **Decision Engine** | Strategy logic with technical indicators | ❌ Next Up |
| **Risk Management** | Position sizing and stop-loss logic | ❌ Pending |
| **Strategy Validation** | Profitable backtesting results | ❌ Pending |
| **Live Trading** | First successful automated trade | ❌ Future |

### Business Milestones

| Milestone | Criteria | Timeline |
|-----------|----------|----------|
| **Break Even** | Monthly P&L > $0 consistently | Month 6-12 |
| **Profitable** | Monthly returns 3-5% sustained | Month 12-18 |
| **Scalable** | Account growth to $2,500+ | Year 2-3 |
| **Passive Income** | $500-1,000/month from trading | Year 5-10 |

---

## 🎯 Development Philosophy & Constraints

### Core Principles

1. **Security First** - Never compromise on credential handling or risk management
2. **Simple Over Complex** - Avoid over-engineering, focus on proven strategies  
3. **Conservative Trading** - Small positions, strict risk management
4. **Thorough Testing** - Extensive validation before live deployment
5. **Maintainable Code** - Clean architecture, good documentation

### Critical Success Factors

| Factor | Status | Notes |
|--------|---------|-------|
| **Robust Authentication** | ✅ ENHANCED | Advanced PEM parsing, comprehensive error handling |
| **Clean Architecture** | ✅ COMPLETE | Repository pattern, dependency injection, testability |
| **Error Handling** | ✅ ENHANCED | Professional UX, retry mechanisms, graceful failures |
| **Strategy Validation** | ❌ CRITICAL | Need backtesting before live trading |
| **Risk Management** | ❌ CRITICAL | Position sizing and stop-loss before real money |

### Known Constraints & Risks

**Technical Risks:**
- API rate limits and connection stability  
- Android doze mode and battery optimization
- Market volatility exceeding risk parameters
- Strategy alpha decay over time

**Business Risks:**  
- 97% of day traders lose money (statistical reality)
- Tax implications of frequent trading
- Regulatory changes affecting crypto trading
- Platform risk (Coinbase API changes)

---

The app has reached a significant milestone with live Coinbase integration and enhanced security (v1.5.1). The foundation is solid and ready for the next phase: implementing the trading decision engine and risk management systems that will enable actual automated trading capability.

**Priority Focus:** Complete Phase 2 (Trading Logic) before attempting Phase 3 (Full API Integration). The decision engine and risk management are the core intellectual property of the trading system and must be implemented and validated thoroughly before moving to execution.
