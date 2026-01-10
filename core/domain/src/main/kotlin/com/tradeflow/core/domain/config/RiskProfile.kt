package com.tradeflow.core.domain.config

import java.math.BigDecimal

/**
 * Pre-configured risk profiles that bundle strategy, risk, technical, and execution parameters.
 *
 * Each profile represents a complete trading configuration optimized for a specific balance range
 * and risk tolerance. This enum provides four profiles ranging from aggressive (small accounts)
 * to ultra-conservative (large accounts).
 *
 * **The Four Profiles:**
 *
 * 1. **AGGRESSIVE** ($100-500): Maximum growth, high risk
 *    - Use case: Small account needs aggressive growth to reach meaningful size
 *    - Position size: 8% per trade
 *    - Stop loss: 8× ATR (tighter stops)
 *    - Drawdown limit: 20%
 *
 * 2. **BALANCED** ($500-1000): Moderate risk/reward
 *    - Use case: Mid-sized account balancing growth and preservation
 *    - Position size: 5.23% per trade (OPTIMIZED via genetic algorithm)
 *    - Stop loss: 8.3× ATR
 *    - Drawdown limit: 15%
 *    - **NOTE:** These values come from multi-regime optimization (see optimization tests)
 *
 * 3. **CONSERVATIVE** ($1000-2000): Capital preservation priority
 *    - Use case: Meaningful account size, protect what you've built
 *    - Position size: 3% per trade
 *    - Stop loss: 12× ATR (wider stops to avoid noise)
 *    - Drawdown limit: 12%
 *
 * 4. **ULTRA_CONSERVATIVE** ($2000+): Maximum protection
 *    - Use case: Large account, preservation > growth
 *    - Position size: 2% per trade
 *    - Stop loss: 15× ATR (very wide stops)
 *    - Drawdown limit: 10%
 *
 * **Usage with AdaptiveOptimizer:**
 * ```kotlin
 * // Automatically select profile based on balance
 * val config = AdaptiveOptimizer.selectProfile(portfolioBalance)
 *
 * // Or manually create a profile
 * val config = RiskProfile.BALANCED.createConfig()
 * ```
 *
 * **Design Philosophy:**
 * - Small accounts NEED aggressive growth (can't live off 5% returns on $100)
 * - Large accounts NEED preservation (losing 20% of $10k is devastating)
 * - Each profile is internally consistent (all parameters work together)
 * - BALANCED profile is heavily optimized via genetic algorithm
 *
 * @see AdaptiveOptimizer for automatic profile selection based on balance
 * @see TradingConfig for the complete configuration object created by each profile
 */
enum class RiskProfile {
    AGGRESSIVE,
    BALANCED,
    CONSERVATIVE,
    ULTRA_CONSERVATIVE;

    /**
     * Returns the balance range (min/max) where this profile is optimal.
     *
     * These ranges are used by [AdaptiveOptimizer] to automatically select the appropriate
     * profile as the portfolio balance grows or shrinks.
     *
     * **Balance Thresholds:**
     * - AGGRESSIVE: $100-500 (small account growth phase)
     * - BALANCED: $500-1000 (transition phase)
     * - CONSERVATIVE: $1000-2000 (preservation begins)
     * - ULTRA_CONSERVATIVE: $2000+ (full capital preservation)
     *
     * **Rationale:**
     * - $100-500: Need 100%+ returns just to reach $1000 → be aggressive
     * - $500-1000: Meaningful capital but room to grow → balanced approach
     * - $1000-2000: Real money now, protect it → conservative
     * - $2000+: Significant capital, preserve at all costs → ultra-conservative
     *
     * @return BalanceRange with min/max balance values for this profile
     */
    fun balanceThreshold(): BalanceRange {
        return when (this) {
            AGGRESSIVE -> BalanceRange(BigDecimal("100"), BigDecimal("500"))
            BALANCED -> BalanceRange(BigDecimal("500"), BigDecimal("1000"))
            CONSERVATIVE -> BalanceRange(BigDecimal("1000"), BigDecimal("2000"))
            ULTRA_CONSERVATIVE -> BalanceRange(BigDecimal("2000"), BigDecimal("10000000"))
        }
    }

    /**
     * Creates a complete TradingConfig with all parameters set for this risk profile.
     *
     * This is the main factory method that bundles strategy, risk, technical, and execution
     * parameters into a cohesive configuration.
     *
     * **Usage:**
     * ```kotlin
     * val config = RiskProfile.BALANCED.createConfig()
     * val orchestrator = TradeOrchestrator(config, ...)
     * ```
     *
     * @return TradingConfig with profile-specific parameters
     */
    fun createConfig(): TradingConfig {
        return TradingConfig(
            strategy = strategyParams(),
            risk = riskParams(),
            technical = technicalParams(),
            execution = executionParams(),
            profile = this
        )
    }

    /**
     * Returns strategy parameters optimized for this risk profile.
     *
     * **Key Differences by Profile:**
     *
     * **AGGRESSIVE:**
     * - Position size: 8% (trend), 12% per grid level
     * - Tighter stops: 8× ATR
     * - Aggressive targets: 25× ATR (3:1 reward/risk)
     *
     * **BALANCED (OPTIMIZED):**
     * - Position size: 5.23% (trend), 7.10% per grid level
     * - Stop: 8.3× ATR
     * - Target: 22.5× ATR (2.7:1 reward/risk)
     * - ADX thresholds: 15.69 (trend), 1.38 (range)
     * - **Source:** Multi-regime genetic algorithm optimization (see QuickMultiRegimeTest.kt)
     * - **Performance:** 86% loss reduction vs default parameters
     *
     * **CONSERVATIVE:**
     * - Position size: 3% (trend), 5% per grid level
     * - Wider stops: 12× ATR (avoid noise)
     * - Conservative targets: 15× ATR (1.25:1 reward/risk)
     *
     * **ULTRA_CONSERVATIVE:**
     * - Position size: 2% (trend), 3% per grid level
     * - Very wide stops: 15× ATR
     * - Tight targets: 12× ATR (0.8:1 reward/risk - prioritizes win rate over magnitude)
     *
     * @return StrategyParameters configured for this profile's risk tolerance
     */
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

    /**
     * Returns risk management parameters for this profile.
     *
     * **Key Differences:**
     *
     * **AGGRESSIVE:**
     * - Max position: 8% (large bets)
     * - Max exposure: 15% (can have 2 positions open)
     * - Drawdown limit: 20% (tolerates larger losses)
     * - Warning at: 18%
     *
     * **BALANCED (Default RiskParameters):**
     * - Max position: 5%
     * - Max exposure: 10%
     * - Drawdown limit: 15%
     * - Warning at: 12%
     *
     * **CONSERVATIVE:**
     * - Max position: 3% (small bets)
     * - Max exposure: 8%
     * - Drawdown limit: 12% (tight circuit breaker)
     * - Warning at: 10%
     *
     * **ULTRA_CONSERVATIVE:**
     * - Max position: 2% (very small bets)
     * - Max exposure: 5%
     * - Drawdown limit: 10% (very tight circuit breaker)
     * - Warning at: 8%
     *
     * **Pattern:** As profile becomes more conservative, all limits tighten to preserve capital.
     *
     * @return RiskParameters with profile-appropriate limits
     */
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

    /**
     * Returns technical analysis parameters for this profile.
     *
     * Currently, all profiles use the same technical parameters (SMA periods, ATR periods, etc.).
     * This could be customized in the future if different profiles benefit from different
     * indicator settings.
     *
     * @return TechnicalParameters (currently identical across all profiles)
     */
    private fun technicalParams(): TechnicalParameters {
        return TechnicalParameters()
    }

    /**
     * Returns execution parameters for this profile.
     *
     * Currently, all profiles use the same execution parameters (dust thresholds, retry logic, etc.).
     * Execution mechanics are generally profile-independent.
     *
     * @return ExecutionParameters (currently identical across all profiles)
     */
    private fun executionParams(): ExecutionParameters {
        return ExecutionParameters()
    }
}

/**
 * Represents a balance range (minimum to maximum) for a risk profile.
 *
 * Used by [RiskProfile.balanceThreshold] to define which balance ranges each profile covers.
 * This enables [AdaptiveOptimizer] to automatically select the appropriate profile as the
 * portfolio balance changes.
 *
 * **Example:**
 * ```kotlin
 * val range = RiskProfile.BALANCED.balanceThreshold()
 * // range.min = $500, range.max = $1000
 * if (portfolioBalance in range.min..range.max) {
 *     // Use BALANCED profile
 * }
 * ```
 *
 * @property min Minimum portfolio balance for this profile (inclusive)
 * @property max Maximum portfolio balance for this profile (exclusive)
 */
data class BalanceRange(
    val min: BigDecimal,
    val max: BigDecimal
)
