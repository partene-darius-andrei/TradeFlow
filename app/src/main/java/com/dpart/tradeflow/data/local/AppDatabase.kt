package com.dpart.tradeflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaceholderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase()
