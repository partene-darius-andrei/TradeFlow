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

enum class Granularity(val seconds: Long) {
    ONE_MINUTE(60),
    FIVE_MINUTE(300),
    FIFTEEN_MINUTE(900),
    THIRTY_MINUTE(1800),
    ONE_HOUR(3600),
    TWO_HOUR(7200),
    FOUR_HOUR(14400),
    SIX_HOUR(21600),
    ONE_DAY(86400)
}
