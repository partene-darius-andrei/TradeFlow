# 🎨 UI Implementation Plan - TradeFlow

**Created:** 2026-01-07
**Status:** Ready for Review and Implementation
**Priority:** Do BEFORE trading logic

---

## Overview

This document provides the master plan for implementing TradeFlow's user interface. All UI work should be completed BEFORE implementing trading logic, as the app needs a way to enter credentials and view status.

---

## Why UI First?

1. **Can't use the app without UI** - No way to enter API credentials
2. **Better testing** - Can test flows with mock data before real trading
3. **Faster iteration** - UI changes don't require recompiling backend logic
4. **User feedback** - Can demo app and get feedback before risky trading features
5. **Parallel work** - UI and trading logic can be developed independently

---

## Ticket Overview

| Ticket | Title | Priority | Effort | Blocked By | Description |
|--------|-------|----------|--------|------------|-------------|
| **20** | UI Design Overview | Reference | N/A | None | Wireframes, design system spec, component library |
| **21** | Core UI - Theme | CRITICAL | Small | None | Material 3 theme, colors, typography, spacing |
| **22** | Core UI - Components | HIGH | Medium | 21 | StatusCard, LoadingButton, PriceDisplay, ModeIndicator, ErrorDisplay |
| **23** | Login/Credentials Screen | CRITICAL | Medium | 21, 22 | Credential input, validation, save to SecureCredentialStore |
| **24** | App Navigation | HIGH | Medium | 21, 23, 25, 26 | NavHost, bottom nav, routing, auth flow |
| **25** | Dashboard Screen | HIGH | Medium | 21, 22 | Portfolio, mode, service controls, orders (mock data) |
| **26** | Settings Screen | MEDIUM | Medium | 21, 22 | Credentials, trading params, notifications, about |

---

## Implementation Order

### Phase 1: Foundation (Tickets 21-22)
**Goal:** Establish theme and reusable components

```
21. Theme & Design System (1 day)
    ↓
22. Base Components (1-2 days)
```

**Deliverables:**
- ✅ TradeFlowTheme composable
- ✅ Dark theme with trading colors (green/red/orange)
- ✅ Material 3 integration
- ✅ Reusable components (cards, buttons, displays)

**Test:** Run preview functions in Android Studio

---

### Phase 2: Authentication (Ticket 08)
**Goal:** Users can enter and save credentials

```
23. Login/Credentials Screen (2 days)
```

**Deliverables:**
- ✅ Full-screen login UI
- ✅ API key and secret input fields
- ✅ Form validation
- ✅ Save to SecureCredentialStore
- ✅ Test connection button (stubbed)

**Test:** Can enter credentials and they persist after app restart

---

### Phase 3: Main Screens (Tickets 25-26)
**Goal:** Build skeleton screens with mock data

```
25. Dashboard Screen (1-2 days)
    ‖
26. Settings Screen (1-2 days)
```

**Deliverables:**
- ✅ Dashboard with portfolio, mode, service controls, orders
- ✅ Settings with credentials, preferences, about
- ✅ All UI working with mock data
- ✅ Service start/stop button functional (local state only)

**Test:** Can navigate between screens, see mock data, interact with controls

---

### Phase 4: Navigation (Ticket 09)
**Goal:** Wire everything together

```
24. App Navigation (1 day)
```

**Deliverables:**
- ✅ NavHost with routes
- ✅ Bottom navigation between Dashboard and Settings
- ✅ Auth flow (no credentials → Login → Dashboard)
- ✅ Logout flow (Settings → Login)

**Test:** Complete user journey from first launch to dashboard to settings

---

## Total Effort Estimate

- **Phase 1:** 2-3 days
- **Phase 2:** 2 days
- **Phase 3:** 2-4 days (can be parallel)
- **Phase 4:** 1 day

**Total:** ~7-10 days for complete UI foundation

---

## Success Criteria

### Phase 1 Complete When:
- [x] Theme applied in previews
- [x] All base components have working previews
- [x] Colors and spacing match design spec

### Phase 2 Complete When:
- [x] User can enter credentials
- [x] Credentials are validated (format check)
- [x] Credentials saved to SecureCredentialStore
- [x] Can navigate to dashboard after save

### Phase 3 Complete When:
- [x] Dashboard shows all sections with mock data
- [x] Settings shows all sections
- [x] Service button toggles state
- [x] Logout button shows confirmation dialog

### Phase 4 Complete When:
- [x] User journey works end-to-end
- [x] Bottom nav switches screens correctly
- [x] Auth flow routes correctly
- [x] Back button behavior is correct

### MVP UI Complete When:
- [x] App launchable and navigable
- [x] Can enter and persist credentials
- [x] Can view dashboard (mock data)
- [x] Can access settings
- [x] Can logout
- [x] All screens look professional in dark theme
- [x] No crashes, no navigation bugs

---

## What Comes AFTER UI Foundation

Once UI is complete, implement in this order:

1. **Domain Models** (Ticket 01) - Candle, Order, Portfolio, Decision
2. **Room Database** (Ticket 03) - Entities, DAOs, migrations
3. **JWT Generator** (Ticket 07) - Coinbase authentication
4. **REST API Client** (Ticket 08) - Connect to real Coinbase data
5. **ViewModels** - Wire UI to real data sources
6. **Decision Engine** (Ticket 05) - Trading logic
7. **Trading Service** (Ticket 16) - Background execution

---

## Design Principles

### 1. Dark Theme First
- Reduce eye strain for 24/7 monitoring
- Better for low-light environments
- Industry standard for trading apps
- Green/red indicators pop better on dark backgrounds

### 2. Information First
- No unnecessary animations or distractions
- Data density over whitespace
- Clear hierarchy (portfolio → mode → orders)
- Status always visible

### 3. Mobile-First Navigation
- Bottom nav for easy thumb reach
- Two main destinations only (Dashboard, Settings)
- Minimal depth (max 2 levels)
- Clear back navigation

### 4. Trust Through Transparency
- Show exactly which credentials are stored
- Display trading parameters clearly
- Explain what the app is doing
- Security assurances visible

### 5. Stateless Components
- All components take data as parameters
- No internal state management
- Easy to test with @Preview
- Reusable across screens

---

## Mock Data Strategy

During UI development, use hardcoded mock data:

**Portfolio:**
```kotlin
val mockPortfolio = Portfolio(
    totalValue = BigDecimal("10245.30"),
    btcAmount = BigDecimal("0.15420000"),
    usdAmount = BigDecimal("745.30"),
    changePercent = BigDecimal("0.0245")
)
```

**Trading Mode:**
```kotlin
val mockMode = TradingMode.TREND
val mockPrice = BigDecimal("61582.00")
val mockAdx = 32
```

**Orders:**
```kotlin
val mockOrders = emptyList<Order>() // Empty state
```

**Why?**
- Develop UI independently of backend
- Test all states (loading, error, success, empty)
- Faster iteration
- Demo-able before trading logic exists

When ViewModels are implemented, replace mock data with:
- `StateFlow<UiState>` from ViewModel
- Data from repositories
- Real-time updates

---

## File Structure

```
TradeFlow/
├── core/
│   └── ui/
│       └── src/main/kotlin/com/tradeflow/core/ui/
│           ├── theme/
│           │   ├── Color.kt
│           │   ├── Typography.kt
│           │   ├── Spacing.kt
│           │   └── Theme.kt
│           ├── component/
│           │   ├── StatusCard.kt
│           │   ├── LoadingButton.kt
│           │   ├── PriceDisplay.kt
│           │   ├── ModeIndicator.kt
│           │   └── ErrorDisplay.kt
│           └── extension/
│               └── BigDecimalExt.kt
│
└── app/
    └── src/main/java/com/dpart/tradeflow/
        ├── navigation/
        │   ├── Screen.kt
        │   ├── AppNavHost.kt
        │   └── BottomNavBar.kt
        └── presentation/
            ├── login/
            │   ├── LoginScreen.kt
            │   ├── LoginViewModel.kt
            │   └── LoginUiState.kt
            ├── dashboard/
            │   ├── DashboardScreen.kt
            │   └── components/
            │       ├── PortfolioCard.kt
            │       ├── ModeCard.kt
            │       ├── ServiceCard.kt
            │       └── OrdersList.kt
            └── settings/
                ├── SettingsScreen.kt
                └── components/
                    ├── CredentialsSection.kt
                    ├── TradingSection.kt
                    ├── NotificationsSection.kt
                    └── AboutSection.kt
```

---

## Testing Strategy

### During Development (Per Ticket)
- Use `@Preview` annotations for rapid iteration
- Test in Android Studio preview panel
- No need to run app for UI-only changes

### After Each Phase
- **Phase 1:** Run app, verify theme applied
- **Phase 2:** Enter credentials, verify they persist
- **Phase 3:** Navigate screens, interact with controls
- **Phase 4:** Complete user journey end-to-end

### Before Marking UI Complete
- [ ] Fresh install → Login → Dashboard works
- [ ] Logout → Login again works
- [ ] All bottom nav switches work
- [ ] Screen rotation preserves state
- [ ] Dark theme looks professional
- [ ] No hardcoded strings
- [ ] No crashes or blank screens

---

## Dependencies

All dependencies already added to project:

```kotlin
// In core:ui/build.gradle.kts
implementation(project(":core:domain"))
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.compose.ui.tooling.preview)
implementation(libs.vico.compose)
implementation(libs.vico.compose.m3)

// In app/build.gradle.kts
implementation(project(":core:domain"))
implementation(project(":core:data"))
implementation(project(":core:ui"))
implementation(libs.androidx.navigation.compose)
implementation(libs.hilt.navigation.compose)
```

---

## Next Steps

1. **Review this plan** - User approval
2. **Implement Ticket 06** - Theme first
3. **Implement Ticket 07** - Base components
4. **Implement Ticket 08** - Login screen
5. **Implement Tickets 25-26** - Dashboard and Settings (parallel)
6. **Implement Ticket 09** - Wire navigation
7. **Test complete flow**
8. **Mark UI foundation complete** ✅

Then proceed with trading logic implementation.

---

## Questions for User

Before starting implementation:

1. **Design approval?** - Do the wireframes in Ticket 20 match your vision?
2. **Color scheme?** - Happy with green/red/orange on dark background?
3. **Navigation?** - Bottom nav with 2 tabs acceptable? Or prefer drawer?
4. **Priority?** - Should we implement all UI before ANY trading logic?
5. **Notifications?** - What notifications do you want (order filled, mode changed, etc)?

---

## Resources

- **Tickets:** `docs/tickets/refined/20-26-*.md`
- **Design Reference:** `docs/tickets/refined/05-ui-design-overview.md`
- **Roadmap:** `docs/roadmap.md`
- **Architecture:** `docs/strategy/overview.md`

---

**Status:** ✅ Ready for review and implementation
**Next:** User approval → Start Ticket 06 (Theme)
