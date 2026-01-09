package com.tradeflow.core.domain.model

import java.math.BigDecimal

/**
 * Common mapping logic to keep data/exchange layers clean.
 */
object ModelMappers {
    fun toBigDecimal(value: String?): BigDecimal = value?.let { BigDecimal(it) } ?: BigDecimal.ZERO
}
