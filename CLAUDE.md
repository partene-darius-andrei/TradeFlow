# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-09
**Project Status:** Phase 2 Complete - Core Trading Logic (v1.8.0)
**Current Build:** #31 SUCCESS

This is the entry point for Claude Code when working with TradeFlow. All essential context, navigation, and workflows are documented here.

---

## 🎯 Quick Navigation

| Document | Purpose | Use When |
|----------|---------|----------|
| **[docs/roadmap.md](docs/roadmap.md)** | Implementation roadmap organized in phases | Planning what to build next |
| **[docs/README.md](docs/README.md)** | Complete documentation index and ticket mapping | Finding specific documentation |
| **[docs/reference.md](docs/reference.md)** | Implementation blueprint with code examples | Implementing features |
| **[docs/ci-claude-integration.md](docs/ci-claude-integration.md)** | CI/CD with Claude API integration | Understanding build pipeline |
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

### What EXISTS (Phases 1 & 2 COMPLETE - v1.8.0)

```
✅ Modern Android app structure
✅ Hilt dependency injection configured
✅ Room database with complete schema (4 entities + 4 DAOs)
✅ Ktor HTTP client configured (OkHttp engine)
✅ Timber logging initialized
✅ Firebase Analytics + Crashlytics
✅ All trading dependencies added (ta4j, nimbus-jose-jwt, security-crypto)
✅ GitHub Actions CI/CD pipeline with Claude API integration
✅ Adaptive app icon with trading chart design (day/night variants)

✅ DOMAIN LAYER COMPLETE (v1.8.0):
✅ core/domain/src/main/kotlin/com/tradeflow/core/domain/
    ├── auth/
    │   ├── AuthTokenProvider.kt        ✅ Token generation interface
    │   └── CredentialStore.kt          ✅ Secure credential storage interface
    ├── error/
    │   └── ExchangeError.kt           ✅ Exchange error types (6 variants)
    ├── model/ ← COMPLETE
    │   ├── Candle.kt                  ✅ OHLCV + Granularity enum (9 timeframes)
    │   ├── Order.kt                   ✅ Order model + Side/Type/Status enums
    │   ├── Decision.kt                ✅ Enhanced sealed class with technical indicators (Wait/Defense/Trend/Range)
    │   ├── Portfolio.kt               ✅ Portfolio snapshot model + utility extensions
    │   ├── Balance.kt                 ✅ Account balance model
    │   └── Ticker.kt                  ✅ Real-time price ticker
    ├── repository/
    │   ├── BracketOrderRepository.kt   ✅ Bracket order support interface
    │   ├── ExchangeRepository.kt       ✅ Core exchange operations (12 methods)
    │   ├── ExchangeWebSocket.kt        ✅ Real-time data streams
    │   └── TradingDataRepository.kt    ✅ Local trading data queries (NEW in v1.8.0)
    ├── strategy/ ← COMPLETE
    │   ├── DecisionEngine.kt           ✅ Decision engine interface
    │   ├── TradingDecisionEngine.kt    ✅ Complete regime-switching implementation with hysteresis + DI
    │   └── StrategyConfig.kt           ✅ Comprehensive strategy parameters
    ├── indicator/ ← COMPLETE with DI
    │   ├── SMACalculator.kt            ✅ Simple Moving Average with ta4j integration + @Inject
    │   ├── ADXCalculator.kt            ✅ Average Directional Index with ta4j integration + @Inject
    │   └── ATRCalculator.kt            ✅ Average True Range with ta4j integration + @Inject
    ├── risk/
    │   └── RiskManager.kt              ✅ Risk management interface with enhanced types + @Inject
    └── usecase/ ← COMPLETE IMPLEMENTATION (v1.8.0)
        ├── ExecuteDecisionUseCase.kt   ✅ Trading decision execution orchestrator
        ├── ExecuteTradingCycleUseCase.kt ✅ Complete trading cycle with risk management
        ├── HandleEmergencyUseCase.kt   ✅ Emergency liquidation handler
        ├── HandleGridFillsUseCase.kt   ✅ Grid order fill detection and profit taking (NEW)
        ├── ManageGridOrdersUseCase.kt  ✅ Grid order management for range trading
        ├── ManageOrdersUseCase.kt      ✅ Order lifecycle and reconciliation
        ├── UpdatePortfolioUseCase.kt   ✅ Portfolio state updates
        └── model/
            ├── ExecutionResult.kt      ✅ Use case result types (Success/Skipped/Failed)
            └── TradingContext.kt       ✅ Trading context data model

✅ DATA LAYER COMPLETE (v1.8.0):
✅ core/data/src/main/kotlin/com/tradeflow/core/data/
    ├── local/ ← COMPLETE
    │   ├── entity/
    │   │   ├── CandleEntity.kt        ✅ Room entity for candles
    │   │   ├── OrderEntity.kt         ✅ Room entity for orders + getRecentFilledOrders()
    │   │   ├── DecisionEntity.kt      ✅ Room entity for decisions
    │   │   └── PortfolioSnapshotEntity.kt ✅ Room entity for portfolio
    │   ├── dao/
    │   │   ├── CandleDao.kt           ✅ CRUD + delete old candles
    │   │   ├── OrderDao.kt            ✅ CRUD + query by status/product + filled orders
    │   │   ├── DecisionDao.kt         ✅ CRUD + latest decision query
    │   │   └── PortfolioDao.kt        ✅ CRUD + snapshot history
    │   └── database/
    │       └── EngineDatabase.kt       ✅ Room database with all 4 entities
    ├── security/
    │   └── StaticCredentialStore.kt    ✅ Static credential injection (replaces UI input)
    ├── mapper/
    │   └── OrderMapper.kt              ✅ OrderEntity ↔ Order domain model mapping (NEW)
    ├── repository/
    │   └── TradingDataRepositoryImpl.kt ✅ Implementation for local data queries (NEW)
    └── di/
        ├── SecurityModule.kt           ✅ Static credential DI binding
        ├── DatabaseModule.kt           ✅ Room database DI
        └── RepositoryModule.kt         ✅ TradingDataRepository DI binding (NEW + PortfolioRepository)

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
    ├── presentation/dashboard/
    │   ├── DashboardScreen.kt          ✅ Complete implementation with ENHANCED real data integration
    │   ├── DashboardViewModel.kt       ✅ Full state management + ROBUST error handling + loading states
    │   └── components/
    │       ├── PortfolioCard.kt        ✅ Live data, BTC/USD balances with "Live Data" indicator + enhanced formatting
    │       ├── ModeCard.kt             ✅ Trading mode + current price
    │       ├── ServiceCard.kt          ✅ Service status + start/stop button
    │       └── OrdersList.kt           ✅ Recent orders + empty state
    └── di/
        └── DomainModule.kt             ✅ DecisionEngine DI binding (NEW in v1.8.0)

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

### 🎉 Major Milestone: Complete Trading Engine Implementation (v1.8.0)

**Phase 2 has been successfully completed with comprehensive trading engine and use case implementation:**

🆕 **Complete Use Case Layer Implementation (v1.8.0):**
- ✅ **ExecuteDecisionUseCase** - Complete decision execution orchestrator
  - Handles all 4 trading modes (DEFENSE/TREND/RANGE/WAIT)
  - Risk validation before order placement
  - Bracket order support for trend trading
  - Grid order management for range trading
  - Comprehensive duplicate prevention

- ✅ **ExecuteTradingCycleUseCase** - Master trading orchestrator
  - Portfolio updates with high water mark tracking
  - Drawdown monitoring with emergency liquidation at 15%
  - Complete trading cycle with safety-first design
  - Grid fill handling integration
  - Order reconciliation and management

- ✅ **HandleEmergencyUseCase** - Emergency liquidation handler
  - Complete order cancellation for emergency situations
  - BTC position liquidation with market orders
  - Portfolio protection during extreme drawdown
  - Robust error handling for critical scenarios

- ✅ **HandleGridFillsUseCase** - Grid trading profit optimization
  - Automatic profit-taking when grid orders fill
  - Dynamic rebalancing of grid positions
  - Fill detection with portfolio integration
  - Enhanced grid trading profitability

- ✅ **ManageGridOrdersUseCase** - Range trading implementation
  - Dynamic grid spacing based on ATR and minimum requirements
  - Risk-validated grid position sizing
  - Intelligent grid level management
  - Integration with portfolio constraints

- ✅ **ManageOrdersUseCase** - Order lifecycle management
  - Order reconciliation between local and exchange state
  - Status synchronization and error recovery
  - Comprehensive order tracking and updates

- ✅ **UpdatePortfolioUseCase** - Portfolio state management
  - Multi-currency balance tracking (BTC/USD)
  - High water mark calculation and persistence
  - Drawdown percentage monitoring
  - Portfolio snapshot history

🆕 **Enhanced Domain Models (v1.8.0):**
- ✅ **ExecutionResult sealed class** - Standardized use case results (Success/Skipped/Failed)
- ✅ **TradingContext data model** - Complete trading state context
- ✅ **Portfolio utility extensions** - getBtcBalance() function for easy balance access
- ✅ **PortfolioRepositoryImpl** - High water mark tracking and portfolio snapshots

🆕 **Comprehensive Unit Testing (v1.8.0):**
- ✅ **ExecuteDecisionUseCaseTest** - All decision modes, risk validation, error handling
- ✅ **HandleEmergencyUseCaseTest** - Emergency scenarios, partial failures, edge cases
- ✅ **ManageGridOrdersUseCaseTest** - Grid spacing, risk integration, partial success
- ✅ **MockK integration** - Isolated unit tests with comprehensive mocking
- ✅ **Edge case coverage** - Error conditions, boundary values, failure scenarios

🆕 **Enhanced Testing Framework (v1.8.0):**
- ✅ **RiskManagerTest** - Portfolio creation with BTC price integration
- ✅ **MockK 1.14.7** - Professional mocking framework for isolated testing
- ✅ **Comprehensive test coverage** - All use cases with success/failure scenarios

### What's MISSING (Phase 3 - API Integration)

```
❌ exchange/coinbase/ - PARTIAL IMPLEMENTATION
    ├── api/
    │   └── CoinbaseApiClient.kt        ❌ Needs: Order placement, candle fetching, order management
    ├── repository/
    │   └── CoinbaseRepository.kt       ❌ Needs: Full ExchangeRepository implementation
    └── websocket/
        └── CoinbaseWebSocket.kt        ❌ Missing: Real-time price and order updates

❌ service/trading/
    └── TradingService.kt              ❌ Missing: 24/7 foreground service orchestration

❌ Testing and Validation
    ├── Integration tests              ❌ Missing: End-to-end API testing
    └── Live system validation        ❌ Missing: 24-hour system test
```

---

## 🏗️ Tech Stack

| Component | Library/Tool | Version | Status | Usage |
|-----------|--------------|---------|--------|-------|
| **Language** | Kotlin | 2.3.0 | ✅ Active | App language |
| **Build System** | Gradle | 8.13 | ✅ Active | Build automation |
| **UI Framework** | Jetpack Compose | BOM 2025.12.01 | ✅ Active | Modern UI |
| **Dependency Injection** | Hilt | 2.57.2 | ✅ Active | DI framework |
| **Database** | Room | 2.8.4 | ✅ Active | Local persistence |
| **HTTP Client** | Ktor | 3.3.3 | ✅ Active | REST API calls |
| **JSON** | kotlinx.serialization | 1.8.0 | ✅ Active | JSON parsing |
| **JWT** | nimbus-jose-jwt | 9.47 | ✅ Active | ES256 JWT signing |
| **Cryptography** | BouncyCastle | 1.78 | ✅ Active | Advanced PEM parsing |
| **Technical Analysis** | ta4j-core | 0.16 | ✅ Active | SMA/ADX/ATR indicators |
| **Testing** | MockK | 1.14.7 | ✅ Active | Mocking framework |
| **Testing** | kotlin-test | 2.1.0 | ✅ Active | Testing assertions |
| **Logging** | Timber | 5.0.1 | ✅ Active | Logging framework |
| **Charts** | Vico | 2.4.0 | ⏳ Ready | Chart visualization |
| **Work Manager** | WorkManager | 2.11.0 | ⏳ Ready | Background tasks |
| **Security** | security-crypto | 1.1.0-alpha06 | ⏳ Ready | Encrypted storage |

**Legend:**
- ✅ **Active** - Currently used in implemented features
- ⏳ **Ready** - Configured and ready for implementation
- ❌ **Missing** - Not yet implemented

---

## 🔧 Dependencies

### Build Dependencies

```kotlin
// Core Android
implementation("androidx.core:core-ktx:1.15.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
implementation("androidx.activity:activity-compose:1.9.3")

// Compose BOM (Bill of Materials)
implementation(platform("androidx.compose:compose-bom:2025.12.01"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.5")

// Hilt DI
implementation("com.google.dagger:hilt-android:2.57.2")
kapt("com.google.dagger:hilt-compiler:2.57.2")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Room Database  
implementation("androidx.room:room-runtime:2.8.4")
implementation("androidx.room:room-ktx:2.8.4")
ksp("androidx.room:room-compiler:2.8.4")

// Ktor HTTP Client
implementation("io.ktor:ktor-client-core:3.3.3")
implementation("io.ktor:ktor-client-okhttp:3.3.3")
implementation("io.ktor:ktor-client-websockets:3.3.3")
implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")

// Kotlinx Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

// JWT & Cryptography
implementation("com.nimbusds:nimbus-jose-jwt:9.47")
implementation("org.bouncycastle:bcprov-jdk18on:1.78")
implementation("org.bouncycastle:bcpkix-jdk18on:1.78")

// Technical Analysis
implementation("org.ta4j:ta4j-core:0.16")

// Testing
testImplementation("io.mockk:mockk:1.14.7")
testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.0")
testImplementation("junit:junit:4.13.2")

// Android Testing
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

### CI/CD Dependencies

```yaml
# .github/workflows/build.yml secrets
COINBASE_API_KEY: "organizations/{org_id}/apiKeys/{key_id}"
COINBASE_API_SECRET: "-----BEGIN EC PRIVATE KEY----- ... -----END EC PRIVATE KEY-----"
ANTHROPIC_API_KEY: "sk-ant-api03-..."
FIREBASE_SERVICE_ACCOUNT_JSON: '{"type": "service_account", ...}'
```

---

## 📋 Development Workflow

### 1. Current Claude Code Workflow

```
1. Claude Code implements features locally
2. Push to claude/* branch
3. GitHub Actions runs with credentials
4. Claude API auto-updates documentation  
5. Firebase App Distribution for testing
6. Claude Code pulls result and continues
```

### 2. File Organization

**Project follows clean architecture:**

```
TradeFlow/
├── app/                           # Application layer (DI + credentials)
├── core/
│   ├── domain/                   # ✅ Pure Kotlin business logic (NO Android deps)
│   ├── data/                     # ✅ Data layer (Room + repositories)
│   └── ui/                       # ✅ Shared UI components
├── exchange/
│   └── coinbase/                 # ✅ Coinbase API integration (isolated)
├── feature/                      # 🚧 Feature modules (future)
└── docs/                         # ✅ Complete documentation
```

### 3. Testing Strategy

```
Unit Tests:     ✅ Use case layer with MockK (isolated)
Integration:    ❌ API testing with small real trades (Phase 3)
End-to-End:     ❌ 24-hour live system validation (Phase 3)
```

---

## 🚨 Critical Notes for Claude

### API Rate Limits & Constraints

| API | Limit | Impact |
|-----|-------|--------|
| **Coinbase REST** | 10,000 requests/hour | Use WebSocket for real-time |
| **Coinbase WebSocket** | 750 connections/second | One connection sufficient |
| **Claude API** | Varies | Used for docs auto-update |

### Fee Structure Impact

| Order Type | Coinbase Fee | Min Profit Needed |
|------------|--------------|-------------------|
| **Maker** (post_only) | 0.60% | 1.5% grid spacing minimum |
| **Taker** (market) | 1.20% | Emergency only |

### Trading Constraints

| Constraint | Value | Reason |
|------------|-------|--------|
| **Risk per trade** | 1-2% of account | Position sizing limit |
| **BTC only** | Until $2,500+ account | Altcoin fees too high |
| **Drawdown limit** | 15% from peak | Emergency liquidation |
| **Grid spacing** | 1.5% minimum | Fee break-even requirement |

---

## 🎯 Next Implementation Phase (Phase 3)

**Priority order for completing trading system:**

### 1. Full REST API Client (Ticket 13) - HIGH

```kotlin
// exchange/coinbase/api/CoinbaseApiClient.kt extensions needed:
suspend fun placeOrder(order: OrderRequest): Result<Order>          // For ExecuteDecisionUseCase
suspend fun cancelOrders(orderIds: List<String>): Result<Int>       // For HandleEmergencyUseCase  
suspend fun getCandles(productId: String, granularity: String, limit: Int): Result<List<Candle>> // For TradingDecisionEngine
suspend fun getProducts(): Result<List<Product>>                    // For product info
```

### 2. WebSocket Client (Ticket 14) - HIGH  

```kotlin
// exchange/coinbase/websocket/CoinbaseWebSocket.kt needed:
fun subscribeTicker(productIds: List<String>): Flow<Ticker>         // For real-time prices
fun subscribeOrderUpdates(): Flow<Order>                           // For order status sync
```

### 3. Trading Service (Ticket 17) - HIGH

```kotlin
// service/trading/TradingService.kt needed:
class TradingService : Service() {
    // Orchestrate ExecuteTradingCycleUseCase every 15 minutes
    // Monitor WebSocket for price updates
    // Handle emergency liquidation on 15% drawdown
}
```

### 4. Integration Testing (Ticket 19) - MEDIUM

```kotlin
// Test complete flow with small real trades (~$10)
// Validate use cases work with real API
// 24-hour stability testing
```

---

## 💡 Quick Commands

```bash
# Build project
./gradlew build

# Run unit tests  
./gradlew test

# Check current status
cat .build-status

# View recent commits
git log --oneline -10
```

---

## 📚 Documentation Quick Links

- **Complete roadmap:** [docs/roadmap.md](docs/roadmap.md)
- **API reference:** [docs/api/coinbase.md](docs/api/coinbase.md)  
- **Trading strategy:** [docs/strategy/overview.md](docs/strategy/overview.md)
- **CI/CD workflows:** [docs/ci-claude-integration.md](docs/ci-claude-integration.md)
- **Implementation status:** [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md)
