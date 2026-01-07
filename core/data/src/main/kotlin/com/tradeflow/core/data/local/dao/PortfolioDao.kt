package com.tradeflow.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tradeflow.core.data.local.entity.PortfolioSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT MAX(CAST(highWaterMark AS REAL)) FROM portfolio_snapshots")
    suspend fun getHighWaterMark(): Double?

    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(): PortfolioSnapshotEntity?

    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSnapshotFlow(): Flow<PortfolioSnapshotEntity?>

    @Query("SELECT * FROM portfolio_snapshots ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSnapshots(limit: Int = 100): Flow<List<PortfolioSnapshotEntity>>

    @Insert
    suspend fun insertSnapshot(snapshot: PortfolioSnapshotEntity)

    @Query("DELETE FROM portfolio_snapshots WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldSnapshots(beforeTimestamp: Long)
}
