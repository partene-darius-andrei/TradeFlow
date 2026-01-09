package com.tradeflow.core.domain.model

import java.math.BigDecimal

sealed class Decision {
    data class Wait(val reason: String) : Decision()
    data class Defense(
        val reason: String,
        val currentPrice: BigDecimal,
        val sma200: BigDecimal
    ) : Decision()
    data class Trend(
        val direction: OrderSide,
        val entryPrice: BigDecimal,
        val stopLoss: BigDecimal,
        val takeProfit: BigDecimal,
        val positionSizePercent: BigDecimal,
        val adx: Double,
        val atr: BigDecimal
    ) : Decision()
    data class Range(
        val gridSpacing: BigDecimal,
        val levels: Int,
        val positionSizePercentPerLevel: BigDecimal,
        val adx: Double,
        val atr: BigDecimal
    ) : Decision()
}
