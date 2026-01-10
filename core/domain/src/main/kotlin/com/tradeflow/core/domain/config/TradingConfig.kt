package com.tradeflow.core.domain.config

import java.math.BigDecimal

data class TradingConfig(
    val strategy: StrategyParameters,
    val risk: RiskParameters,
    val technical: TechnicalParameters,
    val execution: ExecutionParameters,
    val profile: RiskProfile = RiskProfile.BALANCED
) {
    companion object {
        fun forProfile(profile: RiskProfile): TradingConfig {
            return profile.createConfig()
        }

        fun adaptive(portfolioBalance: BigDecimal): TradingConfig {
            return AdaptiveOptimizer.selectProfile(portfolioBalance)
        }
    }
}
