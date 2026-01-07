# 🚨 [SUPERSEDED] TradingService - Risk Management & Drawdown

Effort level: Medium
Priority: High
Status: Done
Blocked by: Replaced by: 🚨 DOMAIN: Risk Manager

## Objective

Implement drawdown monitoring and emergency liquidation.

## File

`service/TradingService.kt` (extend)

## Risk Limits (Hardcoded)

| Limit | Value |
| --- | --- |
| Max position per trade | 5% |
| Max total exposure | 10% |
| Drawdown limit | **15%** |

## Drawdown Calculation

```kotlin
private suspend fun checkDrawdown(price: Double) {
    val accounts = restApi.getAccounts()
    val usd = accounts.find { it.currency == "USD" }?.available ?: 0.0
    val btc = accounts.find { it.currency == "BTC" }?.available ?: 0.0
    
    val totalEquity = usd + (btc * price)
    val hwm = database.portfolioDao().getHighWaterMark() ?: totalEquity
    val newHwm = maxOf(hwm, totalEquity)
    val drawdown = if (newHwm > 0) (newHwm - totalEquity) / newHwm else 0.0
    
    // Save snapshot
    database.portfolioDao().insert(PortfolioSnapshot(
        totalEquityUsd = totalEquity,
        cashUsd = usd,
        btcValue = btc * price,
        highWaterMark = newHwm,
        drawdownPercent = drawdown * 100,
        regime = currentDecision.get()::class.simpleName ?: "UNKNOWN"
    ))
    
    // Emergency liquidation
    if (drawdown > 0.15) {
        Log.e(TAG, "🚨 DRAWDOWN LIMIT HIT")
        emergencyLiquidate(btc)
        stopSelf()
    }
}
```

## Emergency Liquidation

```kotlin
private suspend fun emergencyLiquidate(btcBalance: Double) {
    // 1. Cancel ALL orders
    val allOrders = database.orderDao().getActiveOrders()
    val orderIds = allOrders.mapNotNull { it.exchangeOrderId }
    restApi.cancelOrders(orderIds)
    
    // 2. Market sell ALL BTC
    if (btcBalance > 0.0001) {
        restApi.placeMarketOrder(PRODUCT_ID, "SELL", btcBalance)
    }
    
    // 3. Update notification
    updateNotification("🛑 EMERGENCY STOP - Drawdown limit")
}
```

## High Water Mark

- Tracks peak portfolio value
- Never decreases
- Used to calculate drawdown from peak

## Acceptance Criteria

- Drawdown calculated correctly from HWM
- Service stops at 15% drawdown
- All orders cancelled before market sell
- Notification shows emergency state