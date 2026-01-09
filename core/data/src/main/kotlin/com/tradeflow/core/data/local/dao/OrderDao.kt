package com.tradeflow.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tradeflow.core.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE status IN ('PENDING', 'OPEN')")
    fun getActiveOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE productId = :productId AND status IN ('PENDING', 'OPEN')")
    suspend fun getActiveOrdersForProduct(productId: String): List<OrderEntity>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentOrders(limit: Int = 100): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE productId = :productId AND status = 'FILLED' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentFilledOrders(productId: String, limit: Int = 50): List<OrderEntity>

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE clientOrderId = :clientOrderId")
    suspend fun updateStatus(clientOrderId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)
}
