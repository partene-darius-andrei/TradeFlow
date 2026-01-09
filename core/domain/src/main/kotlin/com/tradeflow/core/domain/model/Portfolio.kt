package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

data class Portfolio(
    val balances: List<Balance>,
    val totalEquityUsd: BigDecimal,
    val timestamp: Instant
) {
    fun getBalance(currency: String): BigDecimal =
        balances.firstOrNull { it.currency == currency }?.available ?: BigDecimal.ZERO

    fun getBtcBalance(): BigDecimal = getBalance("BTC")
    
    fun getUsdBalance(): BigDecimal {
        val usd = getBalance("USD")
        return if (usd > BigDecimal.ZERO) usd else getBalance("USDT")
    }
}
