# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure Complete (Static Credentials)
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

### What EXISTS (Phase 0A Complete: Static Credentials)

```
✅ Modern Android app structure
✅ Hilt dependency injection configured
✅ Room database with empty schema
✅ Ktor HTTP client configured (OkHttp engine)
✅ Timber logging initialized
✅ Firebase Analytics + Crashlytics
✅ All trading dependencies added (ta4j, nimbus-jose-jwt, security-crypto)
✅ GitHub Actions CI/CD pipeline with environment credentials

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
❌ Domain models (Candle, Decision, Order, Portfolio) - Ticket 01
❌ Room database entities/DAOs - Ticket 03
❌ Coinbase REST API client methods - Ticket 08 (JWT complete ✅, REST methods next)
❌ Decision engine (regime switching logic)
❌ Trading service (foreground service)
❌ Risk management
❌ Dashboard and Settings screens
❌ App navigation setup
```

**Progress:** **Authentication infrastructure complete** ✅ JWT token generation, static credential injection, repository contracts, and UI components all implemented. Login screen removed in favor of build-time configuration. Ready for domain models, database schema, and REST API client methods.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0A: Authentication Infrastructure (COMPLETE ✅)
- [x] **Repository interfaces** (Ticket 02) ✅ COMPLETE
- [x] **Secure credential storage** (Ticket 04) ✅ COMPLETE - Now static injection
- [x] **JWT generator** (Ticket 07) ✅ COMPLETE - ES256 signing with proper nonce generation
- [x] **UI components** ✅ COMPLETE - ErrorDisplay, LoadingButton, ModeIndicator, PriceDisplay, StatusCard
- [x] **Credential system** ✅ COMPLETE - Build-time injection replacing UI input

**Latest Changes:** Static Credential System ✅
- **Removed Login Screen:** No longer needed - credentials injected at build time
- **StaticCredentialStore:** Replaces SecureCredentialStore, returns build-injected credentials
- **CredentialsModule:** New DI module providing credentials from BuildConfig
- **Build Integration:** Environment variables and local.properties support
- **Simplified Navigation:** Direct to Dashboard, no authentication flow needed

**Next Up:** Domain models (Ticket 01) - Define Candle, Order, Portfolio, Decision types

### Phase 0B: Core Foundation (Week 2)
- Domain models and Room database schema
- Decision engine (SMA, ADX, ATR)
- Risk manager

### Phase 1: Coinbase Integration
- Complete REST API client (methods for order placement, market data)
- WebSocket client

### Phase 2: Presentation Layer
- Dashboard screen + ViewModel
- Settings screen + ViewModel
- App navigation (simplified - no login)

### Phase 3: Trading Service
- Foreground service
- Battery optimization

### Phase 4: Testing & Validation
- Integration tests
- MVP milestone

**Current Phase:** Phase 0A Complete → Phase 0B (Core Foundation)
**Progress:** Authentication infrastructure complete with static credentials. Domain models and database schema next.

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
│  🆕 build.gradle.kts                   │
│  - Injects env vars → BuildConfig      │
│  - Supports local.properties fallback  │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│            :feature:*                   │  ← UI & ViewModels (no login needed)
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
│  🆗 UPDATED: Static Credential Store   │
│  - StaticCredentialStore (no encryption)│
│  - SecurityModule (DI)                 │
│  - Room database schema (pending)      │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│       :exchange:coinbase                │  ← Coinbase-specific implementation
│  🆕 COMPLETE: JWT Authentication       │     (ISOLATED - swappable)
│  - CoinbaseJwtGenerator (ES256)        │
│  - AuthModule (DI binding)             │
│  - REST API methods (pending)          │
│  - WebSocket client (pending)          │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│           :core:ui                      │  ← Shared UI components
│  🆕 COMPLETE: Base Components          │
│  - StatusCard, LoadingButton           │
│  - PriceDisplay, ModeIndicator         │
│  - ErrorDisplay, BigDecimalExt         │
└─────────────────────────────────────────┘
```

### Key Architectural Decisions

**Static Credentials (New):**
- **Build-time injection** replaces runtime credential entry
- **Environment priority:** CI/CD env vars → local.properties → empty
- **No encryption needed** - credentials never stored on device
- **Simplified UX** - no login screen, direct to trading UI

**Exchange Isolation:**
- All Coinbase code in `:exchange:coinbase` module
- Domain layer defines contracts, Coinbase implements
- Easy to swap exchanges (add `:exchange:kraken`)
- DI binding in `:app` module only

**Domain Purity:**
- `:core:domain` has ZERO Android dependencies
- Pure Kotlin interfaces and models only
- Testable without Android SDK

---

## ⚙️ Tech Stack

### Core Android
- ✅ **Kotlin 2.3.0** (compilation target)
- ✅ **Compose BOM 2025.12.01** (UI framework)
- ✅ **Hilt 2.57.2** (dependency injection)
- ✅ **Room 2.8.4** (local database)
- ✅ **Material 3** (design system)

### Networking & Serialization
- ✅ **Ktor 3.3.3** (HTTP client + WebSocket)
  - Uses OkHttp engine for reliability
  - Coroutines integration
  - Unified client for REST + WebSocket
- ✅ **kotlinx.serialization** (JSON parsing)

### Trading & Security
- ✅ **nimbus-jose-jwt 9.47** (ES256 JWT signing for Coinbase)
- ✅ **ta4j-core 0.16** (technical analysis indicators)
- ✅ **security-crypto 1.1.0-alpha06** (credential encryption - now static)

### Background Processing
- ✅ **work-runtime-ktx 2.10.0** (background tasks)
- ✅ **Coroutines 1.10.2** (async/concurrency)

### Development & Monitoring
- ✅ **Timber 5.0.1** (logging)
- ✅ **Firebase BOM 34.7.0** (Analytics + Crashlytics)
- ✅ **Vico 2.4.0** (charts/graphs)
- ✅ **datastore-preferences 1.1.1** (settings)
- ✅ **material-icons-extended** (UI icons)

### Build System
- ✅ **Gradle 8.13.2**
- ✅ **Android Gradle Plugin 8.8.0**
- ✅ **KSP 2.3.0-1.0.29** (Room compiler)

**Build-time Configuration:**
- Environment variable injection → BuildConfig
- Local properties fallback for development
- CI/CD secrets integration

---

## 🔧 Development Workflow

### Credential Setup

**For CI/CD (GitHub Actions):**
```yaml
# Set in GitHub repo → Settings → Secrets
COINBASE_API_KEY: "organizations/your-org/apiKeys/your-key-id"
COINBASE_API_SECRET: "your-ec-private-key-pem"
```

**For Local Development:**
```properties
# Create/edit local.properties
coinbase.api.key=organizations/your-org/apiKeys/your-key-id
coinbase.api.secret=-----BEGIN EC PRIVATE KEY-----\nYourBase64EncodedKey\n-----END EC PRIVATE KEY-----
```

### Build & Test Flow

```bash
# 1. Set credentials (see above)

# 2. Build app
./gradlew assembleDebug
# Credentials automatically injected into BuildConfig

# 3. Install and test
adb install app/build/outputs/apk/debug/app-debug.apk

# 4. Push for CI/CD
git push origin claude/your-feature-branch
# GitHub Actions builds with CI credentials
```

### CI/CD Pipeline

**Build Workflow (.github/workflows/build.yml):**
1. ✅ Checks out code
2. ✅ Sets up JDK 17
3. ✅ **Injects credentials** from GitHub secrets → environment
4. ✅ Builds debug APK with credentials
5. ✅ Uploads to Firebase App Distribution
6. ✅ Commits build status back to branch
7. ✅ Uploads APK artifact (7-day retention)

**Documentation Workflow (.github/workflows/update-docs.yml):**
1. ✅ Analyzes git diff
2. ✅ Calls Claude API to update documentation
3. ✅ Commits updated docs back to branch

---

## 🔍 Missing Dependencies

*None currently - all required dependencies are added and configured.*

**Recently Added/Updated:**
- ✅ Build-time credential injection system
- ✅ Static credential store (replaces encrypted storage)
- ✅ Environment variable and local properties support

---

## 📁 Key File Locations

### Recently Modified
- `app/build.gradle.kts` - Build-time credential injection
- `app/src/main/java/com/dpart/tradeflow/di/CredentialsModule.kt` - NEW: Credential DI
- `app/src/main/java/com/dpart/tradeflow/MainActivity.kt` - Simplified (no credential check)
- `app/src/main/java/com/dpart/tradeflow/navigation/AppNavHost.kt` - Direct to Dashboard
- `app/src/main/java/com/dpart/tradeflow/navigation/Screen.kt` - Removed Login route
- `core/data/src/main/kotlin/com/tradeflow/core/data/security/StaticCredentialStore.kt` - NEW: Static store
- `core/data/src/main/kotlin/com/tradeflow/core/data/di/SecurityModule.kt` - Updated DI binding

### Removed Files
- `app/src/main/java/com/dpart/tradeflow/presentation/login/LoginScreen.kt` - No longer needed
- `app/src/main/java/com/dpart/tradeflow/presentation/login/LoginViewModel.kt` - No longer needed
- `app/src/main/java/com/dpart/tradeflow/presentation/login/LoginUiState.kt` - No longer needed

### Core Architecture Files
- `core/domain/src/main/kotlin/com/tradeflow/core/domain/` - All interface definitions
- `core/data/src/main/kotlin/com/tradeflow/core/data/` - Data layer implementations
- `core/ui/src/main/kotlin/com/tradeflow/core/ui/` - Shared UI components
- `exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/` - Coinbase integration

### Documentation
- `docs/roadmap.md` - Implementation phases and progress tracking
- `docs/reference.md` - Technical implementation guide
- `docs/README.md` - Documentation index and navigation
- `docs/ci.md` - CI/CD pipeline documentation
