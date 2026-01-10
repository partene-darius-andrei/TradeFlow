package com.tradeflow.core.domain.config

import java.math.BigDecimal
import javax.inject.Inject

class AdaptiveOptimizer @Inject constructor() {

    companion object {
        fun selectProfile(portfolioBalance: BigDecimal): TradingConfig {
            val profile = when {
                portfolioBalance < BigDecimal("500") -> RiskProfile.AGGRESSIVE
                portfolioBalance < BigDecimal("1000") -> RiskProfile.BALANCED
                portfolioBalance < BigDecimal("2000") -> RiskProfile.CONSERVATIVE
                else -> RiskProfile.ULTRA_CONSERVATIVE
            }

            return profile.createConfig()
        }
    }

    fun detectProfileSwitch(
        currentProfile: RiskProfile,
        newBalance: BigDecimal
    ): ProfileSwitchEvent? {
        val newConfig = AdaptiveOptimizer.selectProfile(newBalance)
        return if (newConfig.profile != currentProfile) {
            ProfileSwitchEvent(
                from = currentProfile,
                to = newConfig.profile,
                balance = newBalance,
                reason = "Balance threshold crossed: ${newConfig.profile.balanceThreshold()}"
            )
        } else null
    }
}

data class ProfileSwitchEvent(
    val from: RiskProfile,
    val to: RiskProfile,
    val balance: BigDecimal,
    val reason: String
)
