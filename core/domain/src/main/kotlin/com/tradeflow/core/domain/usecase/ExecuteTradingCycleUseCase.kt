package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.risk.TrailingStopManager
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Result of a single trading cycle execution.
 *
 * Each cycle completes with an execution outcome (success, skip, or failure) and
 * an updated high-water mark for drawdown tracking.
 *
 * **Example:**
 * ```kotlin
 * val result = orchestrator.runCycle("BTC-USD", currentHWM)
 * when (result.execution) {
 *     is ExecutionResult.Success -> log.info("Cycle succeeded: ${result.execution.message}")
 *     is ExecutionResult.Skipped -> log.info("Cycle skipped: ${result.execution.reason}")
 *     is ExecutionResult.Failed -> log.error("Cycle failed: ${result.execution.error}")
 * }
 * val newHWM = result.updatedHighWaterMark
 * ```
 *
 * @property execution Outcome of the cycle (Success, Skipped, or Failed).
 *           Contains human-readable messages for logging/monitoring.
 *
 * @property updatedHighWaterMark New high-water mark after this cycle.
 *           If portfolio equity exceeded previous HWM, this is the new equity value.
 *           Otherwise, this equals the input HWM (unchanged).
 *           Unit: USD (total portfolio value).
 *
 * @see ExecutionResult for the three outcome types
 * @see ExecuteTradingCycleUseCase.runCycle for how this result is generated
 */
data class CycleResult(
    val execution: ExecutionResult,
    val updatedHighWaterMark: BigDecimal
)

/**
 * Main trading cycle orchestrator that coordinates all strategy components.
 *
 * **Responsibility:** THE BRAIN of the trading system. Orchestrates the complete trading loop:
 * 1. **Risk Management:** Monitors drawdown circuit breaker, liquidates on breach
 * 2. **State Tracking:** Tracks current positions and open orders
 * 3. **Decision Making:** Delegates to MakeTradingDecisionUseCase for strategy decisions
 * 4. **Order Execution:** Places/cancels orders based on decisions
 *
 * **Trading Cycle Flow:**
 * ```
 * runCycle() called (every 4 hours in production, or on-demand for backtesting)
 *   ↓
 * 1. Fetch Portfolio State (balances, equity, positions)
 *   ↓
 * 2. Fetch Market Data (current price, candles, open orders)
 *   ↓
 * 3. Update High-Water Mark (for drawdown calculation)
 *   ↓
 * 4. RISK CHECK: Drawdown Circuit Breaker
 *   └─ If breached: EMERGENCY LIQUIDATE ALL + return Failed
 *   └─ Otherwise: continue
 *   ↓
 * 5. State Analysis (am I already in a trade?)
 *   ↓
 * 6. Get Decision from MakeTradingDecisionUseCase (Wait, Trend, or Range)
 *   ↓
 * 7. Execute Decision:
 *   ├─ Wait: Do nothing, log reason
 *   ├─ Trend: Place bracket order (entry + stop + target) if not in trade
 *   └─ Range: Mean-reversion trade targeting SMA200
 *   ↓
 * 8. Return CycleResult (outcome + updated HWM)
 * ```
 *
 * **Risk Profile Configuration:**
 * The orchestrator uses a fixed risk profile (default: BALANCED) throughout execution.
 * Different profiles can be selected when creating the orchestrator:
 * - **AGGRESSIVE:** For small accounts ($100-500), higher risk tolerance
 * - **BALANCED:** For mid-size accounts ($500-1000), moderate risk (default)
 * - **CONSERVATIVE:** For meaningful accounts ($1000-2000), capital preservation
 * - **ULTRA_CONSERVATIVE:** For large accounts ($2000+), maximum protection
 *
 * Profile is set at initialization and remains constant throughout the session.
 * To change profiles, create a new orchestrator instance with the desired profile.
 *
 * **Drawdown Circuit Breaker (CRITICAL SAFETY):**
 * Before every decision execution, the orchestrator checks if drawdown exceeded the limit:
 * ```kotlin
 * val drawdown = (highWaterMark - currentEquity) / highWaterMark
 * if (drawdown > maxDrawdownPercent) {
 *     // EMERGENCY: Cancel all orders + liquidate all BTC
 *     // Prevents catastrophic losses
 * }
 * ```
 *
 * **Example: BALANCED profile (default) has 15% max drawdown.**
 * - High-water mark: $1000 (peak portfolio value)
 * - Current equity: $850
 * - Drawdown: 15% → Circuit breaker TRIPS
 * - Action: Sell all BTC, cancel all orders, HALT trading
 *
 * **Position State Detection:**
 * The orchestrator determines if you're "in a trade" by checking:
 * - `hasBtcBalance`: BTC balance > dust threshold (0.00001 BTC)
 * - `hasOpenOrders`: Any open limit orders exist
 * - `isInTrade = hasBtcBalance || hasOpenOrders`
 *
 * This prevents:
 * - Opening multiple positions when one is already active
 * - Placing new grid orders while old grid is still active
 * - Overexposure to the market
 *
 * **Decision Execution Logic:**
 *
 * **1. Wait Decision:**
 * - Action: Do nothing
 * - Example reasons: "Insufficient candles", "Circuit breaker active"
 * - Result: ExecutionResult.Skipped
 *
 * **2. Trend Decision (Perpetual Futures - LONG or SHORT):**
 * - Trigger: ADX ≥ threshold (strong directional trend)
 * - Direction: LONG (BUY) if price >= SMA200, SHORT (SELL) if price < SMA200
 * - Action (if NOT in trade):
 *   1. Check funding rate (close if > maxAcceptableFundingRate)
 *   2. Calculate position size: `portfolio.totalEquityUsd × positionSizePercent × leverage`
 *   3. Convert to BTC: `sizeUsd / entryPrice`
 *   4. Place bracket order: ENTRY + STOP_LOSS + TAKE_PROFIT
 * - Example LONG: 5% of $1000 × 2x leverage = $100, at $95k/BTC = 0.00105263 BTC
 * - Example SHORT: 5% of $1000 × 2x leverage = $100, at $95k/BTC = 0.00105263 BTC (SHORT)
 * - Result: ExecutionResult.Success("Trend LONG: Opened position") or Skipped("Trend: Already in trade")
 *
 * **3. Range Decision:**
 * - Trigger: ADX < threshold (choppy sideways market)
 * - Action (if NOT in trade):
 *   - Calculate distance from SMA200 (mean)
 *   - If price is sufficiently far from SMA (> 0.5× ATR):
 *     - LONG if price < SMA (expect reversion up)
 *     - SHORT if price > SMA (expect reversion down)
 *   - Target: SMA200 (mean reversion)
 *   - Stop: 2× ATR beyond entry
 * - Result: ExecutionResult.Success("Range LONG/SHORT: Mean-reversion position") or Skipped
 *
 * **Console Output (for monitoring):**
 * ```
 * [RISK] Equity: $1250.00 | HWM: $1300.00
 * [RISK] Drawdown: 3.85%
 * [STATE] Perpetual: LONG 0.01234567 BTC @ $95000 | PnL: $150.00
 * [STATE] Open Orders: 2 | In Trade: true
 * [EXEC] TREND LONG: Size $50.00 (2.0x) = 0.00052632 BTC
 * [EXEC] Funding Rate: 0.01% (acceptable)
 * ```
 *
 * **Error Handling:**
 * - All exchange operations (getPortfolio, placeLimitOrder, etc.) return `Result<T>`
 * - `.getOrThrow()` is used to propagate failures as exceptions
 * - Top-level try/catch wraps entire cycle
 * - On error: Return `CycleResult(ExecutionResult.Failed("Cycle failed: ${e.message}"), highWaterMark)`
 * - This ensures backtesting continues even if one cycle fails
 *
 * **Thread Safety:**
 * Stateless (config is immutable). Safe to call from multiple threads.
 * However, in production, ensure only one cycle runs at a time to prevent race conditions
 * in exchange API calls (e.g., scheduled every 4 hours).
 *
 * **Dependencies:**
 * - **ExchangeRepository:** Fetches market data, places orders (including bracket orders)
 * - **MakeTradingDecisionUseCase:** Generates trading decisions based on market regime
 *
 * **Usage in Backtesting:**
 * ```kotlin
 * val orchestrator = ExecuteTradingCycleUseCase(
 *     exchangeRepository = SimulatedExchange(...),
 *     makeDecisionUseCase = MakeTradingDecisionUseCase(...)
 * )
 *
 * var hwm = BigDecimal.ZERO
 * for (cycle in 1..1000) {
 *     val result = orchestrator.runCycle("BTC-USD", hwm)
 *     hwm = result.updatedHighWaterMark
 *     println("Cycle $cycle: ${result.execution}")
 * }
 * ```
 *
 * **Usage in Production:**
 * ```kotlin
 * // Scheduled every 4 hours (matching FOUR_HOUR candle granularity)
 * @Scheduled(fixedRate = 4, timeUnit = TimeUnit.HOURS)
 * suspend fun runTradingCycle() {
 *     val result = orchestrator.runCycle("BTC-USD", persistedHWM)
 *     persistedHWM = result.updatedHighWaterMark
 *     repository.save(result)
 * }
 * ```
 *
 * @property exchangeRepository Repository for market data and order placement (including bracket orders).
 *           In backtesting, this is SimulatedExchange. In production, CoinbaseRepository.
 *
 * @property makeDecisionUseCase Strategy decision generator (MakeTradingDecisionUseCase).
 *           Creates instances automatically via default parameter.
 *
 * @see MakeTradingDecisionUseCase for how decisions are generated
 * @see Decision for the three decision types (Wait, Trend, Range)
 * @see CycleResult for the return type
 */
class ExecuteTradingCycleUseCase(
    private val exchangeRepository: ExchangeRepository,
    private val makeDecisionUseCase: MakeTradingDecisionUseCase,
    private val config: TradingConfig,
    private val trailingStopManager: TrailingStopManager
) {

    /**
     * Executes a single trading cycle: fetch data, check risk, make decision, execute orders.
     *
     * **This is the main entry point** for the trading system. Call this method periodically
     * (every 4 hours in production, or on-demand in backtesting) to run the complete trading loop.
     *
     * **Complete Cycle Workflow:**
     * 1. Fetch current portfolio state from exchange
     * 2. Fetch market data (current price, candles, open orders)
     * 3. Update high-water mark (if equity increased)
     * 4. **CRITICAL:** Check drawdown circuit breaker
     *    - If breached: EMERGENCY liquidate all positions + return Failed
     * 5. Analyze current state (am I in a trade? any open orders?)
     * 6. Call DecisionEngine to get trading decision
     * 7. Execute the decision (place/cancel orders as needed)
     * 8. Return result + updated high-water mark
     *
     * **Drawdown Circuit Breaker (SAFETY CRITICAL):**
     * Before executing any decision, the orchestrator calculates:
     * ```
     * drawdown = (highWaterMark - currentEquity) / highWaterMark
     * ```
     *
     * If `drawdown > maxDrawdownPercent` (default 15% for BALANCED):
     * 1. Cancel ALL open orders immediately
     * 2. Sell ALL BTC holdings at market price (if balance > dust threshold)
     * 3. Return `ExecutionResult.Failed("EMERGENCY: 15% Drawdown reached. Liquidated.")`
     * 4. **Trading HALTS** - no new positions until system is manually reset
     *
     * This prevents runaway losses from a bad strategy or market crash.
     *
     * **Position State Analysis:**
     * Before executing Trend or Range decisions, the orchestrator checks if already in a trade:
     * - `hasBtcBalance`: BTC balance exceeds dust threshold (0.00001 BTC)
     * - `hasOpenOrders`: Any limit orders are currently open
     * - `isInTrade = hasBtcBalance || hasOpenOrders`
     *
     * If `isInTrade == true`, Trend and Range decisions are SKIPPED to avoid overexposure.
     *
     * **Decision Execution (4 cases):**
     *
     * **Case 1: Wait**
     * - Action: None
     * - Return: `ExecutionResult.Skipped("Wait: ${reason}")`
     * - Example reasons: "Insufficient candles", "ADX neutral zone"
     *
     * **Case 2: Trend (Perpetual Futures - LONG or SHORT)**
     * - Trigger: ADX ≥ threshold (strong trend)
     * - Direction: Decision contains OrderSide.BUY (LONG) or OrderSide.SELL (SHORT)
     * - Action (if NOT in trade):
     *   1. Check funding rate (skip if > maxAcceptableFundingRate)
     *   2. Calculate position size: `equity × positionSizePercent × leverage`
     *   3. Convert USD to BTC: `sizeUsd / entryPrice`
     *   4. Place bracket order: ENTRY + STOP_LOSS + TAKE_PROFIT (perpetual futures)
     * - Example LONG: $1000 × 5% × 2x = $100 at $95k/BTC = 0.00105263 BTC LONG
     * - Example SHORT: $1000 × 5% × 2x = $100 at $95k/BTC = 0.00105263 BTC SHORT
     * - Return: `ExecutionResult.Success("Trend LONG: Opened position")` or Skipped if already in trade
     *
     * **Case 3: Range**
     * - Trigger: ADX < threshold (sideways chop)
     * - Action (if NOT in trade):
     *   1. Calculate position size per level: `equity × positionSizePercentPerLevel`
     *   2. Place grid of buy orders (default 3 levels) at `currentPrice - (gridSpacing × level)`
     * - Action (if grid FILLED but no sell order):
     *   1. Place take-profit sell order at `currentPrice + gridSpacing`
     * - Return: `ExecutionResult.Success("Range: Placed 3/3 grid orders")` or similar
     *
     * **Error Handling:**
     * - Entire cycle wrapped in try/catch
     * - If any exception thrown: Return `CycleResult(ExecutionResult.Failed("Cycle failed: ${e.message}"), highWaterMark)`
     * - This ensures backtesting continues even if one cycle fails (e.g., network error, invalid data)
     *
     * **Console Logging:**
     * Each cycle logs key information for monitoring:
     * ```
     * [RISK] Equity: $1250.00 | HWM: $1300.00
     * [RISK] Drawdown: 3.85%
     * [STATE] Perpetual: LONG 0.01234567 BTC @ $95000 | PnL: $150.00
     * [STATE] Open Orders: 2 | In Trade: true
     * [EXEC] TREND LONG: Size $50.00 (2.0x) = 0.00052632 BTC
     * [EXEC] Funding Rate: 0.01% (acceptable)
     * [EXEC] TREND: Skipped (already in trade)
     * ```
     *
     * **Example Backtesting Usage:**
     * ```kotlin
     * var hwm = BigDecimal.ZERO
     * val results = mutableListOf<CycleResult>()
     *
     * for (cycleIndex in 0..999) {
     *     val result = orchestrator.runCycle("BTC-USD", hwm)
     *     hwm = result.updatedHighWaterMark
     *     results.add(result)
     *
     *     when (result.execution) {
     *         is ExecutionResult.Success -> successCount++
     *         is ExecutionResult.Skipped -> skippedCount++
     *         is ExecutionResult.Failed -> failedCount++
     *     }
     * }
     * ```
     *
     * **Example Production Usage:**
     * ```kotlin
     * @Scheduled(fixedRate = 4, timeUnit = TimeUnit.HOURS)
     * suspend fun tradingLoop() {
     *     val hwm = repository.getHighWaterMark()
     *     val result = orchestrator.runCycle("BTC-USD", hwm)
     *     repository.saveResult(result)
     *     repository.updateHighWaterMark(result.updatedHighWaterMark)
     * }
     * ```
     *
     * @param productId Trading pair identifier (e.g., "BTC-USD").
     *                  Must be a valid product ID supported by the exchange.
     *
     * @param highWaterMark Peak portfolio value (in USD) since system started.
     *                      Used to calculate drawdown percentage.
     *                      If this is the first cycle, pass BigDecimal.ZERO.
     *                      **Critical for circuit breaker logic.**
     *
     * @return CycleResult containing:
     *         - `execution`: Success, Skipped, or Failed with message
     *         - `updatedHighWaterMark`: New HWM if equity increased, otherwise unchanged
     *
     * @throws None - all exceptions are caught and returned as ExecutionResult.Failed
     *
     * @see CycleResult for the return type
     * @see ExecutionResult for the three outcome types
     * @see Decision for the three decision types that can be executed
     */
    suspend fun runCycle(productId: String, highWaterMark: BigDecimal): CycleResult {
        return try {
            val portfolio = exchangeRepository.getPortfolio().getOrThrow()

            val currentPrice = exchangeRepository.getCurrentPrice(productId).getOrThrow().price
            val candles = exchangeRepository.getCandles(productId, config.technical.granularity).getOrThrow()
            val openOrders = exchangeRepository.getOpenOrders(productId).getOrThrow()

            val currentHighWaterMark = if (portfolio.totalEquityUsd > highWaterMark) {
                portfolio.totalEquityUsd
            } else {
                highWaterMark
            }

            // 1. Risk Check (Circuit Breaker)
            println("  [RISK] Equity: \$${portfolio.totalEquityUsd.setScale(2, RoundingMode.HALF_UP)} | HWM: \$${currentHighWaterMark.setScale(2, RoundingMode.HALF_UP)}")
            if (currentHighWaterMark > BigDecimal.ZERO) {
                val drawdown = (currentHighWaterMark - portfolio.totalEquityUsd)
                    .divide(currentHighWaterMark, config.risk.percentDecimalPlaces, RoundingMode.HALF_UP)
                println("  [RISK] Drawdown: ${drawdown.multiply(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)}%")
                if (drawdown > BigDecimal.valueOf(config.risk.maxDrawdownPercent)) {
                    // EMERGENCY: Cancel all orders + close all positions
                    exchangeRepository.cancelOrders(openOrders.map { it.id })

                    // Close perpetual position if exists (PERPETUAL FUTURES ONLY)
                    val perpetualProductId = "${productId.substringBefore("-")}-PERP"
                    val position = exchangeRepository.getPerpetualPosition(perpetualProductId).getOrNull()
                    if (position != null) {
                        exchangeRepository.closePerpetualPosition(perpetualProductId)
                    }

                    return CycleResult(
                        ExecutionResult.Failed("EMERGENCY: ${config.risk.maxDrawdownPercent * 100}% Drawdown reached. Liquidated."),
                        currentHighWaterMark
                    )
                }
            }

            // 2. State Check (PERPETUAL FUTURES ONLY)
            val perpetualProductId = "${productId.substringBefore("-")}-PERP"
            val perpetualPosition = exchangeRepository.getPerpetualPosition(perpetualProductId).getOrNull()
            val hasPerpetualPosition = perpetualPosition != null

            val hasOpenOrders = openOrders.isNotEmpty()
            val isInTrade = hasPerpetualPosition || hasOpenOrders

            if (hasPerpetualPosition) {
                val pos = perpetualPosition!!
                val directionName = if (pos.isLong) "LONG" else "SHORT"
                println("  [STATE] Perpetual: $directionName ${pos.size} BTC @ \$${pos.entryPrice.setScale(0, RoundingMode.HALF_UP)} | PnL: \$${pos.unrealizedPnl.setScale(2, RoundingMode.HALF_UP)}")
            }
            println("  [STATE] Open Orders: ${openOrders.size} | In Trade: $isInTrade")

            val decision = makeDecisionUseCase.execute(candles, currentPrice)

            // 3. Execution
            val executionResult = when (decision) {
                is Decision.Wait -> ExecutionResult.Skipped("Wait: ${decision.reason}")
                is Decision.Trend -> {
                    if (!isInTrade) {
                        // 1. Check funding rate (skip if too expensive)
                        val fundingRate = exchangeRepository.getFundingRate(perpetualProductId).getOrNull()
                        if (fundingRate != null && fundingRate.isTooExpensive(config.execution.maxAcceptableFundingRate)) {
                            println("  [EXEC] TREND: Skipped (funding rate too high: ${fundingRate.toPercentageString()})")
                            ExecutionResult.Skipped("Trend: Funding rate ${fundingRate.toPercentageString()} exceeds limit.")
                        } else {
                            // 2. Calculate position size with leverage
                            val leverage = config.strategy.leverage
                            val sizeUsd = portfolio.totalEquityUsd * decision.positionSizePercent * leverage
                            val btcSize = sizeUsd.divide(decision.entryPrice, 8, RoundingMode.HALF_UP)
                            val directionName = if (decision.direction == OrderSide.BUY) "LONG" else "SHORT"

                            println("  [EXEC] TREND $directionName: Size \$${sizeUsd.setScale(2, RoundingMode.HALF_UP)} (${leverage}x) = ${btcSize.setScale(8, RoundingMode.HALF_UP)} BTC")
                            if (fundingRate != null) {
                                println("  [EXEC] Funding Rate: ${fundingRate.toPercentageString()} (acceptable)")
                            }

                            // 3. Place bracket order on perpetual futures
                            exchangeRepository.placeBracketOrder(
                                perpetualProductId, decision.direction, btcSize,
                                decision.entryPrice, decision.takeProfit, decision.stopLoss
                            ).getOrThrow()

                            ExecutionResult.Success("Trend $directionName: Opened ${btcSize.setScale(4, RoundingMode.HALF_UP)} BTC position.")
                        }
                    } else {
                        println("  [EXEC] TREND: Skipped (already in trade)")

                        // TRAILING STOP MANAGEMENT
                        // If trailing stops are enabled and we have an open position, update the stop-loss dynamically
                        if (config.strategy.useTrailingStop && decision.useTrailingStop) {
                            updateTrailingStop(perpetualProductId, currentPrice, decision.atr, openOrders)
                        }

                        ExecutionResult.Skipped("Trend: Already in trade.")
                    }
                }
                is Decision.Range -> {
                    if (!isInTrade) {
                        // RANGE STRATEGY: Mean-reversion for perpetual futures
                        // In ranging markets (low ADX), price tends to revert to the mean (SMA200).
                        // We trade against extremes and target reversion to the mean.

                        // Calculate SMA200 for mean-reversion baseline
                        val taService = AnalyzeCandlesUseCase()
                        val indicators = taService.calculateAll(candles, config.technical.smaPeriod, config.technical.adxPeriod, config.technical.atrPeriod)
                        val sma = indicators.sma200
                        val atr = decision.atr

                        // Entry threshold: Price must be at least 0.5x ATR away from SMA to enter
                        val entryThreshold = atr * BigDecimal("0.5")
                        val distanceFromSma = (currentPrice - sma).abs()

                        if (distanceFromSma >= entryThreshold) {
                            // Determine direction: LONG if below SMA (expect reversion up), SHORT if above SMA (expect reversion down)
                            val isLong = currentPrice < sma
                            val direction = if (isLong) OrderSide.BUY else OrderSide.SELL

                            // Entry: Current price
                            val entryPrice = currentPrice

                            // Take Profit: SMA (mean reversion target)
                            val takeProfit = sma

                            // Stop Loss: If price continues away from SMA (2x ATR beyond entry)
                            val stopLoss = if (isLong) {
                                entryPrice - (atr * BigDecimal("2.0"))
                            } else {
                                entryPrice + (atr * BigDecimal("2.0"))
                            }

                            // Validate stop/target placement
                            val stopTargetValid = if (isLong) {
                                stopLoss < entryPrice && takeProfit > entryPrice
                            } else {
                                stopLoss > entryPrice && takeProfit < entryPrice
                            }

                            if (stopTargetValid) {
                                // Check funding rate
                                val fundingRate = exchangeRepository.getFundingRate(perpetualProductId).getOrNull()
                                if (fundingRate != null && fundingRate.isTooExpensive(config.execution.maxAcceptableFundingRate)) {
                                    println("  [EXEC] RANGE: Skipped (funding rate too high: ${fundingRate.toPercentageString()})")
                                    ExecutionResult.Skipped("Range: Funding rate ${fundingRate.toPercentageString()} exceeds limit.")
                                } else {
                                    // Calculate position size (smaller than trend: use gridPositionPercentPerLevel)
                                    val leverage = config.strategy.leverage
                                    val sizeUsd = portfolio.totalEquityUsd * decision.positionSizePercentPerLevel * leverage
                                    val btcSize = sizeUsd.divide(entryPrice, 8, RoundingMode.HALF_UP)
                                    val directionName = if (isLong) "LONG" else "SHORT"

                                    println("  [EXEC] RANGE $directionName: Size \$${sizeUsd.setScale(2, RoundingMode.HALF_UP)} (${leverage}x) = ${btcSize.setScale(8, RoundingMode.HALF_UP)} BTC")
                                    println("  [EXEC] Mean Reversion: Entry \$${entryPrice.setScale(0, RoundingMode.HALF_UP)} → Target (SMA) \$${takeProfit.setScale(0, RoundingMode.HALF_UP)} | Stop \$${stopLoss.setScale(0, RoundingMode.HALF_UP)}")
                                    if (fundingRate != null) {
                                        println("  [EXEC] Funding Rate: ${fundingRate.toPercentageString()} (acceptable)")
                                    }

                                    // Place bracket order
                                    exchangeRepository.placeBracketOrder(
                                        perpetualProductId, direction, btcSize,
                                        entryPrice, takeProfit, stopLoss
                                    ).getOrThrow()

                                    ExecutionResult.Success("Range $directionName: Opened ${btcSize.setScale(4, RoundingMode.HALF_UP)} BTC mean-reversion position.")
                                }
                            } else {
                                println("  [EXEC] RANGE: Skipped (invalid stop/target placement)")
                                ExecutionResult.Skipped("Range: Invalid stop/target placement for mean reversion.")
                            }
                        } else {
                            println("  [EXEC] RANGE: Skipped (price too close to SMA: \$${distanceFromSma.setScale(0, RoundingMode.HALF_UP)} < \$${entryThreshold.setScale(0, RoundingMode.HALF_UP)})")
                            ExecutionResult.Skipped("Range: Price too close to SMA for mean reversion entry.")
                        }
                    } else {
                        println("  [EXEC] RANGE: Skipped (already in trade)")
                        ExecutionResult.Skipped("Range: Already in trade.")
                    }
                }
            }

            CycleResult(executionResult, currentHighWaterMark)
        } catch (e: Exception) {
            CycleResult(
                ExecutionResult.Failed("Cycle failed: ${e.message}"),
                highWaterMark
            )
        }
    }

    /**
     * Updates the stop-loss order based on trailing stop calculation.
     *
     * This function is called when we have an open position and trailing stops are enabled.
     * It calculates the current trailing stop price and updates the stop-loss order if needed.
     *
     * **Algorithm:**
     * 1. Fetch the open perpetual position
     * 2. Calculate high water mark (highest/lowest price since entry)
     * 3. Calculate trailing stop state using TrailingStopManager
     * 4. Find existing stop-loss order
     * 5. If trailing stop has moved favorably, cancel old SL and place new one
     *
     * **Trailing Stop Logic:**
     * - LONG: Stop moves UP as price rises (never moves down)
     * - SHORT: Stop moves DOWN as price falls (never moves up)
     * - Protects profits while allowing trend to continue
     *
     * @param productId Perpetual futures product (e.g., "BTC-PERP")
     * @param currentPrice Current market price
     * @param atr Current ATR value for trailing stop calculation
     * @param openOrders List of open orders (to find existing stop-loss)
     */
    private suspend fun updateTrailingStop(
        productId: String,
        currentPrice: BigDecimal,
        atr: BigDecimal,
        openOrders: List<Order>
    ) {
        try {
            // 1. Get open perpetual position
            val position = exchangeRepository.getPerpetualPosition(productId).getOrNull() ?: return

            // 2. Calculate high water mark (highest for LONG, lowest for SHORT)
            // For simplicity, we use currentPrice as a proxy for high water mark
            // In production, this should be tracked across cycles
            val highWaterMark = when (position.side) {
                OrderSide.BUY -> maxOf(currentPrice, position.entryPrice + (position.unrealizedPnl / position.size))
                OrderSide.SELL -> minOf(currentPrice, position.entryPrice - (position.unrealizedPnl / position.size))
            }

            // 3. Calculate trailing stop state
            val trailingState = trailingStopManager.calculateTrailingStop(
                entryPrice = position.entryPrice,
                currentPrice = currentPrice,
                highestPriceSinceEntry = highWaterMark,
                atr = atr,
                direction = position.side
            )

            if (!trailingState.isActive) {
                // Trailing not yet activated, keep initial fixed stop
                return
            }

            // 4. Find existing stop-loss order
            // Stop-loss is opposite side: LONG position has SELL stop, SHORT position has BUY stop
            val stopSide = if (position.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
            val existingStopOrder = openOrders.firstOrNull { order ->
                order.side == stopSide && order.productId == productId
            }

            if (existingStopOrder == null) {
                println("  [TRAIL] Warning: No existing stop-loss order found")
                return
            }

            // 5. Check if trailing stop has moved favorably
            val shouldUpdate = when (position.side) {
                OrderSide.BUY -> trailingState.currentStopPrice > (existingStopOrder.price ?: BigDecimal.ZERO)
                OrderSide.SELL -> trailingState.currentStopPrice < (existingStopOrder.price ?: BigDecimal("999999"))
            }

            if (shouldUpdate) {
                // Cancel old stop-loss
                exchangeRepository.cancelOrder(existingStopOrder.id).getOrNull()

                // Place new stop-loss at trailing stop price
                exchangeRepository.placeLimitOrder(
                    productId = productId,
                    side = stopSide,
                    size = position.size,
                    price = trailingState.currentStopPrice,
                    postOnly = false
                ).getOrNull()

                val cautionFlag = if (trailingState.isInCautionState) " [CAUTION]" else ""
                println("  [TRAIL] Updated ${position.side.name} stop: ${existingStopOrder.price} → ${trailingState.currentStopPrice}$cautionFlag")
            }
        } catch (e: Exception) {
            println("  [TRAIL] Error updating trailing stop: ${e.message}")
        }
    }
}
