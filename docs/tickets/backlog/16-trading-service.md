# ⚡ SERVICE: Trading Foreground Service

Effort level: Large
Priority: High
Status: Not started
Blocked by: DOMAIN: Decision Engine, DOMAIN: Risk Manager, EXCHANGE-API: Repository Interfaces
Module: :service:trading

## Objective

Implement the Android foreground service that orchestrates trading loops.

## Module

`:service:trading`

## Service Configuration

```xml
<service
    android:name=".TradingService"
    android:foregroundServiceType="dataSync"
    android:exported="false" />
```

## Architecture

```
TradingService
├── PriceMonitor (WebSocket subscription)
├── StrategyLoop (every 15 min)
├── RiskMonitor (every 15 min)
└── OrderReconciler (on startup)
```

## Implementation

```kotlin
class TradingService : Service() {
    
    @Inject lateinit var exchangeRepository: ExchangeRepository
    @Inject lateinit var exchangeWebSocket: ExchangeWebSocket
    @Inject lateinit var decisionEngine: DecisionEngine
    @Inject lateinit var riskManager: RiskManager
    @Inject lateinit var orderDao: OrderDao
    @Inject lateinit var portfolioDao: PortfolioDao
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val currentPrice = AtomicReference<BigDecimal?>(null)
    private val currentDecision = AtomicReference<Decision>(Decision.Wait("Starting"))
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        acquireWakeLock()
        
        startPriceMonitor()
        startStrategyLoop()
        reconcileOrders()
        
        return START_STICKY
    }
    
    private fun startPriceMonitor() {
        scope.launch {
            exchangeWebSocket.subscribeTicker(listOf(PRODUCT_ID))
                .collect { ticker ->
                    currentPrice.set(ticker.price)
                    updateNotification(ticker.price)
                }
        }
    }
    
    private fun startStrategyLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val candles = exchangeRepository.getCandles(
                        PRODUCT_ID, Granularity.TWO_HOUR, 350
                    ).getOrThrow()
                    
                    val h4Candles = aggregateToH4(candles)
                    val price = currentPrice.get() ?: continue
                    
                    val decision = decisionEngine.evaluate(h4Candles, price)
                    currentDecision.set(decision)
                    
                    executeDecision(decision, price)
                    checkRisk(price)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Strategy loop error", e)
                }
                
                delay(15 * 60 * 1000)  // 15 minutes
            }
        }
    }
    
    private suspend fun executeDecision(decision: Decision, price: BigDecimal) {
        when (decision) {
            is Decision.Defense -> executeDefense()
            is Decision.Trend -> executeTrend(decision, price)
            is Decision.Range -> executeRange(decision, price)
            is Decision.Wait -> { /* Do nothing */ }
        }
    }
    
    private suspend fun checkRisk(price: BigDecimal) {
        val portfolio = exchangeRepository.getPortfolio().getOrNull() ?: return
        val hwm = portfolioDao.getHighWaterMark()?.toBigDecimal() ?: portfolio.totalEquityUsd
        
        when (val status = riskManager.checkDrawdown(portfolio.totalEquityUsd, hwm)) {
            is RiskStatus.Emergency -> {
                Log.e(TAG, "🚨 EMERGENCY DRAWDOWN")
                emergencyLiquidate()
                stopSelf()
            }
            is RiskStatus.Warning -> {
                updateNotification("⚠️ Warning: ${status.drawdownPercent}% drawdown")
            }
            is [RiskStatus.Safe](http://RiskStatus.Safe) -> { /* Continue */ }
        }
        
        // Save snapshot
        portfolioDao.insert(PortfolioSnapshotEntity(...))
    }
}
```

## Wake Lock

```kotlin
private fun acquireWakeLock() {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock = pm.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "TradeFlow::TradingService"
    ).apply { acquire() }
}
```

## Notification

- Show current mode and price
- Update every strategy cycle
- IMPORTANCE_LOW (silent)

## File Structure

```
service/trading/src/main/kotlin/com/tradeflow/service/trading/
├── TradingService.kt
├── TradingServiceController.kt  (impl)
├── loop/
│   ├── PriceMonitor.kt
│   ├── StrategyLoop.kt
│   ├── RiskMonitor.kt
│   └── OrderReconciler.kt
├── execution/
│   ├── DefenseExecutor.kt
│   ├── TrendExecutor.kt
│   └── RangeExecutor.kt
└── notification/
    └── TradingNotificationManager.kt
```

## Acceptance Criteria

- [ ]  Service survives device sleep
- [ ]  Notification always visible
- [ ]  Loops run at correct intervals
- [ ]  Clean shutdown on stopSelf()
- [ ]  Emergency liquidation works
- [ ]  Uses interfaces, not Coinbase directly