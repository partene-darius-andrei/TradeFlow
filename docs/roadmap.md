# TradeFlow - Master Implementation Plan

**Last Updated:** 2026-01-08  
**Project Status:** Phase 1 Complete - Enhanced Coinbase Integration (v1.5.3)  
**Current Build:** #31 SUCCESS (Version 1.5.3)
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

**Phase 1 COMPLETE - Enhanced Coinbase Integration (v1.5.3):**

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
    ├── AppNavHost.kt            ✅ Complete navigation with CENTRALIZED TopAppBar ("TradeFlow" title)
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

✅ COMPLETE: Enhanced Coinbase API Integration (v1.5.3)
└── exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt  ✅ ES256 JWT with ADVANCED BouncyCastle PEM parsing + enhanced escape handling + comprehensive error recovery
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

✅ COMPLETE: Enhanced Live Portfolio Data Integration (v1.5.3)
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

### 🆕 MAJOR MILESTONE: Enhanced Security & Reliability (v1.5.3)

**Latest stability and security enhancements now live in the app:**

✅ **Enhanced Authentication (v1.5.3):**
- ADVANCED PEM key parsing with BouncyCastle libraries (bcprov-jdk18on, bcpkix-jdk18on 1.78)
- Support for both raw base64 and PEM formats with automatic format detection
- Comprehensive error handling in JWT generation with multiple fallback mechanisms
- Build-time credential injection with IMPROVED security key processing

✅ **Improved API Integration:**
- CoinbaseApiClient with enhanced Ktor HTTP client and robust error handling
- AccountsResponseDto with complete Coinbase API response structure
- Domain mapping from DTOs to Balance models with better error recovery
- Professional UX flow with enhanced loading states and retry mechanisms

✅ **UI & Navigation Improvements:**
- RESOLVED duplicate navigation bar display issue for cleaner interface
- Centralized TopAppBar configuration with "TradeFlow" title
- Enhanced dashboard with complete state management and loading indicators
- Portfolio card shows BTC/USD balances with "Live Data" indicator and enhanced formatting

### 🔄 What's CHANGED Recently (v1.5.3)

**Version Updates:**
- ✅ **app/build.gradle.kts**: Version bumped from 1.5.2 → **1.5.3**
- ✅ **Enhanced PEM key escaping**: Improved support for Coinbase CDP private key formats
- ✅ **UI Navigation**: Fixed duplicate TopAppBar display issue with centralized configuration

**Security Improvements:**
- ✅ **JWT Authentication**: Fixed authentication issues that could cause connection failures
- ✅ **Error Handling**: Improved error handling and debugging capabilities for API connections  
- ✅ **PEM Processing**: Strengthened PEM key processing with better escape sequence handling

**Release Notes Updated:**
- ✅ **release-notes.txt**: Updated to reflect v1.5.3 improvements including enhanced security key parsing, fixed JWT authentication issues, improved error handling, resolved navigation issues, and strengthened PEM key processing

### ❌ What's MISSING (Next Phase)

**Phase 2: Complete REST API Client + Decision Engine (HIGHEST PRIORITY)**

| Ticket | Component | Status | Effort | Priority |
|--------|-----------|--------|---------|----------|
| **13** | **Full REST API Client** | ❌ TODO | Large | CRITICAL |
| **05** | **Decision Engine** | ❌ TODO | Large | HIGH |
| **06** | **Risk Manager** | ❌ TODO | Medium | HIGH |

**Ticket 13 - Full REST API Client** (BLOCKING ALL TRADING):
```kotlin
// MISSING: Complete CoinbaseRepository implementation
class CoinbaseRepository {
    // ✅ DONE: getBalances() - returns live account balances
    // ❌ TODO: getCandles() - fetch OHLCV data for analysis  
    // ❌ TODO: getCurrentPrice() - real-time BTC price
    // ❌ TODO: placeLimitOrder() - grid trading orders
    // ❌ TODO: placeMarketOrder() - emergency liquidation
    // ❌ TODO: placeBracketOrder() - trend trading (entry + TP + SL)
    // ❌ TODO: cancelOrder() - cancel single order
    // ❌ TODO: cancelOrders() - cancel multiple orders  
    // ❌ TODO: getOpenOrders() - check active orders
}
```

**Ticket 05 - Decision Engine** (CORE TRADING LOGIC):
```kotlin  
// MISSING: SMA/ADX/ATR analysis with ta4j
class TradingDecisionEngine {
    // ❌ TODO: SMA(200) - bull/bear trend filter
    // ❌ TODO: ADX(14) - trend strength measurement  
    // ❌ TODO: ATR(14) - volatility for position sizing
    // ❌ TODO: Regime switching (DEFENSE/TREND/RANGE)
    // ❌ TODO: Hysteresis logic (prevent whipsaws)
}
```

**Phase 3: Trading Service (24/7 EXECUTION)**

| Ticket | Component | Status |
|--------|-----------|--------|
| **15** | **Trading Service** | ❌ TODO |
| **16** | **Battery Optimization** | ❌ TODO |

### 💎 What's WORKING NOW

**Live Features (v1.5.3):**
- ✅ **Real Portfolio Data**: Dashboard shows actual Coinbase BTC/USD balances
- ✅ **Robust Authentication**: JWT tokens generated with enhanced security
- ✅ **Professional UI**: Complete dashboard with loading states, error handling, retry functionality
- ✅ **Database Persistence**: All trading data stored in Room database
- ✅ **Build Pipeline**: Automated APK builds with embedded credentials
- ✅ **Error Recovery**: Comprehensive error handling with user-friendly retry options

**Testing Status:**
- ✅ **API Connection**: Successfully connects to live Coinbase Advanced Trade API
- ✅ **JWT Generation**: ES256 tokens accepted by Coinbase
- ✅ **Account Access**: Live BTC/USD balance retrieval working
- ✅ **UI Stability**: No crashes, responsive design, professional UX

---

## Next Sprint: Complete REST API Client (Ticket 13)

### **🎯 Priority 1: Implement Missing API Methods**

**Goal:** Enable order placement and market data fetching

**Files to implement:**
```kotlin
// exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
├── api/CoinbaseApiClient.kt          # Add missing HTTP methods
├── dto/                              # Add DTOs for candles, orders
│   ├── CandleDto.kt                 # OHLCV response structure
│   ├── OrderDto.kt                  # Order placement request/response
│   └── ProductDto.kt                # Market data responses
├── mapper/                           # Add domain mappers  
│   ├── CandleMapper.kt              # CandleDto → Candle
│   └── OrderMapper.kt               # OrderDto → Order
└── repository/CoinbaseRepository.kt  # Complete implementation
```

**API Methods to Add:**
1. **Market Data:**
   - `getCandles(productId, granularity, limit)` → `List<Candle>`
   - `getCurrentPrice(productId)` → `Ticker`

2. **Order Management:**
   - `placeLimitOrder(productId, side, size, price)` → `Order`
   - `placeMarketOrder(productId, side, size)` → `Order` 
   - `placeBracketOrder(...)` → `Order`
   - `cancelOrder(orderId)` → `Unit`
   - `cancelOrders(orderIds)` → `Int`
   - `getOpenOrders(productId)` → `List<Order>`

**Testing Strategy:**
- Start with **getCandles()** (no risk, just market data)
- Then **small limit orders** ($10-20, far from market price)
- Test **order cancellation** immediately after placement
- **Never risk large amounts** during development

### **🧠 Priority 2: Decision Engine (Ticket 05)**

**Goal:** Implement SMA/ADX/ATR analysis with regime switching

**Files to create:**
```kotlin
// core/domain/src/main/kotlin/com/tradeflow/core/domain/
├── strategy/
│   ├── DecisionEngine.kt             # Interface
│   ├── TradingDecisionEngine.kt      # Implementation  
│   └── StrategyConfig.kt             # Configuration
└── indicator/                        # ta4j wrappers
    ├── SMACalculator.kt              # Simple Moving Average
    ├── ADXCalculator.kt              # Average Directional Index
    └── ATRCalculator.kt              # Average True Range
```

**Implementation approach:**
1. **Start simple:** SMA(200) bull/bear filter only
2. **Add ADX:** Trend strength measurement (>25 = trending)
3. **Add ATR:** Position sizing based on volatility  
4. **Add hysteresis:** Prevent rapid mode switching

**Validation:**
- **Unit tests** with known candle data
- **Historical backtesting** with 2023-2024 BTC data
- **Paper trading** before live deployment

### **⚖️ Priority 3: Risk Manager (Ticket 06)**

**Goal:** Prevent catastrophic losses

**Key components:**
- **Position sizing:** Max 2% risk per trade  
- **Drawdown monitoring:** Emergency stop at 15%
- **Order validation:** Prevent oversized orders
- **Emergency liquidation:** Market sell all BTC if needed

---

## Success Metrics

### Phase 2 Complete When:
- [ ] **getCandles()** fetches 350 H4 candles successfully
- [ ] **getCurrentPrice()** returns real-time BTC price  
- [ ] **placeLimitOrder()** places $20 test order at 50% below market
- [ ] **cancelOrder()** cancels test order within 30 seconds
- [ ] **Decision engine** correctly identifies DEFENSE/TREND/RANGE on historical data
- [ ] **Risk manager** prevents orders >2% of account value

### MVP Complete When:
- [ ] **Trading service** runs 24/7 without crashes
- [ ] **Live trading** with $100 for 1 week (monitoring only)
- [ ] **Break-even or better** performance after 30 days
- [ ] **Complete audit trail** of all trades for tax reporting

### Long-term Success:
- [ ] **3% monthly returns** sustained for 6 months
- [ ] **Account growth** from $500 → $1,000+ 
- [ ] **Automated operation** requiring <1 hour/week maintenance
- [ ] **Tax reporting** fully automated with CSV exports

---

## Risk Management

### Development Risks
- **API changes:** Coinbase updates breaking integration → Monitor API changelog
- **Market volatility:** 2025 election/regulation changes → Start very small ($100-500)
- **Technical complexity:** Over-engineering → Keep strategy simple (SMA/ADX only)

### Trading Risks  
- **Capital loss:** Bot makes bad decisions → 2% max per trade, 15% drawdown stop
- **Fee erosion:** High-frequency trading → Target 3-5 trades/day maximum
- **Overconfidence:** Early wins → Stick to position sizing rules always

### Personal Risks
- **Time investment:** 6+ months development → Treat as learning, not guaranteed income
- **Emotional attachment:** Watching trades 24/7 → Set rules and trust system
- **Tax compliance:** Complex crypto reporting → Keep detailed records from day 1

---

## Development Guidelines

### Code Quality Standards
- ✅ **100% build success** in CI/CD pipeline
- ✅ **Unit tests** for all domain logic (decision engine, risk manager)
- ✅ **Integration tests** for API clients (small real trades)
- ✅ **Error handling** for all network operations
- ✅ **Logging** for all trading decisions and outcomes

### Security Requirements  
- ✅ **Never log credentials** or API keys
- ✅ **Encrypted storage** for all sensitive data (using build-time injection)
- ✅ **HTTPS only** for all API communication
- ✅ **Input validation** for all user-entered data
- ✅ **Production safeguards** (max order size, daily loss limits)

### Documentation Standards
- ✅ **Keep CLAUDE.md updated** with every major change
- ✅ **Update roadmap.md** when phases complete  
- ✅ **Document all API integrations** with example requests/responses
- ✅ **Track all architectural decisions** and rationale
- ✅ **Maintain ticket status** in docs/tickets/ folders

---

This roadmap is a living document. Update status as features are implemented and priorities shift based on real-world testing results.
