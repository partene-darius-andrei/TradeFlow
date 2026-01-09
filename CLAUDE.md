# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-09
**Project Status:** Phase 2 In Progress - Core Trading Logic (v1.6.0)
**Current Build:** #31 SUCCESS

This is the entry point for Claude Code when working with TradeFlow. All essential context, navigation, and workflows are documented here.

---

## 🎯 Quick Navigation

| Document | Purpose | Use When |
|----------|---------|----------|
| **[docs/roadmap.md](docs/roadmap.md)** | Implementation roadmap organized in phases | Planning what to build next |
| **[docs/README.md](docs/README.md)** | Complete documentation index and ticket mapping | Finding specific documentation |
| **[docs/reference.md](docs/reference.md)** | Implementation blueprint with code examples | Implementing features |
| **[docs/ci.md](docs/ci.md)** | CI/CD workflows and troubleshooting | Understanding build pipeline |
| **[docs/tickets/](docs/tickets/)** | All ticket files organized by status | Reading detailed requirements |

---

## 📊 Project Overview

**TradeFlow** - Personal automated crypto trading bot for Coinbase Advanced Trade API.

**Vision:**
- Remove human emotions from trading decisions
- Run 24/7 unattended on physical device (when proven)
- Simple UI, simple implementation, easy to maintain
- Backtest → Paper trade → Live (small) → Scale
- Never published - personal use only

**Reality Constraints:**
- Fees matter: ~0.25-0.5% per trade on Advanced Trade
- Most retail algo traders lose money - respect this
- Simple strategies often beat complex ML
- Every trade is a taxable event

**Trading Strategy (Bitcoin-First):**
- **BTC/USDT ONLY** until account reaches $2,500+ (fees kill small-cap altcoins)
- **Risk: 1-2% per trade** ($5-10 max on $500 account)
- **Expected returns: 5% monthly** (exceptional skill, realistic ceiling)
- **Timeline: 5-10 years** to meaningful passive income ($500-1k/month)
- **97% of day traders fail** - Treat first $500 as education, not income
- **See:** [docs/strategy/bitcoin-first-strategy.md](docs/strategy/bitcoin-first-strategy.md) for complete analysis

---

## 🚦 Current Status

### What EXISTS (Phases 1 COMPLETE + Phase 2 IN PROGRESS - v1.6.0)

```
✅ Modern Android app structure
✅ Hilt dependency injection configured
✅ Room database with complete schema (4 entities + 4 DAOs)
✅ Ktor HTTP client configured (OkHttp engine)
✅ Timber logging initialized
✅ Firebase Analytics + Crashlytics
✅ All trading dependencies added (ta4j, nimbus-jose-jwt, security-crypto)
✅ GitHub Actions CI/CD pipeline with environment credentials
✅ Adaptive app icon with trading chart design (day/night variants)

✅ DOMAIN LAYER COMPLETE:
✅ core/domain/src/main/kotlin/com/tradeflow/core/domain/
    ├── auth/
    │   ├── AuthTokenProvider.kt        ✅ Token generation interface
    │   └── CredentialStore.kt          ✅ Secure credential storage interface
    ├── error/
    │   └── ExchangeError.kt           ✅ Exchange error types (6 variants)
    ├── model/ ← Ticket 01 COMPLETE
    │   ├── Candle.kt                  ✅ OHLCV + Granularity enum (9 timeframes)
    │   ├── Order.kt                   ✅ Order model + Side/Type/Status enums
    │   ├── Decision.kt                ✅ Sealed class (Wait/Defense/Trend/Range)
    │   ├── Portfolio.kt               ✅ Portfolio snapshot model
    │   ├── Balance.kt                 ✅ Account balance model
    │   └── Ticker.kt                  ✅ Real-time price ticker
    ├── repository/
    │   ├── BracketOrderRepository.kt   ✅ Bracket order support interface
    │   ├── ExchangeRepository.kt       ✅ Core exchange operations (12 methods)
    │   └── ExchangeWebSocket.kt        ✅ Real-time data streams
    └── strategy/ ← Ticket 15 COMPLETE (NEW)
        ├── DecisionEngine.kt           ✅ Decision engine interface
        ├── TradingDecisionEngine.kt    ✅ Regime-switching implementation
        └── StrategyConfig.kt           ✅ Strategy parameters

🆕 TECHNICAL INDICATORS (v1.6.0):
✅ core/domain/src/main/kotlin/com/tradeflow/core/domain/indicator/
    ├── SMACalculator.kt                ✅ Simple Moving Average with ta4j
    ├── ADXCalculator.kt                ✅ Average Directional Index with ta4j
    └── ATRCalculator.kt                ✅ Average True Range with ta4j

✅ DATA LAYER COMPLETE:
✅ core/data/src/main/kotlin/com/tradeflow/core/data/
    ├── local/ ← Ticket 03 COMPLETE
    │   ├── entity/
    │   │   ├── CandleEntity.kt        ✅ Room entity for candles
    │   │   ├── OrderEntity.kt         ✅ Room entity for orders
    │   │   ├── DecisionEntity.kt      ✅ Room entity for decisions
    │   │   └── PortfolioSnapshotEntity.kt ✅ Room entity for portfolio
    │   └── dao/
    │       ├── CandleDao.kt           ✅ CRUD + delete old candles
    │       ├── OrderDao.kt            ✅ CRUD + query by status/product
    │       ├── DecisionDao.kt         ✅ CRUD + latest decision query
    │       └── PortfolioDao.kt        ✅ CRUD + snapshot history
    ├── security/
    │   └── StaticCredentialStore.kt    ✅ Static credential injection (replaces UI input)
    └── di/
        └── SecurityModule.kt           ✅ Static credential DI binding

✅ COINBASE INTEGRATION COMPLETE (v1.5.5):
✅ exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt     ✅ ES256 JWT with ADVANCED BouncyCastle PEM parsing + enhanced escape handling + comprehensive error recovery
    ├── api/
    │   └── CoinbaseApiClient.kt        ✅ Complete Ktor-based API client (accounts) with robust error handling
    ├── dto/
    │   └── AccountDto.kt               ✅ Account DTOs for API responses  
    ├── mapper/
    │   └── AccountMapper.kt            ✅ DTO to domain mapping
    ├── repository/
    │   └── CoinbaseRepository.kt       ✅ Implementation (getBalances working, others TODO for Phase 2)
    └── di/
        ├── AuthModule.kt               ✅ JWT generator DI binding
        └── ExchangeModule.kt           ✅ Repository DI binding

✅ UI COMPONENTS COMPLETE:
✅ core/ui/src/main/kotlin/com/tradeflow/core/ui/
    ├── component/
    │   ├── ErrorDisplay.kt             ✅ Error state with retry button
    │   ├── LoadingButton.kt            ✅ Button with loading spinner
    │   ├── ModeIndicator.kt            ✅ Trading mode badges (DEFENSE/TREND/RANGE)
    │   ├── PriceDisplay.kt             ✅ Price with +/- color coding
    │   └── StatusCard.kt               ✅ Reusable card container
    └── extension/
        └── BigDecimalExt.kt           ✅ Currency/percentage formatting

✅ PRESENTATION LAYER WITH ENHANCED LIVE DATA (v1.5.5):
✅ app/src/main/java/com/dpart/tradeflow/
    ├── navigation/
    │   └── AppNavHost.kt               ✅ Complete navigation with CENTRALIZED TopAppBar ("TradeFlow" title)
    └── presentation/dashboard/
        ├── DashboardScreen.kt          ✅ Complete implementation with ENHANCED real data integration
        ├── DashboardViewModel.kt       ✅ Full state management + ROBUST error handling + loading states
        └── components/
            ├── PortfolioCard.kt        ✅ Live data, BTC/USD balances with "Live Data" indicator + enhanced formatting
            ├── ModeCard.kt             ✅ Trading mode + current price
            ├── ServiceCard.kt          ✅ Service status + start/stop button
            └── OrdersList.kt           ✅ Recent orders + empty state

✅ APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants (white/black)
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

✅ ENHANCED CREDENTIALS SYSTEM (v1.5.5):
✅ app/build.gradle.kts                 ✅ ADVANCED build-time credential injection with ENHANCED PEM key escaping + security improvements
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig
```

### Major Milestone: Core Trading Logic Implementation (v1.6.0)

**Latest trading engine enhancements now implemented:**

🆕 **Decision Engine Implementation (Ticket 15 - v1.6.0):**
- Complete regime-switching decision engine with hysteresis logic
- SMA(200) trend filter, ADX(14) trend strength, ATR(14) volatility sizing
- 3-candle confirmation for mode switches (prevents whipsaws)
- Comprehensive ta4j integration for technical indicators
- Support for DEFENSE/TREND/RANGE/WAIT trading modes

🆕 **Technical Indicators (NEW):**
- SMACalculator with ta4j BaseBarSeries integration
- ADXCalculator for trend strength analysis  
- ATRCalculator for volatility-based position sizing
- Complete PEM parsing and error handling for all indicators

🆕 **Strategy Configuration:**
- Configurable strategy parameters (SMA period, ADX thresholds, ATR multipliers)
- Position sizing controls (trend vs grid percentages)
- Risk management parameters (stop-loss, take-profit ATR multiples)

### Current App Version

**Version:** 1.6.0 (latest stable with decision engine)
**Key New Features:**
- Complete decision engine implementation with regime switching
- Technical indicator calculations using ta4j library
- Hysteresis logic to prevent false signals and overtrading
- Support for all 4 trading modes with proper validation
- Enhanced testing capabilities with MockK and Kotlin Test

### Tech Stack Status

| Component | Version | Status | Usage |
|-----------|---------|--------|-------|
| **Kotlin** | 2.3.0 | ✅ Active | Language |
| **Compose BOM** | 2025.12.01 | ✅ Active | UI framework |
| **Hilt** | 2.57.2 | ✅ Active | Dependency injection |
| **Room** | 2.8.4 | ✅ Active | Local database (4 entities + 4 DAOs) |
| **Ktor** | 3.3.3 | ✅ Active | HTTP client (accounts endpoint working) |
| **ta4j-core** | 0.16 | ✅ Active | Technical indicators (SMA/ADX/ATR) |
| **nimbus-jose-jwt** | 9.47 | ✅ Active | ES256 JWT authentication |
| **BouncyCastle** | 1.78 | ✅ Active | Advanced PEM key parsing |
| **MockK** | 1.13.8 | ✅ Active | Unit testing mocks |
| **Kotlin Test** | 2.1.0 | ✅ Active | Testing framework |
| **Timber** | 5.0.1 | ✅ Active | Logging |
| **Firebase BOM** | 34.7.0 | ✅ Active | Analytics + Crashlytics |

### What DOESN'T Exist Yet (Next Up - Phase 2 Continuation)

```
❌ REST API Client - Ticket 13 (Full order placement, market data)
❌ WebSocket Client - Ticket 14 (Real-time price feeds)
❌ Risk Manager - Ticket 16 (Position sizing, drawdown tracking)
❌ Integration Tests - Ticket 19 (E2E testing)
❌ Trading Service - Background execution loop
❌ Settings Screen - Strategy parameter configuration
```

---

## 🔧 Development Workflow

### For Claude Code

**Current focus:** Phase 2 completion (REST API + WebSocket clients)

**Next immediate tasks:**
1. **Ticket 13:** Complete REST API client (order placement, candles, products)
2. **Ticket 14:** WebSocket client for real-time data
3. **Ticket 16:** Risk management implementation
4. **Integration testing:** End-to-end validation

**Testing the Decision Engine:**
- Unit tests implemented with comprehensive coverage
- Test cases for all 4 modes (DEFENSE/TREND/RANGE/WAIT)
- Hysteresis logic validation
- MockK integration for indicator mocking

---

## 📁 Current Project Structure

```
TradeFlow/
├── app/                              ✅ Entry point + DI wiring
├── core/
│   ├── domain/                       ✅ Pure Kotlin domain + NEW decision engine
│   ├── data/                         ✅ Room database + security
│   └── ui/                          ✅ Shared UI components
├── exchange/
│   └── coinbase/                     ✅ Coinbase API integration (auth working)
├── docs/                            ✅ Complete documentation
└── .github/workflows/               ✅ CI/CD pipeline
```

---

## 💡 Key Implementation Details

### Decision Engine Logic (NEW - v1.6.0)

The implemented decision engine follows this logic:

1. **DEFENSE Mode:** Instant activation when price < SMA(200) - no hysteresis for safety
2. **TREND Mode:** Requires 3 consecutive candles with ADX > 25 (prevents false signals)  
3. **RANGE Mode:** Requires 3 consecutive candles with ADX < 25 (confirms sideways market)
4. **Hysteresis Reset:** Any move to DEFENSE mode resets confirmation counters

### Technical Indicators Integration

- **SMA(200):** Trend direction filter (bull/bear market)
- **ADX(14):** Trend strength (>25 = trending, <25 = ranging)  
- **ATR(14):** Volatility for stop-loss/take-profit placement
- **All indicators** use ta4j BaseBarSeries for consistent calculations

### Testing Strategy

- **Unit Tests:** MockK for indicator mocking, comprehensive test coverage
- **Integration Tests:** Planned for Phase 3 with small real trades
- **Strategy Validation:** Hysteresis prevents overtrading, mode switching tested

This represents a major milestone - the core trading brain is now implemented and ready for integration with live market data.

