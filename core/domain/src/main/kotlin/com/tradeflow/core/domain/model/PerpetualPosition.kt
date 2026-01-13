package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Represents an open position in a perpetual futures contract.
 *
 * Perpetual futures positions differ from spot holdings in several ways:
 * - **Leverage**: Position size can exceed capital (e.g., 2x leverage with $500 = $1000 position)
 * - **Bidirectional**: Can profit from both UP (LONG) and DOWN (SHORT) movements
 * - **Funding Rate**: Charged every 8 hours to maintain price peg to spot
 * - **Liquidation Risk**: If price moves against you too far, position gets auto-liquidated
 *
 * **Example LONG Position:**
 * ```
 * PerpetualPosition(
 *   productId = "BTC-PERP",
 *   side = OrderSide.BUY,  // LONG position
 *   size = 0.02 BTC,
 *   entryPrice = $95,000,
 *   currentPrice = $96,000,
 *   unrealizedPnl = +$20.00,  // (96k - 95k) × 0.02
 *   leverage = 2.0,
 *   margin = $1,000,  // $95k × 0.02 / 2.0
 *   liquidationPrice = $47,500  // 50% drop = liquidation at 2x leverage
 * )
 * ```
 *
 * **Example SHORT Position:**
 * ```
 * PerpetualPosition(
 *   productId = "BTC-PERP",
 *   side = OrderSide.SELL,  // SHORT position
 *   size = 0.02 BTC,
 *   entryPrice = $95,000,
 *   currentPrice = $94,000,
 *   unrealizedPnl = +$20.00,  // (95k - 94k) × 0.02
 *   leverage = 2.0,
 *   margin = $1,000,
 *   liquidationPrice = $142,500  // 50% rise = liquidation at 2x leverage
 * )
 * ```
 *
 * **Unrealized PnL Calculation:**
 * - LONG: `(currentPrice - entryPrice) × size`
 * - SHORT: `(entryPrice - currentPrice) × size`
 *
 * **Liquidation Price Calculation (simplified):**
 * - LONG: `entryPrice × (1 - 1/leverage)` = entry × 0.5 at 2x leverage
 * - SHORT: `entryPrice × (1 + 1/leverage)` = entry × 1.5 at 2x leverage
 *
 * **Risk Management:**
 * - Monitor `unrealizedPnl` continuously
 * - Close position if approaching `liquidationPrice`
 * - Check funding rate to avoid expensive holdings
 *
 * @property productId Perpetual futures product identifier (e.g., "BTC-PERP", "BTC-PERPETUAL-USD").
 *
 * @property side Position direction:
 *           - BUY = LONG (profit from price increase)
 *           - SELL = SHORT (profit from price decrease)
 *
 * @property size Position size in base currency (e.g., BTC).
 *           This is the notional amount, not the margin required.
 *
 * @property entryPrice Average entry price in quote currency (e.g., USD per BTC).
 *           If position opened in multiple trades, this is the weighted average.
 *
 * @property currentPrice Current market price for the product.
 *           Used to calculate unrealized PnL.
 *
 * @property unrealizedPnl Current profit/loss in quote currency (e.g., USD).
 *           - Positive = profit
 *           - Negative = loss
 *           Not yet realized (position still open).
 *
 * @property leverage Position leverage multiplier (e.g., 2.0 = 2x leverage).
 *           Higher leverage = higher risk and reward.
 *
 * @property margin Margin (collateral) locked for this position in quote currency.
 *           Formula: `(size × entryPrice) / leverage`
 *
 * @property liquidationPrice Price at which position will be forcefully closed.
 *           - LONG: Price falls to this level → liquidation
 *           - SHORT: Price rises to this level → liquidation
 *           Hitting liquidation = lose all margin for this position.
 *
 * @property highWaterMarkPrice Best price reached since position opened (for trailing stops).
 *           - LONG: Tracks highest price reached (maxOf currentPrice over time)
 *           - SHORT: Tracks lowest price reached (minOf currentPrice over time)
 *           Used to prevent trailing stop-loss from moving backwards.
 *
 * @property timestamp When this position snapshot was taken.
 *           Used for staleness checks and logging.
 */
data class PerpetualPosition(
    val productId: String,
    val side: OrderSide,
    val size: BigDecimal,
    val entryPrice: BigDecimal,
    val currentPrice: BigDecimal,
    val unrealizedPnl: BigDecimal,
    val leverage: BigDecimal,
    val margin: BigDecimal,
    val liquidationPrice: BigDecimal,
    val highWaterMarkPrice: BigDecimal,
    val timestamp: Instant = Instant.now()
) {
    init {
        require(size > BigDecimal.ZERO) { "Position size must be positive: $size" }
        require(entryPrice > BigDecimal.ZERO) { "Entry price must be positive: $entryPrice" }
        require(currentPrice > BigDecimal.ZERO) { "Current price must be positive: $currentPrice" }
        require(leverage > BigDecimal.ZERO) { "Leverage must be positive: $leverage" }
        require(margin > BigDecimal.ZERO) { "Margin must be positive: $margin" }
        require(liquidationPrice > BigDecimal.ZERO) { "Liquidation price must be positive: $liquidationPrice" }
        require(highWaterMarkPrice > BigDecimal.ZERO) { "High water mark price must be positive: $highWaterMarkPrice" }
    }

    /**
     * Returns true if position is LONG (BUY), false if SHORT (SELL).
     */
    val isLong: Boolean get() = side == OrderSide.BUY

    /**
     * Returns true if currently profitable (unrealized PnL > 0).
     */
    val isProfitable: Boolean get() = unrealizedPnl > BigDecimal.ZERO

    /**
     * Returns PnL as percentage of margin.
     * Example: +20 USD PnL on 1000 USD margin = +2% return on margin.
     */
    val pnlPercentOfMargin: BigDecimal
        get() = if (margin > BigDecimal.ZERO) {
            unrealizedPnl.divide(margin, 4, java.math.RoundingMode.HALF_UP) * BigDecimal("100")
        } else {
            BigDecimal.ZERO
        }
}
