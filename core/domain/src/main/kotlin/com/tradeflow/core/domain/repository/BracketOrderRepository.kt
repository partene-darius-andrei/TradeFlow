package com.tradeflow.core.domain.repository

import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal

interface BracketOrderRepository : ExchangeRepository {
    suspend fun placeBracketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        entryPrice: BigDecimal,
        takeProfit: BigDecimal,
        stopLoss: BigDecimal
    ): Result<Order>
}
