package com.tradeflow.core.domain.strategy

import java.math.BigDecimal

data class StrategyConfig(
    val smaPeriod: Int = 200,
    val adxPeriod: Int = 14,
    val atrPeriod: Int = 14,
    val adxTrendThreshold: Double = 35.0,
    val adxRangeThreshold: Double = 20.0,
    val stopLossAtrMultiplier: BigDecimal = BigDecimal("7.0"),
    val takeProfitAtrMultiplier: BigDecimal = BigDecimal("14.0"),
    val minGridSpacing: BigDecimal = BigDecimal("0.006"),
    val trendPositionPercent: BigDecimal = BigDecimal("0.05"),
    val gridPositionPercentPerLevel: BigDecimal = BigDecimal("0.02")
)
