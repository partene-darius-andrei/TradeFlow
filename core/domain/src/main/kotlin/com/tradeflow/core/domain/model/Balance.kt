package com.tradeflow.core.domain.model

import java.math.BigDecimal

/**
 * Account balance for a single currency showing available and held amounts.
 *
 * Exchange accounts separate funds into two categories:
 * - **Available:** Funds that can be immediately used for new trades
 * - **Hold:** Funds locked in open orders or pending withdrawals
 *
 * **Why Split Available/Hold:**
 * When you place a limit order, the exchange "holds" the funds so you can't
 * double-spend them. Once the order fills or gets canceled, funds return to "available".
 *
 * **Example (BTC balance):**
 * ```
 * Currency: BTC
 * Available: 0.5 BTC  (can trade this)
 * Hold: 0.1 BTC       (locked in open orders)
 * Total: 0.6 BTC      (account balance = available + hold)
 * ```
 *
 * **Usage in Portfolio:**
 * ```kotlin
 * val btcBalance = repository.getBalance("BTC").getOrThrow()
 * println("Available to trade: ${btcBalance.available} BTC")
 * println("Total balance: ${btcBalance.total} BTC")
 * ```
 *
 * **Position Sizing:**
 * Only the `available` amount should be used for position sizing calculations.
 * The `hold` amount is already allocated and unavailable for new trades.
 *
 * @property currency Currency code (e.g., "BTC", "USD", "ETH").
 *           Typically 3-4 character ISO code or blockchain ticker.
 *
 * @property available Funds available for immediate use in new trades.
 *           Unit: Currency units (e.g., BTC for Bitcoin, USD for US Dollar).
 *           This is the amount used for position sizing.
 *           Can be zero if all funds are locked in orders.
 *
 * @property hold Funds locked in open orders or pending operations.
 *           Unit: Currency units.
 *           These funds are "yours" but temporarily unavailable.
 *           Returns to `available` when orders fill/cancel.
 *
 * @property total Computed property: available + hold.
 *           This is your total account balance for this currency.
 *           **Read-only:** Automatically calculated, not stored.
 *
 * @see Portfolio for aggregated balance across all currencies
 */
data class Balance(
    val currency: String,
    val available: BigDecimal,
    val hold: BigDecimal
) {
    /**
     * Total balance = available + hold.
     *
     * This is your complete account balance for this currency, including both
     * tradeable funds (available) and locked funds (hold).
     *
     * **Example:**
     * ```
     * val balance = Balance("BTC", available = BigDecimal("0.5"), hold = BigDecimal("0.1"))
     * println(balance.total)  // 0.6 BTC
     * ```
     */
    val total: BigDecimal get() = available + hold
}
