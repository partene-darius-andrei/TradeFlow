# TradeFlow - Revised Implementation Roadmap

**Last Updated:** 2026-01-07
**Current Phase:** Phase 0A → Phase 0B (UI Layer)
**Status:** Foundation complete, ready for UI implementation

---

## 🎯 Correct Implementation Order

### ✅ Phase 0A: Foundation (COMPLETE)
**Goal:** Core domain, data structures, and secure credential storage

| Ticket | Title | Status | Why First |
|--------|-------|--------|-----------|
| **01** | Domain Models | ✅ Done | Need data types for everything |
| **02** | Repository Interfaces | ✅ Done | Define contracts for data access |
| **03** | Room Database | ✅ Done | Need persistence layer |
| **04** | Secure Credential Storage | ✅ Done | Must store API keys securely |

**Deliverables:**
- ✅ Candle, Order, Portfolio, Decision models
- ✅ ExchangeRepository, AuthTokenProvider interfaces
- ✅ Room database with entities/DAOs
- ✅ SecureCredentialStore with AES-256 encryption

---

### 🎨 Phase 0B: UI Layer (CURRENT - Ready to Start)
**Goal:** Build user interface to enter credentials and view status

**Why Now?** Can't use the app without UI. Need way to enter API credentials and see what's happening.

| Ticket | Title | Priority | Effort | Description |
|--------|-------|----------|--------|-------------|
| **05** | UI Design Overview | Reference | N/A | Wireframes, design system (read first) |
| **06** | Core UI - Theme | CRITICAL | Small | Material 3 theme, colors, typography |
| **07** | Core UI - Base Components | HIGH | Medium | StatusCard, LoadingButton, PriceDisplay, etc |
| **08** | Login/Credentials Screen | CRITICAL | Medium | API key entry + save to SecureCredentialStore |
| **09** | App Navigation | HIGH | Medium | NavHost, bottom nav, routing |
| **10** | Dashboard Screen | HIGH | Medium | Portfolio, mode, service controls (mock data) |
| **11** | Settings Screen | MEDIUM | Medium | Credentials, preferences, notifications |

**Build Order:** 06 → 07 → 08 → 10/11 → 09

**Deliverables:**
- ✅ Complete UI with dark theme
- ✅ Login flow working
- ✅ Dashboard and Settings screens functional (mock data)
- ✅ Can enter/save credentials
- ✅ Can navigate app

**Estimated Time:** 7-10 days

---

### 🔌 Phase 1: Coinbase Integration
**Goal:** Connect to real Coinbase API and fetch live data

**Why Now?** UI exists, can now show real data instead of mocks.

| Ticket | Title | Priority | Effort | Description |
|--------|-------|----------|--------|-------------|
| **12** | JWT Generator | HIGH | Medium | ES256 token signing for Coinbase auth |
| **13** | REST API Client | HIGH | Large | Complete CoinbaseRepository implementation |
| **14** | WebSocket Client | HIGH | Large | Real-time price and order updates |

**Dependencies:**
- Needs Tickets 04 (credentials) and 08 (login screen)
- Will replace mock data in UI

**Deliverables:**
- ✅ JWT tokens generated correctly
- ✅ Can call Coinbase REST endpoints
- ✅ WebSocket streaming works
- ✅ Dashboard shows REAL portfolio data
- ✅ Orders display in real-time

**Estimated Time:** 5-7 days

---

### 🧠 Phase 2: Trading Logic
**Goal:** Implement decision engine and risk management

**Why Now?** Have UI to monitor, have API to trade, now add the brain.

| Ticket | Title | Priority | Effort | Description |
|--------|-------|----------|--------|-------------|
| **15** | Decision Engine | HIGH | Large | SMA, ADX, ATR indicators + regime switching |
| **16** | Risk Manager | HIGH | Medium | Position sizing, drawdown limits, emergency stop |

**Dependencies:**
- Needs domain models (01)
- Needs API client (13) to place orders
- Needs Dashboard (10) to show mode

**Deliverables:**
- ✅ Engine detects DEFENSE/TREND/RANGE modes
- ✅ Risk manager enforces limits
- ✅ All logic unit tested

**Estimated Time:** 5-7 days

---

### ⚙️ Phase 3: Trading Service
**Goal:** 24/7 background execution

| Ticket | Title | Priority | Effort | Description |
|--------|-------|----------|--------|-------------|
| **17** | Trading Service | HIGH | Large | Foreground service with trading loop |
| **18** | Battery Optimization | MEDIUM | Small | Doze exemption, wake locks |

**Dependencies:**
- Needs everything above

**Deliverables:**
- ✅ Service runs 24/7
- ✅ Survives screen-off
- ✅ Start/stop from Dashboard UI

**Estimated Time:** 3-5 days

---

### 🧪 Phase 4: Testing & Polish
**Goal:** Validate system works end-to-end

| Ticket | Title | Priority | Effort |
|--------|-------|----------|--------|
| **19** | Integration Tests | MEDIUM | Medium |
| **20** | MVP Milestone | HIGH | Small |

---

## 📊 Progress Tracking

### Overall Progress: 4/20 tickets complete (20%)

```
Phase 0A: ████████████████████ 100% (4/4) ✅
Phase 0B: ░░░░░░░░░░░░░░░░░░░░   0% (0/7) ← YOU ARE HERE
Phase 1:  ░░░░░░░░░░░░░░░░░░░░   0% (0/3)
Phase 2:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
Phase 3:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
Phase 4:  ░░░░░░░░░░░░░░░░░░░░   0% (0/2)
```

### Current Sprint: Phase 0B - UI Layer

**Next Up:** Ticket 06 (Core UI Theme)

---

## 🎯 Why This Order Makes Sense

### 1. Foundation First (Tickets 01-04)
- **Domain models** define what data looks like
- **Interfaces** define contracts
- **Database** provides persistence
- **Credentials** secure API keys
- **Result:** Solid base to build on

### 2. UI Second (Tickets 05-11)
- **Can't use app** without UI to enter credentials
- **Mock data** lets us build UI independently
- **Visual progress** - can demo app early
- **Testing** - Easier to test with visual feedback
- **Result:** Usable app that looks complete

### 3. API Third (Tickets 12-14)
- **Now have UI** to show real data
- **Already have credentials** from login screen
- **Replace mocks** with real API calls
- **Result:** App connected to live Coinbase data

### 4. Trading Logic Fourth (Tickets 15-16)
- **Have everything needed** - UI, API, data
- **Can monitor visually** in Dashboard
- **Can test safely** with UI controls
- **Result:** Smart decision-making brain

### 5. Service Fifth (Tickets 17-18)
- **Everything works** in foreground first
- **Move to background** for 24/7 operation
- **Result:** Autonomous trading bot

---

## 🚀 Getting Started with Phase 0B

**Read First:** Ticket 05 (UI Design Overview) - See wireframes and design principles

**Then Build:**
1. **Ticket 06** - Theme (1 day) - Colors, typography, spacing
2. **Ticket 07** - Components (1-2 days) - Reusable UI pieces
3. **Ticket 08** - Login (2 days) - Credential entry screen
4. **Tickets 10-11** - Screens (2-4 days) - Dashboard + Settings with mock data
5. **Ticket 09** - Navigation (1 day) - Wire everything together

**Test:** Complete user journey from fresh install → login → dashboard → settings

---

## 📝 Ticket Locations

```
docs/tickets/
├── done/           # 01, 02, 03 ✅
├── in-review/      # 04 (awaiting review)
├── refined/        # 05-11 (UI tickets - ready to build)
└── backlog/        # 12-20 (future work)
```

---

## ✅ Success Metrics

### Phase 0B Exit Criteria (UI Complete)
- [ ] User can enter and save credentials
- [ ] User can navigate between Dashboard and Settings
- [ ] Dashboard shows portfolio (mock data)
- [ ] Dashboard shows trading mode indicator
- [ ] Dashboard has service start/stop button
- [ ] Settings shows credential info
- [ ] Settings has logout button
- [ ] All screens look professional in dark theme
- [ ] No crashes, smooth navigation

### When Phase 0B is complete:
- ✅ **App is usable** - Can enter credentials and navigate
- ✅ **Looks complete** - Professional UI, ready to demo
- ✅ **Ready for data** - Can swap mock data for real API calls
- ➡️ **Move to Phase 1** - Implement Coinbase API integration

---

**Current Focus:** Phase 0B - Build UI Layer
**Next Ticket:** 06 - Core UI Theme
**Estimated Completion:** 7-10 days for full UI
