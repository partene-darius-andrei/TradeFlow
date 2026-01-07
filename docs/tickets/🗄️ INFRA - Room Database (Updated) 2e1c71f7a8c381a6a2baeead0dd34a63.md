# 🗄️ INFRA - Room Database (Updated)

Effort level: Medium
Priority: High
Status: Not started

## Objective

Local database for persistence. Uses **domain models**, not exchange DTOs.

## Files

```
data/local/
├── EngineDatabase.kt
├── dao/
│   ├── CandleDao.kt
│   ├── OrderDao.kt
│   ├── PortfolioDao.kt
│   └── DecisionDao.kt
└── entity/
    ├── CandleEntity.kt
    ├── OrderEntity.kt
    ├── PortfolioSnapshotEntity.kt
    └── DecisionEntity.kt
```

## Database

```kotlin
@Database(
    entities = [
        CandleEntity::class,
        OrderEntity::class,
        PortfolioSnapshotEntity::class,
        DecisionEntity::class
    ],
    version = 1
)
abstract class EngineDatabase : RoomDatabase() {
    abstract fun candleDao(): CandleDao
    abstract fun orderDao(): OrderDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun decisionDao(): DecisionDao
}
```

## Entities

### OrderEntity

```kotlin
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val clientOrderId: String,
    val exchangeOrderId: String?,
    val productId: String,
    val side: String,  // BUY, SELL
    val orderType: String,  // LIMIT, BRACKET, MARKET
    val status: String,  // PENDING, OPEN, FILLED, CANCELLED
    val size: String,  // BigDecimal as String
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
    val regime: String,  // WAIT, DEFENSE, TREND, RANGE
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
    suspend fun getActiveOrdersForProduct(productId: String): List<OrderEntity>
    
    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE clientOrderId = :clientOrderId")
    suspend fun updateStatus(clientOrderId: String, status: String, updatedAt: Long = System.currentTimeMillis())
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)
}

@Dao
interface PortfolioDao {
    @Query("SELECT MAX(CAST(highWaterMark AS REAL)) FROM portfolio_snapshots")
    suspend fun getHighWaterMark(): Double?
    
    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(): PortfolioSnapshotEntity?
    
    @Insert
    suspend fun insertSnapshot(snapshot: PortfolioSnapshotEntity)
}
```

## Critical Rule

- Store BigDecimal as String (not Double!) for precision
- Entities are **internal** to data layer
- Domain layer uses domain models, mappers convert

## Acceptance Criteria

- [ ]  All entities defined with proper keys
- [ ]  BigDecimal stored as String
- [ ]  Flow support for reactive queries
- [ ]  No exchange-specific fields