package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

data class Order(
    val id: String,
    val clientOrderId: String,
    val productId: String,
    val side: OrderSide,
    val type: OrderType,
    val status: OrderStatus,
    val size: BigDecimal,
    val price: BigDecimal?,
    val filledSize: BigDecimal,
    val avgFilledPrice: BigDecimal?,
    val createdAt: Instant
)

enum class OrderSide { BUY, SELL }
enum class OrderType { MARKET, LIMIT, BRACKET }
enum class OrderStatus { PENDING, OPEN, FILLED, CANCELLED, FAILED }
