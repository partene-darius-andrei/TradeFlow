package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Order placed on the exchange with current status and fill information.
 *
 * **Lifecycle:** PENDING → OPEN → FILLED (or CANCELLED/FAILED)
 *
 * **Example Order (Limit Buy):**
 * ```
 * ID: exch-12345-67890
 * Product: BTC-USD
 * Side: BUY
 * Type: LIMIT
 * Status: OPEN
 * Size: 0.001 BTC
 * Price: $95,000/BTC
 * Filled: 0 BTC (not filled yet)
 * Created: 2025-01-10T12:00:00Z
 * ```
 *
 * **Order Flow:**
 * 1. TradeOrchestrator creates Decision (Trend or Range)
 * 2. Orchestrator calls ExchangeRepository.placeLimitOrder()
 * 3. Exchange returns Order with id and PENDING/OPEN status
 * 4. Order sits in order book waiting for fill
 * 5. Price hits limit → Order fills → status becomes FILLED
 * 6. Or: Order canceled → status becomes CANCELLED
 *
 * **Why Track Orders:**
 * - Prevent placing duplicate orders ("already in trade" check)
 * - Cancel stale orders (defense mode cleanup)
 * - Monitor fill rates (backtesting metrics)
 * - Calculate P&L (filled price vs entry/exit)
 *
 * **Usage:**
 * ```kotlin
 * val openOrders = exchangeRepository.getOpenOrders("BTC-USD").getOrThrow()
 * val hasBuyOrders = openOrders.any { it.side == OrderSide.BUY }
 * if (hasBuyOrders) {
 *     // Don't place more buy orders, already have some open
 * }
 * ```
 *
 * @property id Exchange-assigned order ID.
 *           Unique identifier for this order on the exchange.
 *           Format varies by exchange (e.g., "exch-uuid" for Coinbase).
 *
 * @property clientOrderId Client-assigned order ID (optional, may be empty).
 *           Used to correlate exchange orders with internal tracking.
 *           Not used by this system currently.
 *
 * @property productId Trading pair (e.g., "BTC-USD").
 *           Indicates which market this order is for.
 *
 * @property side Order direction: BUY (long) or SELL (close/short).
 *           BUY orders increase exposure, SELL orders decrease it.
 *
 * @property type Order execution type: MARKET, LIMIT, or BRACKET.
 *           - MARKET: Execute immediately at current price
 *           - LIMIT: Execute only at specified price or better
 *           - BRACKET: Combined order (entry + stop + target) - placeholder, not fully implemented
 *
 * @property status Current order state: PENDING, OPEN, FILLED, CANCELLED, or FAILED.
 *           - PENDING: Just created, not yet on exchange
 *           - OPEN: Active in order book, waiting for fill
 *           - FILLED: Fully executed
 *           - CANCELLED: Canceled before fill
 *           - FAILED: Rejected by exchange
 *
 * @property size Order quantity in base currency (BTC for BTC-USD).
 *           Total amount requested, before fills.
 *           Unit: BTC for BTC-USD pair.
 *
 * @property price Limit price (for LIMIT orders), null for MARKET orders.
 *           Price at which order will execute.
 *           Unit: USD per BTC.
 *
 * @property filledSize Amount already filled (partially filled orders).
 *           If filledSize < size, order is partially filled.
 *           If filledSize == size, order is FILLED.
 *           Unit: BTC for BTC-USD pair.
 *
 * @property avgFilledPrice Average price at which filled portion executed.
 *           Null if not filled yet (filledSize == 0).
 *           Used to calculate actual entry/exit prices for P&L.
 *           Unit: USD per BTC.
 *
 * @property createdAt Timestamp when order was created.
 *           Used for order age tracking and logging.
 *
 * @see OrderSide for BUY vs SELL
 * @see OrderType for MARKET vs LIMIT vs BRACKET
 * @see OrderStatus for lifecycle states
 */
data class Order(
    val id: String,
    val clientOrderId: String,
    val productId: String,
    val side: OrderSide,
    val type: OrderType,
    val status: OrderStatus,
    val size: BigDecimal,
    val price: BigDecimal?,
    val filledSize: BigDecimal,
    val avgFilledPrice: BigDecimal?,
    val createdAt: Instant
)

/**
 * Order direction: BUY (open long position) or SELL (close long position).
 *
 * **BUY:**
 * - Purchase BTC with USD
 * - Opens or adds to long position
 * - Increases BTC exposure
 * - Used in Trend mode (directional long) and Range mode (grid buy levels)
 *
 * **SELL:**
 * - Sell BTC for USD
 * - Closes or reduces long position
 * - Decreases BTC exposure
 * - Used in Defense mode (liquidate holdings) and Range mode (take-profit after grid fill)
 *
 * **Example:**
 * ```kotlin
 * val buyOrder = Order(
 *     ...,
 *     side = OrderSide.BUY,  // Opening long position
 *     size = BigDecimal("0.001"),
 *     price = BigDecimal("95000")
 * )
 * ```
 *
 * **Risk Management:**
 * Only BUY orders are checked against total exposure limit (maxTotalExposurePercent).
 * SELL orders reduce exposure, so no limit check needed.
 *
 * @see Order for the order data structure
 */
enum class OrderSide {
    /** Purchase base currency (BTC) with quote currency (USD). Opens or adds to long position. */
    BUY,

    /** Sell base currency (BTC) for quote currency (USD). Closes or reduces long position. */
    SELL
}

/**
 * Order execution type: how and when the order executes.
 *
 * **Three Types:**
 *
 * **MARKET:**
 * - Executes immediately at current market price
 * - Guaranteed fill (in liquid markets)
 * - No control over execution price (may get slippage)
 * - Used for urgent exits (defense mode liquidation, circuit breaker)
 *
 * **LIMIT:**
 * - Executes only at specified price or better
 * - May not fill immediately (or ever)
 * - Full control over execution price
 * - Used for entries (trend bracket entry, grid levels) and take-profits
 *
 * **BRACKET (Placeholder):**
 * - Combined order: entry + stop-loss + take-profit
 * - Automatically creates all three orders in one request
 * - **NOT fully implemented** in this system (placeholder for future)
 * - Currently handled manually via BracketOrderRepository
 *
 * **Example:**
 * ```kotlin
 * // Market order (immediate execution)
 * val marketOrder = Order(..., type = OrderType.MARKET, price = null)
 *
 * // Limit order (execute at $95,000 or better)
 * val limitOrder = Order(..., type = OrderType.LIMIT, price = BigDecimal("95000"))
 * ```
 *
 * **When to Use Each:**
 * - **MARKET:** Emergency liquidation, circuit breaker, guaranteed exit
 * - **LIMIT:** Normal entries/exits, patient fills, avoid slippage
 * - **BRACKET:** Future feature for one-shot entry+stop+target orders
 *
 * @see Order for the order data structure
 */
enum class OrderType {
    /** Executes immediately at current market price. No price parameter (price = null). */
    MARKET,

    /** Executes only at specified limit price or better. Requires price parameter. */
    LIMIT,

    /** Bracket order (entry + stop + target). Placeholder, not fully implemented. */
    BRACKET
}

/**
 * Order lifecycle status from creation to completion.
 *
 * **Lifecycle Flow:**
 * ```
 * PENDING → OPEN → FILLED
 *    ↓         ↓
 * FAILED   CANCELLED
 * ```
 *
 * **Five States:**
 *
 * **PENDING:**
 * - Order just created, not yet confirmed by exchange
 * - Brief transitional state (milliseconds to seconds)
 * - May transition to OPEN (success) or FAILED (rejected)
 *
 * **OPEN:**
 * - Order active in exchange order book
 * - Waiting for market price to reach limit price (for LIMIT orders)
 * - Can be canceled while in this state
 * - Can partially fill (filledSize < size)
 *
 * **FILLED:**
 * - Order fully executed (filledSize == size)
 * - Terminal state (order lifecycle complete)
 * - avgFilledPrice contains actual execution price
 * - Cannot be modified or canceled
 *
 * **CANCELLED:**
 * - Order canceled before full execution
 * - May have partial fills (filledSize > 0 but < size)
 * - Terminal state (order lifecycle complete)
 * - Canceled by user request or exchange (e.g., IOC timeout)
 *
 * **FAILED:**
 * - Order rejected by exchange
 * - Never entered order book (no fills)
 * - Terminal state (order lifecycle complete)
 * - Common reasons: insufficient funds, invalid price, rate limit
 *
 * **Example Usage:**
 * ```kotlin
 * val openOrders = allOrders.filter { it.status == OrderStatus.OPEN }
 * val filledOrders = allOrders.filter { it.status == OrderStatus.FILLED }
 * ```
 *
 * **In Backtesting:**
 * SimulatedExchange simulates state transitions:
 * - LIMIT orders start as OPEN
 * - MARKET orders start as FILLED
 * - Orders fill when price crosses limit price
 * - Canceled orders transition to CANCELLED
 *
 * @see Order for the order data structure
 */
enum class OrderStatus {
    /** Just created, not yet confirmed by exchange. Transitional state. */
    PENDING,

    /** Active in order book, waiting for fill. Can be canceled. */
    OPEN,

    /** Fully executed. Terminal state. */
    FILLED,

    /** Canceled before full execution. May have partial fills. Terminal state. */
    CANCELLED,

    /** Rejected by exchange. No fills. Terminal state. */
    FAILED
}
