package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Represents the funding rate for a perpetual futures contract.
 *
 * Funding rates are periodic payments between LONG and SHORT traders to maintain
 * the perpetual futures price close to the spot price. They are charged every N hours
 * (typically 8 hours on most exchanges).
 *
 * **How Funding Works:**
 * - **Positive Funding Rate**: Perpetual trading above spot
 *   - Longs pay shorts (incentive to SHORT, pressure to sell)
 * - **Negative Funding Rate**: Perpetual trading below spot
 *   - Shorts pay longs (incentive to LONG, pressure to buy)
 *
 * **Example (Positive Funding):**
 * ```
 * FundingRate(
 *   productId = "BTC-PERP",
 *   rate = 0.0001,  // 0.01% per 8 hours
 *   nextFundingTime = 2025-01-10T16:00:00Z,
 *   predictedRate = 0.00008
 * )
 *
 * Position: LONG 0.02 BTC at $95,000 = $1,900 notional
 * Funding cost: $1,900 × 0.0001 = $0.19 per 8 hours
 * Daily cost: $0.19 × 3 = $0.57 (~0.03% per day)
 * Monthly cost: $0.57 × 30 = $17.10 (~0.9% per month)
 * ```
 *
 * **Example (Negative Funding):**
 * ```
 * FundingRate(
 *   productId = "BTC-PERP",
 *   rate = -0.0001,  // -0.01% per 8 hours
 *   nextFundingTime = 2025-01-10T16:00:00Z,
 *   predictedRate = -0.00009
 * )
 *
 * Position: LONG 0.02 BTC at $95,000 = $1,900 notional
 * Funding PAYMENT: $1,900 × 0.0001 = $0.19 received per 8 hours
 * Daily payment: $0.19 × 3 = $0.57 (get paid to hold LONG!)
 * ```
 *
 * **TradeFlow Risk Management:**
 * - If `abs(rate) > maxAcceptableFundingRate` (default 0.001 = 0.1%):
 *   - Position becomes too expensive to hold
 *   - Close position to avoid bleeding capital via funding
 *
 * **Typical Ranges:**
 * - Normal market: -0.01% to +0.01% per 8h
 * - Euphoric bull (extreme LONG bias): +0.05% to +0.1% per 8h
 * - Panic sell (extreme SHORT bias): -0.05% to -0.1% per 8h
 * - Extreme events: Can spike to ±0.5%+ (close immediately!)
 *
 * @property productId Perpetual futures product identifier (e.g., "BTC-PERP").
 *
 * @property rate Current funding rate as a decimal (not percentage).
 *           - Positive: Longs pay shorts
 *           - Negative: Shorts pay longs
 *           Example: 0.0001 = 0.01% = 1 basis point
 *
 * @property nextFundingTime When the next funding payment will be charged.
 *           Typically every 8 hours on most exchanges.
 *
 * @property predictedRate Estimated funding rate for the next period.
 *           Based on current premium/discount vs spot.
 *           May differ from current rate if market conditions change.
 *
 * @property timestamp When this funding rate snapshot was retrieved.
 *           Used for staleness checks (don't use old funding rates for decisions).
 */
data class FundingRate(
    val productId: String,
    val rate: BigDecimal,
    val nextFundingTime: Instant,
    val predictedRate: BigDecimal,
    val timestamp: Instant = Instant.now()
) {
    /**
     * Returns true if funding rate is positive (longs pay shorts).
     */
    val isPositive: Boolean get() = rate > BigDecimal.ZERO

    /**
     * Returns true if funding rate is negative (shorts pay longs).
     */
    val isNegative: Boolean get() = rate < BigDecimal.ZERO

    /**
     * Returns absolute value of funding rate (magnitude, ignoring sign).
     */
    val absoluteRate: BigDecimal get() = rate.abs()

    /**
     * Returns funding rate as percentage string for display.
     * Example: 0.0001 → "0.01%"
     */
    fun toPercentageString(): String {
        val percentage = rate * BigDecimal("100")
        val sign = if (rate >= BigDecimal.ZERO) "+" else ""
        return "$sign${percentage.setScale(4, java.math.RoundingMode.HALF_UP)}%"
    }

    /**
     * Calculates funding cost for a given position notional value.
     *
     * @param positionNotionalUsd Position size × current price (in USD)
     * @return Funding cost in USD (positive = cost, negative = payment received)
     *
     * Example:
     * ```kotlin
     * val fundingRate = FundingRate("BTC-PERP", BigDecimal("0.0001"), ...)
     * val position = 0.02 BTC × $95,000 = $1,900
     * val cost = fundingRate.calculateCost(BigDecimal("1900"))  // $0.19 cost
     * ```
     */
    fun calculateCost(positionNotionalUsd: BigDecimal): BigDecimal {
        return positionNotionalUsd * rate
    }

    /**
     * Returns true if funding rate exceeds acceptable threshold (too expensive).
     *
     * @param maxAcceptable Maximum acceptable funding rate (e.g., 0.001 = 0.1%)
     * @return True if position should be closed due to high funding cost
     */
    fun isTooExpensive(maxAcceptable: BigDecimal): Boolean {
        return absoluteRate > maxAcceptable
    }
}
