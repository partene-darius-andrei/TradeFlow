package com.tradeflow.core.data.mapper

import com.tradeflow.core.data.local.entity.OrderEntity
import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderStatus
import com.tradeflow.core.domain.model.OrderType
import java.math.BigDecimal
import java.time.Instant

fun OrderEntity.toDomain(): Order {
    return Order(
        id = exchangeOrderId ?: clientOrderId,
        clientOrderId = clientOrderId,
        productId = productId,
        side = OrderSide.valueOf(side),
        type = OrderType.valueOf(orderType),
        status = OrderStatus.valueOf(status),
        size = BigDecimal(size),
        price = price?.let { BigDecimal(it) },
        filledSize = BigDecimal(filledSize),
        avgFilledPrice = avgFilledPrice?.let { BigDecimal(it) },
        createdAt = Instant.ofEpochMilli(createdAt)
    )
}

fun Order.toEntity(): OrderEntity {
    return OrderEntity(
        clientOrderId = clientOrderId,
        exchangeOrderId = id,
        productId = productId,
        side = side.name,
        orderType = type.name,
        status = status.name,
        size = size.toString(),
        price = price?.toString(),
        filledSize = filledSize.toString(),
        avgFilledPrice = avgFilledPrice?.toString(),
        gridLevel = null, // TODO: Add grid level tracking
        createdAt = createdAt.toEpochMilli(),
        updatedAt = System.currentTimeMillis()
    )
}
