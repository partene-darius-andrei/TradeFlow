# 🎨 UI Design Overview - TradeFlow

**Status:** Documentation / Reference
**Created:** 2026-01-07
**Priority:** CRITICAL (Blocks all presentation work)

---

## 📱 App Flow Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      APP LAUNCH                             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
            ┌────────────────┐
            │  Splash Check  │ (Check if credentials exist)
            └────────┬───────┘
                     │
        ┌────────────┴────────────┐
        │ NO                      │ YES
        ▼                         ▼
┌───────────────┐         ┌──────────────┐
│ LOGIN SCREEN  │────────▶│  DASHBOARD   │◀────┐
│               │ Success │              │     │
│ • API Key     │         │ • Portfolio  │     │
│ • Secret Key  │         │ • Mode       │     │
│ • Validate    │         │ • Orders     │     │
│ • Save        │         │ • Service    │     │
└───────────────┘         └──────┬───────┘     │
                                 │             │
                                 │  Bottom     │
                                 │  Nav        │
                                 ▼             │
                         ┌──────────────┐      │
                         │   SETTINGS   │──────┘
                         │              │
                         │ • Credentials│
                         │ • Preferences│
                         │ • About      │
                         └──────────────┘
```

---

## 🎨 Design Principles

### Visual Identity

**Trading-Focused Dark Theme:**
- Primary: Green (#4CAF50) - Profits, bullish, active states
- Secondary: Red (#F44336) - Losses, bearish, warnings
- Tertiary: Orange (#FF9800) - Caution, pending states
- Background: Near-black (#121212) - Reduce eye strain for 24/7 monitoring
- Surface: Dark gray (#1E1E1E) - Card backgrounds

### Typography

**Material 3 Default Typography with Modifications:**
- Display: Roboto Flex (large numbers, prices)
- Headlines: Roboto (screen titles)
- Body: Roboto (general text)
- Monospace: Roboto Mono (order IDs, API keys)

### Spacing System

```kotlin
object TradeFlowSpacing {
    val xs = 4.dp    // Icon padding
    val sm = 8.dp    // List item spacing
    val md = 16.dp   // Card padding (default)
    val lg = 24.dp   // Screen padding
    val xl = 32.dp   // Section spacing
}
```

---

## 📐 Screen Designs (Wireframes)

### 1. Login/Credentials Screen

```
┌────────────────────────────────────────┐
│  ◀  Login to Coinbase                  │
├────────────────────────────────────────┤
│                                        │
│  ┌──────────────────────────────────┐ │
│  │ 🔑 TradeFlow                     │ │
│  │                                  │ │
│  │ Enter your Coinbase Advanced     │ │
│  │ Trade API credentials            │ │
│  └──────────────────────────────────┘ │
│                                        │
│  API Key Name (optional)               │
│  ┌──────────────────────────────────┐ │
│  │ My Trading Bot                   │ │
│  └──────────────────────────────────┘ │
│                                        │
│  API Key *                             │
│  ┌──────────────────────────────────┐ │
│  │ organizations/abc-123/...        │ │
│  └──────────────────────────────────┘ │
│                                        │
│  API Secret *                          │
│  ┌──────────────────────────────────┐ │
│  │ ••••••••••••••••••••             │ │👁
│  └──────────────────────────────────┘ │
│                                        │
│  ┌──────────────────────────────────┐ │
│  │     Test Connection              │ │
│  └──────────────────────────────────┘ │
│                                        │
│  ┌──────────────────────────────────┐ │
│  │       Save & Continue ─────▶     │ │
│  └──────────────────────────────────┘ │
│                                        │
│  ⓘ Credentials are encrypted using   │
│     AES-256 and stored locally        │
│                                        │
└────────────────────────────────────────┘
```

**Key Features:**
- Clear instructions
- Optional name field (for user reference)
- Masked secret input with toggle
- Test connection before saving
- Security assurance message
- Full-screen, no distractions

---

### 2. Dashboard Screen

```
┌────────────────────────────────────────┐
│  TradeFlow            🔔  ⚙           │
├────────────────────────────────────────┤
│                                        │
│  ┌────────────────────────────────┐   │
│  │ Portfolio                       │   │
│  │                                 │   │
│  │ $10,245.30      +$245.30 (+2.5%)│   │
│  │                                 │   │
│  │ BTC: 0.15420000  $9,500.00     │   │
│  │ USD: $745.30                    │   │
│  └────────────────────────────────┘   │
│                                        │
│  ┌────────────────────────────────┐   │
│  │ Current Mode          TREND ↗️  │   │
│  │                                 │   │
│  │ BTC-USD      $61,582.00  +1.2% │   │
│  │ Above SMA(200)  ADX: 32        │   │
│  └────────────────────────────────┘   │
│                                        │
│  ┌────────────────────────────────┐   │
│  │ Service Status                  │   │
│  │                                 │   │
│  │     ⏸ PAUSED                   │   │
│  │                                 │   │
│  │  [  ▶  Start Trading Service ] │   │
│  └────────────────────────────────┘   │
│                                        │
│  Recent Orders             View All ▶  │
│  ┌────────────────────────────────┐   │
│  │ BUY  0.05 BTC  $60,500 ⏳      │   │
│  │ 5 minutes ago                   │   │
│  ├────────────────────────────────┤   │
│  │ SELL 0.03 BTC  $61,200 ✓       │   │
│  │ 2 hours ago                     │   │
│  └────────────────────────────────┘   │
│                                        │
├────────────────────────────────────────┤
│  📊 Dashboard      ⚙ Settings         │
└────────────────────────────────────────┘
```

**Key Features:**
- Portfolio value at-a-glance
- Current trading mode with indicator
- Service start/stop controls
- Recent order feed
- Bottom navigation

---

### 3. Settings Screen

```
┌────────────────────────────────────────┐
│  Settings                              │
├────────────────────────────────────────┤
│                                        │
│  Account                               │
│  ┌────────────────────────────────┐   │
│  │ 🔑 API Credentials              │   │
│  │ organizations/abc-...      ▶   │   │
│  └────────────────────────────────┘   │
│                                        │
│  Trading                               │
│  ┌────────────────────────────────┐   │
│  │ Max Position Size               │   │
│  │ 5% of portfolio            ▶   │   │
│  ├────────────────────────────────┤   │
│  │ Max Drawdown Limit              │   │
│  │ 15%                        ▶   │   │
│  ├────────────────────────────────┤   │
│  │ Emergency Stop                  │   │
│  │ Enabled                    [✓] │   │
│  └────────────────────────────────┘   │
│                                        │
│  Notifications                         │
│  ┌────────────────────────────────┐   │
│  │ Order Filled                [✓] │   │
│  ├────────────────────────────────┤   │
│  │ Mode Changed                [✓] │   │
│  ├────────────────────────────────┤   │
│  │ Emergency Stop              [✓] │   │
│  └────────────────────────────────┘   │
│                                        │
│  About                                 │
│  ┌────────────────────────────────┐   │
│  │ Version 1.0.0-alpha             │   │
│  ├────────────────────────────────┤   │
│  │ Logs                        ▶   │   │
│  ├────────────────────────────────┤   │
│  │ Privacy Policy              ▶   │   │
│  └────────────────────────────────┘   │
│                                        │
├────────────────────────────────────────┤
│  📊 Dashboard      ⚙ Settings         │
└────────────────────────────────────────┘
```

**Key Features:**
- Credential management
- Trading parameters (readonly for now)
- Notification preferences
- About/version info

---

## 🧩 Component Library

### Core Components to Build

| Component | Purpose | Complexity |
|-----------|---------|------------|
| **TradeFlowTheme** | App-wide theme | Low |
| **StatusCard** | Reusable card container | Low |
| **PriceDisplay** | Price with color coding | Low |
| **ModeIndicator** | Visual mode display (DEFENSE/TREND/RANGE) | Medium |
| **PortfolioCard** | Portfolio summary display | Medium |
| **OrderItem** | Single order list item | Medium |
| **LoadingButton** | Button with loading state | Low |
| **ServiceControlCard** | Start/stop service UI | Medium |
| **CredentialInputField** | Secure text input with validation | Medium |
| **ErrorDisplay** | Error state with retry | Low |

---

## 🎯 Implementation Priority

### Phase 1: Foundation (Tickets 21-22)
1. **Theme & Design System** - Colors, typography, spacing
2. **Base Components** - Cards, buttons, displays

### Phase 2: Authentication (Ticket 23)
3. **Login Screen** - Credential input and validation

### Phase 3: Navigation (Ticket 24)
4. **App Navigation** - NavHost, bottom nav, routing

### Phase 4: Main Screens (Tickets 25-26)
5. **Dashboard Screen** - Portfolio, mode, orders (with mock data)
6. **Settings Screen** - Credentials, preferences

---

## 🔗 Related Tickets

- **Ticket 21:** Core UI - Theme & Design System
- **Ticket 22:** Core UI - Base Components
- **Ticket 23:** Login/Credentials Screen
- **Ticket 24:** App Navigation Setup
- **Ticket 25:** Dashboard Screen (Skeleton)
- **Ticket 26:** Settings Screen (Skeleton)

---

## 📝 Design Notes

### Why Dark Theme by Default?
- Traders monitor screens 24/7 - reduce eye strain
- Better visibility in low-light environments
- Industry standard for trading apps
- Green/red price indicators pop better on dark backgrounds

### Why Bottom Navigation?
- Mobile-first: Easy thumb reach
- Two main sections only (Dashboard, Settings)
- Material 3 best practice for 2-5 top-level destinations

### Why Mock Data Initially?
- Develop UI independently of backend
- Test all UI states (loading, error, success, empty)
- Faster iteration on design
- Can demo app before trading logic is complete

---

## ✅ Success Criteria

- [ ] User can enter and save credentials
- [ ] User can navigate between Dashboard and Settings
- [ ] All screens render correctly in dark theme
- [ ] Components are reusable across screens
- [ ] UI looks professional and trading-focused
- [ ] No hardcoded strings (use string resources)
- [ ] All components have @Preview annotations

---

## 🎨 Visual Inspiration

**Similar Apps (for reference only):**
- Coinbase Pro (clean, dark, professional)
- Binance (information density, color coding)
- TradingView (charts, indicators, dark theme)
- Robinhood (simple onboarding, clear CTAs)

**Key Takeaway:** Simple, professional, dark, information-first, minimal distractions.
