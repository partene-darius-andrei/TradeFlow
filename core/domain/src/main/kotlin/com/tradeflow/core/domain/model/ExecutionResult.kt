package com.tradeflow.core.domain.model

/**
 * Sealed class representing the outcome of a trading cycle execution.
 *
 * **Three Possible Outcomes:**
 * 1. **Success:** Cycle completed successfully, orders placed/canceled as needed
 * 2. **Skipped:** Cycle ran but took no action (waiting, already in trade, etc.)
 * 3. **Failed:** Cycle encountered an error and could not complete
 *
 * **Why Type-Safe Outcomes:**
 * Sealed class forces explicit handling of all outcomes. Ensures backtesting and
 * production code properly handle success, skip, and failure cases.
 *
 * **Usage in Backtesting:**
 * ```kotlin
 * var successCount = 0
 * var skippedCount = 0
 * var failedCount = 0
 *
 * for (cycle in 0..999) {
 *     val result = orchestrator.runCycle("BTC-USD", hwm)
 *     when (result.execution) {
 *         is ExecutionResult.Success -> {
 *             successCount++
 *             log.info("Cycle $cycle: ${result.execution.message}")
 *         }
 *         is ExecutionResult.Skipped -> {
 *             skippedCount++
 *             log.debug("Cycle $cycle: ${result.execution.reason}")
 *         }
 *         is ExecutionResult.Failed -> {
 *             failedCount++
 *             log.error("Cycle $cycle: ${result.execution.error}")
 *         }
 *     }
 *     hwm = result.updatedHighWaterMark
 * }
 *
 * println("Success: $successCount, Skipped: $skippedCount, Failed: $failedCount")
 * ```
 *
 * **Common Success Messages:**
 * - "Trend: Opened position" (placed bracket order)
 * - "Range: Placed 3/3 grid orders" (placed grid)
 * - "Range: Placed take-profit for grid fill" (grid level filled, added sell order)
 * - "Defense: Liquidated holdings" (sold BTC in defense mode)
 *
 * **Common Skip Reasons:**
 * - "Wait: Insufficient candles (need 200, have 50)"
 * - "Wait: ADX neutral zone (18.5)"
 * - "Defense: Clean" (defense mode but no holdings to liquidate)
 * - "Trend: Already in trade" (trend signal but position already open)
 * - "Range: Active" (range signal but grid already placed)
 *
 * **Common Failure Errors:**
 * - "EMERGENCY: 15% Drawdown reached. Liquidated." (circuit breaker)
 * - "Cycle failed: Network error" (exchange API error)
 * - "Cycle failed: Insufficient funds" (account balance too low)
 *
 * @see TradeOrchestrator.runCycle for how this is generated
 * @see CycleResult for the wrapper containing this plus high-water mark
 */
sealed class ExecutionResult {
    /**
     * Cycle completed successfully with action taken.
     *
     * **Indicates:**
     * - Orders were placed (trend bracket, grid orders, or take-profit)
     * - Orders were canceled (defense mode cleanup)
     * - Positions were liquidated (defense mode or circuit breaker)
     *
     * **NOT used for:**
     * - Decisions that resulted in no action (use Skipped instead)
     *
     * **Example Messages:**
     * - "Trend: Opened position"
     * - "Range: Placed 3/3 grid orders"
     * - "Defense: Liquidated holdings"
     *
     * @property message Human-readable description of what action was taken.
     *           Format: "{Mode}: {Action}" or "EMERGENCY: {Reason}".
     *           Used for logging and monitoring.
     */
    data class Success(val message: String) : ExecutionResult()

    /**
     * Cycle completed but no action was taken.
     *
     * **Common Reasons:**
     * - Wait decision (insufficient data, ADX neutral zone, etc.)
     * - Defense decision but account is already clean (no BTC to sell)
     * - Trend/Range decision but already in a trade (position/orders already exist)
     *
     * **Interpretation:**
     * Not a failure - the strategy made a conscious decision NOT to trade.
     * This is expected and healthy behavior (prevents overtrading).
     *
     * **Example Reasons:**
     * - "Wait: Insufficient candles"
     * - "Wait: ADX neutral zone (18.5)"
     * - "Defense: Clean"
     * - "Trend: Already in trade"
     * - "Range: Active"
     *
     * @property reason Human-readable explanation of why no action was taken.
     *           Format: "{Decision}: {Reason}" or "{Mode}: {State}".
     *           Used for logging and debugging.
     */
    data class Skipped(val reason: String) : ExecutionResult()

    /**
     * Cycle encountered an error and could not complete.
     *
     * **Common Errors:**
     * - Circuit breaker triggered: "EMERGENCY: 15% Drawdown reached. Liquidated."
     * - Exchange API error: "Cycle failed: Network error"
     * - Insufficient funds: "Cycle failed: Insufficient funds"
     * - Invalid data: "Cycle failed: Invalid candle data"
     *
     * **Handling:**
     * - In backtesting: Log error, continue to next cycle
     * - In production: Log error, alert monitoring system, continue to next scheduled cycle
     *
     * **Circuit Breaker (Special Case):**
     * Circuit breaker failures are INTENTIONAL emergency stops, not bugs.
     * They indicate the strategy has lost too much money and trading must halt.
     * These require manual intervention to reset.
     *
     * @property error Human-readable error description.
     *           Format: "EMERGENCY: {Reason}" for circuit breaker,
     *                   "Cycle failed: {Reason}" for other errors.
     *           Used for logging, alerting, and post-mortem analysis.
     */
    data class Failed(val error: String) : ExecutionResult()
}
