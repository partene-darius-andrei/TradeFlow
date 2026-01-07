# 📊 FEATURE: Dashboard Screen (UI Only)

Effort level: Medium
Priority: High
Blocked by: CORE-UI: Shared Components & Theme
Module: :feature:dashboard

## Objective

Create Dashboard UI components (pure presentation, testable with mock data).

## Module

`:feature:dashboard`

## Screen Layout

```
┌────────────────────────────────────┐
│  BTC-USD    $98,432.50  ▲ 2.3%  │  <- Price Header
├────────────────────────────────────┤
│  Mode: TREND 📈    Running ●  │  <- Status Bar
├────────────────────────────────────┤
│  ┌────────────────────────────────┐  │
│  │  Portfolio                    │  │  <- Portfolio Card
│  │  Total: $12,450.00           │  │
│  │  Cash:  $10,200.00           │  │
│  │  BTC:   $2,250.00 (0.023)    │  │
│  │  Drawdown: 3.2%  HWM: $12.8k │  │
│  └────────────────────────────────┘  │
├────────────────────────────────────┤
│  Active Orders (2)               │  <- Orders Section
│  ┌────────────────────────────────┐  │
│  │ BUY  0.001 @ $97,500 [Grid] │  │
│  └────────────────────────────────┘  │
├────────────────────────────────────┤
│ [Start Engine] [Stop] [🚨 STOP] │  <- Controls
└────────────────────────────────────┘
```

## UI State

```kotlin
data class DashboardUiState(
    val price: PriceUiState = PriceUiState(),
    val mode: ModeUiState = ModeUiState(),
    val portfolio: PortfolioUiState = PortfolioUiState(),
    val orders: List<OrderUiState> = emptyList(),
    val serviceStatus: ServiceStatus = ServiceStatus.STOPPED,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PriceUiState(
    val productId: String = "BTC-USD",
    val current: String = "--",
    val change24h: String = "--",
    val changePercent: String = "--",
    val isUp: Boolean? = null
)

enum class ServiceStatus { STOPPED, STARTING, RUNNING, STOPPING, ERROR }
```

## Screen Composable

```kotlin
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onStartEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onEmergencyStop: () -> Unit,
    onCancelOrder: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pure presentation - no business logic
}
```

## File Structure

```
feature/dashboard/src/main/kotlin/com/tradeflow/feature/dashboard/
├── ui/
│   ├── DashboardScreen.kt
│   ├── PriceHeader.kt
│   ├── StatusBar.kt
│   ├── PortfolioSection.kt
│   ├── OrdersSection.kt
│   └── ControlsBar.kt
└── model/
    └── DashboardUiState.kt
```

## Acceptance Criteria

- [ ]  All components have @Preview
- [ ]  Uses shared components from :core:ui
- [ ]  No business logic in composables
- [ ]  Responsive to different screen sizes
- [ ]  Testable with mock DashboardUiState