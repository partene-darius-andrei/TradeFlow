package com.tradeflow.core.domain.config

import java.math.BigDecimal

/**
 * Trading strategy configuration parameters that control market mode detection and position sizing.
 *
 * These parameters define the core trading logic:
 * - **WHEN** to switch between trend-following and range-bound modes
 * - **HOW MUCH** to allocate per trade in each mode
 * - **WHERE** to place stop-loss and take-profit levels
 * - **HOW** to space grid orders in range mode
 *
 * **Two-Mode Strategy:**
 * 1. **TREND Mode:** Strong directional market (high ADX) → larger positions, wider stops
 * 2. **RANGE Mode:** Choppy sideways market (low ADX) → grid trading, tighter stops
 *
 * **Usage in MakeTradingDecisionUseCase:**
 * ```kotlin
 * val params = StrategyParameters(
 *     adxTrendThreshold = 20.0,  // ADX > 20 = trend mode
 *     confirmationCandles = 3     // Wait 3 candles before mode switch
 * )
 * val mode = if (currentADX > params.adxTrendThreshold) TREND else RANGE
 * ```
 *
 * **Hysteresis and Whipsaw Prevention:**
 * The `confirmationCandles` parameter prevents rapid mode switching (whipsaw).
 * If ADX crosses the threshold, we wait N candles for confirmation before switching modes.
 *
 * @property confirmationCandles Number of consecutive candles that must confirm a mode change before switching.
 *           Example: 3 = wait for 3 candles with ADX > threshold before switching to TREND mode.
 *           **Rationale:** Prevents whipsaw (rapid back-and-forth mode switching) at threshold boundaries.
 *           **Critical for stability:** Without this, strategy oscillates wildly near ADX threshold.
 *           Default: 3 candles (balances responsiveness vs stability).
 *
 * @property initialMode Starting mode when the strategy first boots up (before sufficient candle history).
 *           Default: RANGE mode (conservative default, assumes choppy market until proven otherwise).
 *
 * @property adxTrendThreshold ADX value above which market is considered TRENDING.
 *           Example: 20.0 = if ADX > 20, use trend-following mode.
 *           **ADX Interpretation:**
 *           - ADX < 20: Weak/absent trend (use RANGE mode)
 *           - ADX 20-25: Emerging trend
 *           - ADX 25-50: Strong trend
 *           - ADX > 50: Very strong trend (rare)
 *           Default: 20.0 (standard threshold from technical analysis literature).
 *
 * @property adxRangeThreshold Minimum ADX value required to stay in RANGE mode.
 *           This creates a **hysteresis band** to prevent whipsaw at the trend threshold.
 *           Example: If adxTrendThreshold=20 and adxRangeThreshold=1, then:
 *           - Switch to TREND if ADX rises above 20
 *           - Stay in TREND until ADX drops below 1 (back to RANGE)
 *           **Rationale:** Creates "sticky" modes that don't flip-flop on small ADX movements.
 *           Default: 1.0 (very low floor, mostly relies on confirmationCandles for hysteresis).
 *
 * @property stopLossAtrMultiplier Stop-loss distance from entry, expressed as a multiple of ATR (Average True Range).
 *           Example: 10.0 = stop-loss is 10× ATR below entry price.
 *           If ATR = $500 (BTC), stop-loss is $5,000 below entry.
 *           **Rationale:** ATR-based stops adapt to market volatility automatically.
 *           - High volatility (large ATR) → wider stops (avoid getting stopped out by noise)
 *           - Low volatility (small ATR) → tighter stops (protect capital in quiet markets)
 *           Default: 10× ATR (balanced stop distance).
 *
 * @property takeProfitAtrMultiplier Take-profit distance from entry, expressed as a multiple of ATR.
 *           Example: 20.0 = take-profit is 20× ATR above entry price.
 *           **Risk/Reward Ratio:** takeProfitAtrMultiplier / stopLossAtrMultiplier
 *           - Example: 20 / 10 = 2:1 reward-to-risk ratio
 *           **Rationale:** Larger targets relative to stops increases profitability when right.
 *           Default: 20× ATR (2:1 reward/risk with default stop).
 *
 * @property trendPositionPercent Percentage of portfolio to allocate per trade in TREND mode.
 *           Example: 0.05 = 5% of portfolio per trend trade.
 *           **Rationale:** Trend trades have higher conviction (strong directional move) → larger size.
 *           Default: 5% (standard position size in trend markets).
 *
 * @property gridPositionPercentPerLevel Percentage of portfolio to allocate PER GRID LEVEL in RANGE mode.
 *           Example: 0.08 = 8% per grid level. With 3 grid levels, total exposure = 24%.
 *           **Rationale:** Range mode uses multiple small positions (grid) instead of one large position.
 *           **Total range exposure:** gridPositionPercentPerLevel × gridLevels
 *           Default: 8% per level (allows 3 levels within 24% total exposure).
 *
 * @property gridLevels Number of price levels in the grid for RANGE mode trading.
 *           Example: 3 = place buy orders at 3 different price levels below current price.
 *           **Rationale:** Multiple levels catch different price points in a ranging market.
 *           - More levels = better price averaging but higher total exposure
 *           - Fewer levels = lower exposure but might miss optimal entries
 *           Default: 3 levels (balances coverage vs complexity).
 *
 * @property minGridSpacingAtrMultiplier Minimum spacing between grid levels, expressed as a multiple of ATR.
 *           Example: 0.10 = grid levels must be at least 0.1× ATR apart.
 *           If ATR = $500, grid levels must be >= $50 apart.
 *           **Rationale:** Prevents grid levels from clustering too close together.
 *           - Too close → all levels fill simultaneously (defeats grid purpose)
 *           - Too far → miss intermediate price movements
 *           Default: 0.1× ATR (tight spacing for range markets).
 *
 * @property minGridSpacingFloor Absolute minimum spacing between grid levels as a percentage of price.
 *           Example: 0.01 = 1% minimum spacing regardless of ATR.
 *           **Rationale:** Fallback when ATR-based spacing is too small (low volatility markets).
 *           Ensures grid doesn't degenerate into a single level.
 *           Default: 1% (reasonable minimum for crypto markets).
 *
 * @property leverage Position leverage multiplier for perpetual futures contracts.
 *           Example: 2.0 = 2x leverage (position size × 2).
 *           **Usage:** For a $1000 portfolio with 5% position size and 2x leverage:
 *           - Base position: $50 (5% of $1000)
 *           - Leveraged position: $100 (2x leverage)
 *           **Rationale:** Amplifies returns (and losses) in perpetual futures trading.
 *           **Risk:** Higher leverage = higher profit potential but also higher liquidation risk.
 *           Default: 1.0 (no leverage, spot-equivalent risk).
 *
 * @property useTrailingStop Whether to use ATR-based trailing stops instead of fixed stops.
 *           **Research:** Trailing stops provide +15% performance and -32% drawdown vs fixed stops.
 *           Default: true (enable trailing stops).
 *
 * @property trailingStopActivationAtrMultiplier ATR multiplier for trailing stop activation threshold.
 *           Example: 1.5 = trailing activates after 1.5× ATR profit from entry.
 *           **Rationale:** Ensures position is profitable before locking in gains with trailing.
 *           **Too low:** Premature activation, stop out during normal volatility.
 *           **Too high:** Late activation, miss profit protection.
 *           Default: 1.5× ATR (balanced activation point).
 *
 * @property trailingStopAtrMultiplier ATR multiplier for normal trailing stop distance.
 *           Example: 2.5 = stop trails 2.5× ATR below high water mark.
 *           **Rationale:** Wide enough to avoid whipsaws, tight enough to protect profits.
 *           **Research:** 2-3× ATR optimal for crypto (vs 1.5-2× for traditional markets).
 *           Default: 2.5× ATR (balanced trail distance).
 *
 * @property trailingStopTightenThreshold ATR multiplier for pullback threshold to trigger tightening.
 *           Example: 1.5 = if price pulls back > 1.5× ATR from high, tighten trail.
 *           **Rationale:** Detects potential trend exhaustion or reversal.
 *           **Caution state:** Indicates weakening trend momentum.
 *           Default: 1.5× ATR (early warning of reversal).
 *
 * @property trailingStopTightenAtrMultiplier ATR multiplier for tightened trailing stop distance.
 *           Example: 2.0 = stop tightens to 2.0× ATR in caution state.
 *           **Rationale:** Tighter protection when trend shows weakness.
 *           **Must be < trailingStopAtrMultiplier** (tighter than normal trail).
 *           Default: 2.0× ATR (tightened protection).
 *
 * @see MakeTradingDecisionUseCase for how these parameters drive mode detection and decision-making
 * @see TrailingStopManager for trailing stop implementation
 * @see RiskProfile for pre-configured strategy parameter sets optimized for different risk levels
 */
data class StrategyParameters(
    val confirmationCandles: Int = 3,
    val initialMode: DecisionMode = DecisionMode.RANGE,
    val adxTrendThreshold: Double = 20.0,
    val adxRangeThreshold: Double = 1.0,
    val stopLossAtrMultiplier: BigDecimal = BigDecimal("10.0"),
    val takeProfitAtrMultiplier: BigDecimal = BigDecimal("20.0"),
    val trendPositionPercent: BigDecimal = BigDecimal("0.05"),
    val gridPositionPercentPerLevel: BigDecimal = BigDecimal("0.08"),
    val gridLevels: Int = 3,
    val minGridSpacingAtrMultiplier: BigDecimal = BigDecimal("0.10"),
    val minGridSpacingFloor: BigDecimal = BigDecimal("0.01"),
    val leverage: BigDecimal = BigDecimal("1.0"),
    val useTrailingStop: Boolean = true,
    val trailingStopActivationAtrMultiplier: BigDecimal = BigDecimal("1.5"),
    val trailingStopAtrMultiplier: BigDecimal = BigDecimal("2.5"),
    val trailingStopTightenThreshold: BigDecimal = BigDecimal("1.5"),
    val trailingStopTightenAtrMultiplier: BigDecimal = BigDecimal("2.0")
)

/**
 * Market mode classification for trading strategy.
 *
 * The strategy operates in one of two modes based on ADX (Average Directional Index):
 *
 * **TREND Mode:**
 * - Activated when ADX > adxTrendThreshold (typically 20+)
 * - Market has clear directional momentum
 * - Strategy: Larger positions, wider stops, ride the trend
 * - Example: Bitcoin rallying from $60k to $100k
 *
 * **RANGE Mode:**
 * - Activated when ADX < adxTrendThreshold
 * - Market is choppy/sideways without clear direction
 * - Strategy: Grid trading, multiple small positions, tighter stops
 * - Example: Bitcoin oscillating between $95k-$100k for weeks
 *
 * **Mode Switching:**
 * Requires `confirmationCandles` consecutive candles to confirm before switching.
 * This prevents whipsaw (rapid mode flipping) at the ADX threshold boundary.
 *
 * @see MakeTradingDecisionUseCase for the state machine that manages mode transitions
 */
enum class DecisionMode {
    TREND,
    RANGE
}
