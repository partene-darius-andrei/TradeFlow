package com.tradeflow.core.domain.risk.model

/**
 * Sealed class representing portfolio drawdown severity levels.
 *
 * **Drawdown:** Percentage decline from peak portfolio value (high-water mark).
 * This measures the maximum loss from the highest point.
 *
 * **Three Severity Levels:**
 * 1. **Normal:** Drawdown within acceptable range (< warning threshold)
 * 2. **Warning:** Approaching limit (≥ warning threshold, < max threshold)
 * 3. **LimitBreached:** Circuit breaker triggered (≥ max threshold)
 *
 * **Example Thresholds (BALANCED Profile):**
 * - Normal: < 12% drawdown
 * - Warning: 12-15% drawdown
 * - LimitBreached: ≥ 15% drawdown
 *
 * **Usage:**
 * ```kotlin
 * val status = riskManager.checkDrawdown(currentEquity, highWaterMark)
 * when (status) {
 *     is DrawdownStatus.Normal -> log.info("Drawdown: ${status.drawdownPercent * 100}%")
 *     is DrawdownStatus.Warning -> log.warn("WARNING: ${status.drawdownPercent * 100}% drawdown")
 *     is DrawdownStatus.LimitBreached -> {
 *         log.error("CIRCUIT BREAKER: ${status.drawdownPercent * 100}% drawdown")
 *         liquidateAllPositions()
 *     }
 * }
 * ```
 *
 * @see RiskManager.checkDrawdown for how this is calculated
 */
sealed class DrawdownStatus {
    /**
     * Normal drawdown - portfolio healthy, continue trading.
     *
     * Drawdown is below the warning threshold (typically < 12% for BALANCED).
     * No action needed, trading continues normally.
     *
     * @property drawdownPercent Current drawdown as decimal (0.05 = 5%).
     *           Range: 0.0 (at peak) to warningThreshold (typically 0.12).
     */
    data class Normal(val drawdownPercent: Double) : DrawdownStatus()

    /**
     * Warning drawdown - portfolio underperforming, log alert.
     *
     * Drawdown has crossed the warning threshold but not yet the circuit breaker.
     * Log warning and monitor closely, but continue trading.
     *
     * **Typical Range (BALANCED):** 12-15% drawdown
     *
     * @property drawdownPercent Current drawdown as decimal (0.13 = 13%).
     *           Range: warningThreshold (0.12) to maxThreshold (0.15).
     */
    data class Warning(val drawdownPercent: Double) : DrawdownStatus()

    /**
     * Circuit breaker triggered - emergency stop, liquidate all positions.
     *
     * Drawdown has reached or exceeded the maximum threshold.
     * **CRITICAL:** TradeOrchestrator must immediately:
     * 1. Cancel all open orders
     * 2. Sell all BTC holdings at market price
     * 3. Halt trading until system is manually reset
     *
     * This is the LAST LINE OF DEFENSE against catastrophic losses.
     *
     * **Typical Threshold (BALANCED):** ≥ 15% drawdown
     *
     * @property drawdownPercent Current drawdown as decimal (0.16 = 16%).
     *           Range: maxThreshold (0.15) to 1.0 (100% loss, account wiped).
     */
    data class LimitBreached(val drawdownPercent: Double) : DrawdownStatus()
}
