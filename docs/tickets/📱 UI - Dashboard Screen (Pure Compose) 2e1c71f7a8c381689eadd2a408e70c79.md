# 📱 UI - Dashboard Screen (Pure Compose)

Effort level: Medium
Priority: Medium
Status: Not started

## Objective

Create the dashboard UI using **pure Compose** with no business logic. Can be developed with mock data.

## File

`presentation/ui/dashboard/DashboardScreen.kt`

## UI Components

### 1. Price Header

```kotlin
@Composable
fun PriceHeader(
    currentPrice: BigDecimal,
    priceChange24h: BigDecimal,
    isConnected: Boolean
)
```

- Large price display
- 24h change with color (green/red)
- Connection indicator dot

### 2. Mode Indicator

```kotlin
@Composable
fun ModeIndicator(
    mode: TradingMode,
    reason: String?
)

enum class TradingMode {
    WAIT, DEFENSE, TREND, RANGE
}
```

- Color-coded badge (gray/red/green/blue)
- Tooltip with reason

### 3. Portfolio Card

```kotlin
@Composable
fun PortfolioCard(
    totalEquity: BigDecimal,
    cashBalance: BigDecimal,
    btcValue: BigDecimal,
    drawdownPercent: Double,
    highWaterMark: BigDecimal
)
```

- Equity breakdown
- Drawdown progress bar (red when > 10%)
- HWM indicator

### 4. Active Orders List

```kotlin
@Composable
fun ActiveOrdersList(
    orders: List<OrderUiModel>,
    onCancelOrder: (String) -> Unit
)

data class OrderUiModel(
    val id: String,
    val side: String,
    val price: String,
    val size: String,
    val status: String,
    val gridLevel: Int?  // null if not grid order
)
```

- Swipe to cancel
- Grid level badge for range orders

### 5. Control Buttons

```kotlin
@Composable
fun EngineControls(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEmergencyStop: () -> Unit
)
```

- Start/Stop toggle
- Emergency stop (red, with confirmation dialog)

## Preview Support

```kotlin
@Preview
@Composable
fun DashboardPreview() {
    DashboardScreen(
        state = DashboardUiState(
            currentPrice = BigDecimal("98500.00"),
            mode = TradingMode.TREND,
            // ... mock data
        ),
        onAction = {}
    )
}
```

## NO Business Logic Here

- No repository calls
- No calculations
- Only receives `DashboardUiState`, emits `DashboardAction`

## Acceptance Criteria

- [ ]  All components have @Preview
- [ ]  Works with mock DashboardUiState
- [ ]  Responsive layout (phone + tablet)
- [ ]  Dark mode support
- [ ]  No ViewModel dependencies in composables