package com.tradeflow.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tradeflow.core.data.local.entity.CandleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CandleDao {
    @Query("SELECT * FROM candles WHERE productId = :productId AND granularity = :granularity ORDER BY timestamp DESC LIMIT :limit")
    fun getCandles(productId: String, granularity: String, limit: Int): Flow<List<CandleEntity>>

    @Query("SELECT * FROM candles WHERE productId = :productId AND granularity = :granularity ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestCandle(productId: String, granularity: String): CandleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(candles: List<CandleEntity>)

    @Query("DELETE FROM candles WHERE productId = :productId AND granularity = :granularity AND timestamp < :beforeTimestamp")
    suspend fun deleteOldCandles(productId: String, granularity: String, beforeTimestamp: Long)
}
