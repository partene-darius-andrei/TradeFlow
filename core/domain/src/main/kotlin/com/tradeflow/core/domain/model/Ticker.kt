package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

data class Ticker(
    val productId: String,
    val price: BigDecimal,
    val bid: BigDecimal,
    val ask: BigDecimal,
    val volume24h: BigDecimal,
    val timestamp: Instant
)
