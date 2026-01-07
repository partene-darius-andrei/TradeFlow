# 📊 [SUPERSEDED] UI - Dashboard & Status Screen

Effort level: Medium
Priority: Low
Status: Done
Blocked by: Replaced by: 📊 FEATURE: Dashboard Screen (UI Only) + 📊 FEATURE: Dashboard ViewModel (Logic)

## Objective

Create main dashboard showing trading status.

## Files

- `ui/screens/DashboardScreen.kt`
- `ui/viewmodel/DashboardViewModel.kt`

## Display Elements

### Header

- Current price (from WebSocket)
- Current mode (DEFENSE/TREND/RANGE/WAIT)
- Service status (Running/Stopped)

### Portfolio Card

- Total equity USD
- Cash balance
- BTC value
- Current drawdown %
- High water mark

### Active Orders

- List of open orders
- Show: side, price, size, status
- Grid level indicator for grid orders

### Controls

- **Start Engine** button
- **Stop Engine** button
- **Emergency Stop** button (red, with confirmation)

## Data Flow

```kotlin
@HiltViewModel
class DashboardViewModel : ViewModel() {
    val currentPrice: StateFlow<Double>
    val currentMode: StateFlow<String>
    val portfolio: StateFlow<PortfolioSnapshot?>
    val activeOrders: StateFlow<List<OrderEntity>>
    val serviceRunning: StateFlow<Boolean>
}
```

## Service Communication

- Use `ServiceConnection` to bind to TradingService
- Or use Room + Flow to observe state changes

## Acceptance Criteria

- Shows real-time price updates
- Mode changes reflected immediately
- Can start/stop service from UI
- Emergency stop works with confirmation