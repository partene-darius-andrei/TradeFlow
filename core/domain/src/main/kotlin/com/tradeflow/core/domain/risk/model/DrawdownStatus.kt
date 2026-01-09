package com.tradeflow.core.domain.risk.model

sealed class DrawdownStatus {
    data class Normal(val drawdownPercent: Double) : DrawdownStatus()
    data class Warning(val drawdownPercent: Double) : DrawdownStatus()
    data class LimitBreached(val drawdownPercent: Double) : DrawdownStatus()
}
