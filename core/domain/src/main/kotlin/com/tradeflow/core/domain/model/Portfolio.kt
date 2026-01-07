package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

data class Portfolio(
    val balances: List<Balance>,
    val totalEquityUsd: BigDecimal,
    val timestamp: Instant
)
