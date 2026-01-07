package com.tradeflow.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "candles")
data class CandleEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val timestamp: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
    val granularity: String
)
