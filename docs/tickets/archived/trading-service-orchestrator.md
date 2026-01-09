# ⚙️ SERVICE - Trading Service (Orchestrator)

Effort level: Large
Priority: High

## Objective

Android foreground service that orchestrates trading loops. Uses **interfaces only** - no direct Coinbase imports.

## File

`presentation/service/TradingService.kt`

## Architecture

```
TradingService
    ├── Uses: ExchangeRepository (interface)
    ├── Uses: ExchangeWebSocketService (interface)
    ├── Uses: DecisionEngine (domain)
    ├── Uses: RiskManager (domain)
    ├── Uses: PlaceOrderUseCase (domain)
    └── Uses: Room DAOs (data/local)
```

## Service Configuration

```xml
<service
    android:name=".presentation.service.TradingService"
    android:foregroundServiceType="dataSync"
    android:exported="false" />
```

## Implementation

```kotlin
@AndroidEntryPoint
class TradingService : Service() {

    @Inject lateinit var exchangeRepository: ExchangeRepository
    @Inject lateinit var webSocketService: ExchangeWebSocketService
    @Inject lateinit var decisionEngine: DecisionEngine
    @Inject lateinit var riskManager: RiskManager
    @Inject lateinit var placeOrderUseCase: PlaceOrderUseCase
    @Inject lateinit var portfolioDao: PortfolioDao
    @Inject lateinit var orderDao: OrderDao
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val currentPrice = AtomicReference<BigDecimal>([BigDecimal.ZERO](http://BigDecimal.ZERO))
    private val currentDecision = AtomicReference<Decision>(Decision.Wait("Initializing"))
    
    private var priceJob: Job? = null
    private var strategyJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPriceMonitor()
        startStrategyLoop()
        return START_STICKY
    }
    
    private fun startPriceMonitor() {
        priceJob = scope.launch {
            webSocketService.connect()
            webSocketService.subscribeTicker(listOf(PRODUCT_ID))
                .collect { ticker ->
                    currentPrice.set(ticker.price)
                    updateNotification(ticker.price)
                }
        }
    }
    
    private fun startStrategyLoop() {
        strategyJob = scope.launch {
            while (isActive) {
                try {
                    runStrategyCycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Strategy cycle failed", e)
                }
                delay(15 * 60 * 1000L)  // 15 minutes
            }
        }
    }
    
    private suspend fun runStrategyCycle() {
        val price = currentPrice.get()
        if (price == [BigDecimal.ZERO](http://BigDecimal.ZERO)) return
        
        // 1. Fetch candles
        val endTime = [Instant.now](http://Instant.now)()
        val startTime = endTime.minus(Duration.ofHours(700))  // 350 * 2h
        val candles = exchangeRepository.getCandles(
            productId = PRODUCT_ID,
            granularity = CandleGranularity.TWO_HOUR,
            startTime = startTime,
            endTime = endTime
        ).getOrElse {
            Log.e(TAG, "Failed to fetch candles", it)
            return
        }
        
        // 2. Aggregate to H4
        val h4Candles = aggregateToH4(candles)
        
        // 3. Evaluate decision
        val decision = decisionEngine.evaluate(h4Candles, price)
        currentDecision.set(decision)
        
        // 4. Execute decision
        executeDecision(decision, price)
        
        // 5. Check drawdown
        checkDrawdown(price)
    }
    
    private suspend fun executeDecision(decision: Decision, price: BigDecimal) {
        when (decision) {
            is Decision.Defense -> executeDefense()
            is Decision.Trend -> executeTrend(decision)
            is Decision.Range -> executeRange(decision, price)
            is Decision.Wait -> { /* Do nothing */ }
        }
    }
    
    private suspend fun checkDrawdown(price: BigDecimal) {
        val portfolio = getPortfolio(price)
        val status = riskManager.checkDrawdown(portfolio.totalEquity)
        
        if (status is DrawdownStatus.LimitBreached) {
            Log.e(TAG, "🚨 DRAWDOWN LIMIT HIT: ${status.percent}%")
            emergencyLiquidate()
            stopSelf()
        }
    }
    
    private suspend fun emergencyLiquidate() {
        // 1. Cancel all orders
        val activeOrders = orderDao.getActiveOrdersForProduct(PRODUCT_ID)
        val orderIds = activeOrders.mapNotNull { it.exchangeOrderId }
        if (orderIds.isNotEmpty()) {
            exchangeRepository.cancelOrders(orderIds)
        }
        
        // 2. Market sell all BTC
        val accounts = exchangeRepository.getAccounts().getOrNull() ?: return
        val btcBalance = accounts.find { it.currency == "BTC" }?.available ?: return
        if (btcBalance > BigDecimal("0.0001")) {
            placeOrderUseCase([PlaceOrderRequest.Market](http://PlaceOrderRequest.Market)(
                productId = PRODUCT_ID,
                side = OrderSide.SELL,
                size = btcBalance
            ))
        }
        
        updateNotification("🛑 EMERGENCY STOP")
    }
    
    companion object {
        const val PRODUCT_ID = "BTC-USD"
        const val NOTIFICATION_ID = 1
        private const val TAG = "TradingService"
    }
}
```

## Key Principle

**The service knows NOTHING about Coinbase.** 

All exchange operations go through interfaces. Swap exchanges by changing DI bindings.

## Depends On

- 🔌 INTERFACE - ExchangeRepository
- 🔌 INTERFACE - ExchangeWebSocketService
- 🧠 DecisionEngine
- 🚨 RiskManager
- 🎯 UseCases
- 🗄️ Room Database

## Acceptance Criteria

- [ ]  No Coinbase imports in this file
- [ ]  Uses interfaces via @Inject
- [ ]  Survives device sleep with wake lock
- [ ]  START_STICKY for auto-restart
- [ ]  Clean shutdown on stopSelf()