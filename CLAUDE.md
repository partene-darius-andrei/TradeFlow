# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Vision

**TradeFlow** - Personal automated crypto trading bot for Coinbase Advanced Trade API.

**Goals:**
- Remove human emotions from trading decisions
- Run 24/7 unattended (physical device when proven)
- Simple UI, simple implementation, easy to maintain
- Backtest → Paper trade → Live (small) → Scale
- Never published - personal use only

**Reality Constraints:**
- Fees matter: ~0.25-0.5% per trade on Advanced Trade
- Most retail algo traders lose money - respect this
- Simple strategies often beat complex ML
- Every trade is a taxable event

## Project Management

**Task Tracking:** [TradeFlow Notion Database](https://www.notion.so/2e0c71f7a8c380cf8ae1c02c63987a14?v=2e0c71f7a8c38025a1c8000c923c4485)

All tasks, features, and bugs are tracked in Notion as a Kanban board. Claude Code can read/write tasks via MCP integration (Desktop only).

**Database Properties:**
- **Task name** (Title) - Description of the task
- **Status** (Status) - Workflow state
  - `Not started` - Task created, not yet refined
  - `Refined` - Requirements clear, ready to implement
  - `Ongoing` - Currently being worked on
  - `In review` - Implementation complete, awaiting review/testing
  - `Done` - Completed and verified
- **Priority** (Select) - `High` / `Medium` / `Low`
- **Effort level** (Select) - `Small` / `Medium` / `Large`
- **Assignee** (Person) - Who's responsible (usually you)
- **Due date** (Date) - Target completion date

**Task Workflow:**
1. Create task in Notion with description and priority
2. Refine: Add details, acceptance criteria, mark as `Refined`
3. Claude Code (Desktop) reads task and creates branch `claude/task-description`
4. Implement → Update status to `Ongoing`
5. Build passes → Update status to `In review`
6. Test on device → Move to `Done`

**When to use Notion:**
- ✅ Planning features before implementation
- ✅ Breaking down complex work into tasks
- ✅ Tracking bugs and tech debt
- ✅ Keeping context for Mobile Claude Code sessions
- ❌ Not for in-progress TODOs (use TodoWrite tool during active session)

## Development Workflow

**Remote Development + Testing Pipeline:**

```
┌──────────────────┐
│ Claude Code      │ (Desktop or Mobile)
│ - Implements     │
│ - Pushes branch  │
│ - Creates PR     │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ GitHub Actions   │
│ - Builds APK     │
│ - Runs on PR     │
│ - Uploads to     │
│   Firebase       │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Firebase App     │
│ Distribution     │
│ → Phone          │
│ (Test on device) │
└──────────────────┘
```

**Why this workflow:**
- **Remote development:** Make changes from anywhere, test on actual device
- **No local builds:** Phone is the test environment
- **Fast iteration:** Push → build → test cycle in minutes

**Mobile vs Desktop tradeoffs:**

| Capability | Claude Code Mobile | Claude Code Desktop + IDE |
|------------|-------------------|--------------------------|
| **Basic code changes** | ✅ Yes | ✅ Yes |
| **File edits** | ✅ Yes | ✅ Yes |
| **Git operations** | ✅ Yes | ✅ Yes |
| **MCP Servers** | ❌ No Notion/Coinbase | ✅ Full access |
| **IDE integration** | ❌ No diagnostics | ✅ Live errors |
| **Context depth** | ⚠️ Limited | ✅ Full codebase |
| **Best for** | Small tweaks, fixes | Complex features |

**When to use Mobile:**
- Quick bug fixes
- Small UI tweaks
- Simple refactors
- When away from laptop
- After detailed ticket is written with full context

**When to use Desktop:**
- Complex features requiring Coinbase API docs (MCP)
- Tasks needing Notion integration
- Architecture changes
- Initial feature planning
- Anything requiring deep codebase exploration

**The Feedback Loop (Commit-Back Pattern):**

GitHub Actions commits build results back to the branch, allowing Claude Code to verify changes:

```
1. Claude → implements feature
2. Claude → pushes to branch
3. GitHub Actions → builds
4. GitHub Actions → commits result (.build-status + build-log.txt if failure)
5. Claude → checks committed files to verify build success
```

**Key files for feedback:**
- `.build-status` - Contains "SUCCESS" or "FAILURE"
- `build-log.txt` - Last 200 lines of build output (only on failure)

**How Claude Code uses this:**
After pushing, Claude should check these files to verify the build passed:
```bash
git pull  # Get latest commits from Actions
cat .build-status  # Check if build succeeded
cat build-log.txt  # If failed, read error details
```

This allows Claude to:
- Verify changes compile without local Gradle execution
- Fix build errors by reading `build-log.txt`
- Iterate on fixes until `.build-status` shows SUCCESS
- Ensure PR is ready for testing before user downloads APK

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew clean                  # Clean build
```

## Current Project State (Jan 2026)

**STATUS: Pre-MVP Scaffolding** - Modern Android setup, zero trading logic.

### What Actually Exists

```
com.dpart.tradeflow/
├── MainActivity.kt              ✅ Shows "TradeFlow" text only
├── TradeFlowApp.kt              ✅ Initializes Timber logging
├── di/
│   ├── AppModule.kt             ✅ Empty Hilt module
│   ├── DatabaseModule.kt        ✅ Provides Room database
│   └── NetworkModule.kt         ✅ Provides Ktor HttpClient
└── data/local/
    ├── AppDatabase.kt           ✅ Room DB (empty)
    └── PlaceholderEntity.kt     ✅ Dummy entity
```

### What DOESN'T Exist Yet

```
❌ data/remote/              # No Coinbase API client
❌ data/repository/          # No repositories
❌ domain/model/             # No domain models
❌ domain/repository/        # No repository interfaces
❌ domain/usecase/           # No use cases
❌ presentation/             # No screens beyond MainActivity
❌ trading/engine/           # No trading service
❌ trading/strategy/         # No strategies
❌ trading/risk/             # No risk management
```

**Bottom line:** This is a greenfield project with dependencies configured but no business logic.

## Tech Stack

| Library | Version | Status | Usage |
|---------|---------|--------|-------|
| **Kotlin** | 2.3.0 | ✅ Active | Language |
| **Compose BOM** | 2025.12.01 | ✅ Active | UI framework |
| **Hilt** | 2.57.2 | ✅ Configured | DI modules exist, mostly empty |
| **Room** | 2.8.4 | ⚠️ Scaffolded | Database exists, 1 dummy entity |
| **Ktor** | 3.3.3 | ⚠️ Configured | HttpClient provided, never used |
| **Timber** | 5.0.1 | ✅ Active | Initialized in Application |
| **Vico** | 2.4.0 | ❌ Unused | Dependency added, zero usage |
| **Firebase Analytics** | 34.7.0 | ✅ Active | Monitoring enabled |
| **Firebase Crashlytics** | 34.7.0 | ✅ Active | Error tracking |
| **Coroutines** | 1.10.2 | ❌ Unused | Dependency only |

### Missing Dependencies (Needed for Trading)

| Library | Purpose | Status |
|---------|---------|--------|
| **nimbus-jose-jwt** | JWT ES256 signing for Coinbase | ❌ Not added |
| **ta4j** | Technical indicators (SMA, RSI, etc) | ❌ Not added |
| **security-crypto** | EncryptedSharedPreferences for API keys | ❌ Not added |
| **WorkManager** | Background DCA execution | ❌ Not added |
| **DataStore** | Settings persistence | ❌ Not added |

## Coinbase Advanced Trade API

**REST:** `https://api.coinbase.com/api/v3/brokerage/`
**WebSocket Market:** `wss://advanced-trade-ws.coinbase.com`
**WebSocket User:** `wss://advanced-trade-ws-user.coinbase.com`
**Sandbox:** `https://api-sandbox.coinbase.com` (static responses)

**Auth:** CDP API Keys → JWT tokens (refresh every 2 min for WS)

**Key Channels:** heartbeats, ticker, candles, level2, user, market_trades

## Development Roadmap

**See `docs/plan.md` for detailed implementation blueprint** (2000+ lines of Kotlin code examples, API docs, complete architecture).

**Current Status:** Phase 0 - Nothing implemented beyond scaffolding.

### Phase 0: Foundation (NOT STARTED)
**Required before any trading logic:**
- [ ] Add missing dependencies (nimbus-jose-jwt, ta4j, security-crypto)
- [ ] Choose HTTP library (plan.md recommends OkHttp over Ktor for battery life)
- [ ] Implement JWT token generator for Coinbase ES256 auth
- [ ] Build encrypted credential storage (EncryptedSharedPreferences)
- [ ] Create Room entities (Candle, Order, Portfolio)
- [ ] Implement Coinbase REST API client
- [ ] Implement WebSocket client for real-time data

### Phase 1: Data Foundation (NOT STARTED)
**Coinbase integration + basic strategy:**
- [ ] JWT auth working with Coinbase API
- [ ] Fetch historical candles (H4 timeframe)
- [ ] Calculate indicators: SMA(200), ADX(14), ATR(14)
- [ ] Implement simple regime detection (TREND/RANGE/DEFENSE)
- [ ] Basic settings screen for API key input
- [ ] Dashboard showing current state

### Phase 2: Trading Engine (NOT STARTED)
**Foreground service + order execution:**
- [ ] Foreground service with wake lock
- [ ] Decision engine with strategy logic
- [ ] Order placement (bracket, limit, market)
- [ ] Risk management (position sizing, drawdown limits)
- [ ] Trade logging to Room database
- [ ] Emergency kill switch

### Phase 3: UI & Monitoring (NOT STARTED)
**User interface + trade history:**
- [ ] Trade history screen
- [ ] Performance charts with Vico
- [ ] Real-time price updates
- [ ] Service control (start/stop)

### Future Phases (Vision Only)
- Backtesting engine
- Regime detection with ML
- Advanced strategies (grid, mean reversion)
- On-chain metrics integration

## Build & CI Status

**Current Build:** #27 (Failed - dependency issues)
**CI/CD:** GitHub Actions configured for Android builds
**Distribution:** Firebase App Distribution → partene.darius@gmail.com
**Git Remote:** Not configured (local-only repo)

**See `docs/github_actions.md` for complete CI/CD documentation** (workflow details, troubleshooting, Firebase setup).

## Auto-Documentation

**Documentation updates automatically** when code changes are pushed.

**Workflow:** `.github/workflows/update-docs.yml`

**How it works:**
1. Push to `claude/*` branch or create PR
2. GitHub Actions analyzes git diff
3. Claude API reviews changes
4. Updates `CLAUDE.md` and all `docs/*.md` files
5. Commits updates back to branch

**What gets updated:**
- **CLAUDE.md** - Current state, tech stack, dependencies
- **docs/github_actions.md** - Workflow changes, CI/CD updates
- **docs/plan.md** - Roadmap status, feature checkboxes
- **All docs/*.md** - Any relevant documentation

**Benefits:**
- ✅ Works with Mobile Claude Code (no local setup)
- ✅ Documentation never out of sync
- ✅ No manual doc updates needed
- ✅ Same commit-back pattern as build workflow

**See:** `docs/auto_documentation.md` for complete documentation

**Required Secret:** `ANTHROPIC_API_KEY` in GitHub repo settings

## Key Configuration

- **Package:** `com.dpart.tradeflow`
- **Min SDK:** 24 / **Target SDK:** 36
- **JVM:** 17
- **Gradle:** 8.13.2
- **Kotlin:** 2.3.0
- **Dependencies:** `gradle/libs.versions.toml`

## About plan.md

The root `plan.md` file (~2000 lines) is a **comprehensive implementation blueprint**, not current state:
- Complete Coinbase Advanced Trade API documentation
- Full Kotlin code examples for all components
- JWT auth implementation (ES256)
- WebSocket client with OkHttp
- Decision engine with ta4j indicators
- Trading service with foreground service
- Room database schema
- Risk management logic

**This is aspirational documentation** - treat it as a reference implementation guide, not a status report.

## Critical Rules

1. **Never hardcode API keys** - Encrypted DataStore only
2. **Log every trade** - For debugging and taxes
3. **Paper trade first** - Validate before real money
4. **Account for fees** - In all calculations (0.25-0.60% at Coinbase)
5. **Kill switch always** - Immediate stop capability
6. **Battery optimization** - Request exemption for 24/7 service
