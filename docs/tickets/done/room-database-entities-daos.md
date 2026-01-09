# 🗄️ Room Database - Entities & DAOs

Effort level: Medium
Priority: High

## Objective

Set up Room database for order tracking and portfolio history.

## File

`data/local/EngineDatabase.kt`

## Entities

### OrderEntity

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
    val createdAt: Long,
    val updatedAt: Long
)
```

### PortfolioSnapshot

```kotlin
@Entity(tableName = "portfolio_snapshots")
data class PortfolioSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalEquityUsd: Double,
    val cashUsd: Double,
    val btcValue: Double,
    val highWaterMark: Double,
    val drawdownPercent: Double,
    val regime: String,
    val timestamp: Long
)
```

### GridConfig

```kotlin
@Entity(tableName = "grid_configs")
data class GridConfig(
    @PrimaryKey val productId: String,
    val isActive: Boolean,
    val regime: String,
    val spacing: Double,
    val lastAtr: Double,
    val lastSma: Double,
    val lastAdx: Double,
    val updatedAt: Long
)
```

## DAOs

- `OrderDao` - CRUD for orders, query by status/product
- `PortfolioDao` - Insert snapshots, get high water mark
- `GridDao` - Store/retrieve grid configuration

## Acceptance Criteria

- Database creates on first launch
- Orders persist across app restarts
- Portfolio snapshots stored with timestamp
- Indices speed up common queries