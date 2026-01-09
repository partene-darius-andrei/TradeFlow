package com.tradeflow.core.domain.risk

import java.math.BigDecimal

data class RiskConfig(
    val maxPositionPercent: BigDecimal = BigDecimal("0.05"),
    val maxTotalExposurePercent: BigDecimal = BigDecimal("0.10"),
    val maxDrawdownPercent: Double = 0.15,
    val drawdownWarningPercent: Double = 0.12,
    val minGridSpacingPercent: BigDecimal = BigDecimal("0.015")
)
