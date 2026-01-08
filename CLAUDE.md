# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-08
**Project Status:** Phase 1 Ready to Start - Business Logic
**Current Build:** #30 SUCCESS

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

### What EXISTS (Phases 0A & 0B: COMPLETE)

```
✅ Modern Android app structure
✅ Hilt dependency injection configured
✅ Room database with complete schema (4 entities + 4 DAOs) ← NEW
✅ Ktor HTTP client configured (OkHttp engine)
✅ Timber logging initialized
✅ Firebase Analytics + Crashlytics
✅ All trading dependencies added (ta4j, nimbus-jose-jwt, security-crypto)
✅ GitHub Actions CI/CD pipeline with environment credentials
✅ Adaptive app icon with trading chart design (day/night variants)

🆕 DOMAIN LAYER COMPLETE:
✅ core/domain/src/main/kotlin/com/tradeflow/core/domain/
    ├── auth/
    │   ├── AuthTokenProvider.kt        ✅ Token generation interface
    │   └── CredentialStore.kt          ✅ Secure credential storage interface
    ├── error/
    │   └── ExchangeError.kt           ✅ Exchange error types (6 variants)
    ├── model/ ← NEW: Ticket 01 COMPLETE
    │   ├── Candle.kt                  ✅ OHLCV + Granularity enum (9 timeframes)
    │   ├── Order.kt                   ✅ Order model + Side/Type/Status enums
    │   ├── Decision.kt                ✅ Sealed class (Wait/Defense/Trend/Range)
    │   ├── Portfolio.kt               ✅ Portfolio snapshot model
    │   ├── Balance.kt                 ✅ Account balance model
    │   └── Ticker.kt                  ✅ Real-time price ticker
    └── repository/
        ├── BracketOrderRepository.kt   ✅ Bracket order support interface
        ├── ExchangeRepository.kt       ✅ Core exchange operations (12 methods)
        └── ExchangeWebSocket.kt        ✅ Real-time data streams

🆕 DATA LAYER COMPLETE:
✅ core/data/src/main/kotlin/com/tradeflow/core/data/
    ├── local/ ← NEW: Ticket 03 COMPLETE
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

🆕 COINBASE AUTH COMPLETE:
✅ exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
    ├── auth/
    │   └── CoinbaseJwtGenerator.kt     ✅ ES256 JWT token generation
    └── di/
        └── AuthModule.kt               ✅ JWT generator DI binding

🆕 UI COMPONENTS COMPLETE:
✅ core/ui/src/main/kotlin/com/tradeflow/core/ui/
    ├── component/
    │   ├── ErrorDisplay.kt             ✅ Error state UI with retry button
    │   ├── LoadingButton.kt            ✅ Button with loading spinner
    │   ├── ModeIndicator.kt            ✅ Trading mode visual indicator
    │   ├── PriceDisplay.kt             ✅ Price with color coding (+/-)
    │   └── StatusCard.kt               ✅ Reusable card container
    └── extension/
        └── BigDecimalExt.kt           ✅ Currency/percentage formatting

🆕 DASHBOARD SCREEN COMPLETE:
✅ app/src/main/java/com/dpart/tradeflow/presentation/dashboard/
    ├── DashboardScreen.kt              ✅ Main screen with TopAppBar + scrollable layout
    └── components/
        ├── PortfolioCard.kt            ✅ Portfolio value + asset breakdown
        ├── ModeCard.kt                 ✅ Trading mode + current price
        ├── ServiceCard.kt              ✅ Service status + start/stop button
        └── OrdersList.kt               ✅ Recent orders + empty state

🆕 APP BRANDING COMPLETE:
✅ Adaptive app icon with trading chart design
✅ Day/night background variants (white/black)
✅ Android 8.0+ adaptive icon support
✅ Proper launcher configuration

🆕 CREDENTIALS SYSTEM COMPLETE:
✅ app/build.gradle.kts                 ✅ Build-time credential injection
✅ app/src/main/java/com/dpart/tradeflow/di/
    └── CredentialsModule.kt            ✅ Provides credentials from BuildConfig
```

### Credential Management System

**New Approach:** Static credentials injected at build time, removing need for UI credential entry.

**Configuration (Priority Order):**
1. **Environment Variables** (CI/CD): `COINBASE_API_KEY`, `COINBASE_API_SECRET`
2. **Local Properties** (Dev): Add to `local.properties`:
   ```properties
   coinbase.api.key=organizations/your-org/apiKeys/your-key
   coinbase.api.secret=your-private-key-pem
   ```

**Build Integration:**
- `app/build.gradle.kts` injects credentials into `BuildConfig`
- `CredentialsModule` provides via Hilt DI
- `StaticCredentialStore` returns injected credentials
- No UI credential input needed

### What DOESN'T Exist Yet (Next Up)

```
✅ Domain models - Ticket 01 ✅ DONE
✅ Room database - Ticket 03 ✅ DONE
✅ UI Foundation - Tickets 05-09 ✅ DONE
✅ Dashboard screen - Ticket 10 ✅ DONE (UI skeleton with mock data)
❌ Coinbase REST API client - Ticket 13 ← NEXT for testing auth
❌ Decision engine (regime switching logic) - Ticket 15
❌ Risk manager - Ticket 16
❌ Backtest validation - Phase 1B
❌ Coinbase WebSocket - Ticket 14
❌ Settings screen - Ticket 11 (refined, ready to implement)
❌ Trading service (foreground service) - Tickets 17-18
```

**Progress:** **11/20 tickets done (55% complete)**. Domain foundation, UI foundation, authentication, and dashboard UI complete. **Next up:** Ticket 13 (REST API Client) to enable testing Coinbase authentication and fetching real data.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0A: Domain Foundation (COMPLETE ✅)
- [x] **Modularization** (Ticket 00) ✅ COMPLETE - 8-module Clean Architecture
- [x] **Domain models** (Ticket 01) ✅ COMPLETE - Candle, Order, Decision, Portfolio, Balance, Ticker
- [x] **Repository interfaces** (Ticket 02) ✅ COMPLETE - Exchange contracts
- [x] **Room database** (Ticket 03) ✅ COMPLETE - 4 entities + 4 DAOs
- [x] **Credential storage** (Ticket 04) ✅ COMPLETE - Build-time static injection
- [x] **JWT generator** (Ticket 07-JWT) ✅ COMPLETE - ES256 signing with proper nonce

### Phase 0B: UI Foundation (COMPLETE ✅)
- [x] **UI Design Overview** (Ticket 05) ✅ COMPLETE - Complete visual redesign
- [x] **Core UI Theme** (Ticket 06) ✅ COMPLETE - Material 3 theme + colors
- [x] **Core UI Components** (Ticket 07-UI) ✅ COMPLETE - ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard
- [x] **Login Screen** (Ticket 08) ✅ COMPLETE (obsolete - removed after credential change)
- [x] **App Navigation** (Ticket 09) 🔄 IN REVIEW - Simplified routing (Dashboard + Settings)
- [x] **Dashboard Screen** (Ticket 10) ✅ COMPLETE - UI skeleton with mock data

### Phase 1: Coinbase Integration (CURRENT - Ready to Test Auth)
- [ ] **REST API Client** (Ticket 13) ❌ **NEXT** - Order placement, market data, candles
- [ ] **WebSocket Client** (Ticket 14) ❌ PENDING - Real-time price feeds, order updates

**Current Focus:** Ticket 13 - REST API Client to test Coinbase authentication and fetch real data

### Phase 2: Business Logic
- [ ] **Decision Engine** (Ticket 15) ❌ PENDING - SMA(200), ADX(14), ATR(14) + regime switching
- [ ] **Risk Manager** (Ticket 16) ❌ PENDING - Position sizing, exposure limits, drawdown monitoring

### Phase 2B: Strategy Validation (After Phase 2)
- Backtesting framework with 7-year historical BTC/USDT data
- Strategy validation (52%+ win rate, 1.0+ Sharpe ratio minimum)
- Go/No-Go decision before live trading

### Phase 3: Presentation Layer (In Progress)
- [x] **Dashboard Screen** (Ticket 10) ✅ COMPLETE - UI skeleton with mock data
- [ ] **Settings Screen** (Ticket 11) ❌ PENDING - Preferences, app info

### Phase 4: Trading Service
- **Trading Service** (Ticket 17) - Foreground service orchestration
- **Battery Optimization** (Ticket 18) - Doze exemption, wake locks

### Phase 5: Testing & Validation
- **Integration Tests** (Ticket 19) - Real API with small trades
- **MVP Milestone** (Ticket 20) - Complete system validation

**Current Phase:** Phase 1 - Coinbase Integration (11/20 tickets done, 55% complete)
**Progress:** Domain, UI foundations, and Dashboard UI complete. Next: Ticket 13 (REST API Client) to test Coinbase authentication and fetch real data.

---

## 🏗️ Architecture Overview

### Domain-First Architecture

**Core principle:** Domain layer defines contracts, infrastructure implements them.

```
┌─────────────────────────────────────────┐
│                :app                     │  ← DI wiring + Credential injection ✅
│  🆕 di/CredentialsModule.kt            │
│  - Provides API key from BuildConfig   │
│  - Provides API secret from BuildConfig│
│  🆕 Adaptive app icon (ic_launcher)    │
│  - Trading chart design                │
│  - Day/night background variants       │
└─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────┐
│           :core:domain ✅               │  ← Domain contracts (pure Kotlin)
│  ✅ Interfaces (Ticket 02):            │
│    - ExchangeRepository                │
│    - BracketOrderRepository            │
│    - ExchangeWebSocket                 │
│    - AuthTokenProvider                 │
│    - CredentialStore                   │
│    - ExchangeError sealed class        │
│  ✅ Models (Ticket 01):                │
│    - Candle, Order, Decision           │
│    - Portfolio, Balance, Ticker        │
└─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────┐
│  :exchange:coinbase (auth only) ✅      │  ← Coinbase implementation
│  - CoinbaseJwtGenerator ✅ ES256       │
│  - AuthModule ✅ DI binding            │
│  ❌ CoinbaseRepository (REST methods)  │  ← Next to implement
│  ❌ CoinbaseWebSocket                  │
└─────────────────────────────────────────┘
```

**Key insight:** Phases 0A & 0B complete (10/20 tickets, 50%). Domain foundation and UI foundation done. Next critical blocker: Decision engine (Ticket 15) for regime-switching logic.

---

## 🔧 Tech Stack

### Dependencies Status

| Library | Version | Status | Purpose |
|---------|---------|---------|---------|
| **Kotlin** | 2.3.0 | ✅ Active | Language |
| **Compose BOM** | 2025.12.01 | ✅ Active | UI framework |
| **Hilt** | 2.57.2 | ✅ Active | Dependency injection |
| **Room** | 2.8.4 | ✅ Ready | Local database |
| **Ktor** | 3.3.3 + OkHttp | ✅ Active | HTTP client |
| **Timber** | 5.0.1 | ✅ Active | Logging |
| **Vico** | 2.4.0 | ✅ Ready | Charts for UI |
| **Coroutines** | 1.10.2 | ✅ Active | Async operations |
| **nimbus-jose-jwt** | 9.47 | ✅ Active | JWT ES256 signing |
| **ta4j-core** | 0.16 | ✅ Ready | Technical analysis indicators |
| **security-crypto** | 1.1.0-alpha06 | ❌ Unused | Replaced by static credentials |
| **work-runtime-ktx** | 2.10.0 | ✅ Ready | Background tasks |
| **datastore-preferences** | 1.1.1 | ✅ Ready | Settings persistence |
| **material-icons-extended** | Via BOM | ✅ Active | Icons for ModeIndicator |
| **Firebase** | BOM 34.7.0 | ✅ Active | Analytics + Crashlytics |

**Note:** `security-crypto` dependency still exists but StaticCredentialStore doesn't use it (credentials come from BuildConfig).

---

## 🛠️ Development Workflow

### For Claude Code (Mobile/Web)

**Primary Pattern:** Push → Actions Build → Pull Results

```bash
# 1. Implement feature
git add . && git commit -m "Add domain models" && git push

# 2. Wait for Actions (~3-5 minutes)
# GitHub Actions will:
# - Inject credentials from secrets
# - Build APK with embedded credentials
# - Upload to Firebase App Distribution
# - Update documentation via Claude API
# - Commit status back

# 3. Pull results
git pull

# 4. Check build status
cat .build-status
# Outputs: SUCCESS or FAILURE

# If failed:
cat build-log.txt
# Shows last 200 lines of build output

# 5. Test APK on device
# Download from Firebase App Distribution email
# Or download from GitHub Actions artifacts
```

**Benefits:**
- ✅ **No local Gradle needed** - Perfect for mobile development
- ✅ **Real credentials injected** - APK works with live Coinbase API
- ✅ **Immediate device testing** - Firebase distributes to registered devices
- ✅ **Auto-documentation** - Claude API updates docs based on code changes
- ✅ **Commit-back pattern** - Results available via git pull

### Credential Configuration

**For CI/CD (GitHub Actions):**
1. Go to repo → Settings → Secrets and variables → Actions
2. Add secrets:
   - `COINBASE_API_KEY=organizations/your-org/apiKeys/your-key`
   - `COINBASE_API_SECRET=-----BEGIN EC PRIVATE KEY-----...`
   - `ANTHROPIC_API_KEY=your-claude-api-key` (for auto-docs)

**For local development:**
1. Create `local.properties`:
   ```properties
   coinbase.api.key=organizations/your-org/apiKeys/your-key
   coinbase.api.secret=-----BEGIN EC PRIVATE KEY-----...
   ```
2. Run `./gradlew assembleDebug`

---

## 📚 Quick Code Patterns

### JWT Token Generation (Complete ✅)
```kotlin
// exchange/coinbase/auth/CoinbaseJwtGenerator.kt
class CoinbaseJwtGenerator @Inject constructor(
    private val credentialStore: CredentialStore
) : AuthTokenProvider {
    override suspend fun generateRestToken(method: String, path: String): String {
        val header = mapOf(
            "alg" to "ES256",
            "typ" to "JWT", 
            "kid" to credentialStore.getApiKey(),
            "nonce" to generateNonce()
        )
        // ... ES256 signing implementation
    }
}
```

### Static Credentials (Complete ✅)
```kotlin
// core/data/security/StaticCredentialStore.kt
class StaticCredentialStore @Inject constructor() : CredentialStore {
    override suspend fun getApiKey(): String? = BuildConfig.COINBASE_API_KEY.takeIf { it.isNotBlank() }
    override suspend fun getSecret(): String? = BuildConfig.COINBASE_API_SECRET.takeIf { it.isNotBlank() }
    override suspend fun hasCredentials(): Boolean = 
        !BuildConfig.COINBASE_API_KEY.isNullOrBlank() && 
        !BuildConfig.COINBASE_API_SECRET.isNullOrBlank()
}
```

### UI Components (Complete ✅)
```kotlin
// Example: Using existing components
@Composable
fun PortfolioSection() {
    StatusCard(title = "Portfolio") {
        PriceDisplay(
            price = BigDecimal("50000.00"),
            previousPrice = BigDecimal("49500.00") // Shows green
        )
        ModeIndicator(mode = TradingMode.TREND) // Shows green trend icon
    }
}
```

---

## ❗ Known Issues & Limitations

### Current Blockers

1. ✅ ~~No domain models yet~~ - Ticket 01 COMPLETE ✅
2. ✅ ~~Room database empty~~ - Ticket 03 COMPLETE ✅
3. ✅ ~~Dashboard UI missing~~ - Ticket 10 COMPLETE ✅
4. **JWT tokens untested** - Ticket 13 NEXT: Implement REST client to verify authentication works
5. **Decision engine missing** - Ticket 15: Implement SMA/ADX/ATR indicators with regime switching
6. **Risk manager missing** - Ticket 16: Position sizing, stop-loss, drawdown limits
7. **No trading logic** - Service implementation pending (Tickets 17-18)

### Dependencies Ready But Unused

- **ta4j-core:** Ready for technical indicators (SMA, ADX, ATR) - Needed for Ticket 05
- ✅ ~~Room~~ - NOW ACTIVE: 4 entities + 4 DAOs implemented (Ticket 03 complete)
- **Vico:** Chart library ready for portfolio graphs
- **WorkManager:** Ready for background task scheduling
- **DataStore:** Ready for settings persistence

### Design Decisions Made

1. **Static credentials over UI input** - Simplifies UX, better for CI/CD
2. **Ktor over Retrofit** - Already configured, good Kotlin integration  
3. **Domain-first architecture** - Clean separation, easy to test
4. **Build-time configuration** - No runtime credential management needed

---

## 📞 Integration Points

### External APIs

| API | Purpose | Status | Implementation |
|-----|---------|---------|----------------|
| **Coinbase Advanced Trade** | Order execution, market data | ✅ Auth ready | JWT complete, REST methods next |
| **Firebase** | Analytics, crash reporting | ✅ Active | Build metrics collection |
| **Claude API** | Auto-documentation | ✅ Active | Updates docs on code changes |

### System Integration

| System | Purpose | Status |
|---------|---------|---------|
| **Android Keystore** | Not used (static credentials) | ❌ Unused |
| **Foreground Service** | 24/7 trading loop | ❌ Not implemented |
| **Battery Optimization** | Doze survival | ❌ Not implemented |
| **Notifications** | Trade alerts | ❌ Not implemented |

---

**Next Steps:** Implement REST API Client (Ticket 13) in `:exchange:coinbase` module to test Coinbase authentication and enable fetching real market data. See `coinbase-api-test-plan.md` for implementation options.
