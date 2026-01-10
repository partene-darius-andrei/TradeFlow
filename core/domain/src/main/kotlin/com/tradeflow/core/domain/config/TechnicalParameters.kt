package com.tradeflow.core.domain.config

import com.tradeflow.core.domain.model.Granularity

data class TechnicalParameters(
    val smaPeriod: Int = 200,
    val adxPeriod: Int = 14,
    val atrPeriod: Int = 14,
    val smaLookbackCandles: Int = 10,
    val minCandlesRequired: Int = 200,
    val barDurationMinutes: Int = 240,
    val granularity: Granularity = Granularity.FOUR_HOUR
)
