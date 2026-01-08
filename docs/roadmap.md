# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-08  
**Project Status:** Phase 1 Complete - Enhanced Coinbase Integration (v1.5.4)  
**Current Build:** #31 SUCCESS (Version 1.5.4)
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

**Phase 1 COMPLETE - Enhanced Coinbase Integration (v1.5.4):**

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

✅ COMPLETE: Enhanced Coinbase API Integration (v1.5.4)
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

✅ COMPLETE: Enhanced Live Portfolio Data Integration (v1.5.4)
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
- ✅ **ta4j-core 0.16** (for technical indicators - pending decision engine)
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

### 🆕 MAJOR MILESTONE: Enhanced Security & Reliability (v1.5.4)

**Latest stability and security enhancements now live in the app:**

✅ **Enhanced Authentication (v1.5.4):**
- COMPREHENSIVE PEM key parsing with BouncyCastle libraries (bcprov-jdk18on, bcpkix-jdk18on 1.78)
- Support for both raw base64 and PEM formats with automatic format detection
- Comprehensive error handling in JWT generation with multiple fallback mechanisms
- Build-time credential injection with ENHANCED security key processing

✅ **Improved API Integration:**
- CoinbaseApiClient with enhanced Ktor HTTP client and robust error handling
- AccountsResponseDto with complete Coinbase API response structure
- Domain mapping from DTOs to Balance models with better error recovery

✅ **UI & Navigation Refinements:**
- Resolved duplicate TopAppBar display issue for unified interface
- Enhanced dashboard with comprehensive state management
- Portfolio card refined with better balance formatting and "Live Data" indicator
- Professional error handling with graceful user feedback

### What DOESN'T Exist Yet (Phase 2 Ready)

```
❌ NEXT UP - Phase 2: Core Trading Logic

⚠️ Decision Engine (HIGH PRIORITY):
└── Algorithm Implementation (SMA, ADX, ATR + regime switching with hysteresis)

⚠️ Risk Management (HIGH PRIORITY):
├── Position sizing calculator (1-2% risk per trade)
├── Drawdown monitoring (15% emergency stop)
└── Portfolio value tracking

❌ READY FOR Phase 3: Full API Integration

⚠️ Complete REST API Client:
├── Order placement (market, limit, bracket orders)
├── Market data fetching (candles, current prices)
├── Order management (cancel, status updates)
└── Historical data retrieval

⚠️ WebSocket Integration:
├── Real-time price feeds
├── Order status updates
└── Connection management with auto-reconnect

❌ Phase 4: Trading Service (24/7 Operation)

⚠️ Foreground Service:
├── Trading loop execution (every 15 minutes)
├── Battery optimization handling
├── Background processing
└── Notification management

⚠️ Testing & Validation:
├── Integration tests with live API
├── Paper trading validation
├── Small real trade testing
└── MVP milestone completion
```

---

## 📋 Implementation Phases

### ✅ Phase 1: COMPLETE - Enhanced Coinbase Integration (v1.5.4)

**Status:** 100% COMPLETE ✅

**Major Achievements:**
- ✅ Complete UI foundation with dashboard showing real Coinbase data
- ✅ Full Coinbase authentication with comprehensive JWT generation
- ✅ Enhanced PEM key parsing supporting all Coinbase CDP formats
- ✅ Professional error handling and state management
- ✅ Unified navigation with clean interface design
- ✅ Build-time credential injection with comprehensive security

---

### 🎯 Phase 2: Core Trading Logic (NEXT - 0% Complete)

**Goal:** Implement the decision-making brain and risk management

**Priority Order:**
1. **Decision Engine** (HIGH) - The core algorithm that determines DEFENSE/TREND/RANGE modes
2. **Risk Manager** (HIGH) - Position sizing, stop losses, drawdown limits

**Phase 2 Tickets:**
| Ticket | Component | Status | Effort | Description |
|--------|-----------|--------|--------|-------------|
| **05** | **Decision Engine** | ❌ TODO | Large | SMA(200), ADX(14), ATR(14) + regime switching with 3-candle hysteresis |
| **06** | **Risk Manager** | ❌ TODO | Medium | Position sizing (1-2% risk), drawdown monitoring (15% stop), portfolio tracking |

**Success Criteria:**
- ✅ Engine correctly identifies DEFENSE (price < SMA200), TREND (ADX > 25), RANGE (ADX < 25) modes
- ✅ Hysteresis prevents rapid mode switching (requires 3 consecutive candles for confirmation)
- ✅ Risk manager enforces 1-2% risk per trade, 15% portfolio drawdown limit
- ✅ Unit tests validate all regime switching logic with historical data

**Estimated Time:** 2-3 weeks

---

### Phase 3: Full API Integration (0% Complete)

**Goal:** Complete Coinbase REST API and WebSocket integration

**Phase 3 Tickets:**
| Ticket | Component | Status | Effort | Description |
|--------|-----------|--------|--------|-------------|
| **08** | **Full REST API Client** | ❌ TODO | Large | Order placement, candles, market data, order management |
| **09** | **WebSocket Client** | ❌ TODO | Large | Real-time price feeds, order updates, connection management |
| **10** | **Market Data Integration** | ❌ TODO | Medium | Candle fetching, price monitoring, data storage |

**Dependencies:** Requires Phase 2 (Decision Engine + Risk Manager) complete

**Success Criteria:**
- ✅ Can place market, limit, and bracket orders
- ✅ Real-time price updates via WebSocket
- ✅ Historical candle data fetching (350 H4 candles for SMA200)
- ✅ Order status tracking and updates

**Estimated Time:** 3-4 weeks

---

### Phase 4: Trading Service (0% Complete)

**Goal:** 24/7 automated trading execution

**Phase 4 Tickets:**
| Ticket | Component | Status | Effort | Description |
|--------|-----------|--------|--------|-------------|
| **15** | **Trading Service** | ❌ TODO | Large | Foreground service with 15-minute trading loop |
| **16** | **Battery Optimization** | ❌ TODO | Medium | Doze mode survival, wake locks, service persistence |

**Dependencies:** Requires Phase 2 & 3 complete

**Success Criteria:**
- ✅ Service runs 24/7 without interruption
- ✅ Trading decisions executed automatically every 15 minutes
- ✅ Survives device sleep, battery optimization, app switching
- ✅ Emergency stop on 15% drawdown

**Estimated Time:** 2-3 weeks

---

### Phase 5: Testing & MVP (0% Complete)

**Goal:** Validate system works end-to-end

**Phase 5 Components:**
- Integration testing with live Coinbase API
- Paper trading validation (small amounts)
- Real trade testing ($10-50 positions)
- Performance monitoring and optimization
- MVP milestone - first profitable trade

**Success Criteria:**
- ✅ End-to-end trading flow works (decision → order → execution → tracking)
- ✅ No crashes during 24-hour operation test
- ✅ Correct regime detection on historical data
- ✅ Risk limits enforced (no position > 2% risk)

**Estimated Time:** 2-3 weeks

---

## 📊 Current Progress Summary

```
Phase 1 (Coinbase Integration): ████████████████████ 100% ✅ COMPLETE
Phase 2 (Trading Logic):        ░░░░░░░░░░░░░░░░░░░░   0% ← YOU ARE HERE
Phase 3 (Full API):             ░░░░░░░░░░░░░░░░░░░░   0%
Phase 4 (Trading Service):      ░░░░░░░░░░░░░░░░░░░░   0%
Phase 5 (Testing & MVP):        ░░░░░░░░░░░░░░░░░░░░   0%

Overall Progress: 1/5 phases (20%)
```

### Critical Path to MVP

**Next 4 Tickets (Must complete in order):**
1. ❌ **Ticket 05: Decision Engine** ← IMMEDIATE NEXT STEP
2. ❌ **Ticket 06: Risk Manager**
3. ❌ **Ticket 08: Full REST API Client**
4. ❌ **Ticket 15: Trading Service**

**Estimated Time to MVP:** 8-12 weeks

---

## 🎯 Immediate Next Steps

### 1. Ticket 05: Decision Engine (CURRENT FOCUS)

**File:** `core/domain/src/main/kotlin/com/tradeflow/core/domain/strategy/TradingDecisionEngine.kt`

**Requirements:**
- Implement regime switching logic using ta4j indicators
- SMA(200) for trend filter (price above = bullish regime)
- ADX(14) for trend strength (>25 = trending, <25 = ranging)  
- ATR(14) for volatility-based position sizing
- 3-candle hysteresis to prevent whipsawing (except DEFENSE = instant)

**Algorithm:**
```kotlin
fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision {
    val sma200 = calculateSMA(candles, 200)
    val adx14 = calculateADX(candles, 14) 
    val atr14 = calculateATR(candles, 14)
    
    // Rule 1: DEFENSE (instant, no hysteresis)
    if (currentPrice < sma200) {
        resetCounters()
        return Decision.Defense("Price below SMA200")
    }
    
    // Rule 2: TREND (requires 3 candles with ADX > 25)
    if (adx14 > 25.0) {
        trendConfirmCount++
        if (trendConfirmCount >= 3) {
            return Decision.Trend(...)
        }
    }
    
    // Rule 3: RANGE (requires 3 candles with ADX < 25)  
    // ...
}
```

**Testing:** Unit tests with mock candle data to verify all 4 modes work correctly.

### 2. Ticket 06: Risk Manager

**File:** `core/domain/src/main/kotlin/com/tradeflow/core/domain/risk/TradingRiskManager.kt`

**Requirements:**
- Position sizing: 1-2% risk per trade (not position size - risk amount)
- Portfolio tracking with high-water mark
- Emergency stop at 15% drawdown from peak
- Validation of all orders before placement

### 3. Planning Ticket 08: Full REST API

**Goal:** Extend current `CoinbaseRepository` with full order placement capabilities
- Market orders (emergency liquidation)
- Limit orders (grid/range trading with post_only=true)
- Bracket orders (trend trading with stop-loss + take-profit)

---

## ⚠️ Risk Management Notes

### Financial Reality Check
- **This is educational/experimental** - Not financial advice
- **97% of algorithmic traders lose money** - Respect this statistic  
- **Starting capital: $500** - Treat as education cost, not investment
- **Maximum risk: 1-2% per trade** - $5-10 loss maximum per position
- **Monthly target: 3-5%** - Exceptional performance, difficult to achieve
- **Timeline: 5-10 years** - Path to meaningful passive income

### Development Priorities
- **Simplicity over complexity** - Avoid over-engineering
- **Risk management first** - Never trade without proper stops
- **Small position sizes** - Learn with minimal capital at risk
- **Comprehensive logging** - Track every decision for analysis
- **Tax record keeping** - Every trade is a taxable event

### Technical Risk Management
- **15% portfolio drawdown = EMERGENCY STOP** - Hardcoded limit
- **Multiple exchange isolation** - Only `:app` module knows about Coinbase
- **Real-time monitoring** - Dashboard shows current status
- **Manual override** - Always ability to stop service manually

---

## 📚 Documentation References

- **[docs/reference.md](reference.md)** - Complete implementation examples
- **[docs/strategy/bitcoin-first-strategy.md](strategy/bitcoin-first-strategy.md)** - Why BTC-only trading
- **[docs/tickets/refined/](tickets/refined/)** - Detailed ticket requirements  
- **[docs/api/coinbase.md](api/coinbase.md)** - Coinbase API integration guide
- **[docs/ci.md](ci.md)** - GitHub Actions and credential injection

