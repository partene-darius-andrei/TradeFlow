package com.tradeflow.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val clientOrderId: String,
    val exchangeOrderId: String?,
    val productId: String,
    val side: String,
    val orderType: String,
    val status: String,
    val size: String,
    val price: String?,
    val filledSize: String,
    val avgFilledPrice: String?,
    val gridLevel: Int?,
    val createdAt: Long,
    val updatedAt: Long
)
