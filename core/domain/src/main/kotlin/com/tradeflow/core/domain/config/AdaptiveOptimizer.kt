package com.tradeflow.core.domain.config

import java.math.BigDecimal
import javax.inject.Inject

/**
 * Adaptive risk profile optimizer that automatically adjusts trading risk based on portfolio balance.
 *
 * This class implements a tiered risk management system where the risk profile becomes more
 * conservative as the portfolio balance grows. The goal is to be aggressive when growing a small
 * account (maximize returns) and become protective when the portfolio reaches meaningful size
 * (preserve capital).
 *
 * **Balance Thresholds:**
 * - $0-500: AGGRESSIVE (maximize growth with small capital)
 * - $500-1000: BALANCED (moderate risk/reward)
 * - $1000-2000: CONSERVATIVE (focus on preservation)
 * - $2000+: ULTRA_CONSERVATIVE (capital preservation priority)
 *
 * **Usage in Backtesting:**
 * ```kotlin
 * val config = AdaptiveOptimizer.selectProfile(portfolioBalance)
 * val optimizer = AdaptiveOptimizer()
 * val switchEvent = optimizer.detectProfileSwitch(currentProfile, newBalance)
 * if (switchEvent != null) {
 *     // Profile change detected, log and adjust strategy
 * }
 * ```
 *
 * **Design Rationale:**
 * - Small accounts need aggressive growth to reach meaningful size
 * - Large accounts prioritize capital preservation over growth
 * - Threshold-based switching provides clear, testable behavior
 * - No hysteresis to prevent oscillation (each balance maps to exactly one profile)
 *
 * @see RiskProfile for risk profile definitions and parameters
 * @see TradingConfig for how profiles translate to trading parameters
 */
class AdaptiveOptimizer @Inject constructor() {

    companion object {
        /**
         * Selects the optimal risk profile based on current portfolio balance.
         *
         * This is a pure function with deterministic behavior - the same balance always
         * returns the same profile. Used in both backtesting and live trading.
         *
         * **Threshold Logic:**
         * ```
         * balance < $500   → AGGRESSIVE
         * balance < $1000  → BALANCED
         * balance < $2000  → CONSERVATIVE
         * balance >= $2000 → ULTRA_CONSERVATIVE
         * ```
         *
         * **Example:**
         * ```kotlin
         * val config = AdaptiveOptimizer.selectProfile(BigDecimal("750"))
         * // Returns BALANCED profile config
         * ```
         *
         * @param portfolioBalance Current total portfolio value (cash + position value)
         * @return TradingConfig configured for the appropriate risk profile
         */
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

    /**
     * Detects if portfolio balance crossed a threshold requiring risk profile change.
     *
     * This method compares the current active profile with what the profile SHOULD be
     * given the new balance. If they differ, it returns a ProfileSwitchEvent with
     * details about the change.
     *
     * **Use Case:**
     * Monitor portfolio during trading cycle and adjust risk parameters when
     * balance thresholds are crossed.
     *
     * **Example:**
     * ```kotlin
     * val event = optimizer.detectProfileSwitch(
     *     currentProfile = RiskProfile.BALANCED,
     *     newBalance = BigDecimal("1050")
     * )
     * if (event != null) {
     *     log.info("Profile switch: ${event.from} → ${event.to} at ${event.balance}")
     *     updateTradingConfig(event.to.createConfig())
     * }
     * ```
     *
     * @param currentProfile The risk profile currently active in the trading system
     * @param newBalance Updated portfolio balance after recent trade or market movement
     * @return ProfileSwitchEvent if profile should change, null if no change needed
     */
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

/**
 * Event object representing a risk profile change triggered by balance threshold crossing.
 *
 * This is emitted by [AdaptiveOptimizer.detectProfileSwitch] when the portfolio balance
 * moves across a risk profile threshold boundary.
 *
 * **Usage in Logging:**
 * ```kotlin
 * event?.let { e ->
 *     log.info("PROFILE SWITCH: ${e.from.name} → ${e.to.name}")
 *     log.info("Balance: ${e.balance}, Reason: ${e.reason}")
 * }
 * ```
 *
 * @property from Previous risk profile before balance change
 * @property to New risk profile selected based on updated balance
 * @property balance Portfolio balance that triggered the profile change
 * @property reason Human-readable explanation of why the switch occurred
 */
data class ProfileSwitchEvent(
    val from: RiskProfile,
    val to: RiskProfile,
    val balance: BigDecimal,
    val reason: String
)
