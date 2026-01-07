# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (5/6 Complete) + Login Screen
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

### What EXISTS (Phase 0A Progress: 5/6 Complete + Login Screen)

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

🆕 LOGIN SCREEN COMPLETE:
✅ app/src/main/java/com/dpart/tradeflow/presentation/login/
    ├── LoginScreen.kt                  ✅ Complete credential entry UI
    ├── LoginViewModel.kt               ✅ Form validation + secure storage
    └── LoginUiState.kt                 ✅ UI state management
```

### What DOESN'T Exist Yet (Next Up)

```
❌ Domain models (Candle, Decision, Order, Portfolio) - Ticket 01
❌ Room database entities/DAOs - Ticket 03
❌ Coinbase REST API client methods - Ticket 08 (JWT complete ✅, REST methods next)
❌ Decision engine (regime switching logic)
❌ Trading service (foreground service)
❌ Risk management
❌ Dashboard and Settings screens
❌ App navigation setup
```

**Progress:** **Authentication infrastructure + Login Screen complete** ✅ JWT token generation, secure credential storage, repository contracts, UI components, and complete login flow all implemented. Ready for domain models, database schema, and REST API client methods.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0A: Authentication Infrastructure (CURRENT - 5/6 Complete + Login Screen)
- [x] **Repository interfaces** (Ticket 02) ✅ COMPLETE
- [x] **Secure credential storage** (Ticket 04) ✅ COMPLETE 
- [x] **JWT generator** (Ticket 07) ✅ COMPLETE - ES256 signing with proper nonce generation
- [x] **UI components** (Bonus) ✅ COMPLETE - ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard
- [x] **Login Screen** (Bonus) ✅ COMPLETE - Full credential entry flow with validation and secure storage
- [ ] Domain models (Ticket 01) - Basic domain types
- [ ] Room database schema (Ticket 03) 
- [ ] REST API client methods (Ticket 08) - JWT ✅ done, REST endpoints next

**Latest Completion:** Login/Credentials Screen ✅
- **LoginScreen.kt:** Complete credential entry UI with form validation, masked secret input, test connection button
- **LoginViewModel.kt:** Business logic for form handling, validation, secure storage integration
- **LoginUiState.kt:** UI state management (Initial, Loading, Error, Success states)
- **Features:** API key format validation, secret visibility toggle, save to SecureCredentialStore, error handling

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
**Progress:** 5/6 complete + Login screen - Repository interfaces, credential store, JWT generation, UI components, and complete login flow implemented. Domain models and database schema next.

---

## 🏗️ Architecture Overview

### Domain-First Architecture

**Core principle:** Domain layer defines contracts, infrastructure implements them.

```
┌─────────────────────────────────────────┐
│                :app                     │  ← DI wiring + Login Screen ✅
│  🆕 presentation/login/                 │
│  - LoginScreen.kt (credential UI)      │
│  - LoginViewModel.kt (business logic)  │
│  - LoginUiState.kt (state management)  │
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
│       :exchange:coinbase               │  ← Exchange implementations
│  🆕 COMPLETE: JWT Authentication       │     (Isolated - swappable)
│  - CoinbaseJwtGenerator (ES256)       │
│  - AuthModule (DI)                    │
└─────────────────────────────────────────┘
```

**Key Architecture Decisions:**
- **Clean Architecture:** Domain defines contracts, infrastructure implements
- **Exchange Isolation:** All Coinbase code in :exchange:coinbase module
- **Dependency Inversion:** Features depend on interfaces, not implementations
- **Secure by Default:** Credentials encrypted with AES-256, never logged
- **Single Responsibility:** Each module has clear purpose and boundaries

### Current Dependencies

**Active Libraries (✅ in use):**
- ✅ **nimbus-jose-jwt 9.47** (JWT ES256 signing)
- ✅ **security-crypto 1.1.0-alpha06** (encrypted credentials)
- ✅ **material-icons-extended** (for UI icons - visibility toggle)
- ✅ **ta4j-core 0.16** (technical analysis indicators)
- ✅ **ktor 3.3.3** (HTTP client with OkHttp engine)
- ✅ **timber 5.0.1** (logging)

**Infrastructure Ready (⚠️ configured but not yet utilized):**
- ⚠️ **Room 2.8.4** (local database - schema pending)
- ⚠️ **Hilt 2.57.2** (DI - partially used)
- ⚠️ **work-runtime-ktx 2.10.0** (background tasks)
- ⚠️ **datastore-preferences 1.1.1** (settings persistence)

---

## 🛠️ Development Workflow

### GitHub Actions Integration

**Pattern:** Push → Actions build → Commit back result → Pull for status

```bash
# 1. Implement and push
git push origin claude/feature-branch

# 2. Wait for Actions (3-5 minutes)
gh run watch  # or check GitHub UI

# 3. Pull build result + doc updates
git pull

# 4. Check build status
cat .build-status
# OUTPUT: SUCCESS or FAILURE

# 5. If failure, read logs and fix
cat build-log.txt
# Shows last 200 lines of build output
```

**Auto-Documentation:** Code changes automatically update CLAUDE.md and docs/ via Claude API.

### File Organization

```
TradeFlow/
├── app/src/main/java/com/dpart/tradeflow/
│   ├── MainActivity.kt
│   ├── TradeFlowApp.kt
│   └── presentation/
│       └── login/                    ← ✅ COMPLETE
├── core/
│   ├── domain/src/main/kotlin/      ← ✅ COMPLETE (interfaces)
│   ├── data/src/main/kotlin/        ← ✅ COMPLETE (credential store)
│   └── ui/src/main/kotlin/          ← ✅ COMPLETE (base components)
├── exchange/
│   └── coinbase/src/main/kotlin/    ← ✅ COMPLETE (JWT auth)
└── docs/                            ← ✅ Documentation
```

---

## 💡 Next Steps

**Immediate Priority (Next 2-3 commits):**

1. **Domain Models (Ticket 01)**
   - Create Candle, Order, Portfolio, Decision data classes
   - Use BigDecimal for all monetary values
   - Pure Kotlin in :core:domain

2. **Room Database (Ticket 03)**
   - Create entities based on domain models
   - Set up DAOs with Flow support
   - Configure database module

3. **REST API Client (Ticket 08)**
   - Implement CoinbaseRepository using existing JWT generator
   - Add endpoints: getAccounts, getCandles, placeOrder, cancelOrder
   - Map Coinbase DTOs to domain models

**Goal:** Complete Phase 0A foundation, then move to Phase 1 (Coinbase integration) or Phase 2 (UI screens).

---

## 📚 Context for Claude

**Current Focus:** Building robust foundation before trading logic
**Architecture:** Domain-first with clean separation
**Target:** Personal use only (never published)
**Strategy:** Simple regime-switching (DEFENSE/TREND/RANGE based on SMA + ADX)
**Risk Management:** 15% drawdown = emergency stop

**Key Files to Reference:**
- `docs/roadmap.md` - Implementation phases
- `docs/reference.md` - Code examples and API details
- `docs/tickets/refined/` - Detailed ticket requirements
