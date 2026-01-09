package com.tradeflow.core.domain.risk.model

import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderType
import java.math.BigDecimal

data class PlaceOrderRequest(
    val productId: String,
    val side: OrderSide,
    val type: OrderType,
    val size: BigDecimal,
    val price: BigDecimal?,
    val stopLoss: BigDecimal? = null,
    val takeProfit: BigDecimal? = null
)
