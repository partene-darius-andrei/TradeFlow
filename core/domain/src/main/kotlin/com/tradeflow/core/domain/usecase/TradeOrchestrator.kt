package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.config.AdaptiveOptimizer
import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.BracketOrderRepository
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.strategy.DecisionEngine
import com.tradeflow.core.domain.strategy.TradingDecisionEngine
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

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
 * @see TradeOrchestrator.runCycle for how this result is generated
 */
data class CycleResult(
    val execution: ExecutionResult,
    val updatedHighWaterMark: BigDecimal
)

/**
 * Main trading cycle orchestrator that coordinates all strategy components.
 *
 * **Responsibility:** THE BRAIN of the trading system. Orchestrates the complete trading loop:
 * 1. **Adaptive Risk:** Automatically switches risk profiles based on portfolio balance
 * 2. **Risk Management:** Monitors drawdown circuit breaker, liquidates on breach
 * 3. **State Tracking:** Tracks current positions and open orders
 * 4. **Decision Making:** Delegates to TradingDecisionEngine for strategy decisions
 * 5. **Order Execution:** Places/cancels orders based on decisions
 *
 * **Trading Cycle Flow:**
 * ```
 * runCycle() called (every 4 hours in production, or on-demand for backtesting)
 *   ↓
 * 1. Fetch Portfolio State (balances, equity, positions)
 *   ↓
 * 2. Adaptive Profile Check (auto-switch AGGRESSIVE → BALANCED → CONSERVATIVE → ULTRA_CONSERVATIVE)
 *   ↓
 * 3. Fetch Market Data (current price, candles, open orders)
 *   ↓
 * 4. Update High-Water Mark (for drawdown calculation)
 *   ↓
 * 5. RISK CHECK: Drawdown Circuit Breaker
 *   └─ If breached: EMERGENCY LIQUIDATE ALL + return Failed
 *   └─ Otherwise: continue
 *   ↓
 * 6. State Analysis (am I already in a trade?)
 *   ↓
 * 7. Get Decision from TradingDecisionEngine (Wait, Defense, Trend, or Range)
 *   ↓
 * 8. Execute Decision:
 *   ├─ Wait: Do nothing, log reason
 *   ├─ Defense: Cancel buy orders, sell all BTC (price below SMA)
 *   ├─ Trend: Place bracket order (entry + stop + target) if not in trade
 *   └─ Range: Place grid orders if not in trade, or take-profit if grid filled
 *   ↓
 * 9. Return CycleResult (outcome + updated HWM)
 * ```
 *
 * **Adaptive Risk Profile Switching:**
 * The orchestrator automatically adjusts risk parameters as your portfolio grows:
 * - **$0-500:** AGGRESSIVE (5.23% position size, high risk for growth)
 * - **$500-1000:** BALANCED (balanced risk/reward)
 * - **$1000-2000:** CONSERVATIVE (capital preservation focus)
 * - **$2000+:** ULTRA_CONSERVATIVE (minimal risk, protect gains)
 *
 * When a profile switch occurs:
 * 1. New TradingConfig is loaded with appropriate parameters
 * 2. TradingDecisionEngine state is RESET (clears hysteresis)
 * 3. Console logs the switch: "[ADAPTIVE] BALANCED → CONSERVATIVE | Balance: $1250.00"
 *
 * **State Management (Stateful):**
 * Unlike most domain components, TradeOrchestrator maintains internal state:
 * - `currentConfig`: Active trading configuration (strategy, risk, technical params)
 * - `currentProfile`: Active risk profile (AGGRESSIVE, BALANCED, etc.)
 *
 * This state persists across cycles to enable adaptive profile switching and
 * consistent configuration between cycles.
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
 * **2. Defense Decision:**
 * - Trigger: Price < SMA200 (bearish market structure)
 * - Action: Cancel all BUY orders, sell all BTC holdings (if any)
 * - Goal: Preserve capital in weak market conditions
 * - Result: ExecutionResult.Success("Defense: Liquidated holdings") or Skipped("Defense: Clean")
 *
 * **3. Trend Decision:**
 * - Trigger: ADX ≥ threshold (strong directional trend)
 * - Action (if NOT in trade):
 *   - Calculate position size: `portfolio.totalEquityUsd × positionSizePercent`
 *   - Convert to BTC: `sizeUsd / entryPrice`
 *   - Place bracket order: ENTRY + STOP_LOSS + TAKE_PROFIT
 * - Example: 5% of $1000 = $50, at $95k/BTC = 0.00052632 BTC
 * - Result: ExecutionResult.Success("Trend: Opened position") or Skipped("Trend: Already in trade")
 *
 * **4. Range Decision:**
 * - Trigger: ADX < threshold (choppy sideways market)
 * - Action (if NOT in trade):
 *   - Place multiple grid orders (default: 3 levels)
 *   - Each level: `portfolio.totalEquityUsd × positionSizePercentPerLevel`
 *   - Spacing: ATR-based or percentage-based minimum
 *   - Example: 3 levels @ 8% each = 24% total exposure
 * - Action (if grid FILLED and no sell order):
 *   - Place take-profit sell order at `currentPrice + gridSpacing`
 * - Result: ExecutionResult.Success("Range: Placed 3/3 grid orders") or similar
 *
 * **Console Output (for monitoring):**
 * ```
 * [ADAPTIVE] BALANCED → CONSERVATIVE | Balance: $1250.00
 * [RISK] Equity: $1250.00 | HWM: $1300.00
 * [RISK] Drawdown: 3.85%
 * [STATE] BTC: 0.01234567 | Open Orders: 2 | In Trade: true
 * [EXEC] RANGE: $100.00 per level × 3 levels | Spacing: $1425.00
 * [EXEC] Grid level 1: BUY 0.00105263 BTC @ $95000.00
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
 * NOT thread-safe due to mutable state (currentConfig, currentProfile).
 * In production, ensure only one cycle runs at a time (e.g., scheduled every 4 hours).
 * In backtesting, run cycles sequentially on single thread.
 *
 * **Dependencies:**
 * - **ExchangeRepository:** Fetches market data, places/cancels orders
 * - **BracketOrderRepository:** Places complex bracket orders (entry + stop + target)
 * - **DecisionEngine:** Generates trading decisions (injected as TradingDecisionEngine)
 * - **AdaptiveOptimizer:** Detects when to switch risk profiles
 *
 * **Usage in Backtesting:**
 * ```kotlin
 * val orchestrator = TradeOrchestrator(
 *     exchangeRepository = SimulatedExchange(...),
 *     bracketOrderRepository = SimulatedBracketOrders(...),
 *     decisionEngine = TradingDecisionEngine(...),
 *     adaptiveOptimizer = AdaptiveOptimizer()
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
 * @property exchangeRepository Repository for market data and order placement.
 *           In backtesting, this is SimulatedExchange. In production, CoinbaseRepository.
 *
 * @property bracketOrderRepository Repository for placing bracket orders (entry + stop + target).
 *           Simplifies complex order placement logic.
 *
 * @property decisionEngine Strategy decision generator (typically TradingDecisionEngine).
 *           Injected as DecisionEngine interface for testability.
 *
 * @property adaptiveOptimizer Detects when portfolio balance crosses risk profile thresholds.
 *           Automatically switches from AGGRESSIVE → BALANCED → CONSERVATIVE → ULTRA_CONSERVATIVE.
 *
 * @see TradingDecisionEngine for how decisions are generated
 * @see RiskProfile for the four risk profile tiers
 * @see Decision for the four decision types (Wait, Defense, Trend, Range)
 * @see CycleResult for the return type
 */
class TradeOrchestrator @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val bracketOrderRepository: BracketOrderRepository,
    private val decisionEngine: DecisionEngine,
    private val adaptiveOptimizer: AdaptiveOptimizer
) {
    private var currentConfig: TradingConfig = TradingConfig.forProfile(RiskProfile.BALANCED)
    private var currentProfile: RiskProfile = RiskProfile.BALANCED

    /**
     * Executes a single trading cycle: fetch data, check risk, make decision, execute orders.
     *
     * **This is the main entry point** for the trading system. Call this method periodically
     * (every 4 hours in production, or on-demand in backtesting) to run the complete trading loop.
     *
     * **Complete Cycle Workflow:**
     * 1. Fetch current portfolio state from exchange
     * 2. Check for adaptive risk profile switch (balance-based)
     * 3. Fetch market data (current price, candles, open orders)
     * 4. Update high-water mark (if equity increased)
     * 5. **CRITICAL:** Check drawdown circuit breaker
     *    - If breached: EMERGENCY liquidate all positions + return Failed
     * 6. Analyze current state (am I in a trade? any open orders?)
     * 7. Call DecisionEngine to get trading decision
     * 8. Execute the decision (place/cancel orders as needed)
     * 9. Return result + updated high-water mark
     *
     * **Adaptive Profile Switching:**
     * If portfolio balance crosses a threshold, the orchestrator automatically switches profiles:
     * - $500 threshold: AGGRESSIVE → BALANCED
     * - $1000 threshold: BALANCED → CONSERVATIVE
     * - $2000 threshold: CONSERVATIVE → ULTRA_CONSERVATIVE
     *
     * When switching:
     * - New config loaded with different risk parameters
     * - DecisionEngine state RESET (clears hysteresis)
     * - Logged to console for monitoring
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
     * **Case 2: Defense**
     * - Trigger: Price < SMA200 (bearish market structure)
     * - Action:
     *   1. Cancel all BUY orders (stop trying to enter)
     *   2. If holding BTC: Sell all at market price
     * - Return: `ExecutionResult.Success("Defense: Liquidated holdings")` or Skipped if already clean
     *
     * **Case 3: Trend**
     * - Trigger: ADX ≥ threshold (strong trend)
     * - Action (if NOT in trade):
     *   1. Calculate position size: `equity × positionSizePercent`
     *   2. Convert USD to BTC: `sizeUsd / entryPrice`
     *   3. Place bracket order: ENTRY + STOP_LOSS + TAKE_PROFIT
     * - Return: `ExecutionResult.Success("Trend: Opened position")` or Skipped if already in trade
     *
     * **Case 4: Range**
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
     * [ADAPTIVE] BALANCED → CONSERVATIVE | Balance: $1250.00  (if profile switched)
     * [RISK] Equity: $1250.00 | HWM: $1300.00
     * [RISK] Drawdown: 3.85%
     * [STATE] BTC: 0.01234567 | Open Orders: 2 | In Trade: true
     * [EXEC] TREND: Size $50.00 = 0.00052632 BTC  (or RANGE with grid details)
     * [EXEC] Grid level 1: BUY 0.00105263 BTC @ $95000.00
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
     * @see Decision for the four decision types that can be executed
     */
    suspend fun runCycle(productId: String, highWaterMark: BigDecimal): CycleResult {
        return try {
            val portfolio = exchangeRepository.getPortfolio().getOrThrow()

            // Adaptive profile switching
            val switchEvent = adaptiveOptimizer.detectProfileSwitch(currentProfile, portfolio.totalEquityUsd)
            if (switchEvent != null) {
                currentConfig = TradingConfig.forProfile(switchEvent.to)
                currentProfile = switchEvent.to
                println("  [ADAPTIVE] ${switchEvent.from} → ${switchEvent.to} | Balance: \$${switchEvent.balance.setScale(2, RoundingMode.HALF_UP)}")
                // Reset decision engine state when profile changes
                (decisionEngine as? TradingDecisionEngine)?.resetState()
            }

            val currentPrice = exchangeRepository.getCurrentPrice(productId).getOrThrow().price
            val candles = exchangeRepository.getCandles(productId, currentConfig.technical.granularity).getOrThrow()
            val openOrders = exchangeRepository.getOpenOrders(productId).getOrThrow()

            val currentHighWaterMark = if (portfolio.totalEquityUsd > highWaterMark) {
                portfolio.totalEquityUsd
            } else {
                highWaterMark
            }

            // 1. Risk Check
            println("  [RISK] Equity: \$${portfolio.totalEquityUsd.setScale(2, RoundingMode.HALF_UP)} | HWM: \$${currentHighWaterMark.setScale(2, RoundingMode.HALF_UP)}")
            if (currentHighWaterMark > BigDecimal.ZERO) {
                val drawdown = (currentHighWaterMark - portfolio.totalEquityUsd)
                    .divide(currentHighWaterMark, currentConfig.risk.percentDecimalPlaces, RoundingMode.HALF_UP)
                println("  [RISK] Drawdown: ${drawdown.multiply(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)}%")
                if (drawdown > BigDecimal.valueOf(currentConfig.risk.maxDrawdownPercent)) {
                    exchangeRepository.cancelOrders(openOrders.map { it.id })
                    val btc = portfolio.getBtcBalance()
                    if (btc > currentConfig.execution.minBtcDustThreshold) {
                        exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, btc)
                    }
                    return CycleResult(
                        ExecutionResult.Failed("EMERGENCY: ${currentConfig.risk.maxDrawdownPercent * 100}% Drawdown reached. Liquidated."),
                        currentHighWaterMark
                    )
                }
            }

            // 2. State Check
            val btcBalance = portfolio.getBtcBalance()
            val hasBtcBalance = btcBalance > currentConfig.execution.minBtcDustThreshold
            val hasOpenOrders = openOrders.isNotEmpty()
            val isInTrade = hasBtcBalance || hasOpenOrders
            println("  [STATE] BTC: $btcBalance | Open Orders: ${openOrders.size} | In Trade: $isInTrade")

            val decision = decisionEngine.evaluate(candles, currentPrice)

            // 3. Execution
            val executionResult = when (decision) {
                is Decision.Wait -> ExecutionResult.Skipped("Wait: ${decision.reason}")
                is Decision.Defense -> {
                    val buys = openOrders.filter { it.side == OrderSide.BUY }.map { it.id }
                    if (buys.isNotEmpty()) exchangeRepository.cancelOrders(buys)

                    if (hasBtcBalance) {
                        exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, btcBalance).getOrThrow()
                        ExecutionResult.Success("Defense: Liquidated holdings.")
                    } else {
                        ExecutionResult.Skipped("Defense: Clean.")
                    }
                }
                is Decision.Trend -> {
                    if (!isInTrade) {
                        val sizeUsd = portfolio.totalEquityUsd * decision.positionSizePercent
                        val btcSize = sizeUsd.divide(decision.entryPrice, 8, RoundingMode.HALF_UP)
                        println("  [EXEC] TREND: Size \$${sizeUsd.setScale(2, RoundingMode.HALF_UP)} = ${btcSize.setScale(8, RoundingMode.HALF_UP)} BTC")

                        bracketOrderRepository.placeBracketOrder(
                            productId, decision.direction, btcSize,
                            decision.entryPrice, decision.takeProfit, decision.stopLoss
                        ).getOrThrow()
                        ExecutionResult.Success("Trend: Opened position.")
                    } else {
                        println("  [EXEC] TREND: Skipped (already in trade)")
                        ExecutionResult.Skipped("Trend: Already in trade.")
                    }
                }
                is Decision.Range -> {
                    if (!isInTrade) {
                        val sizeUsd = portfolio.totalEquityUsd * decision.positionSizePercentPerLevel
                        var ordersPlaced = 0
                        println("  [EXEC] RANGE: \$${sizeUsd.setScale(2, RoundingMode.HALF_UP)} per level × ${decision.levels} levels | Spacing: \$${decision.gridSpacing.setScale(2, RoundingMode.HALF_UP)}")

                        for (level in 1..decision.levels) {
                            val levelPrice = currentPrice - (decision.gridSpacing * BigDecimal(level))
                            val btcSize = sizeUsd.divide(levelPrice, 8, RoundingMode.HALF_UP)

                            if (level == 1) {
                                println("  [EXEC] Grid level 1: BUY ${btcSize.setScale(8, RoundingMode.HALF_UP)} BTC @ \$${levelPrice.setScale(2, RoundingMode.HALF_UP)}")
                            }

                            exchangeRepository.placeLimitOrder(
                                productId, OrderSide.BUY, btcSize, levelPrice, true
                            ).onSuccess { ordersPlaced++ }
                        }

                        ExecutionResult.Success("Range: Placed $ordersPlaced/${decision.levels} grid orders.")
                    } else if (hasBtcBalance && openOrders.none { it.side == OrderSide.SELL }) {
                        val targetProfitPrice = currentPrice + decision.gridSpacing
                        exchangeRepository.placeLimitOrder(productId, OrderSide.SELL, btcBalance, targetProfitPrice, true).getOrThrow()
                        ExecutionResult.Success("Range: Placed take-profit for grid fill.")
                    } else {
                        ExecutionResult.Skipped("Range: Active.")
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
}
