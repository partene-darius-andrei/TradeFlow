package com.tradeflow.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tradeflow.core.data.local.dao.*
import com.tradeflow.core.data.local.entity.*

@Database(
    entities = [
        CandleEntity::class,
        OrderEntity::class,
        PortfolioSnapshotEntity::class,
        DecisionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EngineDatabase : RoomDatabase() {
    abstract fun candleDao(): CandleDao
    abstract fun orderDao(): OrderDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun decisionDao(): DecisionDao
}
