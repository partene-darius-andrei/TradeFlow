# ⚙️ [SUPERSEDED] TradingService - Foreground Service Core

Effort level: Large
Priority: High
Status: Done
Blocked by: Replaced by: ⚡ SERVICE: Trading Foreground Service

## Objective

Implement the Android foreground service that runs the trading loops.

## File

`service/TradingService.kt`

## Service Configuration

```xml
<service
    android:name=".service.TradingService"
    android:foregroundServiceType="dataSync"
    android:exported="false" />
```

## Core Loops

### 1. Price Monitor

- Subscribe to WebSocket ticker
- Update `currentPrice` AtomicReference
- Feed order updates to database

### 2. Strategy Loop (every 15 min)

```kotlin
while (isActive) {
    val candles = restApi.getCandles(PRODUCT_ID, "TWO_HOUR", 350)
    val h4Candles = aggregateToH4(candles)
    val decision = decisionEngine.evaluate(h4Candles, currentPrice.get())
    executeDecision(decision, currentPrice.get())
    checkDrawdown(currentPrice.get())
    delay(15 * 60 * 1000)
}
```

### 3. Order Reconciliation

- On startup: Compare local DB with exchange open orders
- Mark missing orders as UNKNOWN/FILLED/CANCELLED
- Sync filled sizes and prices

## Wake Lock

```kotlin
val wakeLock = powerManager.newWakeLock(
    PowerManager.PARTIAL_WAKE_LOCK,
    "Engine::TradingService"
).apply { acquire() }
```

## Notification

- Show current mode and price
- Update every strategy cycle
- Use IMPORTANCE_LOW (silent)

## Return Value

`START_STICKY` - System restarts service if killed

## Acceptance Criteria

- Service survives device sleep
- Notification always visible
- Loops run at correct intervals
- Clean shutdown on stopSelf()