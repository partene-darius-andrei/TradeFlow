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
    │   └── CoinbaseJwtGenerator.kt  ✅ ES256 JWT with BouncyCastle PEM parsing + escape handling
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
├── No UI credential entry       ✅ Simplified UX flow
└── PEM key escape handling      ✅ Proper newline escaping for environment variables

✅ COMPLETE: App Branding
├── Adaptive app icon            ✅ Trading chart design
├── Day/night variants           ✅ White/black backgrounds
└── Version 1.4.0               ✅ Live portfolio data with enhanced PEM key parsing
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
✅ **Advanced PEM Parsing** - Handles traditional EC format and PKCS8 format
✅ **Build Escape Handling** - Proper escape sequence handling for environment variables
✅ **Error State Management** - Loading states, error display with retry functionality
✅ **Real Data Integration** - Portfolio card shows actual BTC/USD balances from API
✅ **Version Increment** - Updated to version 1.4.0 reflecting new capabilities

**Technical Improvements:**
- Enhanced CoinbaseJwtGenerator with comprehensive PEM key support
- Build configuration now properly escapes private key newlines for environment variables
- Added detailed error logging and troubleshooting for PEM key parsing
- Improved DashboardViewModel with proper state management and error recovery

This represents the **first successful live data integration** - a critical foundation for all future trading functionality.

### 🎯 What's Missing (Prioritized)

#### Phase 2A: Complete REST API (HIGH Priority)
**Goal:** Full Coinbase API integration for trading operations

| Ticket | Component | Status | Description |
|--------|-----------|--------|-------------|
| **13-Extended** | Complete REST API Client | ⏳ Next | candles, orders, market data endpoints |
| **14** | WebSocket Client | ⏳ Later | Real-time price + order updates |

#### Phase 2B: Trading Intelligence (HIGH Priority)
**Goal:** Implement core trading strategy logic

| Ticket | Component | Status | Description |
|--------|-----------|--------|-------------|
| **15** | Decision Engine | ⏳ Critical | SMA/ADX/ATR regime switching |
| **16** | Risk Manager | ⏳ Critical | Position sizing + drawdown limits |

#### Phase 2C: Service Layer (MEDIUM Priority)  
**Goal:** 24/7 automated operation

| Ticket | Component | Status | Description |
|--------|-----------|--------|-------------|
| **17** | Trading Service | ⏳ Important | Foreground service for background trading |
| **18** | Battery Optimization | ⏳ Important | Doze mode survival |

#### Phase 3: User Interface Completion (LOW Priority)
**Goal:** Complete user control and monitoring

| Ticket | Component | Status | Description |
|--------|-----------|--------|-------------|
| **11** | Settings Screen | ⏳ Nice-to-have | View credentials, preferences |
| **19** | Integration Tests | ⏳ Validation | End-to-end testing |
| **20** | MVP Milestone | ⏳ Release | Production readiness |

---

## Phase-by-Phase Implementation

### ✅ Phase 0: Foundation (COMPLETE - 100%)
**Delivered:** Jan 2026

**What was built:**
- ✅ Multi-module architecture (8 modules)
- ✅ Domain models and interfaces
- ✅ Room database (4 entities + DAOs)  
- ✅ JWT authentication with BouncyCastle
- ✅ Basic Coinbase API integration (accounts endpoint)
- ✅ Dashboard UI with live portfolio data
- ✅ Build-time credential injection system
- ✅ CI/CD pipeline with Firebase distribution

**Key Achievements:**
1. **Live Data Integration** - App shows real account balances from Coinbase
2. **Secure Authentication** - ES256 JWT tokens with advanced PEM key parsing
3. **Clean Architecture** - Modular design supporting easy exchange swapping
4. **Professional UI** - Adaptive icon, error handling, loading states
5. **DevOps Pipeline** - Automated builds with credential injection

---

### ⏳ Phase 1: Trading Core (0% - Next Up)

**Timeline:** 4-6 weeks  
**Goal:** Complete trading functionality

#### Milestone 1.1: Complete API Integration (2-3 weeks)
- ✅ REST endpoints: candles, orders, market data  
- ✅ Order placement: market, limit, bracket orders
- ✅ WebSocket: real-time prices and order updates
- ✅ Integration testing with small real trades ($10-20)

#### Milestone 1.2: Strategy Engine (2-3 weeks)  
- ✅ Decision Engine: SMA(200) + ADX(14) + ATR(14)
- ✅ Risk Manager: 2% position sizing + 15% drawdown limit
- ✅ Backtesting framework with historical data validation
- ✅ Paper trading with mock orders

#### **Phase 1 Success Criteria:**
- [ ] Can place real orders on Coinbase via app
- [ ] Strategy correctly identifies DEFENSE/TREND/RANGE modes
- [ ] Risk limits properly enforced (no position > 2%)
- [ ] Backtesting shows 52%+ win rate on historical data
- [ ] Paper trading runs without crashes for 24 hours

**Estimated completion:** Mid-March 2026

---

### ⏳ Phase 2: Autonomous Operation (0%)

**Timeline:** 2-3 weeks  
**Goal:** 24/7 background trading service

#### Milestone 2.1: Service Layer
- ✅ Foreground service with proper notifications
- ✅ Battery optimization and Doze mode survival
- ✅ Service start/stop controls from Dashboard
- ✅ Automatic restart after device reboot

#### Milestone 2.2: Monitoring & Control
- ✅ Settings screen for parameter adjustment
- ✅ Real-time portfolio tracking
- ✅ Emergency stop functionality
- ✅ Trading history and performance metrics

#### **Phase 2 Success Criteria:**
- [ ] Service runs 24/7 without user intervention
- [ ] Survives device sleep/wake cycles
- [ ] Can be controlled entirely from mobile UI
- [ ] Emergency stop works within 60 seconds
- [ ] Complete audit trail of all trading decisions

**Estimated completion:** End of March 2026

---

### ⏳ Phase 3: Production & Validation (0%)

**Timeline:** 2-4 weeks  
**Goal:** Live trading with real money

#### Milestone 3.1: Live Trading Preparation
- ✅ Comprehensive integration tests
- ✅ Small-capital live testing ($50-100)
- ✅ Performance monitoring and alerts
- ✅ Tax record generation

#### Milestone 3.2: Strategy Validation  
- ✅ 30-day live performance tracking
- ✅ Compare actual vs. backtested performance
- ✅ Risk limit validation under real conditions
- ✅ Strategy parameter optimization

#### **Phase 3 Success Criteria:**  
- [ ] 30 consecutive days of live trading without major issues
- [ ] Actual performance within 10% of backtested results
- [ ] No risk limit breaches (no position > 2%, no drawdown > 15%)
- [ ] Complete trade records for tax reporting
- [ ] Strategy profitable net of fees over 30-day period

**Estimated completion:** End of April 2026

---

## Success Metrics & Risk Gates

### Phase Gates (Must Pass to Continue)

**Phase 0 → Phase 1:** ✅ PASSED
- ✅ Live API connection working
- ✅ Dashboard showing real account data
- ✅ Authentication flow complete

**Phase 1 → Phase 2:**
- [ ] Backtesting shows 52%+ win rate over 1000+ trades
- [ ] Paper trading runs 7 days without crashes
- [ ] Strategy correctly handles all market conditions (bull, bear, sideways)
- [ ] Risk manager never allows position > 2% or drawdown > 15%

**Phase 2 → Phase 3:**
- [ ] Service runs 7 days uninterrupted
- [ ] Battery optimization works on test device
- [ ] All UI controls functional
- [ ] Emergency stop tested and verified

**Phase 3 → Production Scaling:**
- [ ] 30-day live period with net profit (after fees)
- [ ] No major bugs or risk limit breaches
- [ ] Performance matches backtesting within reason
- [ ] User comfortable with system operation

### Performance Targets

**Conservative Targets (Phase 1):**
- Win rate: 52-55%
- Average return per trade: 0.5-1.0% (net of fees)
- Max drawdown: < 10% (emergency stop at 15%)
- Sharpe ratio: > 0.8

**Optimistic Targets (Phase 3):**
- Win rate: 55-58% 
- Monthly return: 3-5%
- Max drawdown: < 8%
- Sharpe ratio: > 1.0

**Reality Check:**
- These targets are EXTREMELY difficult to achieve
- Most algo traders fail completely
- Success means beating 97% of retail traders
- Even 2-3% monthly returns would be excellent

---

## Risk Management

### Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|---------|------------|
| API changes | Medium | High | Monitor Coinbase developer updates |
| Rate limiting | High | Medium | Implement backoff, use WebSocket for real-time |
| Authentication failure | Low | High | JWT refresh logic, credential validation |
| Service crashes | Medium | High | Comprehensive error handling, auto-restart |
| Battery optimization | High | High | User education, doze exemption |

### Financial Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|---------|------------|  
| Strategy failure | High | High | Start with $500 max, extensive backtesting |
| Market crashes | Medium | High | 15% drawdown emergency stop |
| Exchange insolvency | Low | High | Only use reputable exchanges, limited capital |
| Regulatory changes | Low | Medium | Stay informed, personal use only |
| Tax compliance | Medium | Medium | Complete record keeping |

### Operational Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|---------|------------|
| Phone loss/damage | Medium | Medium | Cloud backup, multiple devices |
| Internet outage | Medium | Low | Mobile data backup |
| User error | High | Medium | Extensive testing, simple UI |
| Over-optimization | High | High | Simple strategy, avoid curve fitting |

---

## Long-term Roadmap (6+ months)

### Phase 4: Multi-Exchange Support (Optional)
- Add Kraken or Binance integration
- Cross-exchange arbitrage opportunities
- Portfolio diversification across exchanges

### Phase 5: Strategy Enhancement (Optional)
- Additional technical indicators
- Multi-timeframe analysis
- Options trading integration (if profitable)

### Phase 6: Scale & Optimize (Optional)
- Larger capital deployment ($2,500+)
- Advanced risk management
- Tax optimization strategies

---

## Current Focus: Phase 1 Implementation

**Immediate next tickets to complete (in order):**

1. **Ticket 13 Extension - Complete REST API Client** (2-3 weeks)
   - Implement candles endpoint for historical data
   - Add order placement endpoints (market, limit, bracket)
   - Add order management (cancel, status, fills)
   - Comprehensive integration testing

2. **Ticket 15 - Decision Engine** (1-2 weeks)
   - Implement SMA/ADX/ATR calculations with ta4j
   - Add regime switching logic with hysteresis
   - Handle edge cases and market gaps
   - Comprehensive unit testing

3. **Ticket 16 - Risk Manager** (1 week)  
   - Position sizing calculations
   - Drawdown monitoring
   - Emergency liquidation logic
   - Integration with decision engine

4. **Phase 1B - Strategy Validation** (1-2 weeks)
   - Backtesting framework
   - Historical data validation
   - Paper trading setup
   - Performance metrics

**Total Phase 1 Estimate:** 5-8 weeks to complete

**Critical success factor:** Phase 1 backtesting must show consistent profitability before proceeding to live trading. If strategy doesn't work on historical data, it won't work with real money.

---

This roadmap represents a measured, professional approach to automated trading. The emphasis is on proper risk management, thorough testing, and realistic expectations rather than promises of quick profits.
