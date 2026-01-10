package com.tradeflow.core.domain.risk.model

import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderType
import java.math.BigDecimal

/**
 * Order placement request containing all parameters needed to validate and place an order.
 *
 * **Purpose:** Packages order parameters into a single object for risk validation.
 * Passed to RiskManager.validateOrder() before placing the actual order.
 *
 * **Order Types:**
 * - **MARKET:** Executes immediately at current market price (price = null)
 * - **LIMIT:** Executes only at specified price or better (price required)
 *
 * **Order Sides:**
 * - **BUY:** Purchase BTC with USD (opens long position)
 * - **SELL:** Sell BTC for USD (closes long position)
 *
 * **Example (Limit Buy Order):**
 * ```kotlin
 * val request = PlaceOrderRequest(
 *     productId = "BTC-USD",
 *     side = OrderSide.BUY,
 *     type = OrderType.LIMIT,
 *     size = BigDecimal("0.001"),  // 0.001 BTC
 *     price = BigDecimal("95000"),  // Buy at $95,000/BTC
 *     stopLoss = BigDecimal("90000"),  // Optional stop
 *     takeProfit = BigDecimal("105000")  // Optional target
 * )
 * ```
 *
 * **Example (Market Sell Order):**
 * ```kotlin
 * val request = PlaceOrderRequest(
 *     productId = "BTC-USD",
 *     side = OrderSide.SELL,
 *     type = OrderType.MARKET,
 *     size = BigDecimal("0.01234567"),  // Sell all BTC
 *     price = null  // Market order, no price specified
 * )
 * ```
 *
 * **Usage Flow:**
 * ```kotlin
 * val request = PlaceOrderRequest(...)
 *
 * // 1. Validate against risk limits
 * val riskCheck = riskManager.validateOrder(request, portfolio, currentPrice)
 *
 * // 2. Place order only if approved
 * when (riskCheck) {
 *     is RiskCheck.Approved -> exchangeRepository.placeLimitOrder(request)
 *     is RiskCheck.Rejected -> log.warn("Order rejected: ${riskCheck.reason}")
 * }
 * ```
 *
 * **Stop Loss / Take Profit (Optional):**
 * For bracket orders (entry + stop + target), specify stopLoss and takeProfit.
 * RiskManager validates these are on the correct side of entry price.
 *
 * @property productId Trading pair identifier (e.g., "BTC-USD").
 *           Must be a valid product ID supported by the exchange.
 *
 * @property side Order direction: BUY (long) or SELL (close).
 *           Determines whether this increases or decreases exposure.
 *
 * @property type Order execution type: MARKET (immediate) or LIMIT (at specific price).
 *           LIMIT orders require non-null price parameter.
 *
 * @property size Order quantity in base currency (BTC for BTC-USD).
 *           Must be positive and above exchange dust threshold (typically 0.00001 BTC).
 *           Unit: BTC for BTC-USD pair.
 *
 * @property price Limit price for LIMIT orders, null for MARKET orders.
 *           For LIMIT orders: Order executes only at this price or better.
 *           For MARKET orders: Must be null, executes at current market price.
 *           Unit: USD per BTC.
 *
 * @property stopLoss Optional stop-loss price for bracket orders.
 *           If specified, RiskManager validates placement (must be on correct side).
 *           Unit: USD per BTC.
 *
 * @property takeProfit Optional take-profit price for bracket orders.
 *           If specified, RiskManager validates placement (must be on correct side).
 *           Unit: USD per BTC.
 *
 * @see RiskManager.validateOrder for how this is validated
 * @see OrderSide for BUY vs SELL
 * @see OrderType for MARKET vs LIMIT
 */
data class PlaceOrderRequest(
    val productId: String,
    val side: OrderSide,
    val type: OrderType,
    val size: BigDecimal,
    val price: BigDecimal?,
    val stopLoss: BigDecimal? = null,
    val takeProfit: BigDecimal? = null
)
