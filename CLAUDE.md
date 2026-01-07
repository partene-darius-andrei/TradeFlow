# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-07
**Project Status:** Phase 0A - Authentication Infrastructure (2/6 Complete)
**Current Build:** #30 FAILURE (Kotlin compatibility issues)

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

🆕 UI THEME SYSTEM IMPLEMENTED:
✅ core/ui/src/main/kotlin/com/tradeflow/core/ui/theme/
    ├── Color.kt                       ✅ Trading-focused color scheme
    ├── Spacing.kt                     ✅ Consistent spacing system  
    ├── Theme.kt                       ✅ TradeFlowTheme composable
    ├── ThemePreview.kt               ✅ Preview components
    └── Typography.kt                 ✅ Material 3 typography scale
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
❌ UI screens beyond MainActivity
```

### ⚠️ Current Build Issues

**Build Status:** FAILURE (Kotlin compatibility)
- **Issue:** Compose libraries compiled with Kotlin 2.3.0, project using Kotlin 2.1.0
- **Affected libraries:** compose-2.4.0-api.jar, core-2.4.0-api.jar, compose-m3-2.4.0-api.jar
- **Resolution needed:** Update project Kotlin version to 2.3.0 or downgrade Compose BOM

**Progress:** **UI theme system implemented** ✅ Complete Material 3 theme with trading-focused colors, consistent spacing, and typography. Ready for UI component development once Kotlin compatibility is resolved.

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

**Latest Completion:** UI Theme System ✅ Implemented complete Material 3 theme with:
- Dark-focused color scheme optimized for trading (green/red/orange indicators)
- Consistent spacing system (TradeFlowSpacing: xs/sm/md/lg/xl/xxl)
- Typography scale for different UI elements (display, headline, title, body, label)
- TradeFlowTheme composable for consistent theming across app
- Preview components for theme validation

**Next Up:** Domain models (Ticket 01) - Define Candle, Order, Portfolio, Decision types

### Phase 0B: Trading Logic (Week 2)
- Decision engine (SMA, ADX, ATR)
- Risk manager

### Phase 1: Coinbase Integration
- Complete REST API client
- WebSocket client

### Phase 2: Presentation Layer
- UI components and theme ✅ Theme system complete
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
**Progress:** 2/6 complete - Repository interfaces + credential store done, UI theme system added, need to resolve Kotlin compatibility

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
│           :core:ui                      │  ← UI theme & components
│  🆕 COMPLETE: Theme System              │     
│  - TradeFlowTheme (Material 3)         │
│  - Trading colors (green/red/orange)   │
│  - Consistent spacing system           │
│  - Typography scale                    │
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
✅ **Consistent UI:** Centralized theme system with trading-focused design

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
- UI Theme System Implementation ✅

---

## 🔧 Tech Stack

### Dependencies Status

| Component | Library | Version | Status |
|-----------|---------|---------|---------|
| **Kotlin** | org.jetbrains.kotlin | 2.1.0 | ⚠️ **NEEDS UPDATE** |
| **Compose BOM** | androidx.compose | 2025.12.01 | ⚠️ **COMPATIBILITY ISSUE** |
| **HTTP/WebSocket** | Ktor | 3.3.3 | ✅ Configured |
| **Database** | Room | 2.8.4 | ✅ Configured |
| **DI** | Hilt | 2.57.2 | ✅ Configured |
| **JSON** | kotlinx.serialization | ✅ Configured |
| **JWT** | nimbus-jose-jwt | 9.47 | ✅ Added |
| **TA** | ta4j-core | 0.16 | ✅ Added |
| **Security** | security-crypto | 1.1.0-alpha06 | ✅ Added |
| **Work** | work-runtime-ktx | 2.10.0 | ✅ Added |
| **Logging** | Timber | 5.0.1 | ✅ Configured |

### Missing Dependencies

**None** - All required dependencies are present. Main issue is Kotlin compatibility between project (2.1.0) and Compose libraries (2.3.0).

### Required Configuration Updates

```kotlin
// build.gradle.kts (Project level)
kotlin("android") version "2.3.0"  // Update from 2.1.0

// OR downgrade Compose BOM to compatible version
composeBom = "2024.09.00"  // Compatible with Kotlin 2.1.0
```

---

## 🚨 Current Priority: Fix Build

**Issue:** Kotlin metadata version mismatch
**Impact:** Can't compile UI components or test theme system
**Resolution:** Update Kotlin version or downgrade Compose BOM

**Once resolved, next implementation priorities:**
1. Domain models (Candle, Order, Portfolio, Decision)
2. Room database entities/DAOs  
3. UI base components using the new theme system
