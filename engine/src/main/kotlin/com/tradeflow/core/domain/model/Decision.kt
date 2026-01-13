package com.tradeflow.core.domain.model

import java.math.BigDecimal

sealed class Decision {

    data class Wait(val reason: String) : Decision()

    data class Trend(
        val direction: OrderSide,
        val entryPrice: BigDecimal,
        val stopLoss: BigDecimal,
        val takeProfit: BigDecimal
    ) : Decision()

    data object Range : Decision()
}
