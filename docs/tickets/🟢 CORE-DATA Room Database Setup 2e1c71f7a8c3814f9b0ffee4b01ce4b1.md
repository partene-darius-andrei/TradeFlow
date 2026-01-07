# 🟢 CORE-DATA: Room Database Setup

Effort level: Medium
Priority: High
Status: Not started
Blocked by: DOMAIN: Core Domain Models
Module: :core:data

## Objective

Set up Room database with exchange-agnostic entities.

## Module

`:core:data`

## Entities

### OrderEntity

```kotlin
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val clientOrderId: String,
    val productId: String,
    val side: String,
    val type: String,
    val status: String,
    val size: String,  // Store as String for precision
    val price: String?,
    val filledSize: String,
    val avgFilledPrice: String?,
    val gridLevel: Int?,  // For grid orders
    val createdAt: Long,
    val updatedAt: Long
)
```

### PortfolioSnapshotEntity

```kotlin
@Entity(tableName = "portfolio_snapshots")
data class PortfolioSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalEquityUsd: String,
    val cashUsd: String,
    val btcValue: String,
    val highWaterMark: String,
    val drawdownPercent: Double,
    val regime: String,
    val timestamp: Long
)
```

### DecisionLogEntity

```kotlin
@Entity(tableName = "decision_log")
data class DecisionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val decisionType: String,  // WAIT, DEFENSE, TREND, RANGE
    val reason: String,
    val sma200: String?,
    val adx14: Double?,
    val atr14: String?,
    val currentPrice: String,
    val timestamp: Long
)
```

## DAOs

```kotlin
@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE status IN ('PENDING', 'OPEN')")
    fun getActiveOrders(): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE productId = :productId AND status IN ('PENDING', 'OPEN')")
    fun getActiveOrdersForProduct(productId: String): Flow<List<OrderEntity>>
    
    @Upsert
    suspend fun upsert(order: OrderEntity)
    
    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateStatus(orderId: String, status: String, updatedAt: Long)
}

@Dao
interface PortfolioDao {
    @Query("SELECT MAX(highWaterMark) FROM portfolio_snapshots")
    suspend fun getHighWaterMark(): String?
    
    @Insert
    suspend fun insert(snapshot: PortfolioSnapshotEntity)
    
    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSnapshot(): Flow<PortfolioSnapshotEntity?>
}
```

## Database

```kotlin
@Database(
    entities = [
        OrderEntity::class,
        PortfolioSnapshotEntity::class,
        DecisionLogEntity::class
    ],
    version = 1
)
abstract class TradeFlowDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun decisionLogDao(): DecisionLogDao
}
```

## File Structure

```
core/data/src/main/kotlin/com/tradeflow/core/data/
├── local/
│   ├── TradeFlowDatabase.kt
│   ├── entity/
│   │   ├── OrderEntity.kt
│   │   ├── PortfolioSnapshotEntity.kt
│   │   └── DecisionLogEntity.kt
│   └── dao/
│       ├── OrderDao.kt
│       ├── PortfolioDao.kt
│       └── DecisionLogDao.kt
└── mapper/
    └── EntityMapper.kt
```

## Acceptance Criteria

- [ ]  All entities store money as String (BigDecimal precision)
- [ ]  DAOs return Flow for reactive UI
- [ ]  Migration strategy defined
- [ ]  No exchange-specific fields in entities