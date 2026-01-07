# 🎯 [SUPERSEDED] TradingService - Decision Execution

Effort level: Large
Priority: High
Blocked by: Replaced by: 🧠 DOMAIN: Decision Engine + ⚡ SERVICE: Trading Foreground Service

## Objective

Implement the execution logic for each decision mode.

## File

`service/TradingService.kt` (extend)

## Execution Methods

### executeDefense()

```kotlin
private suspend fun executeDefense() {
    // Cancel all open BUY orders
    val activeOrders = database.orderDao().getActiveOrdersForProduct(PRODUCT_ID)
    val buyOrders = activeOrders.filter { it.side == "BUY" }
    
    if (buyOrders.isNotEmpty()) {
        val orderIds = buyOrders.mapNotNull { it.exchangeOrderId }
        if (restApi.cancelOrders(orderIds)) {
            buyOrders.forEach {
                database.orderDao().updateStatus(it.clientOrderId, "CANCELLED")
            }
        }
    }
}
```

### executeTrend(decision, price)

```kotlin
private suspend fun executeTrend(decision: Decision.Trend, price: Double) {
    // Check if already in position
    val activeOrders = database.orderDao().getActiveOrdersForProduct(PRODUCT_ID)
    if (activeOrders.any { it.orderType == "BRACKET" && it.status == "OPEN" }) {
        return  // Already have trend position
    }
    
    val size = calculatePositionSize(price)
    val result = restApi.placeBracketOrder(
        productId = PRODUCT_ID,
        side = "BUY",
        baseSize = size,
        entryPrice = price,
        takeProfitPrice = decision.takeProfitPrice,
        stopLossPrice = decision.stopLossPrice
    )
    
    handleOrderResult(result, "BRACKET", price, size)
}
```

### executeRange(decision, price)

```kotlin
private suspend fun executeRange(decision: Decision.Range, price: Double) {
    val activeOrders = database.orderDao().getActiveOrdersForProduct(PRODUCT_ID)
    val existingLevels = activeOrders.mapNotNull { it.gridLevel }.toSet()
    
    // Place grid orders at levels 1-5 below current price
    for (level in 1..5) {
        if (level in existingLevels) continue
        
        val gridPrice = price * (1 - (level * decision.gridSpacing / price))
        val size = calculateGridSize(price)
        
        val result = restApi.placeLimitOrder(
            productId = PRODUCT_ID,
            side = "BUY",
            baseSize = size,
            limitPrice = gridPrice
        )
        
        handleOrderResult(result, "LIMIT", gridPrice, size, level)
    }
}
```

## Position Sizing

- **TREND:** 5% of portfolio per trade
- **GRID:** 2% per level (5 levels = 10% max)

## Acceptance Criteria

- Defense cancels all buy orders
- Trend places bracket with correct TP/SL
- Grid places orders at correct price levels
- No duplicate orders for same grid level