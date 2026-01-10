package com.tradeflow.core.domain.risk.model

/**
 * Sealed class representing the result of order risk validation.
 *
 * **Two Outcomes:**
 * 1. **Approved:** Order passes all risk checks, safe to execute
 * 2. **Rejected:** Order violates one or more risk limits, must NOT be executed
 *
 * **Risk Checks Performed:**
 * - Position size limit (order value ≤ maxPositionPercent)
 * - Total exposure limit (current + new ≤ maxTotalExposurePercent)
 * - Portfolio validity (equity > 0)
 *
 * **Example Usage:**
 * ```kotlin
 * val request = PlaceOrderRequest(
 *     productId = "BTC-USD",
 *     side = OrderSide.BUY,
 *     type = OrderType.LIMIT,
 *     size = BigDecimal("0.001"),
 *     price = BigDecimal("95000")
 * )
 *
 * val riskCheck = riskManager.validateOrder(request, portfolio, currentPrice)
 * when (riskCheck) {
 *     is RiskCheck.Approved -> {
 *         exchangeRepository.placeLimitOrder(request)
 *         log.info("Order placed successfully")
 *     }
 *     is RiskCheck.Rejected -> {
 *         log.warn("Order rejected: ${riskCheck.reason}")
 *         // Do NOT place the order
 *     }
 * }
 * ```
 *
 * **Why Type-Safe Validation:**
 * Sealed class forces explicit handling of both outcomes. Compiler error if
 * Rejected case is not handled, preventing accidental execution of risky orders.
 *
 * @see RiskManager.validateOrder for how this is generated
 */
sealed class RiskCheck {
    /**
     * Order approved - passes all risk checks, safe to execute.
     *
     * This means:
     * - Portfolio equity is positive
     * - Position size ≤ maxPositionPercent limit
     * - Total exposure ≤ maxTotalExposurePercent limit (for BUY orders)
     *
     * Proceed with order placement.
     */
    object Approved : RiskCheck()

    /**
     * Order rejected - violates one or more risk limits, must NOT be executed.
     *
     * **Common Rejection Reasons:**
     * - "Position size 9.50% exceeds limit 5.23%"
     * - "Total exposure 10.45% would exceed limit 10.00%"
     * - "Cannot validate order: portfolio equity is zero or negative"
     *
     * **Action:**
     * Do NOT place the order. Log the reason for debugging and monitoring.
     * Consider adjusting position size or waiting for existing positions to close.
     *
     * @property reason Human-readable explanation of why the order was rejected.
     *           Used for logging, debugging, and user feedback.
     *           Format: "X exceeds limit Y" or "Cannot validate: Z".
     */
    data class Rejected(val reason: String) : RiskCheck()
}
