package com.tradeflow.core.domain.repository

import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal

/**
 * Repository extension for placing bracket orders (entry + stop-loss + take-profit).
 *
 * **Purpose:** Simplifies placing complex multi-leg orders for Trend mode trading.
 * Instead of manually placing 3 separate orders, call one method.
 *
 * **Why Extend ExchangeRepository:**
 * - Inherits all basic exchange operations (getPortfolio, placeLimit Order, etc.)
 * - Adds bracket order capability on top
 * - TradeOrchestrator can use both basic and bracket methods
 *
 * **Bracket Order Structure:**
 * A bracket order consists of THREE orders placed simultaneously:
 * 1. **Entry:** Limit order at entryPrice (opens position)
 * 2. **Take-Profit:** Limit order at takeProfit (closes position with profit)
 * 3. **Stop-Loss:** Stop-market order at stopLoss (closes position to limit loss)
 *
 * **Example Bracket Order (Long Trend):**
 * ```
 * Entry: BUY 0.001 BTC @ $95,000 (limit order)
 * Take-Profit: SELL 0.001 BTC @ $105,000 (limit order, +$10 profit)
 * Stop-Loss: SELL 0.001 BTC @ $90,000 (stop order, -$5 loss)
 * → Risk/Reward: 2:1 (risk $5k to make $10k)
 * ```
 *
 * **How It Works (Ideal Implementation):**
 * 1. Place entry limit order at entryPrice
 * 2. When entry fills:
 *    a. Place take-profit limit order at takeProfit
 *    b. Place stop-loss stop order at stopLoss
 * 3. When either take-profit or stop-loss fills:
 *    a. Cancel the other order (OCO: One-Cancels-Other)
 *
 * **Current Implementation (Manual Simulation):**
 * The actual implementation in SimulatedExchange is simplified:
 * - Places entry order immediately
 * - Take-profit and stop-loss are tracked but not as actual exchange orders
 * - Simulates fills based on price crossing levels
 * - **TODO:** Implement proper OCO (One-Cancels-Other) logic for production
 *
 * **Usage in TradeOrchestrator (Trend Mode):**
 * ```kotlin
 * // Trend decision has entry, stop, and target calculated
 * val decision = decisionEngine.evaluate(candles, currentPrice) as Decision.Trend
 *
 * // Place all three orders in one call
 * bracketOrderRepository.placeBracketOrder(
 *     productId = "BTC-USD",
 *     side = decision.direction,  // BUY for long
 *     size = positionSizeBtc,
 *     entryPrice = decision.entryPrice,
 *     takeProfit = decision.takeProfit,
 *     stopLoss = decision.stopLoss
 * )
 * ```
 *
 * **Why Bracket Orders are Powerful:**
 * - **Risk Management:** Stop-loss automatically limits downside
 * - **Profit Taking:** Take-profit automatically locks in gains
 * - **Fire and Forget:** Set it up once, let it run
 * - **Emotional Control:** Pre-defined exits prevent panic/greed decisions
 *
 * **Production Exchange Support:**
 * - **Coinbase:** Does NOT natively support bracket orders (manual implementation needed)
 * - **Interactive Brokers:** Native bracket order support
 * - **TD Ameritrade:** Native bracket order support
 * - For Coinbase, must place 3 orders separately and link them manually
 *
 * **TODO for Production:**
 * 1. Implement OCO (One-Cancels-Other) logic when one leg fills
 * 2. Handle partial fills (entry partially filled, adjust TP/SL size)
 * 3. Order correlation tracking (link entry to TP/SL orders)
 * 4. Failure recovery (what if entry fills but TP/SL placement fails?)
 *
 * @see ExchangeRepository for basic exchange operations
 * @see Decision.Trend for how bracket order parameters are calculated
 * @see TradeOrchestrator.runCycle for where this is used
 */
interface BracketOrderRepository : ExchangeRepository {
    /**
     * Places a bracket order: entry + take-profit + stop-loss in one operation.
     *
     * **Three Orders Placed:**
     * 1. Entry limit order at entryPrice
     * 2. Take-profit limit order at takeProfit (opposite side of entry)
     * 3. Stop-loss stop order at stopLoss (opposite side of entry)
     *
     * **Validation (performed by Decision.Trend):**
     * - For LONG (BUY): stopLoss < entryPrice < takeProfit
     * - For SHORT (SELL): takeProfit < entryPrice < stopLoss
     * - All prices must be positive
     * - Size must be above dust threshold
     *
     * **Example (Long Bracket):**
     * ```kotlin
     * placeBracketOrder(
     *     productId = "BTC-USD",
     *     side = OrderSide.BUY,        // Long position
     *     size = BigDecimal("0.001"),   // 0.001 BTC
     *     entryPrice = BigDecimal("95000"),    // Entry at $95k
     *     takeProfit = BigDecimal("105000"),   // Exit at $105k (+$10 profit)
     *     stopLoss = BigDecimal("90000")       // Stop at $90k (-$5 loss)
     * )
     * // Result: 2:1 risk/reward ratio
     * ```
     *
     * **Example (Short Bracket) - If Supported:**
     * ```kotlin
     * placeBracketOrder(
     *     productId = "BTC-USD",
     *     side = OrderSide.SELL,       // Short position
     *     size = BigDecimal("0.001"),   // 0.001 BTC
     *     entryPrice = BigDecimal("95000"),    // Entry at $95k
     *     takeProfit = BigDecimal("85000"),    // Exit at $85k (+$10 profit)
     *     stopLoss = BigDecimal("100000")      // Stop at $100k (-$5 loss)
     * )
     * ```
     *
     * **Return Value:**
     * Returns the ENTRY order. Take-profit and stop-loss are placed but not returned.
     * Caller should monitor entry order for fills, then check if TP/SL are active.
     *
     * **Error Handling:**
     * - Entry order fails: Returns ExchangeError, no orders placed
     * - TP/SL order fails after entry: **CRITICAL** - entry is exposed without protection!
     * - Implementation should either:
     *   a. Cancel entry if TP/SL fails (transactional approach)
     *   b. Retry TP/SL placement with exponential backoff
     *   c. Alert monitoring system (manual intervention needed)
     *
     * **Current Implementation (SimulatedExchange):**
     * - Creates entry order immediately
     * - Simulates TP/SL fills based on price movement
     * - No actual separate orders for TP/SL (tracked internally)
     *
     * @param productId Trading pair (e.g., "BTC-USD").
     * @param side OrderSide.BUY for long, OrderSide.SELL for short.
     * @param size Position size in base currency (BTC).
     * @param entryPrice Entry limit order price.
     * @param takeProfit Take-profit limit order price.
     *                   Must be > entryPrice for LONG, < entryPrice for SHORT.
     * @param stopLoss Stop-loss stop order price.
     *                 Must be < entryPrice for LONG, > entryPrice for SHORT.
     *
     * @return Result<Order> with the ENTRY order on success, or ExchangeError on failure.
     *
     * @see Decision.Trend for where these parameters come from
     * @see Order for the returned order structure
     */
    suspend fun placeBracketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        entryPrice: BigDecimal,
        takeProfit: BigDecimal,
        stopLoss: BigDecimal
    ): Result<Order>
}
