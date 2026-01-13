package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

data class Candle(
    val timestamp: Instant,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal
)
