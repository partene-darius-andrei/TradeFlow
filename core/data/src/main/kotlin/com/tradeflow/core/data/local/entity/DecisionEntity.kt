package com.tradeflow.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val reason: String?,
    val direction: String?,
    val entryPrice: String?,
    val stopLoss: String?,
    val takeProfit: String?,
    val positionSize: String?,
    val gridSpacing: String?,
    val levels: Int?,
    val positionSizePerLevel: String?,
    val timestamp: Long
)
