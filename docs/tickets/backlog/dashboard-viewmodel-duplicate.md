# 🧠 UI - Dashboard ViewModel

Effort level: Medium
Priority: Medium
Status: Not started

## Objective

ViewModel that bridges domain use cases to UI state. **No Coinbase imports allowed.**

## File

`presentation/viewmodel/DashboardViewModel.kt`

## State Definition

```kotlin
data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    
    // Price
    val currentPrice: BigDecimal = [BigDecimal.ZERO](http://BigDecimal.ZERO),
    val priceChange24h: BigDecimal = [BigDecimal.ZERO](http://BigDecimal.ZERO),
    val isConnected: Boolean = false,
    
    // Mode
    val mode: TradingMode = TradingMode.WAIT,
    val modeReason: String? = null,
    
    // Portfolio
    val totalEquity: BigDecimal = [BigDecimal.ZERO](http://BigDecimal.ZERO),
    val cashBalance: BigDecimal = [BigDecimal.ZERO](http://BigDecimal.ZERO),
    val btcValue: BigDecimal = [BigDecimal.ZERO](http://BigDecimal.ZERO),
    val drawdownPercent: Double = 0.0,
    val highWaterMark: BigDecimal = [BigDecimal.ZERO](http://BigDecimal.ZERO),
    
    // Orders
    val activeOrders: List<OrderUiModel> = emptyList(),
    
    // Service
    val isServiceRunning: Boolean = false
)

sealed class DashboardAction {
    object StartEngine : DashboardAction()
    object StopEngine : DashboardAction()
    object EmergencyStop : DashboardAction()
    data class CancelOrder(val orderId: String) : DashboardAction()
}
```

## Implementation

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val getActiveOrdersUseCase: GetActiveOrdersUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val webSocketService: ExchangeWebSocketService,  // Interface!
    private val serviceController: TradingServiceController
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()
    
    init {
        observePrice()
        observePortfolio()
        observeOrders()
        observeServiceState()
    }
    
    private fun observePrice() {
        viewModelScope.launch {
            webSocketService.subscribeTicker(listOf("BTC-USD"))
                .collect { ticker ->
                    _state.update { it.copy(
                        currentPrice = ticker.price,
                        isConnected = true
                    )}
                }
        }
    }
    
    fun onAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.StartEngine -> serviceController.start()
            is DashboardAction.StopEngine -> serviceController.stop()
            is DashboardAction.EmergencyStop -> {
                viewModelScope.launch {
                    serviceController.emergencyStop()
                }
            }
            is DashboardAction.CancelOrder -> {
                viewModelScope.launch {
                    cancelOrderUseCase(action.orderId)
                }
            }
        }
    }
}
```

## Depends On

- 🔌 INTERFACE - ExchangeWebSocketService
- 🎯 USECASE - GetPortfolio
- 🎯 USECASE - CancelOrder
- ⚙️ TradingService

## Acceptance Criteria

- [ ]  Uses interfaces only (no Coinbase imports)
- [ ]  Proper error handling in state
- [ ]  Testable with fake repositories
- [ ]  Survives configuration changes