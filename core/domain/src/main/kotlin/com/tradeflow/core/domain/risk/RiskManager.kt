package com.tradeflow.core.domain.risk

import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.risk.model.DrawdownStatus
import com.tradeflow.core.domain.risk.model.PlaceOrderRequest
import com.tradeflow.core.domain.risk.model.RiskCheck
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Risk management service that validates orders and monitors portfolio drawdown.
 *
 * **Responsibility:** THE GUARDIAN of capital. Prevents the strategy from:
 * 1. Taking excessively large positions (position size limits)
 * 2. Accumulating too much total exposure (total exposure limits)
 * 3. Suffering catastrophic losses (drawdown monitoring + circuit breaker)
 * 4. Placing orders with invalid parameters
 *
 * **Three Core Functions:**
 * 1. **Order Validation:** Check if a proposed order violates risk limits
 * 2. **Drawdown Monitoring:** Track portfolio decline from peak (high-water mark)
 * 3. **Position Sizing:** Calculate safe position sizes for Trend and Grid trades
 *
 * **Risk Hierarchy (Defense in Depth):**
 * ```
 * Level 1: Per-Position Limit (maxPositionPercent = 5%)
 *   ↓
 * Level 2: Total Exposure Limit (maxTotalExposurePercent = 10%)
 *   ↓
 * Level 3: Drawdown Warning (drawdownWarningPercent = 12%)
 *   ↓
 * Level 4: CIRCUIT BREAKER (maxDrawdownPercent = 15%)
 *   └─ Trading HALTS, all positions liquidated
 * ```
 *
 * **Why Multiple Risk Layers:**
 * - **Per-position limit:** Prevents a single bad trade from destroying the portfolio
 * - **Total exposure limit:** Prevents accumulating too many simultaneous positions
 * - **Drawdown warning:** Early alert that strategy is underperforming
 * - **Circuit breaker:** Last resort emergency stop to prevent catastrophic loss
 *
 * **Example Risk Limits (BALANCED Profile):**
 * - Max position size: 5% of portfolio ($50 max on $1000 portfolio)
 * - Max total exposure: 10% of portfolio ($100 total across all positions)
 * - Drawdown warning: 12% decline from peak
 * - Circuit breaker: 15% decline from peak (EMERGENCY STOP)
 *
 * **Order Validation Logic:**
 * Before placing any order, RiskManager checks:
 * 1. **Portfolio validity:** Equity must be positive (can't validate with zero balance)
 * 2. **Position size check:** Order value ≤ maxPositionPercent
 * 3. **Exposure check (BUY orders only):**
 *    - Calculate current BTC exposure: `btcBalance × price / equity`
 *    - Calculate new exposure: `current + new order`
 *    - Reject if new exposure > maxTotalExposurePercent
 *
 * **Why Only BUY Orders Checked for Exposure:**
 * - BUY orders INCREASE exposure (add risk)
 * - SELL orders DECREASE exposure (reduce risk)
 * - We only limit increases to prevent overexposure
 *
 * **Drawdown Monitoring:**
 * Drawdown measures portfolio decline from its peak (high-water mark):
 * ```kotlin
 * drawdown = (highWaterMark - currentEquity) / highWaterMark
 * ```
 *
 * **Drawdown States:**
 * - **Normal (< 12%):** Portfolio healthy, continue trading normally
 * - **Warning (12-15%):** Portfolio underperforming, log warning, continue cautiously
 * - **Limit Breached (≥ 15%):** EMERGENCY - circuit breaker triggers, liquidate all
 *
 * **Example Drawdown Scenarios:**
 *
 * **Scenario 1: Normal Drawdown**
 * - High-water mark: $1000 (peak portfolio value)
 * - Current equity: $950
 * - Drawdown: 5% → Normal, continue trading
 *
 * **Scenario 2: Warning Drawdown**
 * - High-water mark: $1000
 * - Current equity: $880
 * - Drawdown: 12% → Warning logged, continue trading
 *
 * **Scenario 3: Circuit Breaker**
 * - High-water mark: $1000
 * - Current equity: $850
 * - Drawdown: 15% → EMERGENCY STOP
 * - Action: TradeOrchestrator cancels all orders + liquidates all BTC + halts trading
 *
 * **Position Sizing Methods:**
 *
 * **Trend Position Sizing:**
 * - Single large position (default: 5.23% of portfolio for BALANCED)
 * - Calculation: `portfolioEquity × maxPositionPercent / entryPrice`
 * - Example: $1000 × 5.23% / $95,000 = 0.00055053 BTC
 *
 * **Grid Position Sizing:**
 * - Multiple small positions (default: 3 levels)
 * - Total exposure split across levels
 * - Calculation: `(portfolioEquity × maxTotalExposurePercent) / gridLevels / entryPrice`
 * - Example: ($1000 × 10%) / 3 levels / $95,000 = 0.00035088 BTC per level
 * - Total: 3 × 0.00035088 = 0.00105264 BTC (10% of portfolio)
 *
 * **Grid Spacing Validation:**
 * Ensures grid levels aren't placed too close together (prevents order spam).
 * Minimum spacing: 1.5% of current price (or ATR-based, whichever is larger).
 *
 * **Thread Safety:**
 * Stateless and thread-safe. All calculations use input parameters only.
 * Safe to call from multiple threads simultaneously.
 *
 * **Configuration:**
 * All risk limits configured via TradingConfig:
 * - `config.risk.maxPositionPercent`: Per-position limit
 * - `config.risk.maxTotalExposurePercent`: Total exposure limit
 * - `config.risk.maxDrawdownPercent`: Circuit breaker threshold
 * - `config.risk.drawdownWarningPercent`: Warning threshold
 * - `config.risk.minGridSpacingPercent`: Minimum grid spacing
 * - `config.risk.btcDecimalPlaces`: BTC precision (8 = satoshi)
 * - `config.risk.percentDecimalPlaces`: Percentage precision (4)
 *
 * **Usage Example (Order Validation):**
 * ```kotlin
 * val request = PlaceOrderRequest(
 *     productId = "BTC-USD",
 *     side = OrderSide.BUY,
 *     type = OrderType.LIMIT,
 *     size = BigDecimal("0.001"),
 *     price = BigDecimal("95000")
 * )
 *
 * val riskCheck = riskManager.validateOrder(request, portfolio, currentPrice)
 * when (riskCheck) {
 *     is RiskCheck.Approved -> placeLimitOrder(request)
 *     is RiskCheck.Rejected -> log.warn("Order rejected: ${riskCheck.reason}")
 * }
 * ```
 *
 * **Usage Example (Drawdown Monitoring):**
 * ```kotlin
 * val status = riskManager.checkDrawdown(portfolio.totalEquityUsd, highWaterMark)
 * when (status) {
 *     is DrawdownStatus.Normal -> log.info("Drawdown: ${status.drawdownPercent}%")
 *     is DrawdownStatus.Warning -> log.warn("WARNING: ${status.drawdownPercent}% drawdown")
 *     is DrawdownStatus.LimitBreached -> {
 *         log.error("CIRCUIT BREAKER: ${status.drawdownPercent}% drawdown")
 *         liquidateAllPositions()
 *     }
 * }
 * ```
 *
 * **Usage Example (Position Sizing):**
 * ```kotlin
 * // Trend position
 * val trendSize = riskManager.calculateTrendPositionSize(portfolio, entryPrice)
 * placeBracketOrder(trendSize, entryPrice, stopLoss, takeProfit)
 *
 * // Grid positions
 * val gridSize = riskManager.calculateGridPositionSize(portfolio, 3, entryPrice)
 * for (level in 1..3) {
 *     placeLimitOrder(gridSize, entryPrice - spacing * level)
 * }
 * ```
 *
 * @property config Trading configuration containing all risk parameters.
 *           Risk limits (maxPositionPercent, maxDrawdownPercent, etc.) loaded from here.
 *
 * @see RiskCheck for the validation result type (Approved or Rejected)
 * @see DrawdownStatus for the drawdown monitoring result type (Normal, Warning, or LimitBreached)
 * @see PlaceOrderRequest for the order validation input type
 * @see TradingConfig.risk for risk parameter configuration
 */
class RiskManager constructor(
    private val config: TradingConfig
) {

    /**
     * Validates a proposed order against risk limits.
     *
     * **Checks Performed:**
     * 1. **Portfolio Validity:** Equity must be positive
     * 2. **Position Size Limit:** Order value ≤ maxPositionPercent of portfolio
     * 3. **Total Exposure Limit (BUY orders only):**
     *    - Current BTC exposure + new order ≤ maxTotalExposurePercent
     *
     * **Why BUY Orders Only for Exposure Check:**
     * - BUY orders INCREASE exposure (add new risk)
     * - SELL orders DECREASE exposure (reduce risk, close positions)
     * - We only need to limit increases to prevent overexposure
     *
     * **Validation Flow:**
     * ```
     * Is portfolio equity positive?
     *   NO → REJECT "portfolio equity is zero or negative"
     *   YES → Continue
     *   ↓
     * Calculate order value = size × price
     * Calculate position % = orderValue / portfolioEquity
     *   ↓
     * Is position % > maxPositionPercent?
     *   YES → REJECT "Position size X% exceeds limit Y%"
     *   NO → Continue
     *   ↓
     * Is this a BUY order?
     *   NO (SELL) → APPROVE (sell orders reduce exposure)
     *   YES → Continue to exposure check
     *   ↓
     * Calculate current BTC exposure = btcBalance × price / equity
     * Calculate new exposure = current + position %
     *   ↓
     * Is new exposure > maxTotalExposurePercent?
     *   YES → REJECT "Total exposure X% would exceed limit Y%"
     *   NO → APPROVE
     * ```
     *
     * **Example 1: Valid Order (Approved)**
     * - Portfolio equity: $1000
     * - Order: BUY 0.0005 BTC @ $95,000 = $47.50
     * - Position %: $47.50 / $1000 = 4.75%
     * - Max position: 5.23%
     * - Current BTC exposure: 0%
     * - New exposure: 4.75%
     * - Max total exposure: 10%
     * - Result: **APPROVED** (4.75% < 5.23% AND 4.75% < 10%)
     *
     * **Example 2: Position Too Large (Rejected)**
     * - Portfolio equity: $1000
     * - Order: BUY 0.001 BTC @ $95,000 = $95
     * - Position %: $95 / $1000 = 9.5%
     * - Max position: 5.23%
     * - Result: **REJECTED** "Position size 9.50% exceeds limit 5.23%"
     *
     * **Example 3: Total Exposure Too High (Rejected)**
     * - Portfolio equity: $1000
     * - Current BTC holdings: 0.0006 BTC @ $95,000 = $57
     * - Current exposure: 5.7%
     * - New order: BUY 0.0005 BTC @ $95,000 = $47.50 (4.75%)
     * - Position %: 4.75% (OK, < 5.23%)
     * - New total exposure: 5.7% + 4.75% = 10.45%
     * - Max total exposure: 10%
     * - Result: **REJECTED** "Total exposure 10.45% would exceed limit 10.00%"
     *
     * **Example 4: SELL Order (Approved Without Exposure Check)**
     * - Order: SELL 0.001 BTC @ $95,000 = $95
     * - Position %: 9.5% (exceeds 5.23% limit)
     * - But side = SELL
     * - Result: **APPROVED** (SELL orders don't increase exposure, no limit check)
     *
     * @param request Order parameters including side, size, price.
     *                If price is null (market order), currentPrice is used.
     *
     * @param portfolio Current portfolio state with balances and total equity.
     *                  Equity must be positive for validation to proceed.
     *
     * @param currentPrice Current market price, used if request.price is null.
     *                     Also used to calculate current BTC exposure value.
     *
     * @return RiskCheck.Approved if order passes all checks,
     *         RiskCheck.Rejected with reason string if any check fails.
     *
     * @see RiskCheck for the result type
     * @see PlaceOrderRequest for the input type
     * @see TradingConfig.risk for limit configuration
     */
    fun validateOrder(
        request: PlaceOrderRequest,
        portfolio: Portfolio,
        currentPrice: BigDecimal
    ): RiskCheck {
        if (portfolio.totalEquityUsd <= BigDecimal.ZERO) {
            return RiskCheck.Rejected("Cannot validate order: portfolio equity is zero or negative")
        }

        val orderPrice = request.price ?: currentPrice
        val orderValueUsd = request.size * orderPrice

        val positionPercent = orderValueUsd
            .divide(portfolio.totalEquityUsd, config.risk.percentDecimalPlaces, RoundingMode.HALF_UP)

        if (positionPercent > config.risk.maxPositionPercent) {
            return RiskCheck.Rejected(
                "Position size ${formatPercent(positionPercent)} exceeds limit ${formatPercent(config.risk.maxPositionPercent)}"
            )
        }

        if (request.side == OrderSide.BUY) {
            val currentBtcValue = portfolio.getBtcBalance() * currentPrice
            val currentExposure = currentBtcValue
                .divide(portfolio.totalEquityUsd, config.risk.percentDecimalPlaces, RoundingMode.HALF_UP)
            val newExposure = currentExposure + positionPercent

            if (newExposure > config.risk.maxTotalExposurePercent) {
                return RiskCheck.Rejected(
                    "Total exposure ${formatPercent(newExposure)} would exceed limit ${formatPercent(config.risk.maxTotalExposurePercent)}"
                )
            }
        }

        return RiskCheck.Approved
    }

    /**
     * Monitors portfolio drawdown and returns severity status.
     *
     * **Drawdown Definition:**
     * Percentage decline from peak portfolio value (high-water mark).
     * ```kotlin
     * drawdown = (highWaterMark - currentEquity) / highWaterMark
     * ```
     *
     * **Three Severity Levels:**
     * 1. **Normal (< 12%):** Portfolio healthy, continue trading normally
     * 2. **Warning (12-15%):** Portfolio underperforming, log warning, continue cautiously
     * 3. **Limit Breached (≥ 15%):** Circuit breaker triggered, EMERGENCY STOP
     *
     * **Why Monitor Drawdown:**
     * Prevents catastrophic losses by triggering circuit breaker when portfolio declines
     * beyond acceptable limits. This is the LAST LINE OF DEFENSE against runaway losses.
     *
     * **Circuit Breaker Mechanism:**
     * When drawdown ≥ maxDrawdownPercent (15% for BALANCED):
     * 1. TradeOrchestrator receives DrawdownStatus.LimitBreached
     * 2. Cancels ALL open orders immediately
     * 3. Sells ALL BTC holdings at market price
     * 4. Returns ExecutionResult.Failed("EMERGENCY: 15% Drawdown reached. Liquidated.")
     * 5. Trading HALTS until system is manually reset
     *
     * **High-Water Mark (HWM) Tracking:**
     * - HWM = peak portfolio value since trading started
     * - Updated after each cycle: `hwm = max(hwm, currentEquity)`
     * - Never decreases (even if portfolio drops)
     * - Used to measure maximum decline from peak
     *
     * **Example Scenarios:**
     *
     * **Scenario 1: Normal Operation (5% drawdown)**
     * - HWM: $1000 (peak value)
     * - Current equity: $950
     * - Drawdown: (1000 - 950) / 1000 = 5.0%
     * - Status: DrawdownStatus.Normal(0.05)
     * - Action: Continue trading normally
     *
     * **Scenario 2: Warning Level (13% drawdown)**
     * - HWM: $1000
     * - Current equity: $870
     * - Drawdown: (1000 - 870) / 1000 = 13.0%
     * - Status: DrawdownStatus.Warning(0.13)
     * - Action: Log warning, continue trading (but monitor closely)
     *
     * **Scenario 3: Circuit Breaker (16% drawdown)**
     * - HWM: $1000
     * - Current equity: $840
     * - Drawdown: (1000 - 840) / 1000 = 16.0%
     * - Status: DrawdownStatus.LimitBreached(0.16)
     * - Action: EMERGENCY - liquidate all positions, halt trading
     *
     * **Scenario 4: Zero HWM (first cycle)**
     * - HWM: $0 (no previous cycles)
     * - Current equity: $1000
     * - Drawdown: 0% (cannot calculate, HWM is zero)
     * - Status: DrawdownStatus.Normal(0.0)
     * - Action: Continue normally, HWM will update to $1000
     *
     * **Scenario 5: Recovery (portfolio recovered)**
     * - HWM: $1000 (old peak)
     * - Current equity: $1050 (new peak!)
     * - Drawdown: -5% (negative = above HWM)
     * - Handled in TradeOrchestrator: HWM updated to $1050
     * - Next cycle uses $1050 as new HWM
     *
     * **BALANCED Profile Thresholds:**
     * - Warning: 12% decline from peak
     * - Circuit breaker: 15% decline from peak
     *
     * **AGGRESSIVE Profile (higher risk tolerance):**
     * - Warning: 15% decline
     * - Circuit breaker: 20% decline
     *
     * **CONSERVATIVE Profile (lower risk tolerance):**
     * - Warning: 8% decline
     * - Circuit breaker: 10% decline
     *
     * @param currentEquity Current total portfolio value in USD.
     *                      If portfolio value increased, TradeOrchestrator updates HWM first.
     *
     * @param highWaterMark Peak portfolio value since trading started.
     *                      If zero (first cycle), drawdown is calculated as 0%.
     *
     * @return DrawdownStatus indicating severity:
     *         - Normal: Safe to continue trading
     *         - Warning: Log alert, continue cautiously
     *         - LimitBreached: Circuit breaker triggered, HALT trading
     *
     * @see DrawdownStatus for the result type
     * @see TradingConfig.risk.maxDrawdownPercent for circuit breaker threshold
     * @see TradingConfig.risk.drawdownWarningPercent for warning threshold
     */
    fun checkDrawdown(
        currentEquity: BigDecimal,
        highWaterMark: BigDecimal
    ): DrawdownStatus {
        val drawdown = if (highWaterMark > BigDecimal.ZERO) {
            (highWaterMark - currentEquity)
                .divide(highWaterMark, config.risk.percentDecimalPlaces, RoundingMode.HALF_UP)
                .toDouble()
        } else {
            0.0
        }

        return when {
            drawdown >= config.risk.maxDrawdownPercent ->
                DrawdownStatus.LimitBreached(drawdown)
            drawdown >= config.risk.drawdownWarningPercent ->
                DrawdownStatus.Warning(drawdown)
            else ->
                DrawdownStatus.Normal(drawdown)
        }
    }

    /**
     * Calculates position size for a single Trend trade.
     *
     * **Trend Strategy:**
     * - One large directional position (default: 5.23% of portfolio for BALANCED)
     * - Higher risk, higher reward vs grid strategy
     * - Used when ADX indicates strong trending market
     *
     * **Calculation:**
     * ```kotlin
     * positionSizeUsd = portfolio.totalEquityUsd × config.risk.maxPositionPercent
     * positionSizeBtc = positionSizeUsd / entryPrice
     * ```
     *
     * **Example (BALANCED Profile):**
     * - Portfolio equity: $1000
     * - Max position: 5.23% (from genetic algorithm optimization)
     * - Entry price: $95,000/BTC
     * - Position size USD: $1000 × 0.0523 = $52.30
     * - Position size BTC: $52.30 / $95,000 = 0.00055053 BTC
     *
     * **Why Percentage-Based:**
     * - Scales with portfolio size automatically
     * - Small portfolio ($100): 5.23% = $5.23 position
     * - Large portfolio ($10,000): 5.23% = $523 position
     * - Maintains consistent risk across all portfolio sizes
     *
     * **Risk Profiles:**
     * - AGGRESSIVE: ~7% per position (higher risk for growth)
     * - BALANCED: 5.23% (optimized via genetic algorithm)
     * - CONSERVATIVE: ~3% (capital preservation focus)
     * - ULTRA_CONSERVATIVE: ~1.5% (minimal risk)
     *
     * **Rounding:**
     * Result rounded to 8 decimal places (satoshi precision).
     * Example: 0.00055053 BTC (not 0.000550526315789474...)
     *
     * @param portfolio Current portfolio state with total equity.
     *                  Used to calculate percentage-based position size.
     *
     * @param entryPrice Planned entry price for the trend trade.
     *                   Used to convert USD position size to BTC quantity.
     *                   Unit: USD per BTC.
     *
     * @return Position size in BTC, rounded to 8 decimal places (satoshi precision).
     *         This is the BTC quantity to use in the bracket order.
     *
     * @see TradingConfig.risk.maxPositionPercent for the position size limit
     * @see calculateGridPositionSize for grid position sizing
     */
    fun calculateTrendPositionSize(
        portfolio: Portfolio,
        entryPrice: BigDecimal
    ): BigDecimal {
        val riskAmountUsd = portfolio.totalEquityUsd * config.risk.maxPositionPercent
        return riskAmountUsd
            .divide(entryPrice, config.risk.btcDecimalPlaces, RoundingMode.HALF_UP)
    }

    /**
     * Calculates position size PER LEVEL for a Grid trading strategy.
     *
     * **Grid Strategy:**
     * - Multiple small positions at different price levels (default: 3 levels)
     * - Lower risk per position, but higher total exposure
     * - Used when ADX indicates ranging/choppy market
     *
     * **Calculation:**
     * ```kotlin
     * totalExposureUsd = portfolio.totalEquityUsd × config.risk.maxTotalExposurePercent
     * perLevelUsd = totalExposureUsd / gridLevels
     * perLevelBtc = perLevelUsd / entryPrice
     * ```
     *
     * **Example (BALANCED Profile, 3 Grid Levels):**
     * - Portfolio equity: $1000
     * - Max total exposure: 10%
     * - Grid levels: 3
     * - Total exposure USD: $1000 × 0.10 = $100
     * - Per level USD: $100 / 3 = $33.33
     * - Entry price: $95,000/BTC
     * - Per level BTC: $33.33 / $95,000 = 0.00035088 BTC
     * - **Total grid exposure:** 3 × 0.00035088 = 0.00105264 BTC ($100)
     *
     * **Why Split Across Levels:**
     * - Distributes risk across multiple price points
     * - Lower risk per individual level vs single trend trade
     * - Increases fill probability (price likely to hit at least one level)
     * - Mean reversion strategy: buy dips, sell rallies
     *
     * **Comparison to Trend Position:**
     * - **Trend:** 1 position × 5.23% = 5.23% total exposure
     * - **Grid:** 3 positions × 3.33% = 10% total exposure
     * - Grid has HIGHER total exposure but LOWER per-position risk
     *
     * **Risk Profiles (3-level grid):**
     * - AGGRESSIVE: ~15% total / 5% per level
     * - BALANCED: 10% total / 3.33% per level
     * - CONSERVATIVE: ~6% total / 2% per level
     * - ULTRA_CONSERVATIVE: ~3% total / 1% per level
     *
     * **Grid Level Placement (example):**
     * - Current price: $95,000
     * - Grid spacing: $1,500 (1.58%)
     * - Level 1: BUY 0.00035088 BTC @ $93,500 (95k - 1.5k)
     * - Level 2: BUY 0.00035088 BTC @ $92,000 (95k - 3k)
     * - Level 3: BUY 0.00035088 BTC @ $90,500 (95k - 4.5k)
     *
     * **Rounding:**
     * Result rounded to 8 decimal places (satoshi precision).
     * Example: 0.00035088 BTC per level
     *
     * **Validation:**
     * Requires gridLevels > 0, throws IllegalArgumentException otherwise.
     *
     * @param portfolio Current portfolio state with total equity.
     *                  Used to calculate total grid exposure.
     *
     * @param gridLevels Number of grid levels (default: 3).
     *                   Total exposure is split evenly across all levels.
     *                   Must be positive (> 0).
     *
     * @param entryPrice Approximate entry price for the grid (typically current price).
     *                   Used to convert USD per-level size to BTC quantity.
     *                   Each level may have slightly different actual price.
     *
     * @return Position size PER LEVEL in BTC, rounded to 8 decimal places.
     *         To get total grid exposure: multiply by gridLevels.
     *
     * @throws IllegalArgumentException if gridLevels ≤ 0
     *
     * @see TradingConfig.risk.maxTotalExposurePercent for total grid exposure limit
     * @see calculateTrendPositionSize for single position sizing
     */
    fun calculateGridPositionSize(
        portfolio: Portfolio,
        gridLevels: Int,
        entryPrice: BigDecimal
    ): BigDecimal {
        require(gridLevels > 0) { "Grid levels must be positive" }

        val totalRiskUsd = portfolio.totalEquityUsd * config.risk.maxTotalExposurePercent
        val perLevelRiskUsd = totalRiskUsd
            .divide(BigDecimal(gridLevels), config.risk.btcDecimalPlaces, RoundingMode.HALF_UP)

        return perLevelRiskUsd
            .divide(entryPrice, config.risk.btcDecimalPlaces, RoundingMode.HALF_UP)
    }

    /**
     * Validates that grid spacing meets minimum threshold to prevent order spam.
     *
     * **Why Minimum Spacing Matters:**
     * - Too-close grid levels = many orders with tiny price differences
     * - Exchange may reject (spam detection)
     * - Excessive trading fees on fills
     * - Poor fill prices due to order book impact
     *
     * **Minimum Spacing (BALANCED Profile):**
     * - Default: 1.5% of current price
     * - Example @ $95,000: 1.5% = $1,425 minimum spacing
     * - This ensures meaningful price separation between levels
     *
     * **How Spacing is Determined:**
     * In MakeTradingDecisionUseCase, grid spacing is calculated as:
     * ```kotlin
     * gridSpacing = max(
     *     atr × minGridSpacingAtrMultiplier,  // Volatility-based
     *     currentPrice × minGridSpacingFloor  // Percentage-based floor
     * )
     * ```
     *
     * This method validates that the calculated spacing meets the minimum floor.
     *
     * **Example (Valid Spacing):**
     * - Current price: $95,000
     * - Calculated spacing: $1,500 (1.58%)
     * - Min spacing percent: 1.5% (config.risk.minGridSpacingPercent)
     * - Spacing percent: $1,500 / $95,000 = 0.0158 = 1.58%
     * - Result: **VALID** (1.58% ≥ 1.5%)
     *
     * **Example (Invalid Spacing):**
     * - Current price: $95,000
     * - Calculated spacing: $1,000 (1.05%)
     * - Min spacing percent: 1.5%
     * - Spacing percent: $1,000 / $95,000 = 0.0105 = 1.05%
     * - Result: **INVALID** (1.05% < 1.5%)
     * - MakeTradingDecisionUseCase would reject this grid configuration
     *
     * **When This is Used:**
     * Called by MakeTradingDecisionUseCase after calculating grid spacing to ensure
     * the spacing is wide enough before creating Range decision.
     *
     * @param spacingPercent Grid spacing as a percentage (e.g., 0.015 for 1.5%).
     *                       Calculated as: gridSpacing / currentPrice.
     *
     * @return true if spacing meets minimum threshold (spacingPercent ≥ minGridSpacingPercent),
     *         false otherwise.
     *
     * @see TradingConfig.risk.minGridSpacingPercent for the minimum threshold
     * @see Decision.Range.gridSpacing for how grid spacing is used
     */
    fun validateGridSpacing(spacingPercent: BigDecimal): Boolean {
        return spacingPercent >= config.risk.minGridSpacingPercent
    }

    /**
     * Formats a decimal percentage as a human-readable percentage string.
     *
     * **Conversion:**
     * - Input: Decimal (0.0523)
     * - Output: Percentage string ("5.23%")
     *
     * **Examples:**
     * - 0.0523 → "5.23%"
     * - 0.10 → "10.00%"
     * - 0.15 → "15.00%"
     * - 1.0 → "100.00%"
     *
     * **Used For:**
     * - Error messages in RiskCheck.Rejected
     * - Log messages in validation methods
     * - Human-readable output for debugging
     *
     * @param value Decimal percentage (0.0 to 1.0 or higher).
     *
     * @return Formatted percentage string with 2 decimal places and % suffix.
     */
    private fun formatPercent(value: BigDecimal): String {
        return "${(value * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)}%"
    }
}
