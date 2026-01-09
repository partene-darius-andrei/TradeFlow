package com.tradeflow.core.domain.model

import java.math.BigDecimal

fun Portfolio.getBtcBalance(): BigDecimal {
    return balances
        .firstOrNull { it.currency == "BTC" }
        ?.available
        ?: BigDecimal.ZERO
}

fun Portfolio.getUsdBalance(): BigDecimal {
    return balances
        .firstOrNull { it.currency == "USD" || it.currency == "USDT" }
        ?.available
        ?: BigDecimal.ZERO
}
