package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Complete portfolio state snapshot showing all currency balances and total equity.
 *
 * **Responsibility:** Aggregates all account balances into a single portfolio view with
 * total USD-denominated equity value. Used for risk calculations and position sizing.
 *
 * **What's Included:**
 * - Individual currency balances (BTC, USD, USDT, etc.)
 * - Total portfolio value converted to USD
 * - Timestamp of the snapshot
 *
 * **Example Portfolio:**
 * ```
 * Timestamp: 2025-01-10T12:00:00Z
 * Balances:
 *   - BTC: 0.01234567 available, 0.001 held
 *   - USD: 500.00 available, 0 held
 * Total Equity: $1,672.84 USD
 * ```
 *
 * **How Total Equity is Calculated:**
 * Total equity = (BTC balance × BTC/USD price) + USD balance + USDT balance
 *
 * This calculation is performed by UpdatePortfolioUseCase when fetching portfolio state
 * from the exchange. The Portfolio object stores the pre-calculated total.
 *
 * **Why Store Total Equity:**
 * - Risk calculations need portfolio value in consistent USD denomination
 * - Position sizing: "5% of portfolio" = `totalEquityUsd × 0.05`
 * - Drawdown tracking: Compare current equity to high-water mark
 * - Avoids recalculating price conversions repeatedly
 *
 * **Usage in Position Sizing:**
 * ```kotlin
 * val portfolio = exchangeRepository.getPortfolio().getOrThrow()
 * val positionSizeUsd = portfolio.totalEquityUsd * BigDecimal("0.0523")  // 5.23%
 * val positionSizeBtc = positionSizeUsd / currentBtcPrice
 * ```
 *
 * **Usage in Risk Management:**
 * ```kotlin
 * val drawdownStatus = riskManager.checkDrawdown(portfolio.totalEquityUsd, highWaterMark)
 * ```
 *
 * **Multi-Currency Support:**
 * The balances list can contain any currencies supported by the exchange:
 * - BTC (Bitcoin)
 * - USD (US Dollar)
 * - USDT (Tether stablecoin)
 * - ETH, SOL, etc. (other cryptocurrencies)
 *
 * However, this strategy only trades BTC-USD, so typically only BTC and USD matter.
 *
 * **Thread Safety:**
 * Immutable data class - thread-safe for reading. Create new instance to update.
 *
 * @property balances List of individual currency balances.
 *           Each balance shows available and held amounts for one currency.
 *           Can be empty if account has zero balance (new account).
 *
 * @property totalEquityUsd Total portfolio value in USD.
 *           Includes all currencies converted to USD equivalent.
 *           Used for position sizing, drawdown tracking, and risk calculations.
 *           Unit: USD.
 *
 * @property timestamp When this portfolio snapshot was captured.
 *           Used for auditing and time-series analysis of portfolio growth.
 *           Should match the candle timestamp for backtesting consistency.
 *
 * @see Balance for individual currency balance structure
 * @see UpdatePortfolioUseCase for how portfolio state is fetched and calculated
 */
data class Portfolio(
    val balances: List<Balance>,
    val totalEquityUsd: BigDecimal,
    val timestamp: Instant
) {
    /**
     * Gets available balance for a specific currency.
     *
     * Returns the **available** (not held) balance for the given currency.
     * If currency not found in balances list, returns ZERO.
     *
     * **Why Available Only:**
     * Only available balance can be used for new trades. Held balance is already
     * locked in open orders and unavailable.
     *
     * **Example:**
     * ```kotlin
     * val btc = portfolio.getBalance("BTC")  // 0.01234567
     * val eth = portfolio.getBalance("ETH")  // 0 (not in balances list)
     * ```
     *
     * @param currency Currency code (e.g., "BTC", "USD", "USDT").
     *                 Case-sensitive, must match Balance.currency exactly.
     *
     * @return Available balance for the currency, or ZERO if currency not found.
     */
    fun getBalance(currency: String): BigDecimal =
        balances.firstOrNull { it.currency == currency }?.available ?: BigDecimal.ZERO

    /**
     * Gets available BTC balance.
     *
     * Convenience method for `getBalance("BTC")`.
     * Returns ZERO if no BTC balance exists.
     *
     * **Usage:**
     * ```kotlin
     * val btc = portfolio.getBtcBalance()
     * if (btc > minDustThreshold) {
     *     // Enough BTC to sell
     * }
     * ```
     *
     * @return Available BTC balance, or ZERO if no BTC.
     */
    fun getBtcBalance(): BigDecimal = getBalance("BTC")

    /**
     * Gets available USD balance, with fallback to USDT.
     *
     * Returns USD balance if available and positive.
     * If USD is zero or missing, returns USDT balance instead.
     *
     * **Why USDT Fallback:**
     * Some exchanges (like Coinbase) use USDT (Tether stablecoin) instead of USD.
     * This method handles both cases transparently.
     *
     * **Example:**
     * ```kotlin
     * // Portfolio has USDT but no USD
     * val usd = portfolio.getUsdBalance()  // Returns USDT balance
     * ```
     *
     * @return USD balance if positive, otherwise USDT balance, or ZERO if neither exists.
     */
    fun getUsdBalance(): BigDecimal {
        val usd = getBalance("USD")
        return if (usd > BigDecimal.ZERO) usd else getBalance("USDT")
    }
}
