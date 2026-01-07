package com.tradeflow.core.ui.extension

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.toCurrencyString(): String {
    return "$${String.format("%,.2f", this.setScale(2, RoundingMode.HALF_UP))}"
}

fun BigDecimal.toPercentageString(): String {
    val percent = this.multiply(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
    val sign = if (percent >= BigDecimal.ZERO) "+" else ""
    return "$sign$percent%"
}

fun BigDecimal.toCryptoString(): String {
    return this.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
