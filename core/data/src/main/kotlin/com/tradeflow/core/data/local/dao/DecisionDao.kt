package com.tradeflow.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tradeflow.core.data.local.entity.DecisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionDao {
    @Query("SELECT * FROM decisions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestDecision(): DecisionEntity?

    @Query("SELECT * FROM decisions ORDER BY timestamp DESC LIMIT 1")
    fun getLatestDecisionFlow(): Flow<DecisionEntity?>

    @Query("SELECT * FROM decisions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentDecisions(limit: Int = 100): Flow<List<DecisionEntity>>

    @Insert
    suspend fun insert(decision: DecisionEntity)

    @Query("DELETE FROM decisions WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldDecisions(beforeTimestamp: Long)
}
