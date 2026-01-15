package com.tradeflow.core.domain.model

import com.tradeflow.core.domain.StrategyConfig
import java.math.BigDecimal
import java.math.RoundingMode

data class Order(
    val direction: Side,
    val entryPrice: BigDecimal,
    val stopLoss: BigDecimal,
    val takeProfit: BigDecimal,
    val leverage: BigDecimal = BigDecimal.ONE,
    var exitPrice: BigDecimal? = null,
    var exitReason: String? = null
) {
    enum class Side {
        BUY,
        SELL
    }

    val isOpen: Boolean get() = exitPrice == null

    fun calculatePnl(): BigDecimal {
        val exit = exitPrice ?: return BigDecimal.ZERO
        return when (direction) {
            Side.BUY -> (exit - entryPrice).divide(entryPrice, StrategyConfig.PNL_PRECISION_DECIMAL_PLACES, RoundingMode.HALF_UP)
            Side.SELL -> (entryPrice - exit).divide(entryPrice, StrategyConfig.PNL_PRECISION_DECIMAL_PLACES, RoundingMode.HALF_UP)
        }
    }
}
