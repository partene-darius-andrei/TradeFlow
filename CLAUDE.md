# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (3/6 Complete)
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

### What EXISTS (Phase 0A Progress: 3/6 Complete)

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
```

### What DOESN'T Exist Yet (Next Up)

```
❌ Domain models (Candle, Decision, Order, Portfolio) - Ticket 01
❌ Room database entities/DAOs - Ticket 03
❌ Coinbase REST API client - Ticket 08 (partial complete - JWT ✅)
❌ Decision engine (regime switching logic)
❌ Trading service (foreground service)
❌ Risk management
❌ UI beyond MainActivity
```

**Progress:** **Authentication infrastructure complete** ✅ JWT token generation, secure credential storage, and repository contracts all implemented. Ready to implement domain models and database schema.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0A: Authentication Infrastructure (CURRENT - 3/6 Complete)
- [x] **Repository interfaces** (Ticket 02) ✅ COMPLETE
- [x] **Secure credential storage** (Ticket 04) ✅ COMPLETE 
- [x] **JWT generator** (Ticket 07) ✅ COMPLETE
- [ ] Domain models (Ticket 01) - Basic domain types
- [ ] Room database schema (Ticket 03) 
- [ ] REST API client partial (Ticket 08) - JWT done, REST methods next

**Latest Completion:** JWT generator (Ticket 07) ✅ Implemented `CoinbaseJwtGenerator` with ES256 signing for Coinbase Advanced Trade API authentication. Generates both REST API tokens (with URI claim) and WebSocket tokens.

**Next Up:** Domain models (Ticket 01) - Define Candle, Order, Portfolio, Decision types

### Phase 0B: Trading Logic (Week 2)
- Decision engine (SMA, ADX, ATR)
- Risk manager

### Phase 1: Coinbase Integration
- Complete REST API client
- WebSocket client

### Phase 2: Presentation Layer
- UI components and theme
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
**Progress:** 3/6 complete - Repository interfaces, credential store, JWT generation done. Domain models next.

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
│        :exchange:coinbase               │  ← Implementation
│  🆕 PARTIAL: Auth Infrastructure       │     (Isolated & swappable)
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

---

## 🎫 Ticket System

**Location:** `docs/tickets/` (organized by status)

```
tickets/
├── backlog/        # Not started yet
├── refined/        # Ready for implementation (user-approved)  
├── ongoing/        # Currently being worked on
├── in-review/      # Implementation complete, awaiting review
├── done/           # Completed and verified ✅ Ticket 02, 04, 07
└── archived/       # Superseded/duplicate tickets
```

**Recent Completions:** 
- Ticket 02 (Repository Interfaces) ✅
- Ticket 04 (Secure Credential Store) ✅  
- Ticket 07 (JWT Generator) ✅

**File Mapping:**
| Ticket | Module | Key Files |
|--------|--------|-----------|
| Ticket 02 | :core:domain | AuthTokenProvider.kt, CredentialStore.kt, ExchangeRepository.kt |
| Ticket 04 | :core:data | SecureCredentialStore.kt, SecurityModule.kt |
| Ticket 07 | :exchange:coinbase | CoinbaseJwtGenerator.kt, AuthModule.kt |

---

## 🧠 Context for AI Assistants

### Tech Stack

| Component | Technology | Status |
|-----------|------------|---------|
| **HTTP Client** | Ktor 3.3.3 (OkHttp engine) | ✅ Configured |
| **Database** | Room 2.8.4 | ⚠️ Schema pending |
| **DI** | Hilt 2.57.2 | ✅ Active |
| **Serialization** | kotlinx.serialization | ✅ Configured |
| **JWT Signing** | nimbus-jose-jwt 9.47 | ✅ Active |
| **TA Indicators** | ta4j-core 0.16 | ✅ Ready |
| **Security** | security-crypto 1.1.0-alpha06 | ✅ Active |
| **Async** | Coroutines 1.10.2 | ✅ Active |
| **Logging** | Timber 5.0.1 | ✅ Active |
| **UI** | Compose BOM 2025.12.01 | ✅ Ready |

### Dependencies Status

**✅ Authentication Infrastructure:**
- AES-256-GCM encrypted credential storage
- ES256 JWT token generation with 2-minute expiry
- Repository interfaces for exchange abstraction

**⚠️ Implementation Pending:**
- Domain models (Candle, Order, Decision, Portfolio)
- Room database schema and DAOs
- Coinbase REST API methods
- WebSocket client implementation
- Decision engine with SMA/ADX/ATR
- Trading service with foreground execution

**❌ Not Yet Implemented:**
- UI screens and ViewModels  
- Risk management
- Integration tests
- Battery optimization

### Development Notes

**Key Constraint:** Coinbase sandbox only returns static data. Real API testing required with small amounts.

**Critical API Details:**
- Maker fees: 0.60% (requires 1.5% minimum grid spacing)
- JWT expiry: 2 minutes (regenerate per request)
- WebSocket timeout: 60-90 seconds (heartbeat required)
- Rate limits: 10,000 REST requests/hour

**Next Implementation Priority:** Domain models → Room database → REST API methods → WebSocket → Decision engine → Trading service.

### FILE ORGANIZATION

```
app/
├── src/main/java/com/dpart/tradeflow/
│   ├── MainActivity.kt (entry point)
│   ├── TradeFlowApp.kt (Hilt application)
│   ├── di/ (3 empty modules)
│   └── data/local/ (2 dummy files)
└── core/
    ├── domain/ (interfaces & contracts) ✅
    ├── data/ (Room + security) ✅ partial
    └── ui/ (shared components) ❌
└── exchange/
    └── coinbase/ (auth complete) ✅ partial
└── feature/ (UI screens) ❌
```

