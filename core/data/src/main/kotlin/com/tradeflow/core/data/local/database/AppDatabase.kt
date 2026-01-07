package com.tradeflow.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tradeflow.core.data.local.entity.PlaceholderEntity

@Database(
    entities = [PlaceholderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase()
