package com.tradeflow.core.domain.model

import java.math.BigDecimal

data class Indicators(
    val sma200: BigDecimal,
    val sma200Previous: BigDecimal,
    val adx: Double,
    val atr: BigDecimal,
    val rsi: Double,
    val volumeSma: BigDecimal,
    val currentVolume: BigDecimal,
    val volumeRatio: Double,
    val obv: BigDecimal,
)
