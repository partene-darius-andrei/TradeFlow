package com.tradeflow.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_snapshots")
data class PortfolioSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalEquityUsd: String,
    val cashUsd: String,
    val btcValue: String,
    val highWaterMark: String,
    val drawdownPercent: Double,
    val regime: String,
    val timestamp: Long
)
