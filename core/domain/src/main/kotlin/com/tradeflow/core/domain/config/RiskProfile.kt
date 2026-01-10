package com.tradeflow.core.domain.config

import java.math.BigDecimal

enum class RiskProfile {
    AGGRESSIVE,
    BALANCED,
    CONSERVATIVE,
    ULTRA_CONSERVATIVE;

    fun balanceThreshold(): BalanceRange {
        return when (this) {
            AGGRESSIVE -> BalanceRange(BigDecimal("100"), BigDecimal("500"))
            BALANCED -> BalanceRange(BigDecimal("500"), BigDecimal("1000"))
            CONSERVATIVE -> BalanceRange(BigDecimal("1000"), BigDecimal("2000"))
            ULTRA_CONSERVATIVE -> BalanceRange(BigDecimal("2000"), BigDecimal("10000000"))
        }
    }

    fun createConfig(): TradingConfig {
        return TradingConfig(
            strategy = strategyParams(),
            risk = riskParams(),
            technical = technicalParams(),
            execution = executionParams(),
            profile = this
        )
    }

    private fun strategyParams(): StrategyParameters {
        return when (this) {
            AGGRESSIVE -> StrategyParameters(
                trendPositionPercent = BigDecimal("0.08"),
                gridPositionPercentPerLevel = BigDecimal("0.12"),
                stopLossAtrMultiplier = BigDecimal("8.0"),
                takeProfitAtrMultiplier = BigDecimal("25.0")
            )
            BALANCED -> StrategyParameters(
                confirmationCandles = 4,
                adxTrendThreshold = 15.69036802888202,
                adxRangeThreshold = 1.3818857881651878,
                stopLossAtrMultiplier = BigDecimal("8.298988671516664"),
                takeProfitAtrMultiplier = BigDecimal("22.53153609428897"),
                trendPositionPercent = BigDecimal("0.0523"),
                gridPositionPercentPerLevel = BigDecimal("0.0710")
            )
            CONSERVATIVE -> StrategyParameters(
                trendPositionPercent = BigDecimal("0.03"),
                gridPositionPercentPerLevel = BigDecimal("0.05"),
                stopLossAtrMultiplier = BigDecimal("12.0"),
                takeProfitAtrMultiplier = BigDecimal("15.0")
            )
            ULTRA_CONSERVATIVE -> StrategyParameters(
                trendPositionPercent = BigDecimal("0.02"),
                gridPositionPercentPerLevel = BigDecimal("0.03"),
                stopLossAtrMultiplier = BigDecimal("15.0"),
                takeProfitAtrMultiplier = BigDecimal("12.0")
            )
        }
    }

    private fun riskParams(): RiskParameters {
        return when (this) {
            AGGRESSIVE -> RiskParameters(
                maxPositionPercent = BigDecimal("0.08"),
                maxTotalExposurePercent = BigDecimal("0.15"),
                maxDrawdownPercent = 0.20,
                drawdownWarningPercent = 0.18
            )
            BALANCED -> RiskParameters()
            CONSERVATIVE -> RiskParameters(
                maxPositionPercent = BigDecimal("0.03"),
                maxTotalExposurePercent = BigDecimal("0.08"),
                maxDrawdownPercent = 0.12,
                drawdownWarningPercent = 0.10
            )
            ULTRA_CONSERVATIVE -> RiskParameters(
                maxPositionPercent = BigDecimal("0.02"),
                maxTotalExposurePercent = BigDecimal("0.05"),
                maxDrawdownPercent = 0.10,
                drawdownWarningPercent = 0.08
            )
        }
    }

    private fun technicalParams(): TechnicalParameters {
        return TechnicalParameters()
    }

    private fun executionParams(): ExecutionParameters {
        return ExecutionParameters()
    }
}

data class BalanceRange(
    val min: BigDecimal,
    val max: BigDecimal
)
