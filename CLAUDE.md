# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (Domain Complete)
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

### What EXISTS (Phase 0A Progress)

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
```

### What DOESN'T Exist Yet (Next Up)

```
❌ Domain models (Candle, Decision, Order, Portfolio) - Ticket 01
❌ Room database entities/DAOs - Ticket 03
❌ Secure credential implementation - Ticket 04
❌ Coinbase JWT generator - Ticket 07
❌ Coinbase REST API client - Ticket 08
❌ Decision engine (regime switching logic)
❌ Trading service (foreground service)
❌ Risk management
❌ UI beyond MainActivity
```

**Progress:** **Repository interfaces complete** ✅ Domain contracts defined for all exchange operations. Ready for implementation phase.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0A: Authentication Infrastructure (CURRENT)
- [x] **Repository interfaces** (Ticket 02) ✅ COMPLETE
- [ ] Domain models (Ticket 01) - Basic domain types
- [ ] Room database schema (Ticket 03) 
- [ ] Secure credential storage (Ticket 04)
- [ ] JWT token generator (Ticket 07)
- [ ] REST API client partial (Ticket 08)

**Completed Milestone:** Repository interfaces define clean contracts between domain and infrastructure layers. All exchange operations abstracted behind interfaces.

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
**Progress:** Repository interfaces complete, domain models next

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
│        :exchange:coinbase               │  ← Implementation
│         (Coming in Phase 1)             │     (Isolated & swappable)
└─────────────────────────────────────────┘
```

### Key Architectural Wins

✅ **Clean separation:** Domain defines what, infrastructure defines how
✅ **Testability:** Interfaces enable easy mocking and unit testing  
✅ **Swappable exchanges:** Add Kraken by implementing same interfaces
✅ **Zero coupling:** Features never import exchange implementations directly

---

## 🎫 Ticket System

**Location:** `docs/tickets/` (organized by status)

```
tickets/
├── backlog/        # Not started yet
├── refined/        # Ready for implementation (user-approved)  
├── ongoing/        # Currently being worked on
├── in-review/      # Implementation complete, awaiting review
├── done/           # Completed and verified ✅ Ticket 02
└── archived/       # Superseded/duplicate tickets
```

**Recent Completion:** Ticket 02 (Repository Interfaces) moved to `done/` ✅

**Phase 0A Active Tickets:**
- Ticket 01: Domain Models (Candle, Order, Portfolio, Decision) - **NEXT**
- Ticket 03: Room Database (entities, DAOs)
- Ticket 04: Secure Credential Store (EncryptedSharedPreferences)  
- Ticket 07: JWT Generator (ES256 for Coinbase)
- Ticket 08: REST API Client (getAccounts endpoint)

**Workflow:**
1. Check [docs/roadmap.md](docs/roadmap.md) for next priority
2. Find ticket in `docs/tickets/backlog/`
3. Move to `ongoing/` when starting
4. Create branch: `claude/ticket-##-description`
5. Implement → Build → Test → Commit
6. Move to `in-review/` when complete
7. After approval → move to `done/`

---

## 🛠️ Development Workflow

### Remote Development Pipeline

```
┌──────────────────┐
│ Claude Code      │ (Desktop or Mobile)
│ - Implements     │
│ - Pushes branch  │
└────────┬─────────┘
         ▼
┌──────────────────┐
│ GitHub Actions   │
│ - Builds APK     │
│ - Uploads to     │
│   Firebase       │
│ - Commits status │
└────────┬─────────┘
         ▼
┌──────────────────┐
│ Firebase App     │
│ Distribution     │
│ → Phone          │
│ (Test on device) │
└──────────────────┘
```

### When to Use Desktop vs Mobile

| Scenario | Use Desktop | Use Mobile |
|----------|-------------|------------|
| Complex features | ✅ Full IDE, MCP servers | ❌ Limited context |
| Quick bug fixes | ⚠️ Overkill | ✅ Fast and easy |
| API integration | ✅ Coinbase MCP server | ❌ No MCP access |
| Simple refactors | ⚠️ Either works | ✅ Convenient |

### Build-Before-Push Protocol

**Desktop (with Gradle):**
```bash
1. Implement feature
2. Run: ./gradlew assembleDebug
3. If SUCCESS → push
4. If FAILURE → fix and retry
```

**Mobile (no Gradle):**
```bash
1. Implement feature
2. Push to branch
3. GitHub Actions builds
4. git pull && cat .build-status
5. If FAILURE → cat build-log.txt → fix → retry
```

---

## 🧭 Tech Stack

### Dependencies Status

| Category | Library | Status | Notes |
|----------|---------|--------|--------|
| **Core** | Kotlin 2.3.0 | ✅ READY | Latest stable |
| | Compose BOM 2025.12.01 | ✅ READY | Latest UI toolkit |
| | Coroutines 1.10.2 | ✅ READY | Async/concurrency |
| **DI** | Hilt 2.57.2 | ✅ READY | Dependency injection |
| **Database** | Room 2.8.4 | ✅ READY | Local persistence |
| **Network** | Ktor 3.3.3 | ✅ READY | HTTP + WebSocket |
| **Auth** | nimbus-jose-jwt 9.47 | ✅ READY | JWT ES256 signing |
| **Trading** | ta4j-core 0.16 | ✅ READY | Technical indicators |
| **Security** | security-crypto 1.1.0-alpha06 | ✅ READY | Encrypted credentials |
| **Background** | work-runtime-ktx 2.10.0 | ✅ READY | Background tasks |
| **Charts** | Vico 2.4.0 | ✅ READY | UI visualization |
| **Logging** | Timber 5.0.1 | ✅ READY | Debug logging |
| **Settings** | datastore-preferences 1.1.1 | ✅ READY | User preferences |
| **Analytics** | Firebase BOM 34.7.0 | ✅ READY | Crashlytics + Analytics |

### Module Structure

```
✅ :app                     # Application module (DI wiring)
✅ :core:domain             # Pure Kotlin domain layer - INTERFACES COMPLETE
⏳ :core:data              # Room database + security  
⏳ :core:ui                # Shared UI components
⏳ :exchange:coinbase      # Coinbase implementation
⏳ :feature:dashboard      # Dashboard UI + ViewModel
⏳ :feature:settings       # Settings UI + ViewModel
⏳ :service:trading        # Foreground service
```

**Status:** Foundation complete, domain contracts defined, ready for implementation.

---

## 🔄 What Changed Recently

**Latest Updates:**
- **Repository interfaces complete** - All exchange operations abstracted
- **Error handling defined** - 6 typed error variants for consistent handling
- **Authentication contracts** - Token provider and credential storage interfaces
- **WebSocket abstraction** - Real-time data streams with connection state management
- **Clean architecture enforced** - Domain layer defines all contracts

**Impact:** 
- ✅ Implementation can proceed with clear interfaces
- ✅ Easy to mock for testing
- ✅ Exchange swapping possible with zero domain changes
- ✅ Type-safe error handling across all operations

**Next Priority:** Domain models (Ticket 01) to define the core data types that flow through these interfaces.

---

## 📚 Key Documentation

### Most Important Files
1. **[docs/roadmap.md](docs/roadmap.md)** - Phase-by-phase implementation plan
2. **[docs/reference.md](docs/reference.md)** - Complete implementation guide with code examples  
3. **[docs/tickets/in-review/02-repository-interfaces.md](docs/tickets/done/02-repository-interfaces.md)** - Just completed interface definitions

### API Integration Reference
- **[docs/api/coinbase.md](docs/api/coinbase.md)** - Complete Coinbase API guide
- **[docs/implementation/security.md](docs/implementation/security.md)** - JWT + credential storage examples
- **[docs/implementation/clients.md](docs/implementation/clients.md)** - REST + WebSocket implementation patterns

### Strategy & Architecture
- **[docs/strategy/overview.md](docs/strategy/overview.md)** - Trading strategy + Android architecture
- **[docs/implementation/domain.md](docs/implementation/domain.md)** - Domain models + decision engine examples

---

## 🎯 Immediate Next Steps

**Priority 1:** Domain Models (Ticket 01)
- Define Candle, Order, Portfolio, Decision data classes
- Use BigDecimal for money, Instant for timestamps
- Pure Kotlin, no Android dependencies

**Priority 2:** Room Database (Ticket 03)  
- Entities for persistence
- DAOs with Flow support
- Proper indexing for queries

**Priority 3:** Credential Storage (Ticket 04)
- EncryptedSharedPreferences implementation
- Secure API key storage for Coinbase

**Success Criteria:** By end of Phase 0A, should be able to authenticate with Coinbase API and retrieve account balances.

