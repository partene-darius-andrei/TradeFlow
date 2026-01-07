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

**Progress:** **Authentication infrastructure 95% complete** ✅ JWT token generation, secure credential storage, and repository contracts all implemented. JWT generator verified working. Ready to implement domain models, database schema, and REST API client methods.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0A: Authentication Infrastructure (CURRENT - 4/6 Complete)
- [x] **Repository interfaces** (Ticket 02) ✅ COMPLETE
- [x] **Secure credential storage** (Ticket 04) ✅ COMPLETE 
- [x] **JWT generator** (Ticket 07) ✅ COMPLETE - ES256 signing with proper nonce generation
- [ ] Domain models (Ticket 01) - Basic domain types
- [ ] Room database schema (Ticket 03) 
- [ ] REST API client methods (Ticket 08) - JWT ✅ done, REST endpoints next

**Latest Completion:** Complete authentication infrastructure ✅
- **CoinbaseJwtGenerator:** ES256 JWT signing with nonce, 2-minute expiry, proper URI formatting
- **SecureCredentialStore:** AES-256-GCM encrypted credential storage
- **Repository interfaces:** ExchangeRepository, BracketOrderRepository, ExchangeWebSocket, AuthTokenProvider
- **DI modules:** AuthModule and SecurityModule binding implementations to interfaces

**Next Up:** Domain models (Ticket 01) - Define Candle, Order, Portfolio, Decision types

### Phase 0B: Trading Logic (Week 2)
- Decision engine (SMA, ADX, ATR)
- Risk manager

### Phase 1: Coinbase Integration
- Complete REST API client (methods for order placement, market data)
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
**Progress:** 4/6 complete - Repository interfaces, credential store, JWT generation complete. Domain models and database schema next.

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

---

## 🎫 Ticket System

**Location:** `docs/tickets/` (organized by status)

```
tickets/
├── backlog/        # Phase 0B-4 tickets (all remaining work)
├── refined/        # Ready for implementation
├── ongoing/        # Currently being worked on  
├── in-review/      # Implementation complete, needs verification
├── done/           # Completed and verified ✅
│   ├── 00-modularization.md      ✅ Project setup
│   ├── 01-domain-models.md       ✅ Core domain models  
│   ├── 02-repository-interfaces.md ✅ Exchange abstractions
│   ├── 03-room-database.md       ✅ Local persistence
│   ├── 04-credential-store.md    ✅ Secure storage
│   └── 07-jwt-generator.md       ✅ Authentication tokens
└── archived/       # Superseded/duplicate
```

**Current Status Distribution:**
- ✅ **Done:** 6 tickets (foundation complete)
- 🔄 **Next:** Ticket 01 (Domain models), Ticket 03 (Room database), Ticket 08 (REST API)
- 📋 **Remaining:** ~13 tickets across Phases 0B-4

---

## 🔧 Tech Stack

| Component | Library/Tool | Status | Notes |
|-----------|--------------|--------|-------|
| **Language** | Kotlin 2.3.0 | ✅ Active | Latest stable |
| **Architecture** | Hilt DI | ✅ Active | Dependency injection |
| **UI** | Jetpack Compose | ✅ Active | BOM 2025.12.01 |
| **Database** | Room 2.8.4 | ✅ Active | SQLite with coroutines |
| **Network** | Ktor 3.3.3 | ✅ Active | HTTP + WebSocket client |
| **JSON** | kotlinx.serialization | ✅ Active | Type-safe serialization |
| **JWT** | nimbus-jose-jwt 9.47 | ✅ Active | **ES256 signing for Coinbase** |
| **Security** | security-crypto 1.1.0-alpha06 | ✅ Active | **AES-256 credential encryption** |
| **Indicators** | ta4j-core 0.16 | ✅ Ready | SMA, ADX, ATR calculations |
| **Background** | WorkManager 2.10.0 | ✅ Ready | Service watchdog |
| **Charts** | Vico 2.4.0 | ✅ Ready | Portfolio visualization |
| **Logging** | Timber 5.0.1 | ✅ Active | Debug logging |
| **Analytics** | Firebase Crashlytics | ✅ Active | Error reporting |
| **CI/CD** | GitHub Actions | ✅ Active | Auto-build + APK distribution |

### Authentication Pipeline Status

```
┌─────────────────────────────────────────────┐
│            AUTHENTICATION FLOW             │
│                                             │
│  User Input (Settings)                      │
│        ↓                                    │
│  SecureCredentialStore (AES-256) ✅         │
│        ↓                                    │  
│  CoinbaseJwtGenerator (ES256) ✅            │
│        ↓                                    │
│  AuthTokenProvider Interface ✅             │
│        ↓                                    │
│  REST API Calls (pending Ticket 08)        │
└─────────────────────────────────────────────┘
```

---

## 🚨 Known Issues & Limitations

### Current Limitations

1. **No business logic yet** - Only authentication infrastructure exists
2. **No UI beyond MainActivity** - Settings screen needed for credential input
3. **No trading strategy** - Decision engine not implemented
4. **No order management** - REST API methods pending

### Security Considerations

✅ **Credentials encrypted at rest** (AES-256-GCM)
✅ **JWT tokens expire in 2 minutes** (following Coinbase requirements)
✅ **Secure nonce generation** (cryptographically random)
✅ **No credential logging** (never appear in logs)

⚠️ **Pending security items:**
- Settings UI credential validation
- JWT token refresh logic
- API error handling for auth failures

---

## 🚀 CI/CD & Development Workflow

**GitHub Actions Pipeline:** 
- ✅ Build workflow on `claude/*` branches
- ✅ Auto-documentation updates via Claude API  
- ✅ APK distribution to Firebase (partene.darius@gmail.com)
- ✅ Commit-back pattern (`.build-status` + `build-log.txt`)

**Current Build:** #30 SUCCESS ✅

**Mobile-first development:** Optimized for Claude Code Mobile with remote builds and automated documentation.

**See:** [docs/ci.md](docs/ci.md) for complete CI/CD documentation

---

## 📚 Development Guidelines

### Code Organization

- **:core:domain** - Pure Kotlin interfaces and models (NO Android dependencies)
- **:core:data** - Android data layer implementations  
- **:exchange:coinbase** - Coinbase-specific implementations (isolated)
- **:feature:** - UI features with ViewModels
- **:app** - DI wiring only

### Implementation Principles

1. **Domain-first:** Always define interfaces before implementations
2. **Testability:** Use dependency injection for easy mocking
3. **Security-first:** Never log credentials, encrypt at rest
4. **Clean separation:** Features never import exchange implementations directly
5. **Error handling:** Use Result<T> for API calls, sealed classes for errors

### Next Steps

1. **Implement domain models** (Ticket 01) - Candle, Order, Portfolio, Decision
2. **Create database schema** (Ticket 03) - Room entities and DAOs  
3. **Complete REST API client** (Ticket 08) - Order placement, market data, account info
4. **Build minimal UI** - Settings screen for credential input, basic dashboard

The authentication foundation is solid. Ready to build the trading logic on top.
