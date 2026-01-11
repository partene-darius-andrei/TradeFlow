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
 * @property leverage Leverage multiplier for perpetual futures (1x-10x typically).
 *           TradeFlow uses 2x leverage for moderate amplification:
 *           - With $500 capital + 2x leverage = $1000 effective position size
 *           - Profits and losses are multiplied by leverage factor
 *           - Higher leverage increases both gains and risks
 *           Default: 2.0 (2x leverage - moderate risk/reward).
 *
 * @property fundingRateIntervalHours How often funding is charged on perpetual futures (hours).
 *           Perpetual futures charge funding every N hours to maintain price peg to spot.
 *           - Positive funding: Longs pay shorts
 *           - Negative funding: Shorts pay longs
 *           Default: 8 hours (Coinbase/Binance standard).
 *
 * @property maxAcceptableFundingRate Maximum acceptable funding rate before closing position (decimal).
 *           If funding rate exceeds this threshold, position becomes too expensive to hold.
 *           Example: 0.0005 = 0.05% per 8 hours = ~0.15%/day = ~4.5%/month cost
 *           Default: 0.0010 (0.1% per 8 hours = max acceptable cost).
 *
 * @see TradingConfig for how execution parameters integrate with overall trading configuration
 */
data class ExecutionParameters(
    val minBtcDustThreshold: BigDecimal = BigDecimal("0.00001"),
    val postOnlyOrders: Boolean = true,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000,
    val leverage: BigDecimal = BigDecimal("2.0"),
    val fundingRateIntervalHours: Int = 8,
    val maxAcceptableFundingRate: BigDecimal = BigDecimal("0.0010")
)
