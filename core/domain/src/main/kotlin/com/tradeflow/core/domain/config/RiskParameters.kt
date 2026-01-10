package com.tradeflow.core.domain.config

import java.math.BigDecimal

data class RiskParameters(
    val maxPositionPercent: BigDecimal = BigDecimal("0.05"),
    val maxTotalExposurePercent: BigDecimal = BigDecimal("0.10"),
    val maxDrawdownPercent: Double = 0.15,
    val drawdownWarningPercent: Double = 0.12,
    val minGridSpacingPercent: BigDecimal = BigDecimal("0.015"),
    val percentDecimalPlaces: Int = 4,
    val btcDecimalPlaces: Int = 8
)
