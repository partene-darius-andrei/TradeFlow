package com.tradeflow.core.domain.config

import java.math.BigDecimal

/**
 * Complete trading system configuration bundling all parameter categories.
 *
 * This is the top-level configuration object that aggregates:
 * - **Strategy:** Mode detection, position sizing, stop/target placement
 * - **Risk:** Position limits, exposure caps, drawdown circuit breakers
 * - **Technical:** Indicator periods and candle timeframe
 * - **Execution:** Order mechanics, dust thresholds, retry logic
 * - **Profile:** The risk profile this configuration represents
 *
 * **Three Ways to Create TradingConfig:**
 *
 * 1. **Manual Construction** (for custom/test configs):
 * ```kotlin
 * val config = TradingConfig(
 *     strategy = StrategyParameters(...),
 *     risk = RiskParameters(...),
 *     technical = TechnicalParameters(...),
 *     execution = ExecutionParameters(...),
 *     profile = RiskProfile.BALANCED
 * )
 * ```
 *
 * 2. **From Risk Profile** (most common in production):
 * ```kotlin
 * val config = TradingConfig.forProfile(RiskProfile.BALANCED)
 * // Uses pre-optimized parameters from RiskProfile enum
 * ```
 *
 * 3. **Adaptive Selection** (automatic profile selection):
 * ```kotlin
 * val config = TradingConfig.adaptive(portfolioBalance = BigDecimal("750"))
 * // Automatically selects BALANCED profile for $750 balance
 * ```
 *
 * **Usage in Trading System:**
 * ```kotlin
 * val config = TradingConfig.forProfile(RiskProfile.BALANCED)
 *
 * // Inject into trading components
 * val decisionEngine = MakeTradingDecisionUseCase(config.strategy)
 * val riskManager = RiskManager(config.risk)
 * val technicalService = AnalyzeCandlesUseCase()
 * val orchestrator = TradeOrchestrator(
 *     config = config,
 *     decisionEngine = decisionEngine,
 *     riskManager = riskManager,
 *     // ...
 * )
 * ```
 *
 * **Consistency Guarantee:**
 * When created via `forProfile()` or `adaptive()`, all parameters are guaranteed to be
 * internally consistent. For example:
 * - risk.maxPositionPercent ≤ risk.maxTotalExposurePercent
 * - strategy.adxRangeThreshold < strategy.adxTrendThreshold
 * - technical.minCandlesRequired ≥ max(smaPeriod, adxPeriod, atrPeriod)
 *
 * **Parameter Optimization:**
 * The BALANCED profile parameters are heavily optimized via genetic algorithm (see
 * optimization tests). Other profiles use conservative defaults suitable for their
 * balance ranges.
 *
 * @property strategy Strategy parameters controlling mode detection and position sizing
 * @property risk Risk management parameters with position limits and circuit breakers
 * @property technical Technical indicator configuration (SMA, ADX, ATR periods)
 * @property execution Order execution mechanics (dust threshold, retries, post-only flag)
 * @property profile The risk profile this configuration represents (for logging/identification)
 *
 * @see RiskProfile for pre-configured profiles with optimized parameters
 * @see AdaptiveOptimizer for automatic profile selection based on balance
 */
data class TradingConfig(
    val strategy: StrategyParameters,
    val risk: RiskParameters,
    val technical: TechnicalParameters,
    val execution: ExecutionParameters,
    val profile: RiskProfile = RiskProfile.BALANCED
) {
    companion object {
        /**
         * Creates TradingConfig from a specific risk profile.
         *
         * This is the recommended way to create production configs. Each profile has
         * pre-configured, internally-consistent parameters optimized for its balance range.
         *
         * **Available Profiles:**
         * - AGGRESSIVE: Small accounts ($100-500), maximize growth
         * - BALANCED: Mid-size accounts ($500-1000), moderate risk
         * - CONSERVATIVE: Meaningful accounts ($1000-2000), preserve capital
         * - ULTRA_CONSERVATIVE: Large accounts ($2000+), maximum protection
         *
         * **Example:**
         * ```kotlin
         * val config = TradingConfig.forProfile(RiskProfile.BALANCED)
         * // All parameters are internally consistent and optimized
         * ```
         *
         * @param profile The risk profile to use for parameter selection
         * @return TradingConfig with all parameters set for the specified profile
         */
        fun forProfile(profile: RiskProfile): TradingConfig {
            return profile.createConfig()
        }

        /**
         * Creates TradingConfig by automatically selecting the appropriate profile for current balance.
         *
         * This delegates to [AdaptiveOptimizer.selectProfile] which maps balance ranges to
         * risk profiles. As the portfolio grows, this will automatically select more conservative
         * profiles.
         *
         * **Balance → Profile Mapping:**
         * - $100-500 → AGGRESSIVE
         * - $500-1000 → BALANCED
         * - $1000-2000 → CONSERVATIVE
         * - $2000+ → ULTRA_CONSERVATIVE
         *
         * **Example:**
         * ```kotlin
         * val config = TradingConfig.adaptive(portfolioBalance = BigDecimal("750"))
         * // Returns BALANCED profile config (since $750 is in $500-1000 range)
         * ```
         *
         * **Use Case:**
         * Use this in live trading where portfolio balance changes over time. The config
         * will automatically become more conservative as you build wealth.
         *
         * @param portfolioBalance Current total portfolio value (cash + position value)
         * @return TradingConfig with profile auto-selected based on balance
         */
        fun adaptive(portfolioBalance: BigDecimal): TradingConfig {
            return AdaptiveOptimizer.selectProfile(portfolioBalance)
        }
    }
}
