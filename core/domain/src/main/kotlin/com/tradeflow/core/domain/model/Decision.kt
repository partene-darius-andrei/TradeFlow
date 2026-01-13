package com.tradeflow.core.domain.model

import java.math.BigDecimal

/**
 * Type of financial product to trade.
 *
 * **SPOT:** Traditional spot trading where you buy/sell the actual asset.
 * - Only LONG positions (buy to profit from uptrend)
 * - Immediate settlement
 * - No funding fees
 *
 * **PERPETUAL:** Perpetual futures contracts (derivatives).
 * - Both LONG and SHORT positions (profit from up OR down)
 * - No expiry date (unlike traditional futures)
 * - Funding rate charged every 8 hours (~0.01%)
 * - Supports leverage (1x-10x typically)
 *
 * **TradeFlow Strategy:**
 * Uses PERPETUAL exclusively to enable shorting in bear markets.
 * With perpetuals + 2x leverage, the strategy can profit in both directions:
 * - Bull market (price > SMA200): LONG positions
 * - Bear market (price < SMA200): SHORT positions
 */
enum class ProductType {
    /** Traditional spot trading (long only) */
    SPOT,

    /** Perpetual futures (long and short, supports leverage) */
    PERPETUAL
}

/**
 * Sealed class hierarchy representing trading decisions from MakeTradingDecisionUseCase.
 *
 * The decision engine analyzes market conditions (ADX, SMA, ATR) and outputs ONE of three
 * decision types. Each decision type contains all parameters needed to execute it.
 *
 * **Three Decision Types:**
 *
 * 1. **Wait:** Do nothing (insufficient data, circuit breaker active, etc.)
 * 2. **Trend:** Strong directional move (high ADX), place directional trade
 * 3. **Range:** Choppy sideways market (low ADX), mean-reversion trade
 *
 * **Decision Flow:**
 * ```
 * Market Data → MakeTradingDecisionUseCase → Decision (one of 3 types)
 *                                            ↓
 *                                      TradeOrchestrator
 *                                            ↓
 *                                       Execute Orders
 * ```
 *
 * **Usage:**
 * ```kotlin
 * val decision = decisionEngine.decide(context)
 * when (decision) {
 *     is Decision.Wait -> log.info("Waiting: ${decision.reason}")
 *     is Decision.Trend -> orchestrator.executeTrendTrade(decision)
 *     is Decision.Range -> orchestrator.executeGridTrade(decision)
 * }
 * ```
 *
 * **Validation:**
 * Each decision subclass validates its parameters in init blocks. Invalid parameters
 * (negative prices, wrong stop/target placement, etc.) throw IllegalArgumentException.
 *
 * @see MakeTradingDecisionUseCase for how decisions are generated
 * @see TradeOrchestrator for how decisions are executed
 */
sealed class Decision {

    /**
     * Decision to wait and not place any trades.
     *
     * Wait decisions occur when:
     * - Insufficient candle data for technical analysis
     * - Circuit breaker is active (max drawdown exceeded)
     * - No clear market regime (transitioning between modes)
     * - Risk checks failed (exposure limits exceeded)
     *
     * **Example Reasons:**
     * - "Insufficient candles (need 200, have 50)"
     * - "Circuit breaker active: 16% drawdown exceeds 15% limit"
     * - "Mode transition: waiting for 3 confirmation candles"
     * - "Max exposure reached: 10% limit"
     *
     * **Usage:**
     * ```kotlin
     * when (decision) {
     *     is Decision.Wait -> {
     *         log.info("Waiting: ${decision.reason}")
     *         // Do nothing, re-evaluate next cycle
     *     }
     *     // ...
     * }
     * ```
     *
     * @property reason Human-readable explanation of why we're waiting.
     *           Used for logging and debugging.
     */
    data class Wait(val reason: String) : Decision()

    /**
     * Decision to place a directional trend-following trade.
     *
     * Trend decisions occur when ADX is HIGH (typically > 20), indicating a strong
     * directional market move. In trend mode, the strategy places a single larger
     * position in the direction of the trend with wider stops and targets.
     *
     * **When Trend Activates:**
     * - ADX > adxTrendThreshold (default 20)
     * - Market has clear directional momentum
     * - Goal: Capture large moves with higher position size
     *
     * **Trend Strategy:**
     * - Single position sized at `trendPositionPercent` of portfolio (e.g., 5%)
     * - Stop-loss: `entryPrice ± (stopLossAtrMultiplier × ATR)` (e.g., 10× ATR)
     * - Take-profit: `entryPrice ± (takeProfitAtrMultiplier × ATR)` (e.g., 20× ATR)
     * - Direction: BUY (long) or SELL (short) based on trend direction
     *
     * **Example (Long Trend Trade):**
     * ```
     * Direction: BUY (long)
     * Entry Price: $95,000
     * Stop Loss: $90,000 (95k - 10×$500 ATR)
     * Take Profit: $105,000 (95k + 10×$500 ATR)
     * Position Size: 5% of portfolio
     * ADX: 28.5 (strong trend)
     * ATR: $500
     * → Expected outcome: 2:1 reward/risk ratio
     * ```
     *
     * **Validation:**
     * - For LONG (BUY): stopLoss < entryPrice < takeProfit
     * - For SHORT (SELL): takeProfit < entryPrice < stopLoss
     * - Position size must be between 0 and 1 (0% to 100%)
     * - All prices and ATR must be positive
     *
     * **Usage:**
     * ```kotlin
     * when (decision) {
     *     is Decision.Trend -> {
     *         val orderSize = portfolioBalance * decision.positionSizePercent
     *         orchestrator.placeTrendOrder(
     *             side = decision.direction,
     *             entryPrice = decision.entryPrice,
     *             stopLoss = decision.stopLoss,
     *             takeProfit = decision.takeProfit,
     *             size = orderSize
     *         )
     *     }
     *     // ...
     * }
     * ```
     *
     * @property productType Type of product to trade (SPOT or PERPETUAL).
     *           SPOT supports LONG only. PERPETUAL supports both LONG and SHORT.
     *           TradeFlow uses PERPETUAL exclusively for bidirectional trading.
     *
     * @property direction Order side (BUY for long, SELL for short).
     *           Determined by trend direction indicator (not explicitly stored in this decision).
     *
     * @property entryPrice Price at which to enter the trade.
     *           Typically current market price or a limit order slightly away from current price.
     *
     * @property stopLoss Stop-loss price to limit downside risk.
     *           For LONG: must be < entryPrice. For SHORT: must be > entryPrice.
     *           Distance from entry = stopLossAtrMultiplier × ATR.
     *
     * @property takeProfit Take-profit price to lock in gains.
     *           For LONG: must be > entryPrice. For SHORT: must be < entryPrice.
     *           Distance from entry = takeProfitAtrMultiplier × ATR.
     *
     * @property positionSizePercent Percentage of portfolio to allocate to this trade (0.0 to 1.0).
     *           Example: 0.05 = 5% of portfolio. With $1000 balance, position = $50.
     *
     * @property adx Current ADX value that triggered this trend decision.
     *           Stored for logging/analysis. Should be > adxTrendThreshold.
     *
     * @property atr Current ATR value used to calculate stop/target distances.
     *           Unit: Same as price (e.g., dollars for BTC-USD).
     *
     * @property useTrailingStop Whether this position should use trailing stop logic.
     *           If true, the initial stopLoss will transition to a trailing stop
     *           after the position reaches trailingStopActivationPrice.
     *           Default from StrategyParameters.useTrailingStop.
     *
     * @property trailingStopActivationPrice Price at which trailing stop activates.
     *           For LONG: entryPrice + (activationAtrMultiplier × ATR)
     *           For SHORT: entryPrice - (activationAtrMultiplier × ATR)
     *           Only relevant if useTrailingStop = true.
     *
     * @property trailingStopDistance Initial trail distance when trailing activates.
     *           Calculated as: trailingStopAtrMultiplier × ATR
     *           Used by execution system to track trailing stop position.
     *           Only relevant if useTrailingStop = true.
     *
     * @throws IllegalArgumentException if validation rules are violated (see init block)
     */
    data class Trend(
        val productType: ProductType,
        val direction: OrderSide,
        val entryPrice: BigDecimal,
        val stopLoss: BigDecimal,
        val takeProfit: BigDecimal,
        val positionSizePercent: BigDecimal,
        val adx: Double,
        val atr: BigDecimal,
        val useTrailingStop: Boolean,
        val trailingStopActivationPrice: BigDecimal,
        val trailingStopDistance: BigDecimal
    ) : Decision() {
        init {
            require(entryPrice > BigDecimal.ZERO) { "Entry price must be positive: $entryPrice" }
            require(atr > BigDecimal.ZERO) { "ATR must be positive: $atr" }
            require(positionSizePercent > BigDecimal.ZERO && positionSizePercent <= BigDecimal("0.20")) {
                "Position size must be between 0 and 0.20 (20%) for safety: $positionSizePercent"
            }

            // Validate product type vs direction compatibility
            if (productType == ProductType.SPOT && direction == OrderSide.SELL) {
                throw IllegalArgumentException("SPOT trading only supports LONG (BUY) positions. Use PERPETUAL for SHORT (SELL).")
            }

            when (direction) {
                OrderSide.BUY -> {
                    require(stopLoss < entryPrice) {
                        "For LONG: stopLoss ($stopLoss) must be < entryPrice ($entryPrice)"
                    }
                    require(takeProfit > entryPrice) {
                        "For LONG: takeProfit ($takeProfit) must be > entryPrice ($entryPrice)"
                    }
                }
                OrderSide.SELL -> {
                    require(stopLoss > entryPrice) {
                        "For SHORT: stopLoss ($stopLoss) must be > entryPrice ($entryPrice)"
                    }
                    require(takeProfit < entryPrice) {
                        "For SHORT: takeProfit ($takeProfit) must be < entryPrice ($entryPrice)"
                    }
                }
            }
        }
    }

    /**
     * Decision to place grid orders in a ranging (choppy, sideways) market.
     *
     * Range decisions occur when ADX is LOW (typically < 20), indicating weak or absent
     * trend and sideways price action. In range mode, the strategy places multiple small
     * orders at different price levels (grid) to capture mean reversion.
     *
     * **When Range Activates:**
     * - ADX < adxTrendThreshold (default 20)
     * - Market is choppy, oscillating without clear direction
     * - Goal: Profit from price oscillations with multiple small positions
     *
     * **Grid Strategy:**
     * - Place `levels` buy orders below current price (e.g., 3 levels)
     * - Each level separated by `gridSpacing` (e.g., 1.5% apart)
     * - Each level sized at `positionSizePercentPerLevel` (e.g., 8% each)
     * - Total exposure: `levels × positionSizePercentPerLevel` (e.g., 3 × 8% = 24%)
     *
     * **Example (3-Level Grid):**
     * ```
     * Current Price: $95,000
     * Grid Spacing: $1,500 (1.5%)
     * Levels: 3
     * Position per Level: 8% of portfolio
     *
     * Grid Orders:
     * - Level 1: Buy at $93,500 (95k - 1.5k), size = 8%
     * - Level 2: Buy at $92,000 (95k - 3k), size = 8%
     * - Level 3: Buy at $90,500 (95k - 4.5k), size = 8%
     * Total Exposure: 24% of portfolio
     *
     * → As price oscillates, different levels fill
     * → Sell when price rises back up (mean reversion)
     * ```
     *
     * **Grid Spacing Calculation:**
     * Grid spacing is typically calculated as:
     * ```kotlin
     * gridSpacing = max(
     *     atr * minGridSpacingAtrMultiplier,  // Volatility-based
     *     currentPrice * minGridSpacingFloor  // Percentage-based floor
     * )
     * ```
     *
     * **Validation:**
     * - Grid spacing must be positive
     * - Number of levels must be > 0
     * - Position size per level must be between 0 and 1 (0% to 100%)
     * - ATR must be positive
     *
     * **Usage:**
     * ```kotlin
     * when (decision) {
     *     is Decision.Range -> {
     *         val orderSize = portfolioBalance * decision.positionSizePercentPerLevel
     *         for (i in 1..decision.levels) {
     *             val levelPrice = currentPrice - (decision.gridSpacing * i)
     *             orchestrator.placeGridOrder(
     *                 price = levelPrice,
     *                 size = orderSize
     *             )
     *         }
     *     }
     *     // ...
     * }
     * ```
     *
     * @property gridSpacing Price distance between consecutive grid levels.
     *           Example: $1,500 means levels are spaced $1,500 apart.
     *           Unit: Same as price (e.g., dollars for BTC-USD).
     *           Calculated to prevent levels from being too close together.
     *
     * @property levels Number of grid levels to place.
     *           Example: 3 = place 3 buy orders at different price points below current price.
     *           Typical values: 3-5 levels.
     *
     * @property positionSizePercentPerLevel Percentage of portfolio allocated PER LEVEL (0.0 to 1.0).
     *           Example: 0.08 = 8% per level. With 3 levels, total = 24%.
     *           **Note:** This is PER LEVEL, not total grid exposure.
     *
     * @property adx Current ADX value that triggered this range decision.
     *           Stored for logging/analysis. Should be < adxTrendThreshold.
     *
     * @property atr Current ATR value used to calculate grid spacing.
     *           Unit: Same as price (e.g., dollars for BTC-USD).
     *
     * @throws IllegalArgumentException if validation rules are violated (see init block)
     */
    data class Range(
        val gridSpacing: BigDecimal,
        val levels: Int,
        val positionSizePercentPerLevel: BigDecimal,
        val adx: Double,
        val atr: BigDecimal
    ) : Decision() {
        init {
            require(gridSpacing > BigDecimal.ZERO) { "Grid spacing must be positive: $gridSpacing" }
            require(levels > 0) { "Levels must be positive: $levels" }
            require(positionSizePercentPerLevel > BigDecimal.ZERO && positionSizePercentPerLevel <= BigDecimal("0.20")) {
                "Position size per level must be between 0 and 0.20 (20%) for safety: $positionSizePercentPerLevel"
            }
            require(atr > BigDecimal.ZERO) { "ATR must be positive: $atr" }
        }
    }
}
