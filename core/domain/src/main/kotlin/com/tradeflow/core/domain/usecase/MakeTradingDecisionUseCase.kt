package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.config.DecisionMode
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal

/**
 * STATEFUL trading decision engine with 3-candle hysteresis to prevent whipsaw mode switching.
 *
 * This is the CORE of the trading strategy. It analyzes market conditions (ADX, SMA, ATR) and
 * produces trading decisions (Trend, Range, Defense, Wait) while maintaining state to prevent
 * rapid oscillation between modes.
 *
 * **Why Stateful:**
 * Without state, ADX fluctuating around the threshold (e.g., 19.8, 20.1, 19.9, 20.2) would cause
 * constant mode switching (whipsaw). This destroys profitability through:
 * - Excessive order placement/cancellation
 * - Trading fees on rapid entry/exit
 * - Poor fill prices from urgency
 * - Emotional stress and system instability
 *
 * **Hysteresis Solution:**
 * Require N consecutive candles (default 3) to CONFIRM a mode change before switching.
 * This creates "sticky" modes that don't flip-flop on small ADX movements.
 *
 * **State Machine Diagram:**
 * ```
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    CURRENT MODE: RANGE                       │
 * └─────────────────────────────────────────────────────────────┘
 *                              │
 *                     ADX crosses threshold
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │              CANDIDATE MODE: TREND (count=1)                 │
 * │  Wait for confirmationCandles (e.g., 3) consecutive candles │
 * └─────────────────────────────────────────────────────────────┘
 *                              │
 *                  Next candle: ADX still > threshold
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │              CANDIDATE MODE: TREND (count=2)                 │
 * └─────────────────────────────────────────────────────────────┘
 *                              │
 *                  Next candle: ADX still > threshold
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │              CANDIDATE MODE: TREND (count=3)                 │
 * │              ✅ CONFIRMATION COMPLETE                        │
 * └─────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    CURRENT MODE: TREND                       │
 * │               lastMode = TREND, count = 0                   │
 * └─────────────────────────────────────────────────────────────┘
 * ```
 *
 * **Two-Mode Selection (Defense handled by orchestrator):**
 *
 * 1. **Trend Mode (ADX-based):**
 *    - Trigger: ADX >= adxTrendThreshold (default 20)
 *    - Action: Directional position (LONG if price > SMA200, SHORT if price < SMA200)
 *    - Rationale: Strong momentum, ride the trend in either direction
 *
 * 2. **Range Mode (ADX-based):**
 *    - Trigger: ADX <= adxRangeThreshold (default 1)
 *    - Action: Grid trading with multiple small positions
 *    - Rationale: Choppy market, profit from oscillations
 *
 * **Note:** Defense mode (circuit breaker at 15% drawdown) is handled by
 * ExecuteTradingCycleUseCase, not by this decision engine.
 *
 * **ADX Neutral Zone:**
 * If ADX is between adxRangeThreshold and adxTrendThreshold (e.g., ADX = 10 with thresholds
 * at 1 and 20), the engine stays in the CURRENT mode. This prevents whipsaw in the
 * middle zone where regime is unclear.
 *
 * **State Variables:**
 * - `lastMode`: The currently active mode (TREND or RANGE)
 * - `candidateMode`: A new mode being considered (null if no change pending)
 * - `confirmationCount`: How many consecutive candles have confirmed the candidate mode
 *
 * **Example Execution Flow:**
 * ```
 * Candle 1: ADX=19 → RANGE mode (current)
 * Candle 2: ADX=21 → candidate=TREND, count=1, decision=Wait("Confirming 1/3")
 * Candle 3: ADX=22 → candidate=TREND, count=2, decision=Wait("Confirming 2/3")
 * Candle 4: ADX=23 → candidate=TREND, count=3, SWITCH! decision=Trend(...)
 * Candle 5: ADX=24 → TREND mode (current), decision=Trend(...)
 * ```
 *
 * **Backtesting Note:**
 * This state persists across `evaluate()` calls. When backtesting, call `resetState()` at
 * the start of each backtest run to ensure clean initial state.
 *
 * @property taService Technical analysis service for calculating SMA, ADX, ATR
 * @property config Complete trading configuration (strategy, risk, technical params)
 *
 * @see Decision for the output decision types
 * @see StrategyParameters for hysteresis configuration (confirmationCandles, ADX thresholds)
 */
class MakeTradingDecisionUseCase(
    private val taService: AnalyzeCandlesUseCase,
    private val config: TradingConfig
) {

    /**
     * Last confirmed mode (TREND or RANGE).
     *
     * This is the "stable" mode that the strategy is currently operating in.
     * It only changes after `confirmationCandles` consecutive candles confirm a new mode.
     *
     * Initialized to the configured initial mode (default: RANGE).
     */
    private var lastMode: Mode = Mode.valueOf(config.strategy.initialMode.name)

    /**
     * Number of consecutive candles that have confirmed the candidate mode.
     *
     * Increments each time a candle confirms the same candidate mode.
     * Resets to 0 when:
     * - Desired mode matches current mode (no change needed)
     * - Desired mode changes to a different candidate (switch targets)
     * - Confirmation completes and mode switches
     * - Defense mode activates (overrides everything)
     */
    private var confirmationCount = 0

    /**
     * The new mode being considered for switching (TREND or RANGE).
     *
     * Null if no mode change is pending. Once set, each subsequent candle that
     * confirms this mode increments confirmationCount. After N confirmations,
     * this becomes the new lastMode.
     *
     * Example state progression:
     * - lastMode=RANGE, candidateMode=null, count=0 (stable RANGE)
     * - lastMode=RANGE, candidateMode=TREND, count=1 (considering TREND)
     * - lastMode=RANGE, candidateMode=TREND, count=2 (still considering)
     * - lastMode=RANGE, candidateMode=TREND, count=3 (switch triggered!)
     * - lastMode=TREND, candidateMode=null, count=0 (stable TREND)
     */
    private var candidateMode: Mode? = null

    /**
     * Internal mode enum (TREND or RANGE).
     *
     * This shadows DecisionMode to avoid exposing internal state machine details.
     * Maps 1:1 with DecisionMode but exists separately for implementation clarity.
     */
    private enum class Mode { TREND, RANGE }

    /**
     * Resets state machine to initial conditions.
     *
     * **When to call:**
     * - Before each backtest run to ensure clean state
     * - After a long period of inactivity (live trading restart)
     * - When switching trading configs (different strategy parameters)
     *
     * **Effect:**
     * - lastMode → initial mode from config (default RANGE)
     * - candidateMode → null
     * - confirmationCount → 0
     *
     * **Example:**
     * ```kotlin
     * val engine = MakeTradingDecisionUseCase(taService, config)
     * // Run backtest 1
     * runBacktest(engine, historicalData1)
     *
     * engine.resetState()  // ← CRITICAL: reset before next run
     * // Run backtest 2 with clean state
     * runBacktest(engine, historicalData2)
     * ```
     */
    fun resetState() {
        lastMode = Mode.valueOf(config.strategy.initialMode.name)
        candidateMode = null
        confirmationCount = 0
    }

    /**
     * Evaluates market conditions and produces a trading decision.
     *
     * This is the main entry point called once per candle (e.g., every 4 hours for 4H candles).
     * It performs a three-tier evaluation:
     *
     * **Evaluation Steps:**
     *
     * 1. **Candle Count Check:**
     *    - Verify sufficient candle history (typically 200+)
     *    - Return Wait if insufficient data
     *
     * 2. **Technical Indicator Calculation:**
     *    - Calculate SMA, ADX, ATR using taService
     *    - All three indicators calculated in a single pass
     *
     * 3. **Desired Mode Determination:**
     *    - ADX >= adxTrendThreshold → wants TREND
     *    - ADX <= adxRangeThreshold → wants RANGE
     *    - ADX in neutral zone (between thresholds) → wants currentMode (stay put)
     *
     * 4. **Hysteresis Application:**
     *    - If desiredMode == lastMode: Return decision for current mode (no change)
     *    - If desiredMode != candidateMode: Start new confirmation (count = 1)
     *    - If desiredMode == candidateMode: Increment count
     *    - If count >= confirmationCandles: Switch to new mode
     *    - Otherwise: Return Wait decision while confirming
     *
     * **ADX Neutral Zone Example:**
     * ```
     * adxRangeThreshold = 1.0
     * adxTrendThreshold = 20.0
     * currentMode = RANGE
     *
     * ADX=0.5  → wants RANGE (ADX <= 1.0)
     * ADX=10   → wants RANGE (neutral zone, stay in current)
     * ADX=19   → wants RANGE (neutral zone, stay in current)
     * ADX=21   → wants TREND (ADX >= 20.0)
     * ```
     *
     * **Confirmation Example:**
     * ```
     * Initial: lastMode=RANGE, candidate=null, count=0
     *
     * Candle 1: ADX=21 (wants TREND)
     *   → candidateMode=TREND, count=1
     *   → Return Wait("Confirming mode switch to TREND (1/3)")
     *
     * Candle 2: ADX=22 (wants TREND)
     *   → candidateMode=TREND, count=2
     *   → Return Wait("Confirming mode switch to TREND (2/3)")
     *
     * Candle 3: ADX=20.5 (wants TREND)
     *   → candidateMode=TREND, count=3
     *   → count >= 3: SWITCH!
     *   → lastMode=TREND, candidateMode=null, count=0
     *   → Return Trend(...)
     *
     * Candle 4: ADX=24 (wants TREND)
     *   → desiredMode == lastMode, no change
     *   → Return Trend(...)
     * ```
     *
     * **Confirmation Interrupted Example:**
     * ```
     * Initial: lastMode=RANGE, candidate=null, count=0
     *
     * Candle 1: ADX=21 (wants TREND)
     *   → candidateMode=TREND, count=1
     *   → Return Wait("Confirming 1/3")
     *
     * Candle 2: ADX=19 (wants RANGE - neutral zone, stays in RANGE)
     *   → desiredMode=RANGE (current mode)
     *   → candidateMode=null, count=0 (RESET)
     *   → Return Range(...)
     *
     * → Confirmation interrupted, back to stable RANGE mode
     * ```
     *
     * **Logging Output:**
     * The method prints diagnostic information:
     * - Current price, SMA, ADX, ATR values
     * - ADX comparison to thresholds and desired mode
     * - Final decision details (position size, stop/target levels)
     *
     * @param candles List of historical candles (oldest first).
     *                Must contain at least `minCandlesRequired` candles (typically 200).
     *
     * @param currentPrice Current market price for entry calculations.
     *                     Used to determine Defense mode (vs SMA) and calculate
     *                     stop-loss/take-profit levels.
     *
     * @return Decision object (Wait, Defense, Trend, or Range) with all parameters
     *         needed for order execution.
     *
     * @see createDecision for how Trend and Range decisions are constructed
     */
    fun execute(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        if (candles.size < config.technical.minCandlesRequired) {
            return Decision.Wait("Not enough candles: ${candles.size}/${config.technical.minCandlesRequired}")
        }

        val indicators = taService.calculateAll(
            candles,
            config.technical.smaPeriod,
            config.technical.adxPeriod,
            config.technical.atrPeriod,
            config.technical.rsiPeriod,
            config.technical.volumeSmaPeriod,
            config.technical.cmfPeriod
        )

        println("  [DECISION] Price: $currentPrice | SMA: ${indicators.sma200.setScale(0, java.math.RoundingMode.HALF_UP)} | ADX: ${indicators.adx.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} | ATR: ${indicators.atr.setScale(0, java.math.RoundingMode.HALF_UP)}")
        println("  [DECISION] RSI: ${indicators.rsi.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} | Volume: ${indicators.volumeRatio.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}x avg | CMF: ${indicators.cmf.toBigDecimal().setScale(3, java.math.RoundingMode.HALF_UP)}")

        // 1. Determine desired mode based on Trend Strength (ADX)
        val desiredMode = when {
            indicators.adx >= config.strategy.adxTrendThreshold -> {
                println("  [DECISION] ADX ${indicators.adx} >= ${config.strategy.adxTrendThreshold} → Wants TREND")
                Mode.TREND
            }
            indicators.adx <= config.strategy.adxRangeThreshold -> {
                println("  [DECISION] ADX ${indicators.adx} <= ${config.strategy.adxRangeThreshold} → Wants RANGE")
                Mode.RANGE
            }
            else -> {
                // ADX in neutral zone (between range and trend thresholds)
                // Stay in current mode to avoid whipsaw
                println("  [DECISION] ADX ${indicators.adx} in neutral zone (${config.strategy.adxRangeThreshold}-${config.strategy.adxTrendThreshold}) → Stay in $lastMode")
                lastMode
            }
        }

        // 2. Apply Hysteresis (require N consecutive confirmations before switching)
        if (desiredMode == lastMode) {
            // Already in desired mode, reset any pending switch
            candidateMode = null
            confirmationCount = 0
            return createDecision(lastMode, currentPrice, indicators)
        }

        // Mode change is desired, apply confirmation logic
        if (desiredMode != candidateMode) {
            // New candidate mode detected, start fresh confirmation
            candidateMode = desiredMode
            confirmationCount = 1
        } else {
            // Same candidate as before, increment confirmation count
            confirmationCount++
        }

        // Check if we have enough confirmations to switch
        if (confirmationCount >= config.strategy.confirmationCandles) {
            // ✅ CONFIRMATION COMPLETE - switch to new mode
            lastMode = desiredMode
            candidateMode = null
            confirmationCount = 0
            return createDecision(lastMode, currentPrice, indicators)
        }

        // Still waiting for confirmation
        return Decision.Wait("Confirming mode switch to $desiredMode ($confirmationCount/${config.strategy.confirmationCandles})")
    }

    /**
     * Creates a Trend or Range decision based on confirmed mode.
     *
     * This method is called ONLY after mode confirmation is complete (or when already
     * in the correct mode). It constructs the appropriate Decision object with all
     * parameters calculated from indicators and config.
     *
     * **Trend Decision Calculation:**
     * - Entry: currentPrice (market order or limit near market)
     * - Direction: BUY (LONG) if price > SMA200, SELL (SHORT) if price < SMA200
     * - LONG Stop Loss: entryPrice - (ATR × stopLossAtrMultiplier)
     * - LONG Take Profit: entryPrice + (ATR × takeProfitAtrMultiplier)
     * - SHORT Stop Loss: entryPrice + (ATR × stopLossAtrMultiplier)
     * - SHORT Take Profit: entryPrice - (ATR × takeProfitAtrMultiplier)
     * - Position Size: trendPositionPercent of portfolio (e.g., 5%)
     *
     * **Range Decision Calculation:**
     * - Grid Spacing: max(ATR × minGridSpacingAtrMultiplier, minGridSpacingFloor)
     * - Levels: gridLevels (e.g., 3)
     * - Position per Level: gridPositionPercentPerLevel (e.g., 8%)
     * - Total Exposure: levels × positionPerLevel (e.g., 3 × 8% = 24%)
     *
     * **ATR-Based Stops/Targets:**
     * Using ATR makes stops and targets volatility-adaptive:
     * - High ATR (volatile market) → wider stops/targets
     * - Low ATR (quiet market) → tighter stops/targets
     * This prevents getting stopped out by normal volatility in choppy markets.
     *
     * **Example Trend:**
     * ```
     * currentPrice = $95,000
     * ATR = $500
     * stopLossAtrMultiplier = 10
     * takeProfitAtrMultiplier = 20
     *
     * stopLoss = $95,000 - (10 × $500) = $90,000
     * takeProfit = $95,000 + (20 × $500) = $105,000
     * Reward/Risk = ($105k - $95k) / ($95k - $90k) = $10k / $5k = 2:1
     * ```
     *
     * **Example Range:**
     * ```
     * ATR = $500
     * minGridSpacingAtrMultiplier = 0.10
     * minGridSpacingFloor = $1,000
     *
     * spacing = max($500 × 0.10, $1,000) = max($50, $1,000) = $1,000
     * levels = 3
     * positionPerLevel = 8%
     *
     * Grid:
     * - Level 1: Buy at $94,000, size 8%
     * - Level 2: Buy at $93,000, size 8%
     * - Level 3: Buy at $92,000, size 8%
     * ```
     *
     * @param mode The confirmed mode (TREND or RANGE)
     * @param currentPrice Current market price for entry/grid calculations
     * @param indicators Technical indicators (SMA, ADX, ATR)
     *
     * @return Decision.Trend or Decision.Range with calculated parameters
     */
    private fun createDecision(mode: Mode, currentPrice: BigDecimal, indicators: AnalyzeCandlesUseCase.Indicators): Decision {
        return when (mode) {
            Mode.TREND -> {
                // Determine direction: LONG (BUY) if price > SMA200, SHORT (SELL) if price < SMA200
                val isLong = currentPrice >= indicators.sma200
                val direction = if (isLong) OrderSide.BUY else OrderSide.SELL

                // RSI Momentum Filter: Block only extreme opposite momentum
                // FIX: Relaxed from RSI > 50 to RSI > 30 for LONG (was blocking 90% of trades)
                // LONG blocked only if RSI < 30 (extreme bearish)
                // SHORT blocked only if RSI > 70 (extreme bullish)
                val rsiBlocksTrade = if (isLong) indicators.rsi < 30.0 else indicators.rsi > 70.0
                if (rsiBlocksTrade) {
                    val reason = if (isLong) "RSI < 30 (extreme bearish)" else "RSI > 70 (extreme bullish)"
                    println("  [DECISION] ❌ RSI filter: ${indicators.rsi.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} blocks ${if (isLong) "LONG" else "SHORT"} ($reason)")
                    return Decision.Wait("RSI ${indicators.rsi.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} blocks ${if (isLong) "LONG" else "SHORT"} ($reason)")
                }

                // Volume Confirmation Filter: Volume must be significantly above average
                // Research: Volume > 1.5x improves breakout success from 39% to 65% (+26 percentage points)
                if (indicators.volumeRatio < config.technical.minVolumeRatio) {
                    println("  [DECISION] ❌ Volume filter: ${indicators.volumeRatio.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}x below required ${config.technical.minVolumeRatio}x threshold")
                    return Decision.Wait("Volume ${indicators.volumeRatio.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}x below required ${config.technical.minVolumeRatio}x threshold")
                }

                // CMF Confirmation (optional, adds additional confidence layer)
                // CMF > 0.05 for LONG = money flowing in (bullish)
                // CMF < -0.05 for SHORT = money flowing out (bearish)
                val cmfConfirmsDirection = if (isLong) indicators.cmf > 0.05 else indicators.cmf < -0.05
                if (!cmfConfirmsDirection) {
                    println("  [DECISION] ⚠️  CMF weak: ${indicators.cmf.toBigDecimal().setScale(3, java.math.RoundingMode.HALF_UP)} weakly supports ${if (isLong) "LONG" else "SHORT"} (not blocking, but lower confidence)")
                }

                // Calculate stop loss and take profit based on direction
                val sl = if (isLong) {
                    currentPrice - (indicators.atr * config.strategy.stopLossAtrMultiplier)
                } else {
                    currentPrice + (indicators.atr * config.strategy.stopLossAtrMultiplier)
                }

                val tp = if (isLong) {
                    currentPrice + (indicators.atr * config.strategy.takeProfitAtrMultiplier)
                } else {
                    currentPrice - (indicators.atr * config.strategy.takeProfitAtrMultiplier)
                }

                // Calculate trailing stop parameters
                val trailingStopActivationPrice = if (isLong) {
                    currentPrice + (indicators.atr * config.strategy.trailingStopActivationAtrMultiplier)
                } else {
                    currentPrice - (indicators.atr * config.strategy.trailingStopActivationAtrMultiplier)
                }

                val trailingStopDistance = indicators.atr * config.strategy.trailingStopAtrMultiplier

                val directionName = if (isLong) "LONG" else "SHORT"
                println("  [DECISION] ✅ All filters passed: RSI=${indicators.rsi.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} Vol=${indicators.volumeRatio.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}x CMF=${indicators.cmf.toBigDecimal().setScale(3, java.math.RoundingMode.HALF_UP)}")

                if (config.strategy.useTrailingStop) {
                    println("  [DECISION] → Trailing Stop: Activates at $trailingStopActivationPrice | Trail distance: $trailingStopDistance")
                }

                println("  [DECISION] → Final: TREND $directionName ${config.strategy.trendPositionPercent.multiply(BigDecimal("100"))}% | Entry: $currentPrice | SL: $sl | TP: $tp")

                Decision.Trend(
                    productType = com.tradeflow.core.domain.model.ProductType.PERPETUAL,
                    direction = direction,
                    entryPrice = currentPrice,
                    stopLoss = sl,
                    takeProfit = tp,
                    positionSizePercent = config.strategy.trendPositionPercent,
                    adx = indicators.adx,
                    atr = indicators.atr,
                    useTrailingStop = config.strategy.useTrailingStop,
                    trailingStopActivationPrice = trailingStopActivationPrice,
                    trailingStopDistance = trailingStopDistance
                )
            }
            Mode.RANGE -> {
                // Grid spacing is the larger of:
                // 1. ATR-based spacing (adapts to volatility)
                // 2. Percentage-based floor (prevents too-tight spacing in low volatility)
                val spacing = (indicators.atr * config.strategy.minGridSpacingAtrMultiplier).max(config.strategy.minGridSpacingFloor)
                println("  [DECISION] → Final: RANGE ${config.strategy.gridLevels} levels | Spacing: $$spacing | ${config.strategy.gridPositionPercentPerLevel.multiply(BigDecimal("100"))}% per level")
                Decision.Range(
                    gridSpacing = spacing,
                    levels = config.strategy.gridLevels,
                    positionSizePercentPerLevel = config.strategy.gridPositionPercentPerLevel,
                    adx = indicators.adx,
                    atr = indicators.atr
                )
            }
        }
    }
}
