# TradeFlow Documentation

**Last Updated:** 2026-01-07

Complete documentation index and reference guide.

---

## 🔴 Critical (Start Here)

Read these first when starting any work:

- **[roadmap.md](roadmap.md)** - Implementation roadmap
  - Tickets organized across 5 phases
  - Dependency graph and file roadmap
  - Quality gates and success criteria

- **[../CLAUDE.md](../CLAUDE.md)** - Project context for AI
  - Current project state (what exists vs what doesn't)
  - Tech stack and dependencies
  - Development workflow and CI/CD
  - Documentation guidelines

---

## 📘 Implementation Reference

- **[reference.md](reference.md)** - **Parent document** with overview and links to all implementation guides
  - Hierarchical structure with child documents (<1000 lines each)
  - Complete navigation to API, strategy, and code examples

### Child Documents (Organized by Category)

**API Integration:**
- **[api/coinbase.md](api/coinbase.md)** - Complete Coinbase API reference
  - REST endpoints, WebSocket channels, authentication (JWT ES256)
  - Order types (market, limit, bracket), fee structure, candle data

**Strategy & Architecture:**
- **[strategy/overview.md](strategy/overview.md)** - Trading strategy and Android architecture
  - Regime switching logic (DEFENSE/TREND/RANGE)
  - Risk limits, tech stack, project structure

**Implementation Code Examples:**
- **[implementation/domain.md](implementation/domain.md)** - Domain models and decision engine
- **[implementation/security.md](implementation/security.md)** - Credential storage and JWT generation
- **[implementation/clients.md](implementation/clients.md)** - REST and WebSocket clients
- **[implementation/storage.md](implementation/storage.md)** - Database and trading service
- **[implementation/config.md](implementation/config.md)** - Gradle dependencies, manifest, testing

- **[ci.md](ci.md)** - CI/CD workflows
  - Build workflow (assembleDebug → Firebase App Distribution)
  - Auto-documentation workflow
  - Commit-back pattern (`.build-status` + `build-log.txt`)
  - Troubleshooting guide

- **[auto-docs.md](auto-docs.md)** - Auto-doc workflow
  - How it works (Claude API integration)
  - What gets updated automatically
  - File parsing and updates

---

## 🎫 Tickets (All in Repo)

**Location:** `docs/tickets/`

All tickets exported from Notion are maintained in the repository, organized by status:

```
tickets/
├── backlog/        # Not started yet
├── refined/        # Ready for implementation (user-approved)
├── ongoing/        # Currently being worked on
├── in-review/      # Implementation complete, awaiting review
├── done/           # Completed and verified
└── archived/       # Superseded/duplicate tickets
```

**Workflow:** Tickets move through folders as work progresses. See [roadmap.md](roadmap.md) for canonical ticket list.

### Quick Reference (Canonical Tickets)

| Phase | Tickets | Description |
|-------|---------|-------------|
| **Phase 0** | 01-06 | Foundation (models, interfaces, database, credentials, engine, risk) |
| **Phase 1** | 07-09 | Coinbase Integration (JWT, REST API, WebSocket) |
| **Phase 2** | 10-15 | Presentation (UI components, screens, ViewModels, navigation) |
| **Phase 3** | 16-17 | Trading Service (foreground service, battery optimization) |
| **Phase 4** | 18-19 | Testing & MVP Validation |

### Detailed Ticket Mapping

**Phase 0: Foundation**
- 01: Domain Models → `📦 DOMAIN Core Domain Models`
- 02: Repository Interfaces → `🔌 EXCHANGE-API Repository Interfaces`
- 03: Room Database → `🗄️ INFRA - Room Database (Updated)`
- 04: Credential Store → `🔐 CORE-DATA Secure Credential Store`
- 05: Decision Engine → `🧠 DOMAIN Decision Engine`
- 06: Risk Manager → `🚨 DOMAIN - Risk Manager`

**Phase 1: Coinbase Integration**
- 07: JWT Generator → `🟡 COINBASE JWT Token Generator`
- 08: REST API Client → `🟡 COINBASE REST API Client`
- 09: WebSocket Client → `🟡 COINBASE WebSocket Client`

**Phase 2: Presentation Layer**
- 10: Core UI Components → `🎨 CORE-UI Shared Components & Theme`
- 11: Dashboard Screen → `📊 FEATURE Dashboard Screen (UI Only)`
- 12: Dashboard ViewModel → `📊 FEATURE Dashboard ViewModel (Logic)`
- 13: Settings Screen → `⚙️ FEATURE Settings Screen (UI Only)`
- 14: Settings ViewModel → `⚙️ FEATURE Settings ViewModel (Logic)`
- 15: App Navigation → `📱 APP Main Application & Navigation`

**Phase 3: Trading Service**
- 16: Trading Service → `⚡ SERVICE Trading Foreground Service`
- 17: Battery Optimization → `🔋 SERVICE Battery Optimization & Doze`

**Phase 4: Testing & Validation**
- 18: Integration Tests → `🧪 TEST Integration Tests`
- 19: MVP Milestone → `🚀 Milestone MVP Ready for Testing`

### Ticket Organization

```
tickets/
├── backlog/               # Tickets not yet started
├── refined/               # User-approved, ready to implement
├── ongoing/               # Currently being worked on
├── in-review/             # Implementation complete, awaiting review
├── done/                  # Completed and verified
└── archived/              # Superseded/duplicate tickets
```

**File Naming Convention:**
```
[Emoji] [Category] Title [ID].md

Example:
🟡 COINBASE JWT Token Generator 2e1c71f7a8c381c7906df5b5cf0f977e.md
```

**Usage:**
1. Find next ticket in roadmap.md
2. Map to original file name using table above
3. Read detailed ticket file for implementation requirements

**Note:** Some duplicate tickets exist (multiple versions of same concept). Use canonical mapping above for primary tickets.

---

## 📝 Temporary Documentation

**Location:** `docs/temp/`

**Purpose:** Session-specific analysis, planning, and exploration documents.

**Lifecycle:**
- **Keep while:** Action pending or decision needed
- **Archive when:** Action complete (move to `docs/archive/YYYY-MM/`)
- **Delete when:** Truly temporary or superseded

**Current:** Empty (previous session docs deleted after cleanup)

---

## 📂 Directory Structure

```
docs/
├── README.md                  # This file - Complete index
├── reference.md     # Implementation blueprint (~2000 lines)
├── ci.md          # CI/CD workflows
├── auto-docs.md      # Auto-doc workflow
│
├── temp/                      # Temporary session docs
├── archive/                   # Historical documentation
│
└── tickets/                   # All tickets maintained in repo
    ├── archived/              # 10 superseded tickets
    └── [59 original exports]  # Emoji-prefixed ticket files
```

---

## 📋 Documentation Guidelines

**When to create new .md files:**

| Type | Location | Example |
|------|----------|---------|
| Temporary analysis | `temp/session_YYYY-MM-DD_[name].md` | Alignment checks, session summaries |
| Permanent guide | `docs/[name].md` | API guides, workflow docs |
| Critical context | Update `CLAUDE.md` or `roadmap.md` | Project state, architecture decisions |

**Cleanup Schedule:**
- **Weekly:** Review `temp/` folder, delete completed items
- **Monthly:** Archive old temp docs to `archive/YYYY-MM/`
- **Per Phase:** Update roadmap.md checkboxes

**Complete documentation guidelines:** See [CLAUDE.md](../CLAUDE.md) → "Documentation Guidelines" section

---

## 🔍 Quick Reference

**Finding Information:**

| Need | Read |
|------|------|
| Current project state | [CLAUDE.md](../CLAUDE.md) |
| Implementation roadmap | [roadmap.md](roadmap.md) |
| Code examples | [reference.md](reference.md) |
| CI/CD issues | [ci.md](ci.md) |
| Ticket details | Map ticket # → file name (table above) |

**Starting a Claude Code Session:**
1. Read [CLAUDE.md](../CLAUDE.md) (5 min context load)
2. Read [roadmap.md](roadmap.md) (understand roadmap)
3. Check current phase and next ticket
4. Read ticket file for implementation details
5. Reference [reference.md](reference.md) for code examples

**Implementing a Ticket:**
1. Create branch: `claude/ticket-##-description`
2. Read ticket file for requirements
3. Reference reference.md for examples
4. Implement → Build → Test → Commit
5. Update roadmap.md checkboxes when complete

---

**All documentation is maintained in the repository for version control and easy access.**
