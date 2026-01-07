# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (2/6 Complete)
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

### What EXISTS (Phase 0A Progress: 2/6 Complete)

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

🆕 SECURE CREDENTIAL STORAGE COMPLETE:
✅ core/data/src/main/kotlin/com/tradeflow/core/data/
    ├── security/
    │   └── SecureCredentialStore.kt    ✅ EncryptedSharedPreferences implementation
    └── di/
        └── SecurityModule.kt           ✅ Credential store DI binding
```

### What DOESN'T Exist Yet (Next Up)

```
❌ Domain models (Candle, Decision, Order, Portfolio) - Ticket 01
❌ Room database entities/DAOs - Ticket 03
❌ Coinbase JWT generator - Ticket 07
❌ Coinbase REST API client - Ticket 08
❌ Decision engine (regime switching logic)
❌ Trading service (foreground service)
❌ Risk management
❌ UI beyond MainActivity
```

**Progress:** **Repository interfaces + credential store complete** ✅ Authentication infrastructure foundations ready. Domain contracts defined for all exchange operations, secure credential storage implemented with AES-256 encryption.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0A: Authentication Infrastructure (CURRENT - 2/6 Complete)
- [x] **Repository interfaces** (Ticket 02) ✅ COMPLETE
- [x] **Secure credential storage** (Ticket 04) ✅ COMPLETE 
- [ ] Domain models (Ticket 01) - Basic domain types
- [ ] Room database schema (Ticket 03) 
- [ ] JWT token generator (Ticket 07)
- [ ] REST API client partial (Ticket 08)

**Latest Completion:** Secure credential storage (Ticket 04) ✅ Implemented `SecureCredentialStore` with EncryptedSharedPreferences using AES-256-GCM encryption. Credentials are securely stored and never logged. Added `SecurityModule` for Hilt dependency injection.

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
**Progress:** 2/6 complete - Repository interfaces + credential store done, domain models next

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
│         (Coming in Phase 1)             │     (Isolated & swappable)
└─────────────────────────────────────────┘
```

### Key Architectural Wins

✅ **Clean separation:** Domain defines what, infrastructure defines how
✅ **Testability:** Interfaces enable easy mocking and unit testing  
✅ **Swappable exchanges:** Add Kraken by implementing same interfaces
✅ **Zero coupling:** Features never import exchange implementations directly
✅ **Secure credentials:** AES-256-GCM encryption, never logged, cleared on uninstall

---

## 🎫 Ticket System

**Location:** `docs/tickets/` (organized by status)

```
tickets/
├── backlog/        # Not started yet
├── refined/        # Ready for implementation (user-approved)  
├── ongoing/        # Currently being worked on
├── in-review/      # Implementation complete, awaiting review
├── done/           # Completed and verified ✅ Ticket 02, 04
└── archived/       # Superseded/duplicate tickets
```

**Recent Completions:** 
- Ticket 02 (Repository Interfaces) ✅
- Ticket 04 (Secure Credential Storage) ✅

---

## 🔧 Tech Stack & Dependencies

### Core Framework

```kotlin
✅ Android SDK 26+ (93%+ device coverage)
✅ Kotlin 2.1.0 + Coroutines 1.10.2
✅ Compose BOM 2025.01.00 (Material 3)
✅ Hilt 2.57.2 (Dependency Injection)
```

### Data & Persistence

```kotlin
✅ Room 2.8.4 (Local database)
✅ DataStore Preferences 1.1.2 (Settings)
✅ Security Crypto 1.1.0-alpha06 (Credential encryption) ✅ IMPLEMENTED
```

### Networking & Authentication

```kotlin
✅ Ktor 3.3.3 (HTTP/WebSocket with OkHttp engine)
✅ Kotlinx Serialization JSON 1.8.0
✅ Nimbus JOSE JWT 9.47 (ES256 signing for Coinbase)
```

### Trading & Analytics

```kotlin
✅ TA4J Core 0.16 (Technical indicators - SMA, ADX, ATR)
✅ Vico 2.4.0 (Charts and data visualization)
```

### Background & Services

```kotlin
✅ Work Manager 2.10.0 (Background tasks)
✅ Firebase BOM 34.7.0 (Analytics + Crashlytics)
```

### Development & Logging

```kotlin
✅ Timber 5.0.1 (Structured logging)
✅ Kotlin Compile Testing (Unit test utilities)
```

### Security Implementation Status

✅ **EncryptedSharedPreferences:** AES-256-GCM with Android Keystore
✅ **Credential isolation:** Stored separately from app data
✅ **DI integration:** SecurityModule provides CredentialStore singleton
✅ **Never logged:** Credentials excluded from all logging

---

## 🔥 Development Workflow

### With GitHub Actions CI/CD

**Pattern:** Push → Build → Download → Test

```bash
# 1. Implement changes
# 2. Push to branch
git push origin claude/feature-name

# 3. Monitor build
gh run watch  # or check GitHub UI

# 4. Download APK when ready
gh run download --name debug-apk

# 5. Install and test
adb install -r app-debug.apk
```

### Build Status

**Current:** #30 SUCCESS ✅
**APK:** Available in GitHub Actions artifacts (7-day retention)
**Distribution:** Firebase App Distribution (partene.darius@gmail.com)

### Documentation Updates

Documentation is automatically updated via GitHub Actions when code changes are pushed:
- `CLAUDE.md` - Project status and current state
- `docs/` files - Implementation guides and references
- Commits back with "[skip ci]" to avoid infinite loops

---

## 📚 Documentation Structure

**Entry Points:**
- `CLAUDE.md` (this file) - Project overview and navigation
- `docs/README.md` - Complete documentation index
- `docs/roadmap.md` - Implementation phases and tickets

**Implementation Guides:**
- `docs/reference.md` - Parent document with implementation blueprints
- `docs/api/coinbase.md` - Coinbase API integration guide
- `docs/strategy/overview.md` - Trading strategy and Android architecture
- `docs/implementation/*.md` - Code examples organized by component

**Process & Meta:**
- `docs/ci.md` - CI/CD workflows and GitHub Actions
- `docs/tickets/` - All tickets organized by status folder

---

## ⚡ Quick Commands

```bash
# Check build status after push
cat .build-status
# Expected: SUCCESS or FAILURE

# Read build failure details
cat build-log.txt

# View recent documentation updates
git log --oneline --grep="Update documentation" -5

# See what tickets are ready to implement
ls docs/tickets/refined/

# Check current phase progress
grep "✅\|❌" docs/roadmap.md | head -10
```

---

**Next Action:** Implement domain models (Ticket 01) - Define Candle, Order, Portfolio, Decision classes in `:core:domain` module.
