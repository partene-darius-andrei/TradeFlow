package com.tradeflow.core.domain.config

import java.math.BigDecimal

data class StrategyParameters(
    val confirmationCandles: Int = 3,
    val initialMode: DecisionMode = DecisionMode.RANGE,
    val adxTrendThreshold: Double = 20.0,
    val adxRangeThreshold: Double = 1.0,
    val stopLossAtrMultiplier: BigDecimal = BigDecimal("10.0"),
    val takeProfitAtrMultiplier: BigDecimal = BigDecimal("20.0"),
    val trendPositionPercent: BigDecimal = BigDecimal("0.05"),
    val gridPositionPercentPerLevel: BigDecimal = BigDecimal("0.08"),
    val gridLevels: Int = 3,
    val minGridSpacingAtrMultiplier: BigDecimal = BigDecimal("0.10"),
    val minGridSpacingFloor: BigDecimal = BigDecimal("0.01")
)

enum class DecisionMode {
    TREND,
    RANGE
}
