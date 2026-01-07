# 📊 FEATURE: Dashboard ViewModel (Logic)

Effort level: Medium
Priority: High
Blocked by: FEATURE: Dashboard Screen (UI Only), EXCHANGE-API: Repository Interfaces
Module: :feature:dashboard

## Objective

Implement Dashboard ViewModel connecting UI to domain/exchange layers.

## Module

`:feature:dashboard`

## ViewModel

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val exchangeWebSocket: ExchangeWebSocket,
    private val decisionEngine: DecisionEngine,
    private val riskManager: RiskManager,
    private val orderDao: OrderDao,
    private val portfolioDao: PortfolioDao,
    private val serviceController: TradingServiceController
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        observePrice()
        observePortfolio()
        observeOrders()
        observeServiceStatus()
    }
    
    private fun observePrice() {
        viewModelScope.launch {
            exchangeWebSocket.subscribeTicker(listOf("BTC-USD"))
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { ticker ->
                    _uiState.update { state ->
                        state.copy(
                            price = state.price.copy(
                                current = ticker.price.formatCurrency(),
                                isUp = ticker.price > state.price.previousPrice
                            )
                        )
                    }
                }
        }
    }
    
    fun startEngine() {
        viewModelScope.launch {
            _uiState.update { it.copy(serviceStatus = ServiceStatus.STARTING) }
            serviceController.start()
        }
    }
    
    fun stopEngine() {
        viewModelScope.launch {
            _uiState.update { it.copy(serviceStatus = ServiceStatus.STOPPING) }
            serviceController.stop()
        }
    }
    
    fun emergencyStop() {
        viewModelScope.launch {
            _uiState.update { it.copy(serviceStatus = ServiceStatus.STOPPING) }
            serviceController.emergencyStop()
        }
    }
    
    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            exchangeRepository.cancelOrder(orderId)
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }
}
```

## Service Controller Interface

```kotlin
interface TradingServiceController {
    val isRunning: StateFlow<Boolean>
    fun start()
    fun stop()
    fun emergencyStop()
}
```

## Dependencies

- `:core:domain` - DecisionEngine, RiskManager, models
- `:core:data` - DAOs
- `:exchange:api` - ExchangeRepository, ExchangeWebSocket
- `:service:trading` - TradingServiceController

## File Structure

```
feature/dashboard/src/main/kotlin/com/tradeflow/feature/dashboard/
└── viewmodel/
    ├── DashboardViewModel.kt
    └── TradingServiceController.kt  (interface)
```

## Acceptance Criteria

- [ ]  Connects to exchange via interfaces (not Coinbase directly)
- [ ]  Transforms domain models to UI state
- [ ]  Handles errors gracefully
- [ ]  Unit testable with fake repositories