package com.tradeflow.core.domain.config

import java.math.BigDecimal

/**
 * Order execution configuration parameters for controlling how orders are placed on the exchange.
 *
 * These parameters govern the mechanical aspects of order execution including minimum order sizes,
 * order types, and retry behavior for failed executions.
 *
 * **Purpose:**
 * - Prevent dust orders that exchanges reject (too small to execute)
 * - Configure maker/taker preference via post-only orders
 * - Handle transient failures with configurable retry logic
 *
 * **Usage in Trading:**
 * ```kotlin
 * val params = ExecutionParameters(
 *     minBtcDustThreshold = BigDecimal("0.0001"),
 *     postOnlyOrders = true  // Maker orders only, get fee rebate
 * )
 * if (orderSize < params.minBtcDustThreshold) {
 *     // Skip order - too small
 * }
 * ```
 *
 * **Design Choices:**
 * - Post-only orders avoid taker fees and reduce market impact
 * - Dust threshold prevents rejected orders and wasted API calls
 * - Retry logic handles transient network/exchange issues
 *
 * @property minBtcDustThreshold Minimum BTC order size in BTC units (e.g., 0.00001 BTC = $1 at $100k/BTC).
 *           Orders smaller than this are rejected by most exchanges. Default: 0.00001 BTC (~$1 at current prices).
 *
 * @property postOnlyOrders If true, all orders are post-only (maker orders that add liquidity).
 *           Post-only orders NEVER take liquidity, which means:
 *           - They get maker fee rebates instead of paying taker fees
 *           - They're rejected if they would match immediately
 *           - They reduce market impact by not crossing the spread
 *           Default: true (prefer maker fees and reduced slippage).
 *
 * @property maxRetries Maximum number of retry attempts for failed order placements.
 *           Retries handle transient failures like network timeouts or exchange rate limits.
 *           Default: 3 retries.
 *
 * @property retryDelayMs Delay in milliseconds between retry attempts.
 *           Exponential backoff is NOT implemented - this is a fixed delay.
 *           Default: 1000ms (1 second).
 *
 * @see TradingConfig for how execution parameters integrate with overall trading configuration
 */
data class ExecutionParameters(
    val minBtcDustThreshold: BigDecimal = BigDecimal("0.00001"),
    val postOnlyOrders: Boolean = true,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000
)
