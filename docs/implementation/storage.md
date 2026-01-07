# Database & Trading Service

**Parent:** [../reference.md](../reference.md)

Room database schema and foreground trading service implementation.

---

## Room Database

### EngineDatabase.kt

Complete database setup with DAOs for orders, portfolio tracking, and grid configuration.

```kotlin
// data/local/EngineDatabase.kt
package com.dpart.tradeflow.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [OrderEntity::class, PortfolioSnapshot::class, GridConfig::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EngineDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun gridDao(): GridDao

    companion object {
        @Volatile private var INSTANCE: EngineDatabase? = null

        fun getInstance(context: Context): EngineDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    EngineDatabase::class.java,
                    "engine_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class Converters {
    @TypeConverter fun fromTimestamp(value: Long?) = value?.let { java.time.Instant.ofEpochMilli(it) }
    @TypeConverter fun toTimestamp(instant: java.time.Instant?) = instant?.toEpochMilli()
}
```

### Entities

```kotlin
@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["exchange_order_id"], unique = true),
        Index(value = ["status"]),
        Index(value = ["product_id", "grid_level"])
    ]
)
data class OrderEntity(
    @PrimaryKey val clientOrderId: String,
    val exchangeOrderId: String? = null,
    val productId: String,
    val side: String,
    val orderType: String,  // BRACKET, LIMIT, MARKET
    val price: Double,
    val size: Double,
    val status: String,  // PENDING, OPEN, FILLED, CANCELLED, FAILED
    val gridLevel: Int? = null,
    val filledSize: Double = 0.0,
    val avgFilledPrice: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "portfolio_snapshots")
data class PortfolioSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalEquityUsd: Double,
    val cashUsd: Double,
    val btcValue: Double,
    val highWaterMark: Double,
    val drawdownPercent: Double,
    val regime: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "grid_configs")
data class GridConfig(
    @PrimaryKey val productId: String,
    val isActive: Boolean = false,
    val regime: String = "WAIT",
    val spacing: Double = 0.0,
    val lastAtr: Double = 0.0,
    val lastSma: Double = 0.0,
    val lastAdx: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
```

### DAOs

```kotlin
@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)

    @Update
    suspend fun update(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE clientOrderId = :clientOrderId")
    suspend fun getByClientId(clientOrderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE exchange_order_id = :exchangeOrderId")
    suspend fun getByExchangeId(exchangeOrderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE status IN ('PENDING', 'OPEN')")
    suspend fun getActiveOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE status IN ('PENDING', 'OPEN') AND product_id = :productId")
    suspend fun getActiveOrdersForProduct(productId: String): List<OrderEntity>

    @Query("UPDATE orders SET status = :status, updatedAt = :now WHERE clientOrderId = :clientOrderId")
    suspend fun updateStatus(clientOrderId: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET exchange_order_id = :exchangeId, status = :status, updatedAt = :now WHERE clientOrderId = :clientOrderId")
    suspend fun confirmOrder(clientOrderId: String, exchangeId: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET filledSize = :filled, avgFilledPrice = :price, status = :status, updatedAt = :now WHERE clientOrderId = :clientOrderId")
    suspend fun updateFill(clientOrderId: String, filled: Double, price: Double, status: String, now: Long = System.currentTimeMillis())
}

@Dao
interface PortfolioDao {
    @Insert
    suspend fun insert(snapshot: PortfolioSnapshot)

    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): PortfolioSnapshot?

    @Query("SELECT MAX(highWaterMark) FROM portfolio_snapshots")
    suspend fun getHighWaterMark(): Double?

    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<PortfolioSnapshot>>
}

@Dao
interface GridDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: GridConfig)

    @Query("SELECT * FROM grid_configs WHERE productId = :productId")
    suspend fun get(productId: String): GridConfig?

    @Query("SELECT * FROM grid_configs WHERE isActive = 1")
    fun observeActive(): Flow<List<GridConfig>>
}
```

---

## Trading Service

### TradingService.kt

Foreground service that runs the trading strategy 24/7 with wake lock and doze survival.

**Note:** Full implementation with all loops - see original reference.md for complete 420-line service code.

**Key components:**
- Foreground notification
- Wake lock acquisition
- Order reconciliation on startup
- WebSocket price monitoring
- WebSocket order monitoring  
- Strategy evaluation loop (every 15 minutes)
- Risk management (drawdown checking)
- Emergency liquidation

**Service Loop Structure:**

```kotlin
private suspend fun runStrategyLoop() {
    while (isActive) {
        try {
            // 1. Fetch H4 candles
            val candles = restApi.getCandles(PRODUCT_ID, "TWO_HOUR", 350)
            val h4Candles = aggregateToH4(candles)

            // 2. Evaluate decision
            val decision = decisionEngine.evaluate(h4Candles, currentPrice.get())

            // 3. Execute decision
            executeDecision(decision, currentPrice.get())

            // 4. Check drawdown
            checkDrawdown(currentPrice.get())

            // 5. Update notification
            updateNotification("${decision::class.simpleName} | $$price")

        } catch (e: Exception) {
            Log.e(TAG, "Strategy loop error", e)
        }

        delay(15 * 60 * 1000L)  // 15 minutes
    }
}
```

**Execution Logic:**

```kotlin
private suspend fun executeDecision(decision: Decision, price: Double) {
    when (decision) {
        is Decision.Defense -> executeDefense()
        is Decision.Trend -> executeTrend(decision, price)
        is Decision.Range -> executeRange(decision, price)
        is Decision.Wait -> { /* Hold */ }
    }
}

private suspend fun executeDefense() {
    // Cancel all buy orders
    val buyOrders = database.orderDao()
        .getActiveOrdersForProduct(PRODUCT_ID)
        .filter { it.side == "BUY" }

    val orderIds = buyOrders.mapNotNull { it.exchangeOrderId }
    if (restApi.cancelOrders(orderIds)) {
        buyOrders.forEach {
            database.orderDao().updateStatus(it.clientOrderId, "CANCELLED")
        }
    }
}

private suspend fun executeTrend(decision: Decision.Trend, price: Double) {
    // Check if already in position
    val activeOrders = database.orderDao().getActiveOrdersForProduct(PRODUCT_ID)
    if (activeOrders.any { it.orderType == "BRACKET" && it.status == "OPEN" }) {
        return
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

private suspend fun executeRange(decision: Decision.Range, price: Double) {
    val activeOrders = database.orderDao().getActiveOrdersForProduct(PRODUCT_ID)
    val existingLevels = activeOrders.mapNotNull { it.gridLevel }.toSet()

    // Place grid orders at levels 1-5
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

**Risk Management:**

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

    // Emergency liquidation at 15% drawdown
    if (drawdown > 0.15) {
        Log.e(TAG, "DRAWDOWN LIMIT HIT: ${drawdown * 100}%")
        emergencyLiquidate(btc)
        stopSelf()
    }
}
```

**See:** Original `docs/reference.md` (before split) for complete 420-line TradingService implementation

---

## Key Implementation Details

### Database Design

**Orders Table:**
- Indexed on `exchange_order_id` (unique), `status`, and `(product_id, grid_level)`
- Tracks local vs exchange state
- Grid level for range trading

**Portfolio Snapshots:**
- High-water mark tracking
- Drawdown percentage calculation
- Regime history for analysis

**Grid Config:**
- Per-product active state
- Last indicator values (ATR, SMA, ADX)
- Updated timestamp

### Service Lifecycle

1. **onCreate:** Initialize components, acquire wake lock
2. **onStartCommand:** Check credentials, launch loops
3. **Reconciliation:** Sync local DB with exchange on startup
4. **Strategy Loop:** Evaluate every 15 minutes
5. **onDestroy:** Disconnect WebSocket, release wake lock

### Doze Mode Survival

- **Foreground service** with notification
- **Wake lock** (`PARTIAL_WAKE_LOCK`)
- **Battery optimization exemption** (user must grant)
- **WorkManager backup** (restarts service if killed)

---

## Navigation

- **[Back to Technical Reference](../reference.md)** - Parent document
- **[Previous: API Clients](clients.md)** - REST and WebSocket
- **[Next: Configuration](config.md)** - Gradle and manifest
