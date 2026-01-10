package com.tradeflow.core.domain.config

import java.math.BigDecimal

data class ExecutionParameters(
    val minBtcDustThreshold: BigDecimal = BigDecimal("0.00001"),
    val postOnlyOrders: Boolean = true,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000
)
