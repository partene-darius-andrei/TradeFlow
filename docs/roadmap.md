# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-08
**Project Status:** Phase 1 Complete - Coinbase Integration
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
│   ├── DashboardScreen.kt       ✅ Main screen with real data integration
│   ├── DashboardViewModel.kt    ✅ State management + API integration
│   └── components/              ✅ PortfolioCard, ModeCard, ServiceCard, OrdersList
├── di/
│   ├── AppModule.kt             ✅ Empty Hilt module
│   ├── DatabaseModule.kt        ✅ Provides Room database
│   ├── NetworkModule.kt         ✅ Provides Ktor HttpClient (OkHttp engine)
│   └── CredentialsModule.kt     ✅ Provides build-injected credentials
├── data/local/
│   ├── AppDatabase.kt           ✅ Empty Room DB
│   └── PlaceholderEntity.kt     ✅ Dummy entity
└── navigation/
    ├── AppNavHost.kt            ✅ Complete navigation with TopAppBar
    └── Screen.kt                ✅ Dashboard + Settings routes only

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
    │   └── CoinbaseJwtGenerator.kt  ✅ ES256 JWT with BouncyCastle PEM parsing
    ├── api/
    │   └── CoinbaseApiClient.kt     ✅ Complete Ktor-based API client (accounts)
    ├── dto/
    │   └── AccountDto.kt            ✅ Account DTOs for API responses  
    ├── mapper/
    │   └── AccountMapper.kt         ✅ DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt    ✅ Partial implementation (getBalances working)
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

✅ COMPLETE: Live Data Dashboard
├── App now displays real Coinbase account balances
├── ViewModel with proper state management
├── Error handling with retry functionality
├── Loading states during API calls
├── Portfolio card shows BTC/USD balances with "Live Data" indicator
└── Navigation with TopAppBar ("TradeFlow" title)

✅ COMPLETE: Credential System
├── Build-time injection          ✅ Environment vars → BuildConfig → DI
├── Local development support     ✅ local.properties fallback
├── CI/CD integration            ✅ GitHub secrets → environment vars
└── No UI credential entry       ✅ Simplified UX flow

✅ COMPLETE: App Branding
├── Adaptive app icon            ✅ Trading chart design
├── Day/night variants           ✅ White/black backgrounds
└── Version 1.3.0               ✅ Live portfolio data release
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
- ✅ **BouncyCastle 1.78** ✅ ACTIVE (for PEM key parsing)
- ✅ **ta4j-core 0.16** (for technical indicators)
- ✅ **security-crypto 1.1.0-alpha06** (for encrypted storage - now unused)
- ✅ **work-runtime-ktx 2.10.0** (for background tasks)
- ✅ **datastore-preferences 1.1.1** (for settings)
- ✅ **material-icons-extended** ✅ ACTIVE (for ModeIndicator icons)
- ✅ Firebase Analytics + Crashlytics (BOM 34.7.0)

**CI/CD:**
- ✅ GitHub Actions: Build workflow on `claude/*` branches with credential injection
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

### 🆕 MAJOR MILESTONE: Live Coinbase Integration Complete

**Dashboard now displays real Coinbase account balances with proper error handling!**

**What was accomplished:**
✅ **BouncyCastle Integration** - Added PEM key parsing libraries to JWT generator
✅ **Enhanced JWT Authentication** - Robust EC private key parsing from PEM format
✅ **Complete API Client** - Ktor-based client with proper error handling
✅ **Account Data Retrieval** - Successfully fetching real BTC/USD balances
✅ **ViewModel Integration** - Complete state management with loading/error states
✅ **UI Updates** - Portfolio card shows live data with "Live Data" indicator
✅ **Navigation Polish** - TopAppBar moved to AppNavHost for cleaner architecture
✅ **Version Bump** - Updated to 1.3.0 reflecting live data integration

This represents the first successful end-to-end connection between the app and live Coinbase data - a critical foundation milestone for the trading bot.

### 🎯 Phase 0A-1 Progress: Core Integration (COMPLETE ✅)

**✅ COMPLETE:**
- [x] **Ticket 00:** Project Modularization - 8-module Clean Architecture
- [x] **Ticket 01:** Domain Models - All trading entities (Candle, Order, Decision, etc.)
- [x] **Ticket 02:** Repository Interfaces - Exchange contracts
- [x] **Ticket 03:** Room Database - 4 entities + 4 DAOs
- [x] **Ticket 04:** Secure Credential Store - Build-time static injection
- [x] **Ticket 07:** JWT Generator - ES256 + BouncyCastle PEM parsing
- [x] **Ticket 10A:** Dashboard Screen - Live portfolio display
- [x] **Ticket 12A:** Dashboard ViewModel - Real API integration
- [x] **Ticket 13A:** Basic API Client - Account data retrieval
- [x] **UI Foundation:** Complete component library (StatusCard, PriceDisplay, etc.)
- [x] **App Branding:** Adaptive icon with trading chart design
- [x] **CI/CD Pipeline:** GitHub Actions + Firebase Distribution
- [x] **Navigation:** Complete app navigation with TopAppBar

### 🎯 Current Status: 13/20 tickets complete (65%)

```
Phase 0A: ████████████████████ 100% (6/6) ✅ COMPLETE
Phase 1:  ████████████████████ 100% (3/3) ✅ COMPLETE (Basic API Integration)
Phase 2:  ░░░░░░░░░░░░░░░░░░░░   0% (0/6) ← NEXT FOCUS
Phase 3:  ░░░░░░░░░░░░░░░░░░░░   0% (0/3)
Phase 4:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
```

### Current Sprint: Phase 2 - Trading Logic Implementation

**Next Priority:** Ticket 05 (Decision Engine) - Core trading strategy logic

---

## 🎯 Phase 2: Trading Logic (IN PROGRESS - 0/6 complete)

**Goal:** Implement trading strategy and risk management

| Ticket | Title | Status | Priority | Dependency |
|--------|-------|--------|----------|------------|
| **05** | Decision Engine (SMA+ADX+ATR) | ← NEXT | CRITICAL | None |
| **06** | Risk Manager | Ready | HIGH | Ticket 05 |
| **13** | Full REST API Client | Ready | HIGH | None |
| **14** | WebSocket Client | Ready | MEDIUM | Ticket 13 |
| **11** | Settings Screen | Ready | MEDIUM | None |
| **Validation** | Backtesting + Paper Trading | Ready | HIGH | Tickets 05-06 |

**Estimated Timeline:** 4-6 weeks

---

## 🎯 Phase 3: Service Implementation (PENDING - 0/3 complete)

**Goal:** 24/7 background execution and full UI

| Ticket | Title | Dependency |
|--------|-------|------------|
| **17** | Trading Service (Foreground) | Phase 2 complete |
| **18** | Battery Optimization | Ticket 17 |
| **Advanced UI** | Charts, Order History, Analytics | Phase 2 complete |

**Estimated Timeline:** 3-4 weeks

---

## 🎯 Phase 4: Testing & Deployment (PENDING - 0/2 complete)

**Goal:** Validation and MVP readiness

| Component | Description |
|-----------|-------------|
| **19** | Integration Tests | End-to-end API testing |
| **20** | MVP Milestone | First live trade capability |

**Estimated Timeline:** 2-3 weeks

---

## 📊 Detailed Progress Tracking

### Phase 0A: Foundation (COMPLETE ✅)
- [x] **00: Modularization** (2026-01-07) - 8-module Clean Architecture
- [x] **01: Domain Models** (2026-01-07) - Complete trading entities  
- [x] **02: Repository Interfaces** (2026-01-07) - Exchange abstraction contracts
- [x] **03: Room Database** (2026-01-07) - 4 entities + 4 DAOs
- [x] **04: Credential Store** (2026-01-07) - Build-time static injection
- [x] **UI Components** (2026-01-07) - Complete reusable component library

### Phase 1: Basic Integration (COMPLETE ✅)
- [x] **07: JWT Generator** (2026-01-07) - ES256 + BouncyCastle
- [x] **10A: Dashboard** (2026-01-08) - Live portfolio display
- [x] **13A: API Client** (2026-01-08) - Account data retrieval

### Phase 2: Trading Logic (IN PROGRESS - 0/6)
- [ ] **05: Decision Engine** ← NEXT PRIORITY
  - SMA(200) trend filtering
  - ADX(14) trend strength measurement  
  - ATR(14) volatility-based position sizing
  - Regime switching with hysteresis (DEFENSE/TREND/RANGE)
  - **Estimated:** 5-7 days
  
- [ ] **06: Risk Manager**
  - Position size calculator (10% max position, 1-2% risk)
  - Stop-loss placement logic  
  - Portfolio drawdown monitoring (15% emergency stop)
  - **Estimated:** 3-5 days
  
- [ ] **13: Full REST API Client**  
  - Order placement (market, limit, bracket)
  - Market data (candles, products, prices)
  - Order management (cancel, status, history)
  - **Estimated:** 5-7 days
  
- [ ] **14: WebSocket Client**
  - Real-time price feeds (ticker channel)
  - Order status updates (user channel) 
  - Connection management with auto-reconnect
  - **Estimated:** 5-7 days
  
- [ ] **11: Settings Screen**
  - Trading parameter configuration
  - Notification preferences
  - About/version information
  - **Estimated:** 2-3 days
  
- [ ] **Validation: Strategy Testing**
  - Backtesting framework with historical data
  - Paper trading with small amounts ($10-20)
  - Performance metrics and win rate tracking
  - **Estimated:** 7-10 days

### Phase 3: Service Implementation (PENDING)
- [ ] **17: Trading Service** - 24/7 foreground service
- [ ] **18: Battery Optimization** - Doze mode survival
- [ ] **Advanced UI** - Charts, analytics, order history

### Phase 4: Testing & Deployment (PENDING)  
- [ ] **19: Integration Tests** - End-to-end validation
- [ ] **20: MVP Milestone** - First live trading capability

---

## 🎯 Success Criteria by Phase

### Phase 2 Success (Trading Logic)
- [x] ✅ Dashboard shows live Coinbase portfolio data
- [ ] Decision engine correctly identifies market regimes (DEFENSE/TREND/RANGE)
- [ ] Risk manager enforces position limits and stops
- [ ] Can place and cancel orders via REST API
- [ ] WebSocket provides real-time price updates
- [ ] Backtesting shows 52%+ win rate over 6 months historical data
- [ ] Paper trading profitable for 2+ weeks

### Phase 3 Success (Service)
- [ ] Trading service runs 24/7 without crashes  
- [ ] Service survives device sleep and doze mode
- [ ] UI shows real-time service status and orders
- [ ] Emergency stop works correctly

### Phase 4 Success (MVP)
- [ ] End-to-end live trading capability
- [ ] First profitable live trade executed
- [ ] All risk limits enforced
- [ ] Data properly logged for tax reporting

---

## 🚀 Getting Started with Phase 2

**Current Focus:** Ticket 05 (Decision Engine)

**Implementation Steps:**
1. **Create `EngineDecisionEngine.kt`** in `:core:domain/strategy/`
2. **Integrate ta4j library** for SMA, ADX, ATR calculations  
3. **Implement regime detection** logic with 4 modes
4. **Add hysteresis** to prevent whipsawing
5. **Unit tests** with historical data samples
6. **Integration** with existing domain models

**File Locations:**
```
core/domain/src/main/kotlin/com/tradeflow/core/domain/
├── strategy/
│   ├── DecisionEngine.kt           # Interface (exists)
│   ├── EngineDecisionEngine.kt     # ← IMPLEMENT THIS
│   └── StrategyConfig.kt           # ← Configuration data class
```

**Key Requirements:**
- Pure Kotlin (no Android dependencies in core:domain)
- Uses existing domain models (Candle, Decision, etc.)  
- Integrates with ta4j for technical indicators
- Comprehensive unit test coverage
- Clear regime switching logic with hysteresis

**Acceptance Criteria:**
- [ ] Correctly calculates SMA(200), ADX(14), ATR(14)
- [ ] Identifies all 4 modes: WAIT, DEFENSE, TREND, RANGE  
- [ ] Hysteresis prevents rapid mode switching
- [ ] Unit tests achieve >90% code coverage
- [ ] Integration tests with sample market data

**This is the core trading brain - once complete, we have the logic to determine when and how to trade based on market conditions.**

---

## 🎯 Long-term Roadmap (Post-MVP)

### Phase 5: Advanced Features (3-6 months)
- Multiple asset support (ETH, SOL when account > $2,500)
- Advanced chart analysis and visualization  
- Performance analytics and reporting
- Tax loss harvesting optimization
- Cloud backup and sync

### Phase 6: Strategy Enhancement (6-12 months)  
- Multiple strategy support (Grid, DCA, Momentum)
- Machine learning for regime detection
- Cross-exchange arbitrage opportunities
- Advanced risk management (correlation, VaR)
- Portfolio rebalancing automation

### Phase 7: Scale & Optimization (1+ years)
- Multiple exchange support (Kraken, Binance when proven)
- Institutional features (larger capital management)
- Advanced order types (TWAP, iceberg, conditional)
- Professional reporting and compliance tools
- Community features (strategy sharing, leaderboards)

**Note:** These phases only pursue if Phase 2-4 prove consistently profitable. No point in advanced features if basic strategy loses money.

---

## 📈 Success Metrics

### Phase 2 Targets (Next 6 weeks)
- **Technical:** Decision engine implemented with >90% test coverage
- **Integration:** Live API connection with error handling
- **Validation:** Backtesting shows positive expectancy
- **UI:** Settings screen functional with all parameters

### Phase 3 Targets (Next 3 months)  
- **Service:** 24/7 uptime >95% over 30-day period
- **Performance:** First profitable live trades executed
- **Risk:** No position exceeds 10% of account, no drawdown >15%
- **Reliability:** Zero critical bugs in live trading

### Phase 4 Targets (Next 6 months)
- **Profitability:** Net positive returns after fees over 3-month period  
- **Win Rate:** Achieve and maintain >52% win rate
- **Risk-Adjusted:** Sharpe ratio >1.0 (returns exceed risk)
- **Scalability:** Ready to increase position sizes if edge confirmed

**Conservative Timeline to Profitability: 6-12 months**  
**Realistic Outcome: Break-even while learning (success by itself)**  
**Optimistic Outcome: 3-5% monthly returns (exceptional achievement)**
