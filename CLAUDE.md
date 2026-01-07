# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (4/6 Complete)
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

---

## 🚦 Current Status

### What EXISTS (Phase 0A Progress: 4/6 Complete)

```
✅ Modern Android app structure
✅ Hilt dependency injection configured
✅ Room database with empty schema
✅ Ktor HTTP client configured (OkHttp engine)
✅ Timber logging initialized
✅ Firebase Analytics + Crashlytics
✅ All trading dependencies added (ta4j, nimbus-jose-jwt, security-crypto)
✅ GitHub Actions CI/CD pipeline

🆕 DOMAIN LAYER COMPLETE:
✅ core/domain/src/main/kotlin/com/tradeflow/core/domain/
    ├── auth/
    │   ├── AuthTokenProvider.kt        ✅ Token generation interface
    │   └── CredentialStore.kt          ✅ Secure credential storage interface
    ├── error/
    │   └── ExchangeError.kt           ✅ Exchange error types (6 variants)
    └── repository/
        ├── BracketOrderRepository.kt   ✅ Bracket order support interface
        ├── ExchangeRepository.kt       ✅ Core exchange operations (12 methods)
        └── ExchangeWebSocket.kt        ✅ Real-time data streams

🆕 DATA LAYER COMPLETE:
✅ core/data/src/main/kotlin/com/tradeflow/core/data/
    ├── security/
    │   └── SecureCredentialStore.kt    ✅ EncryptedSharedPreferences implementation
    └── di/
        └── SecurityModule.kt           ✅ Credential store DI binding

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
```

### What DOESN'T Exist Yet (Next Up)

```
❌ Domain models (Candle, Decision, Order, Portfolio) - Ticket 01
❌ Room database entities/DAOs - Ticket 03
❌ Coinbase REST API client methods - Ticket 08 (JWT complete ✅, REST methods next)
❌ Decision engine (regime switching logic)
❌ Trading service (foreground service)
❌ Risk management
❌ UI beyond MainActivity
```

**Progress:** **Authentication infrastructure 95% complete + UI foundation** ✅ JWT token generation, secure credential storage, repository contracts, and reusable UI components all implemented. Ready to implement domain models, database schema, and REST API client methods.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0A: Authentication Infrastructure (CURRENT - 4/6 Complete)
- [x] **Repository interfaces** (Ticket 02) ✅ COMPLETE
- [x] **Secure credential storage** (Ticket 04) ✅ COMPLETE 
- [x] **JWT generator** (Ticket 07) ✅ COMPLETE - ES256 signing with proper nonce generation
- [x] **UI components** (Bonus) ✅ COMPLETE - ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard
- [ ] Domain models (Ticket 01) - Basic domain types
- [ ] Room database schema (Ticket 03) 
- [ ] REST API client methods (Ticket 08) - JWT ✅ done, REST endpoints next

**Latest Completion:** UI foundation components ✅
- **ErrorDisplay:** Error state UI with optional retry button
- **LoadingButton:** Button with loading spinner and text
- **ModeIndicator:** Trading mode badge (DEFENSE/TREND/RANGE/WAIT) with colors and icons
- **PriceDisplay:** Price with directional color coding (green up, red down)
- **StatusCard:** Reusable card container with optional title
- **BigDecimalExt:** Currency formatting (toCurrencyString, toPercentageString, toCryptoString)

**Next Up:** Domain models (Ticket 01) - Define Candle, Order, Portfolio, Decision types

### Phase 0B: Trading Logic (Week 2)
- Decision engine (SMA, ADX, ATR)
- Risk manager

### Phase 1: Coinbase Integration
- Complete REST API client (methods for order placement, market data)
- WebSocket client

### Phase 2: Presentation Layer
- UI theme and additional components
- Dashboard screen + ViewModel
- Settings screen + ViewModel
- App navigation

### Phase 3: Trading Service
- Foreground service
- Battery optimization

### Phase 4: Testing & Validation
- Integration tests
- MVP milestone

**Current Phase:** Phase 0A (Authentication Infrastructure)
**Progress:** 4/6 complete + UI foundation - Repository interfaces, credential store, JWT generation, UI components complete. Domain models and database schema next.

---

## 🏗️ Architecture Overview

### Domain-First Architecture

**Core principle:** Domain layer defines contracts, infrastructure implements them.

```
┌─────────────────────────────────────────┐
│                :app                     │  ← DI wiring only
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            :feature:*                   │  ← UI & ViewModels
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│           :core:domain                  │  ← Interfaces & Models
│  🆕 COMPLETE: Repository Interfaces     │     (Pure Kotlin - No Android)
│  - ExchangeRepository (12 methods)     │
│  - BracketOrderRepository              │
│  - ExchangeWebSocket (real-time)       │
│  - AuthTokenProvider                   │
│  - CredentialStore                     │
│  - ExchangeError (6 error types)       │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│           :core:data                    │  ← Data layer implementations
│  🆕 COMPLETE: Secure Credential Store  │
│  - SecureCredentialStore (AES-256)     │
│  - SecurityModule (DI)                 │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│           :core:ui                      │  ← UI layer implementations
│  🆕 COMPLETE: Base Components          │
│  - StatusCard, LoadingButton           │
│  - PriceDisplay, ModeIndicator         │
│  - ErrorDisplay, BigDecimalExt         │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│        :exchange:coinbase               │  ← Implementation
│  🆕 COMPLETE: Auth Infrastructure      │     (Isolated & swappable)
│  - CoinbaseJwtGenerator (ES256)        │
│  - AuthModule (DI binding)             │
│  📋 TODO: REST API methods             │
└─────────────────────────────────────────┘
```

### Key Architectural Wins

✅ **Clean separation:** Domain defines what, infrastructure defines how
✅ **Testability:** Interfaces enable easy mocking and unit testing  
✅ **Swappable exchanges:** Add Kraken by implementing same interfaces
✅ **Zero coupling:** Features never import exchange implementations directly
✅ **Secure credentials:** AES-256-GCM encryption, never logged, cleared on uninstall
✅ **JWT authentication:** ES256 tokens with 2-minute expiry, nonce generation, proper URI formatting
✅ **Complete auth pipeline:** From secure storage → JWT generation → API authentication
✅ **UI foundation:** Reusable components for trading-focused interface

---

## 🎫 Ticket System

All tickets are maintained in the repository under [docs/tickets/](docs/tickets/), organized by status:

```
tickets/
├── refined/        # User-approved, ready to implement  
├── ongoing/        # Currently being worked on
├── in-review/      # Implementation complete, awaiting review
├── done/           # Completed and verified
└── archived/       # Superseded/duplicate tickets
```

**Current tickets by phase:**

**Phase 0:** Foundation (6 tickets)
- 01: Domain Models (ready)
- 02: Repository Interfaces ✅ DONE
- 03: Room Database (ready)
- 04: Credential Store ✅ DONE
- 05: Decision Engine (blocked by 01)
- 06: Risk Manager (blocked by 01)

**Phase 1:** Coinbase Integration (3 tickets)
- 07: JWT Generator ✅ DONE
- 08: REST API Client (ready, JWT complete)
- 09: WebSocket Client (blocked by 08)

**See:** [docs/roadmap.md](docs/roadmap.md) for complete ticket status and dependencies

---

## 📱 Tech Stack & Dependencies

### Core Android
- **Kotlin:** 2.3.0
- **Target/Compile SDK:** 34, **Min SDK:** 26  
- **Compose BOM:** 2025.12.01 ✅ ACTIVE (Material 3)
- **Hilt:** 2.57.2 ✅ ACTIVE (dependency injection)
- **Room:** 2.8.4 ⚠️ CONFIGURED (database not yet used)
- **Navigation Compose:** 2.8.5 ❌ NOT_YET_USED
- **Lifecycle ViewModel:** 2.9.0 ❌ NOT_YET_USED

### Trading & Crypto
- **ta4j-core:** 0.16 ❌ NOT_YET_USED (technical analysis)
- **nimbus-jose-jwt:** 9.47 ✅ ACTIVE (ES256 JWT signing)
- **security-crypto:** 1.1.0-alpha06 ✅ ACTIVE (AES-256 credential encryption)

### Networking & Data
- **Ktor Client:** 3.3.3 ⚠️ CONFIGURED (HTTP client for Coinbase API)
- **Ktor WebSockets:** 3.3.3 ❌ NOT_YET_USED (real-time data)
- **kotlinx-serialization:** 1.8.0 ❌ NOT_YET_USED (JSON parsing)
- **kotlinx-coroutines:** 1.10.2 ✅ ACTIVE (async programming)

### Background & Monitoring  
- **WorkManager:** 2.10.0 ❌ NOT_YET_USED (background tasks)
- **DataStore Preferences:** 1.1.1 ❌ NOT_YET_USED (settings)
- **Timber:** 5.0.1 ✅ ACTIVE (logging)
- **Firebase BOM:** 34.7.0 ✅ ACTIVE (Analytics + Crashlytics)

### UI & Charts
- **Material Icons Extended:** 1.8.1 ✅ ACTIVE (trading icons in ModeIndicator)
- **Vico:** 2.4.0 ❌ NOT_YET_USED (charts for price/portfolio)

### Testing
- **JUnit:** 4.13.2 ❌ NO_TESTS_YET
- **Compose UI Test:** (BOM version) ❌ NO_TESTS_YET

---

## 🔧 Development Workflow

### Claude Code Integration

**The GitHub Actions workflow is designed for remote development:**

1. **Code remotely** in Claude Code (mobile/desktop)
2. **Push to `claude/*` branch** → triggers build
3. **GitHub Actions** builds APK + updates docs
4. **Result committed back** as `.build-status` + updated docs
5. **`git pull`** to see build result and updated documentation

### Check Build Status
```bash
# Pull latest (includes build status + doc updates)
git pull

# Check if build passed
cat .build-status
# Output: SUCCESS or FAILURE

# If failed, see error details
cat build-log.txt
```

### Key Files
- **`.build-status`** - SUCCESS/FAILURE from latest build
- **`build-log.txt`** - Error details if build failed (created on failure only)
- **Auto-updated docs** - CLAUDE.md, docs/ci.md, docs/roadmap.md updated by AI

---

## 🎯 Current Implementation Focus

**Immediate next steps:**

1. **Implement domain models** (Ticket 01):
   - Candle (OHLCV market data)
   - Order (trading orders with status)
   - Portfolio (account balances)
   - Decision (strategy decisions: WAIT/DEFENSE/TREND/RANGE)

2. **Set up Room database schema** (Ticket 03):
   - OrderEntity, PortfolioSnapshotEntity
   - OrderDao, PortfolioDao with Flow support
   - Database migrations

3. **Complete REST API client** (Ticket 08):
   - Use existing JWT generator
   - Implement getCandles, getAccounts, placeOrder methods
   - Handle rate limits and errors

**Ready to implement:** All foundational pieces (auth, storage, UI components) are complete. Can focus on domain models and business logic.

