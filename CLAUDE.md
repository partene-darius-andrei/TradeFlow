# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-09
**Project Status:** Phase 2 Complete - Core Trading Logic (v1.6.0)
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

### What EXISTS (Phases 1 & 2 COMPLETE - v1.6.0)

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
    │   ├── Decision.kt                ✅ Enhanced sealed class with technical indicators (Wait/Defense/Trend/Range)
    │   ├── Portfolio.kt               ✅ Portfolio snapshot model + utility extensions
    │   ├── Balance.kt                 ✅ Account balance model
    │   └── Ticker.kt                  ✅ Real-time price ticker
    ├── repository/
    │   ├── BracketOrderRepository.kt   ✅ Bracket order support interface
    │   ├── ExchangeRepository.kt       ✅ Core exchange operations (12 methods)
    │   └── ExchangeWebSocket.kt        ✅ Real-time data streams
    ├── strategy/ ← Ticket 15 COMPLETE ✅
    │   ├── DecisionEngine.kt           ✅ Decision engine interface
    │   ├── TradingDecisionEngine.kt    ✅ Complete regime-switching implementation with hysteresis
    │   └── StrategyConfig.kt           ✅ Comprehensive strategy parameters
    ├── indicator/ ← NEW IN v1.6.0
    │   ├── SMACalculator.kt            ✅ Simple Moving Average with ta4j integration
    │   ├── ADXCalculator.kt            ✅ Average Directional Index with ta4j integration
    │   └── ATRCalculator.kt            ✅ Average True Range with ta4j integration
    ├── risk/
    │   └── RiskManager.kt              ✅ Risk management interface with enhanced types
    └── usecase/ ← NEW - Use Case Layer Implementation
        ├── ExecuteDecisionUseCase.kt   ✅ Trading decision execution orchestrator
        ├── ExecuteTradingCycleUseCase.kt ✅ Complete trading cycle with risk management
        ├── HandleEmergencyUseCase.kt   ✅ Emergency liquidation handler
        ├── ManageGridOrdersUseCase.kt  ✅ Grid order management for range trading
        ├── ManageOrdersUseCase.kt      ✅ Order lifecycle and reconciliation
        ├── UpdatePortfolioUseCase.kt   ✅ Portfolio state updates
        └── model/
            ├── ExecutionResult.kt      ✅ Use case result types
            └── TradingContext.kt       ✅ Trading context data model

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
    │   └── CoinbaseRepository.kt       ✅ Implementation (getBalances working, others TODO for Phase 3)
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

### 🎉 Major Milestone: Complete Core Trading Logic (v1.6.0)

**Phase 2 has been successfully completed with comprehensive trading engine and use case implementation:**

🆕 **Use Case Layer Implementation (NEW in v1.6.0):**
- ✅ **ExecuteDecisionUseCase** - Complete decision execution orchestrator
  - Handles all 4 trading modes (DEFENSE/TREND/RANGE/WAIT)
  - Risk validation before order placement
  - Bracket order support for trend trading
  - Grid order management for range trading
- ✅ **ExecuteTradingCycleUseCase** - Complete trading cycle orchestrator
  - Portfolio updates with high water mark tracking
  - Drawdown monitoring with emergency liquidation
  - Strategy evaluation and execution
  - Order reconciliation and management
- ✅ **HandleEmergencyUseCase** - Emergency liquidation handler
  - Cancel all open orders
  - Market sell all BTC positions
  - Complete portfolio liquidation
- ✅ **ManageGridOrdersUseCase** - Grid trading implementation
  - Dynamic grid spacing calculation
  - Risk-validated position sizing
  - Partial success handling
  - Fee break-even validation (1.5% minimum spacing)
- ✅ **ManageOrdersUseCase** - Order lifecycle management
  - Stale order cancellation (48-hour timeout)
  - Order reconciliation between local and exchange
  - Status synchronization
- ✅ **UpdatePortfolioUseCase** - Portfolio state management
  - Real-time equity calculation
  - Multi-currency balance handling
  - Snapshot creation for tracking

🆕 **Enhanced Domain Models:**
- ✅ **ExecutionResult** sealed class for use case results
- ✅ **TradingContext** data model for strategy execution
- ✅ **Portfolio utility extensions** (getBtcBalance function)

🆕 **Technical Indicators Complete:**
- ✅ SMACalculator with ta4j BaseBarSeries integration
- ✅ ADXCalculator for trend strength measurement  
- ✅ ATRCalculator for volatility-based risk management
- ✅ Complete error handling and validation

🆕 **Strategy Configuration:**
- ✅ Configurable parameters (SMA period: 200, ADX period: 14, ATR period: 14)
- ✅ Risk management controls (stop-loss: 3x ATR, take-profit: 6x ATR)
- ✅ Position sizing (trend: 5%, grid: 2% per level)
- ✅ Grid spacing controls (minimum 1.5% for fee break-even)

🆕 **Comprehensive Unit Testing:**
- ✅ MockK integration for fast, isolated testing
- ✅ ExecuteDecisionUseCaseTest with all mode coverage
- ✅ HandleEmergencyUseCaseTest with failure scenarios
- ✅ ManageGridOrdersUseCaseTest with risk validation
- ✅ Edge case coverage and error handling validation

### What's MISSING (Phase 3 Implementation)

**Next critical priorities for live trading capability:**

❌ **Full REST API Client** (Ticket 13)
- Complete CoinbaseRepository implementation
- Order placement methods (bracket, limit, market)
- Candle data fetching (handle 350-candle limit)
- Product queries and order management

❌ **WebSocket Client** (Ticket 14)  
- Real-time price feeds
- Order status updates
- Connection management with auto-reconnect

❌ **Risk Manager Implementation** (Ticket 16)
- Position sizing calculations
- Drawdown monitoring
- Risk validation for orders

❌ **Trading Service** (Ticket 17)
- 24/7 foreground service
- Strategy loop orchestration
- Background execution management

---

## 🔄 Development Workflow

### Git Workflow
```bash
# Create feature branch from main
git checkout main
git pull origin main
git checkout -b claude/feature-name

# Make changes, commit frequently
git add .
git commit -m "feat: implement feature X"

# Push for CI build
git push origin claude/feature-name

# Check build status
cat .build-status  # SUCCESS or FAILURE
cat build-log.txt  # Error details if failed

# Create PR when ready
gh pr create --title "Feature: X" --body "Implements feature X"
```

### CI/CD Pipeline

**GitHub Actions builds automatically on push to `claude/*` or `main`:**

1. ✅ Injects Coinbase credentials from GitHub secrets
2. ✅ Builds APK with embedded credentials
3. ✅ Uploads to Firebase App Distribution
4. ✅ Commits build status back (`.build-status` + `build-log.txt`)
5. ✅ Updates documentation automatically

**Auto-documentation workflow:**
1. ✅ Analyzes code changes
2. ✅ Updates CLAUDE.md and docs/ using Claude API
3. ✅ Commits documentation updates back to branch

### Dependencies

**Active Libraries (v1.6.0):**
- ✅ **ta4j-core 0.16** - Technical indicators (SMA/ADX/ATR) ✅ ACTIVE
- ✅ **mockk 1.14.7** - Unit testing with mocks ✅ ACTIVE  
- ✅ **kotlin-test 2.1.0** - Testing framework ✅ ACTIVE
- ✅ **nimbus-jose-jwt 9.47** - ES256 JWT signing ✅ ACTIVE
- ✅ **BouncyCastle 1.78** - Advanced PEM key parsing ✅ ACTIVE
- ✅ Kotlin 2.3.0, Compose BOM 2025.12.01, Hilt 2.57.2, Room 2.8.4, Ktor 3.3.3

**Module Structure:**
```
✅ :app - Application entry point + DI wiring (100% complete)
✅ :core:domain - Pure Kotlin business logic (100% complete with use cases)
✅ :core:data - Room database + security (100% complete)  
✅ :core:ui - Shared Compose components (100% complete)
🟡 :exchange:coinbase - Coinbase API (40% - auth complete, REST/WS pending)
```

---

## 🎯 Next Steps

### Phase 3: API Integration & Service (Target: 2-3 weeks)

1. **Ticket 13: Full REST API Client** (HIGH)
   - Complete CoinbaseRepository with order placement
   - Candle fetching with TWO_HOUR aggregation
   - Error handling and rate limiting

2. **Ticket 14: WebSocket Client** (HIGH)
   - Real-time ticker and order updates
   - Auto-reconnect and health monitoring

3. **Ticket 16: Risk Manager** (MEDIUM)
   - Position sizing implementation
   - Drawdown calculation and emergency triggers

4. **Ticket 17: Trading Service** (HIGH)
   - 24/7 foreground service with use case orchestration
   - Battery optimization and doze survival

### Phase 4: Testing & Validation

5. **Ticket 19: Integration Tests** (MEDIUM)
   - End-to-end testing with small real trades
   - API validation and error handling

6. **Ticket 20: MVP Milestone** (HIGH)
   - 24-hour live system validation
   - Complete trading cycle verification

**Target Timeline:** 4-6 weeks to first live trade capability

**Success Criteria:** Autonomous trading bot that can run 24/7 with $100-500 test capital, making 1-3 trades per day with proper risk management.
