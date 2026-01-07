# TradeFlow - Claude Code Entry Point

**Last Updated:** 2026-01-07
**Project Status:** Pre-MVP Scaffolding (Phase 0 - Foundation)
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

### What EXISTS (Phase 0 - Scaffolding Complete)

```
✅ Modern Android app structure
✅ Hilt dependency injection configured
✅ Room database with empty schema
✅ Ktor HTTP client configured (OkHttp engine)
✅ Timber logging initialized
✅ Firebase Analytics + Crashlytics
✅ GitHub Actions CI/CD pipeline
✅ All trading dependencies added (ta4j, nimbus-jose-jwt, security-crypto)
```

### What DOESN'T Exist Yet (Everything Else)

```
❌ Coinbase API integration (JWT auth, REST, WebSocket)
❌ Domain models (Candle, Decision, Order, Portfolio)
❌ Decision engine (regime switching logic)
❌ Trading service (foreground service)
❌ Risk management
❌ UI beyond MainActivity
```

**Bottom Line:** Greenfield project with solid foundation, zero business logic.

---

## 📋 Implementation Roadmap

**See:** [docs/roadmap.md](docs/roadmap.md) for complete roadmap

### Phase 0A: Authentication (NEXT - Week 1)
- Project modularization (8 modules)
- Domain models (basic ones)
- Repository interfaces
- Secure credential storage
- JWT token generator
- REST API (getAccounts endpoint only)

**Goal:** Can authenticate with Coinbase and see real account balances

### Phase 0B: Trading Logic (Week 2)
- Room database schema
- Decision engine (SMA, ADX, ATR)
- Risk manager

### Phase 1: Coinbase Integration
- JWT token generator
- REST API client
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

**Current Progress:** Phase 0A not started (authentication-first approach)

---

## 🎫 Ticket System

**Location:** `docs/tickets/` (organized by status)

```
tickets/
├── backlog/        # Not started yet
├── refined/        # Ready for implementation (user-approved)
├── ongoing/        # Currently being worked on
├── in-review/      # Implementation complete, awaiting review
├── done/           # Completed and verified
└── archived/       # Superseded/duplicate tickets
```

**Workflow:**
1. Check [docs/roadmap.md](docs/roadmap.md) Phase 0A for next ticket
2. Find ticket file in `docs/tickets/backlog/`
3. Create branch: `claude/ticket-##-description`
4. Move ticket: `backlog/` → `ongoing/` (start work)
5. Implement → Build → Test → Commit
6. Move ticket: `ongoing/` → `in-review/` (ready for review)
7. After user approval → move to `done/`
8. Update docs/roadmap.md checkboxes

**Phase 0A Tickets (Authentication - Week 1):**
- Ticket 00: Project Modularization (CRITICAL - DO FIRST)
- Ticket 01: Domain Models
- Ticket 02: Repository Interfaces
- Ticket 04: Credential Store
- Ticket 07: JWT Generator (moved from Phase 1)
- Ticket 08: REST API (partial - getAccounts only)

**See:** [docs/roadmap.md](docs/roadmap.md) for complete timeline and success criteria

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
4. Check .build-status file
5. If FAILURE → read build-log.txt and fix
```

**See:** [docs/ci.md](docs/ci.md) for CI/CD details

---

## 📚 Technical Documentation

### Quick Reference

| What You Need | Read This |
|---------------|-----------|
| **How to implement features** | [docs/reference.md](docs/reference.md) |
| **Coinbase API endpoints** | [docs/api/coinbase.md](docs/api/coinbase.md) |
| **Strategy logic** | [docs/strategy/overview.md](docs/strategy/overview.md) |
| **Code examples** | [docs/implementation/*.md](docs/implementation/) |
| **Dependencies & config** | [docs/implementation/config.md](docs/implementation/config.md) |

### Documentation Structure

```
docs/
├── README.md                       # Complete index and navigation
├── reference.md                     # Parent doc with links to all implementation guides
│
├── api/
│   └── coinbase.md     # REST/WebSocket API reference
│
├── strategy/
│   └── overview.md         # Strategy specification + architecture
│
├── implementation/
│   ├── domain.md              # Domain models + decision engine
│   ├── security.md            # Credential storage + JWT generation
│   ├── clients.md              # REST + WebSocket clients
│   ├── storage.md          # Database + trading service
│   └── config.md            # Gradle + manifest + testing
│
├── ci.md                            # CI/CD workflows
├── auto-docs.md                     # Auto-doc system
│
└── tickets/                        # All ticket files organized by status
    ├── backlog/                    # Not started
    ├── refined/                    # User-approved, ready to implement
    ├── ongoing/                    # In progress
    ├── in-review/                  # Awaiting review
    ├── done/                       # Completed
    └── archived/                   # Superseded/duplicate
```

**All files <1000 lines** with hierarchical links for easy navigation.

**See:** [docs/README.md](docs/README.md) for complete navigation

---

## 🔧 Tech Stack

### Current Dependencies

| Library | Version | Status |
|---------|---------|--------|
| **Kotlin** | 2.3.0 | ✅ Active |
| **Compose BOM** | 2025.12.01 | ✅ Active |
| **Hilt** | 2.57.2 | ✅ Configured (modules empty) |
| **Room** | 2.8.4 | ⚠️ Scaffolded (dummy entity) |
| **Ktor** | 3.3.3 | ⚠️ Configured (OkHttp engine) |
| **Firebase** | 34.7.0 | ✅ Active (Analytics + Crashlytics) |
| **Timber** | 5.0.1 | ✅ Active |

### Trading Dependencies (Added, Not Used)

| Library | Purpose | Version |
|---------|---------|---------|
| **nimbus-jose-jwt** | JWT ES256 signing for Coinbase | 9.47 |
| **ta4j-core** | Technical indicators (SMA, ADX, ATR) | 0.16 |
| **security-crypto** | EncryptedSharedPreferences | 1.1.0-alpha06 |
| **work-runtime-ktx** | Background execution backup | 2.10.0 |
| **datastore-preferences** | Settings persistence | 1.1.1 |

---

## 🎨 Coinbase Integration

**REST API:** `https://api.coinbase.com/api/v3/brokerage/`
**WebSocket Market:** `wss://advanced-trade-ws.coinbase.com`
**WebSocket User:** `wss://advanced-trade-ws-user.coinbase.com`

**Authentication:** CDP API Keys → JWT ES256 tokens (2-minute expiry)

**Key Endpoints:**
- POST `/orders` - Create order
- POST `/orders/batch_cancel` - Cancel orders
- GET `/orders/historical/batch` - Order history
- GET `/accounts` - Account balances
- GET `/products/{id}/candles` - OHLCV data

**WebSocket Channels:**
- `heartbeats` - Keep-alive (required)
- `ticker` - Real-time prices
- `user` - Order updates (auth required)

**See:** [docs/api/coinbase.md](docs/api/coinbase.md) for complete API reference

---

## 📝 Documentation Guidelines

### When to Create New .md Files

| Type | Location | Example | Lifespan |
|------|----------|---------|----------|
| **Temporary analysis** | `docs/temp/session_YYYY-MM-DD_[name].md` | Alignment checks, session summaries | Until action complete |
| **Permanent guide** | `docs/[category]/[name].md` | API guides, workflow docs | Permanent |
| **Critical context** | Update `CLAUDE.md` or `roadmap.md` | Project state, roadmap | Updated continuously |

### Cleanup Protocol

**Monthly:**
- Review `docs/temp/` - archive or delete completed items
- Review `docs/` - ensure all docs are still relevant
- Archive to `docs/archive/YYYY-MM/` if needed

**Per Phase:**
- Update docs/roadmap.md checkboxes
- Update CLAUDE.md "Current Status" section

---

## ⚡ Critical Rules

1. **Never hardcode API keys** - Use EncryptedSharedPreferences only
2. **Log every trade** - For debugging and taxes
3. **Paper trade first** - Validate before real money
4. **Account for fees** - 0.60% maker at intro tier → 1.5% minimum grid spacing
5. **Kill switch always** - Emergency liquidation at 15% drawdown
6. **Battery optimization** - Request exemption for 24/7 service
7. **Build before push** - Desktop: run `./gradlew assembleDebug` first

---

## 🚀 Getting Started (For New Sessions)

### 1. Load Context (5 minutes)
```
Read: CLAUDE.md (this file)
Read: docs/roadmap.md (Phase 0A: Authentication-first strategy)
```

### 2. Find Next Task
```
1. Open docs/roadmap.md
2. Check Phase 0A table (first unchecked ticket)
3. Read ticket file in docs/tickets/backlog/
```

### 3. Implement
```
1. Create branch: claude/ticket-##-description
2. Read docs/reference.md for code examples
3. Implement → Build → Test → Commit
4. Update docs/roadmap.md checkboxes
```

### 4. Verify
```
Desktop: ./gradlew assembleDebug
Mobile: Push → Check .build-status
```

### Week 1 Goal
**By end of Week 1:** Can authenticate with Coinbase and see real account balances
- Day 1: Modularization (Ticket 00)
- Day 2: Domain models (Ticket 01)
- Day 3: Interfaces (Ticket 02)
- Day 4: Credentials + JWT (Tickets 04, 07)
- Day 5: REST API (Ticket 08) → **See real balances!**

---

## 📞 Quick Links

- **Roadmap:** [docs/roadmap.md](docs/roadmap.md) ⭐ START HERE (Phase 0A: Authentication)
- **Docs Hub:** [docs/README.md](docs/README.md)
- **Implementation Guide:** [docs/reference.md](docs/reference.md)
- **CI/CD:** [docs/ci.md](docs/ci.md)
- **Tickets:** [docs/tickets/](docs/tickets/)

---

**Last Build:** #30 SUCCESS (NetworkModule.kt fix - switched to OkHttp engine)
**Next:** Start Phase 0A - Implement Ticket 00 (Modularization)
**Week 1 Goal:** Authentication working, can see real Coinbase balances
