package com.tradeflow.core.domain.model

import com.tradeflow.core.domain.TradingConfig
import java.math.BigDecimal
import java.math.RoundingMode

data class Order(
    val direction: OrderSide,
    val entryPrice: BigDecimal,
    val stopLoss: BigDecimal,
    val takeProfit: BigDecimal,
    val leverage: BigDecimal = BigDecimal.ONE,
    var exitPrice: BigDecimal? = null,
    var exitReason: String? = null
) {
    val isOpen: Boolean get() = exitPrice == null

    fun calculatePnl(): BigDecimal {
        val exit = exitPrice ?: return BigDecimal.ZERO
        return when (direction) {
            OrderSide.BUY -> (exit - entryPrice).divide(entryPrice, TradingConfig.Technical.PNL_PRECISION_DECIMAL_PLACES, RoundingMode.HALF_UP)
            OrderSide.SELL -> (entryPrice - exit).divide(entryPrice, TradingConfig.Technical.PNL_PRECISION_DECIMAL_PLACES, RoundingMode.HALF_UP)
        }
    }
}
