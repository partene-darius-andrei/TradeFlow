package com.tradeflow.core.domain.utils

import java.math.BigDecimal

internal fun String.bd(): BigDecimal = BigDecimal(this)
