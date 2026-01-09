package com.tradeflow.core.domain.risk.model

sealed class RiskCheck {
    object Approved : RiskCheck()
    data class Rejected(val reason: String) : RiskCheck()
}
