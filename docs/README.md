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
| **Phase 1** | 00-11 | Foundation + UI + Coinbase Integration ✅ COMPLETE |
| **Phase 2** | 13-16 | Core Trading Logic (REST API, WebSocket, Decision Engine, Risk Manager) |
| **Phase 3** | 19-20 | Testing & MVP Validation |

**Architecture:** Foreground app (no service needed) - high process priority, trading loop as coroutine

### Detailed Ticket Mapping

**✅ Phase 1: COMPLETE (Tickets 00-11)**
- 00: Modularization → `🏗️ PROJECT Modularization`
- 01: Domain Models → `📦 DOMAIN Core Domain Models`
- 02: Repository Interfaces → `🔌 EXCHANGE-API Repository Interfaces`
- 03: Room Database → `🗄️ INFRA Room Database`
- 04: Credential Store → `🔐 CORE-DATA Secure Credential Store`
- 05: UI Design Overview → `🎨 UI Design Overview`
- 06: Core UI Theme → `🎨 CORE-UI Theme`
- 07: Core UI Base Components → `🎨 CORE-UI Base Components`
- 07: JWT Generator → `🟡 COINBASE JWT Token Generator`
- 08: Login Credentials Screen → `🔐 Login Screen` (replaced by build-time injection)
- 09: App Navigation → `📱 APP Navigation`
- 10: Dashboard Screen Skeleton → `📊 Dashboard Screen (Mock Data)`
- 10A: Dashboard Real Portfolio → `📊 Dashboard with Live Coinbase Data`
- 11: Settings Screen Skeleton → `⚙️ Settings Screen`

**🎯 Phase 2: Core Trading Logic (NEXT)**
- 13: Full REST API Client → `🟡 COINBASE Complete REST API` (orders, candles)
- 14: WebSocket Client → `🟡 COINBASE Real-time Feeds`
- 15: Decision Engine → `🧠 DOMAIN Decision Engine` (SMA/ADX/ATR)
- 16: Risk Manager → `🚨 DOMAIN Risk Manager` (Position sizing, stops)

**Phase 3: Testing & Validation**
- 19: Integration Tests → `🧪 TEST Integration Tests`
- 20: MVP Milestone → `🚀 Milestone MVP Ready for Testing`

**🗄️ Archived (Not needed):**
- 17: Trading Service → Foreground app doesn't need service
- 18: Battery Optimization → Different concerns for foreground app

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
