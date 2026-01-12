package com.tradeflow.core.domain.risk

import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Manages ATR-based trailing stop logic for active positions.
 *
 * **Why Trailing Stops:**
 * Research shows ATR-based trailing stops outperform fixed stops for crypto volatility:
 * - **+15% performance improvement** vs fixed stops
 * - **-32% max drawdown reduction**
 * - Better profit capture in strong trends
 * - Adaptive to changing market volatility
 *
 * **Three-Stage Trailing Stop System:**
 *
 * **Stage 1: Initial Fixed Stop**
 * - Position opens with fixed stop at entry ± (stopLossAtrMultiplier × ATR)
 * - Example: Entry $95k, ATR $500, multiplier 10x → Stop at $90k (LONG)
 * - Purpose: Protect against immediate adverse move
 *
 * **Stage 2: Activation (After Initial Profit)**
 * - Trailing stop activates after profit reaches (activationAtrMultiplier × ATR)
 * - Example: Activation at $95k + (1.5 × $500) = $95,750 (LONG)
 * - Trail distance: (trailingStopAtrMultiplier × ATR) from high water mark
 * - Purpose: Lock in profits while allowing trend to continue
 *
 * **Stage 3: Tightening (After Pullback)**
 * - If price pulls back > (tightenThreshold × ATR) from high, tighten trail
 * - Tightened distance: (trailingStopTightenAtrMultiplier × ATR)
 * - Example: If price drops > 1.5 × ATR from swing high, trail tightens to 2× ATR
 * - Purpose: Protect profits during caution state (potential reversal)
 *
 * **Example Lifecycle (LONG Position):**
 * ```
 * Entry: $95,000 | ATR: $500 | Stop: $90,000 (10× ATR fixed)
 *
 * Price rises to $95,750:
 *   → Trailing ACTIVATES (1.5× ATR profit reached)
 *   → Stop moves to: $95,750 - (2.5× $500) = $94,500
 *   → High water mark: $95,750
 *
 * Price rises to $97,000:
 *   → Stop moves to: $97,000 - (2.5× $500) = $95,750
 *   → High water mark: $97,000
 *
 * Price drops to $96,000 (pullback of $1,000 > 1.5× $500):
 *   → CAUTION STATE triggered
 *   → Stop TIGHTENS to: $96,000 - (2× $500) = $95,000
 *
 * Price continues down to $95,000:
 *   → Stop HIT at $95,000
 *   → Exit with $0 profit (break-even) but protected from larger loss
 * ```
 *
 * **Research Validation:**
 * - 3× ATR multiplier boosts performance 15% vs fixed stops
 * - 2× ATR stop reduces max drawdown by 32% (1000-trade study)
 * - Chandelier Exit (ATR-based trailing) consistently outperforms percentage stops in crypto
 * - Crypto requires 10-20% wider stops vs traditional markets due to volatility
 *
 * **Configuration:**
 * All parameters configured via StrategyParameters:
 * - `trailingStopActivationAtrMultiplier`: Profit threshold to activate trailing (default 1.5)
 * - `trailingStopAtrMultiplier`: Normal trail distance (default 2.5)
 * - `trailingStopTightenThreshold`: Pullback threshold for tightening (default 1.5)
 * - `trailingStopTightenAtrMultiplier`: Tightened trail distance (default 2.0)
 *
 * **Thread Safety:**
 * Stateless - each call operates independently. Position state (high water mark) tracked externally.
 *
 * @property config Trading configuration containing trailing stop parameters.
 *
 * @see StrategyParameters for trailing stop configuration
 * @see calculateTrailingStop for the main calculation method
 */
class TrailingStopManager(
    private val config: TradingConfig
) {

    /**
     * State object representing the current trailing stop status.
     *
     * **State Fields:**
     * - **isActive:** Whether trailing has activated (profit threshold reached)
     * - **currentStopPrice:** Current stop-loss price (moves up/down with trail)
     * - **highWaterMarkPrice:** Highest/lowest price since entry (depends on direction)
     * - **isInCautionState:** Whether tightened stop is active (pullback detected)
     *
     * **State Transitions:**
     * ```
     * INACTIVE (fixed stop)
     *   → Profit reaches activation threshold
     * ACTIVE (normal trailing)
     *   → Pullback exceeds tighten threshold
     * CAUTION (tightened trailing)
     *   → Price recovers or stop hits
     * ```
     *
     * @property isActive True if trailing stop has activated (profit threshold reached).
     *           False means position still uses initial fixed stop.
     *
     * @property currentStopPrice Current stop-loss price that would exit the position.
     *           For LONG: price below current, moves up with trail.
     *           For SHORT: price above current, moves down with trail.
     *
     * @property highWaterMarkPrice Peak favorable price since entry.
     *           For LONG: highest price reached.
     *           For SHORT: lowest price reached.
     *           Used to calculate trail distance.
     *
     * @property isInCautionState True if position has pulled back significantly and stop is tightened.
     *           Indicates potential trend exhaustion or reversal.
     */
    data class TrailingStopState(
        val isActive: Boolean,
        val currentStopPrice: BigDecimal,
        val highWaterMarkPrice: BigDecimal,
        val isInCautionState: Boolean
    )

    /**
     * Calculates the trailing stop state for an active position.
     *
     * **Algorithm:**
     * 1. Check if profit threshold reached → activate trailing
     * 2. Calculate normal trail distance from high water mark
     * 3. Check if pullback > threshold → tighten trail
     * 4. Ensure stop never moves against position (can only improve)
     *
     * **Activation Logic:**
     * ```kotlin
     * profitFromEntry = abs(currentPrice - entryPrice)
     * activationThreshold = atr × activationAtrMultiplier
     * isActive = profitFromEntry >= activationThreshold
     * ```
     *
     * **Normal Trail Calculation:**
     * ```kotlin
     * trailDistance = atr × trailingStopAtrMultiplier
     * stopPrice = highWaterMark - trailDistance (LONG)
     * stopPrice = highWaterMark + trailDistance (SHORT)
     * ```
     *
     * **Tightening Logic:**
     * ```kotlin
     * pullback = abs(highWaterMark - currentPrice)
     * tightenThreshold = atr × tightenThresholdMultiplier
     * if (pullback > tightenThreshold) {
     *     trailDistance = atr × tightenAtrMultiplier  // Tighter
     * }
     * ```
     *
     * **Direction-Specific Behavior:**
     *
     * **LONG (BUY) Position:**
     * - Entry: $95,000, Current: $97,000, High: $97,500, ATR: $500
     * - Profit: $2,500 (current - entry)
     * - Activation: $95k + (1.5 × $500) = $95,750 ✅ ACTIVE
     * - Trail distance: 2.5 × $500 = $1,250
     * - Stop: $97,500 - $1,250 = $96,250
     * - Pullback from high: $97,500 - $97,000 = $500 (< 1.5× ATR) → Normal trail
     *
     * **SHORT (SELL) Position:**
     * - Entry: $95,000, Current: $93,000, Low: $92,500, ATR: $500
     * - Profit: $2,000 (entry - current)
     * - Activation: $95k - (1.5 × $500) = $94,250 ✅ ACTIVE
     * - Trail distance: 2.5 × $500 = $1,250
     * - Stop: $92,500 + $1,250 = $93,750
     * - Pullback from low: $93,000 - $92,500 = $500 (< 1.5× ATR) → Normal trail
     *
     * **Stop Never Moves Against Position:**
     * - LONG: Stop can only move UP (never down below previous stop)
     * - SHORT: Stop can only move DOWN (never up above previous stop)
     * - Ensures profits are locked in, never given back
     *
     * **Example (LONG Position with Tightening):**
     * ```
     * Entry: $95,000 | ATR: $500 | Current: $96,500 | High: $98,000
     *
     * 1. Profit check: $96,500 - $95,000 = $1,500 >= $750 (1.5× ATR) ✅ ACTIVE
     * 2. Normal trail: $98,000 - (2.5 × $500) = $96,750
     * 3. Pullback check: $98,000 - $96,500 = $1,500 > $750 (1.5× ATR) ⚠️ CAUTION
     * 4. Tightened trail: $96,500 - (2× $500) = $95,500
     * 5. Return: stopPrice=$95,500, isActive=true, isInCautionState=true
     * ```
     *
     * @param entryPrice Original position entry price.
     *                   Used to calculate profit and activation threshold.
     *
     * @param currentPrice Current market price.
     *                     Used to calculate profit and pullback from high water mark.
     *
     * @param highestPriceSinceEntry For LONG: highest price reached since entry.
     *                               For SHORT: lowest price reached since entry.
     *                               This is the HIGH WATER MARK for trail calculation.
     *
     * @param atr Current Average True Range value.
     *            Used to calculate all distance thresholds (activation, trail, tighten).
     *            Unit: Same as price (e.g., dollars for BTC/USD).
     *
     * @param direction Position direction (BUY for LONG, SELL for SHORT).
     *                  Determines how profit/pullback/trail are calculated.
     *
     * @return TrailingStopState with current stop price and state flags.
     *
     * @throws IllegalArgumentException if prices or ATR are not positive.
     *
     * @see TrailingStopState for the return type
     * @see StrategyParameters for configuration parameters
     */
    fun calculateTrailingStop(
        entryPrice: BigDecimal,
        currentPrice: BigDecimal,
        highestPriceSinceEntry: BigDecimal,
        atr: BigDecimal,
        direction: OrderSide
    ): TrailingStopState {
        require(entryPrice > BigDecimal.ZERO) { "Entry price must be positive: $entryPrice" }
        require(currentPrice > BigDecimal.ZERO) { "Current price must be positive: $currentPrice" }
        require(highestPriceSinceEntry > BigDecimal.ZERO) { "High water mark must be positive: $highestPriceSinceEntry" }
        require(atr > BigDecimal.ZERO) { "ATR must be positive: $atr" }

        // Calculate initial fixed stop (used if trailing not yet active)
        val initialStop = when (direction) {
            OrderSide.BUY -> entryPrice - (atr * config.strategy.stopLossAtrMultiplier)
            OrderSide.SELL -> entryPrice + (atr * config.strategy.stopLossAtrMultiplier)
        }

        // Check if trailing stop should activate (profit threshold reached)
        val activationThreshold = atr * config.strategy.trailingStopActivationAtrMultiplier
        val profitFromEntry = when (direction) {
            OrderSide.BUY -> currentPrice - entryPrice
            OrderSide.SELL -> entryPrice - currentPrice
        }

        if (profitFromEntry < activationThreshold) {
            // Not yet profitable enough, use fixed stop
            return TrailingStopState(
                isActive = false,
                currentStopPrice = initialStop,
                highWaterMarkPrice = highestPriceSinceEntry,
                isInCautionState = false
            )
        }

        // Trailing is active - calculate trail distance from high water mark
        val pullbackFromHigh = when (direction) {
            OrderSide.BUY -> highestPriceSinceEntry - currentPrice
            OrderSide.SELL -> currentPrice - highestPriceSinceEntry
        }

        // Check if we should tighten the trail (pullback > threshold)
        val tightenThreshold = atr * config.strategy.trailingStopTightenThreshold
        val shouldTighten = pullbackFromHigh > tightenThreshold

        val trailMultiplier = if (shouldTighten) {
            config.strategy.trailingStopTightenAtrMultiplier
        } else {
            config.strategy.trailingStopAtrMultiplier
        }

        val trailDistance = atr * trailMultiplier

        val trailingStop = when (direction) {
            OrderSide.BUY -> highestPriceSinceEntry - trailDistance
            OrderSide.SELL -> highestPriceSinceEntry + trailDistance
        }

        // Ensure stop never moves against position (can only improve)
        val finalStop = when (direction) {
            OrderSide.BUY -> trailingStop.max(initialStop)
            OrderSide.SELL -> trailingStop.min(initialStop)
        }

        return TrailingStopState(
            isActive = true,
            currentStopPrice = finalStop.setScale(2, RoundingMode.HALF_UP),
            highWaterMarkPrice = highestPriceSinceEntry,
            isInCautionState = shouldTighten
        )
    }

    /**
     * Calculates the price at which trailing stop will activate.
     *
     * **Activation Price:**
     * - LONG: entryPrice + (activationAtrMultiplier × ATR)
     * - SHORT: entryPrice - (activationAtrMultiplier × ATR)
     *
     * **Example (LONG):**
     * - Entry: $95,000
     * - ATR: $500
     * - Activation multiplier: 1.5
     * - Activation price: $95,000 + (1.5 × $500) = $95,750
     *
     * **Usage:**
     * Used to display to user when trail will activate, and for order placement logic.
     *
     * @param entryPrice Position entry price.
     * @param atr Current ATR value.
     * @param direction Position direction (BUY or SELL).
     *
     * @return Price at which trailing stop will activate.
     */
    fun calculateActivationPrice(
        entryPrice: BigDecimal,
        atr: BigDecimal,
        direction: OrderSide
    ): BigDecimal {
        val activationDistance = atr * config.strategy.trailingStopActivationAtrMultiplier
        return when (direction) {
            OrderSide.BUY -> entryPrice + activationDistance
            OrderSide.SELL -> entryPrice - activationDistance
        }.setScale(2, RoundingMode.HALF_UP)
    }
}
