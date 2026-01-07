package com.tradeflow.core.domain.model

import java.math.BigDecimal

data class Balance(
    val currency: String,
    val available: BigDecimal,
    val hold: BigDecimal
) {
    val total: BigDecimal get() = available + hold
}
