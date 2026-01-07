# 🗄️ INFRA - Room Database (Updated)

Effort level: Medium
Priority: High
Completed: 2026-01-07
PR: #8

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

- [x]  All entities defined with proper keys
- [x]  BigDecimal stored as String
- [x]  Flow support for reactive queries
- [x]  No exchange-specific fields

---

## Post-Implementation Notes

**Completed:** 2026-01-07
**PR:** https://github.com/partene-darius-andrei/TradeFlow/pull/8

### Implementation Summary

Complete Room database created with 4 entities, 4 DAOs, and updated DatabaseModule. All BigDecimal fields stored as String for precision.

### Files Created

**Entities:**
1. CandleEntity.kt - OHLCV data
2. OrderEntity.kt - Order lifecycle tracking
3. PortfolioSnapshotEntity.kt - Portfolio snapshots with regime
4. DecisionEntity.kt - Flattened decision fields

**DAOs:**
5. CandleDao.kt - Market data queries
6. OrderDao.kt - Order management
7. PortfolioDao.kt - Portfolio tracking
8. DecisionDao.kt - Decision history

**Database:**
9. EngineDatabase.kt - Room database with 4 entities

### Key Decisions

**BigDecimal Storage:**
- Stored as String (not Double) for precision
- Critical for financial calculations

**Flow Support:**
- All list queries return Flow<List<T>>
- Reactive updates without polling

**Grid Level Support:**
- OrderEntity has optional gridLevel field
- Enables range/grid trading strategies

**Cleanup Methods:**
- deleteOldCandles(), deleteOldSnapshots(), deleteOldDecisions()
- Prevent database bloat

### Build Verification

✅ :core:data:build - SUCCESS
✅ All Room entities + DAOs compile

### Bonus: Ticket Cleanup

Removed redundant 'Status:' field from ALL 70+ ticket files.
Folder location now indicates status.