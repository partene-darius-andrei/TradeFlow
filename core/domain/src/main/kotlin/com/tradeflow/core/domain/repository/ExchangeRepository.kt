package com.tradeflow.core.domain.repository

import com.tradeflow.core.domain.model.*
import java.math.BigDecimal

/**
 * Repository interface for exchange operations (market data, account, orders).
 *
 * **Purpose:** Abstracts exchange API from domain logic. Domain layer depends on this
 * interface, while data layer (CoinbaseRepository, SimulatedExchange) implements it.
 *
 * **Two Implementations:**
 * 1. **CoinbaseRepository:** Real Coinbase API integration (production)
 * 2. **SimulatedExchange:** In-memory backtesting engine (testing)
 *
 * **Why Interface:**
 * - Domain layer doesn't know/care which exchange is used
 * - Easy to swap Coinbase for Kraken, Binance, or simulated exchange
 * - Enables backtesting without hitting real API
 * - Dependency inversion: domain defines contract, data layer implements
 *
 * **Method Categories:**
 * 1. **Account:** getBalances(), getPortfolio()
 * 2. **Market Data:** getCandles(), getCurrentPrice()
 * 3. **Order Placement:** placeMarketOrder(), placeLimitOrder()
 * 4. **Order Management:** cancelOrder(), getOpenOrders(), getOrder()
 *
 * **Error Handling:**
 * All methods return Result<T> instead of throwing exceptions:
 * - Success: Result.success(value)
 * - Failure: Result.failure(ExchangeError.*)
 *
 * **Example Usage (Production):**
 * ```kotlin
 * val repository: ExchangeRepository = CoinbaseRepository(...)
 * val portfolio = repository.getPortfolio().getOrThrow()
 * val candles = repository.getCandles("BTC-USD", FOUR_HOUR).getOrThrow()
 * ```
 *
 * **Example Usage (Backtesting):**
 * ```kotlin
 * val repository: ExchangeRepository = SimulatedExchange(historicalCandles)
 * val portfolio = repository.getPortfolio().getOrThrow()  // Simulated balance
 * val order = repository.placeLimitOrder(...).getOrThrow()  // Simulated order
 * ```
 *
 * @see CoinbaseRepository for production implementation
 * @see SimulatedExchange for backtesting implementation
 * @see BracketOrderRepository for bracket order extension
 */
interface ExchangeRepository {
    /**
     * Fetches current balances for all currencies in the account.
     *
     * Returns a list of Balance objects showing available and held amounts
     * for each currency (BTC, USD, USDT, etc.).
     *
     * **Example Response:**
     * ```
     * [
     *   Balance(currency="BTC", available=0.01234567, hold=0.001),
     *   Balance(currency="USD", available=500.00, hold=0.0)
     * ]
     * ```
     *
     * **When to Use:**
     * - Need raw balance data for specific currencies
     * - Building custom portfolio calculations
     *
     * **Prefer getPortfolio() Instead:**
     * Most use cases should use getPortfolio() which includes total equity.
     *
     * @return Result<List<Balance>> on success, or ExchangeError on failure.
     *
     * @see getPortfolio for portfolio with total equity calculation
     */
    suspend fun getBalances(): Result<List<Balance>>

    /**
     * Fetches complete portfolio state with total USD-denominated equity.
     *
     * Returns Portfolio object containing:
     * - All currency balances
     * - Total equity converted to USD
     * - Timestamp of snapshot
     *
     * **Total Equity Calculation:**
     * ```
     * totalEquityUsd = (BTC balance × BTC/USD price) + USD balance + USDT balance
     * ```
     *
     * **Example Response:**
     * ```
     * Portfolio(
     *   balances = [Balance("BTC", 0.01234567, 0.001), Balance("USD", 500.00, 0.0)],
     *   totalEquityUsd = 1672.84,  // (0.01234567 × $95000) + $500
     *   timestamp = 2025-01-10T12:00:00Z
     * )
     * ```
     *
     * **Usage:**
     * This is the PRIMARY method for getting account state. Used by:
     * - TradeOrchestrator for position sizing
     * - RiskManager for drawdown calculation
     * - Adaptive profile switching
     *
     * @return Result<Portfolio> on success, or ExchangeError on failure.
     */
    suspend fun getPortfolio(): Result<Portfolio>

    /**
     * Fetches historical candlestick (OHLCV) data for technical analysis.
     *
     * Returns a list of Candle objects in chronological order (oldest first).
     * Used by AnalyzeCandlesUseCase to calculate SMA, ADX, ATR indicators.
     *
     * **Example Request:**
     * ```
     * getCandles("BTC-USD", Granularity.FOUR_HOUR, limit = 250)
     * ```
     *
     * **Example Response:**
     * ```
     * [
     *   Candle(timestamp=2025-01-01T00:00:00Z, open=94000, high=95000, low=93500, close=94500, volume=123.45),
     *   Candle(timestamp=2025-01-01T04:00:00Z, open=94500, high=96000, low=94000, close=95500, volume=234.56),
     *   ...
     *   Candle(timestamp=2025-01-10T08:00:00Z, open=95000, high=97000, low=94500, close=96500, volume=150.5)
     * ]
     * // 250 candles × 4 hours = 1000 hours of data
     * ```
     *
     * **Minimum Candles Required:**
     * - 200+ for SMA200 calculation
     * - MakeTradingDecisionUseCase rejects decisions if insufficient candles
     *
     * **Granularity Options:**
     * - FOUR_HOUR (default, used by this strategy)
     * - ONE_DAY, ONE_HOUR, etc. (configurable)
     *
     * @param productId Trading pair (e.g., "BTC-USD").
     * @param granularity Candle timeframe (e.g., FOUR_HOUR).
     * @param limit Max candles to return (default: 350).
     *              More candles = more historical context but slower API call.
     *
     * @return Result<List<Candle>> in chronological order, or ExchangeError on failure.
     */
    suspend fun getCandles(
        productId: String,
        granularity: Granularity,
        limit: Int = 350
    ): Result<List<Candle>>

    /**
     * Fetches current real-time market ticker (price, bid/ask, volume).
     *
     * Returns Ticker with most recent trade price and order book best bid/ask.
     * Used for position sizing and order placement at current market price.
     *
     * **Example Response:**
     * ```
     * Ticker(
     *   productId = "BTC-USD",
     *   price = 95123.45,      // Last trade price
     *   bid = 95100.00,        // Best buy order
     *   ask = 95150.00,        // Best sell order
     *   volume24h = 1234.56,   // 24h volume in BTC
     *   timestamp = 2025-01-10T12:34:56Z
     * )
     * ```
     *
     * **Usage:**
     * ```kotlin
     * val ticker = repository.getCurrentPrice("BTC-USD").getOrThrow()
     * val positionSizeUsd = portfolio.totalEquityUsd * 0.0523
     * val positionSizeBtc = positionSizeUsd / ticker.price
     * ```
     *
     * @param productId Trading pair (e.g., "BTC-USD").
     *
     * @return Result<Ticker> on success, or ExchangeError on failure.
     */
    suspend fun getCurrentPrice(productId: String): Result<Ticker>

    /**
     * Places a market order (executes immediately at current market price).
     *
     * Market orders guarantee execution but not price. Use for urgent exits
     * (defense mode liquidation, circuit breaker emergency stop).
     *
     * **Execution:**
     * - BUY: Executes at ask price (or slightly higher if slippage)
     * - SELL: Executes at bid price (or slightly lower if slippage)
     * - Fills immediately (in liquid markets)
     *
     * **Example (Emergency Sell):**
     * ```kotlin
     * val btcBalance = portfolio.getBtcBalance()
     * repository.placeMarketOrder("BTC-USD", OrderSide.SELL, btcBalance)
     * // Sells all BTC immediately at market price
     * ```
     *
     * **When to Use:**
     * - Defense mode: Liquidate holdings when price < SMA200
     * - Circuit breaker: Emergency exit on max drawdown
     * - Guaranteed exit needed (don't care about exact price)
     *
     * **When NOT to Use:**
     * - Normal entries/exits (use limit orders to avoid slippage)
     * - Large orders (may cause significant slippage)
     *
     * @param productId Trading pair (e.g., "BTC-USD").
     * @param side BUY or SELL.
     * @param size Order quantity in base currency (BTC).
     *
     * @return Result<Order> with FILLED or PENDING status, or ExchangeError on failure.
     */
    suspend fun placeMarketOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal
    ): Result<Order>

    /**
     * Places a limit order (executes only at specified price or better).
     *
     * Limit orders guarantee price but not execution. Use for patient entries/exits
     * where getting a good price matters more than immediate execution.
     *
     * **Execution:**
     * - BUY limit: Executes at limitPrice or LOWER (better for buyer)
     * - SELL limit: Executes at limitPrice or HIGHER (better for seller)
     * - May not fill if price never reaches limit
     *
     * **Post-Only Mode (default):**
     * - postOnly = true: Order becomes maker (pays lower fees)
     * - If order would execute immediately, it's REJECTED instead
     * - Prevents accidentally paying taker fees
     *
     * **Example (Grid Order):**
     * ```kotlin
     * repository.placeLimitOrder(
     *     productId = "BTC-USD",
     *     side = OrderSide.BUY,
     *     size = BigDecimal("0.001"),
     *     price = BigDecimal("93500"),  // Buy at $93,500 or lower
     *     postOnly = true  // Maker order, lower fees
     * )
     * ```
     *
     * **When to Use:**
     * - Trend mode: Entry orders at specific price
     * - Range mode: Grid orders below current price
     * - Take-profit orders above entry price
     * - Any situation where price control > immediacy
     *
     * @param productId Trading pair (e.g., "BTC-USD").
     * @param side BUY or SELL.
     * @param size Order quantity in base currency (BTC).
     * @param price Limit price (USD per BTC).
     * @param postOnly If true, order rejected if would execute immediately (default: true).
     *
     * @return Result<Order> with OPEN status, or ExchangeError on failure.
     */
    suspend fun placeLimitOrder(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        price: BigDecimal,
        postOnly: Boolean = true
    ): Result<Order>

    /**
     * Cancels a single open order by ID.
     *
     * **Example:**
     * ```kotlin
     * repository.cancelOrder("order-12345-67890")
     * ```
     *
     * **When to Use:**
     * - Cancel specific stale order
     * - Defense mode: Cancel individual BUY orders
     *
     * **Error Cases:**
     * - Order already filled: Returns error
     * - Order doesn't exist: Returns error
     * - Network error: Returns ExchangeError.NetworkError
     *
     * @param orderId Exchange order ID to cancel.
     *
     * @return Result<Unit> on success, or ExchangeError on failure.
     */
    suspend fun cancelOrder(orderId: String): Result<Unit>

    /**
     * Cancels multiple orders in a batch operation.
     *
     * More efficient than calling cancelOrder() multiple times.
     * Continues canceling even if some orders fail.
     *
     * **Example:**
     * ```kotlin
     * val openOrders = repository.getOpenOrders("BTC-USD").getOrThrow()
     * val buyOrderIds = openOrders.filter { it.side == OrderSide.BUY }.map { it.id }
     * repository.cancelOrders(buyOrderIds)  // Cancel all buy orders
     * ```
     *
     * **When to Use:**
     * - Defense mode: Cancel all BUY orders at once
     * - Circuit breaker: Cancel all open orders
     * - Strategy switch: Clear old orders before placing new ones
     *
     * @param orderIds List of order IDs to cancel.
     *
     * @return Result<Int> with count of successfully canceled orders, or ExchangeError on failure.
     */
    suspend fun cancelOrders(orderIds: List<String>): Result<Int>

    /**
     * Fetches all open orders for a specific product.
     *
     * Returns orders with status = OPEN (active in order book).
     * Used to check if already in a trade before placing new orders.
     *
     * **Example:**
     * ```kotlin
     * val openOrders = repository.getOpenOrders("BTC-USD").getOrThrow()
     * val hasBuyOrders = openOrders.any { it.side == OrderSide.BUY }
     * if (hasBuyOrders) {
     *     // Already have buy orders, don't place more
     * }
     * ```
     *
     * **Usage in TradeOrchestrator:**
     * - Check `openOrders.isNotEmpty()` to determine "in trade" status
     * - Prevents duplicate order placement
     *
     * @param productId Trading pair (e.g., "BTC-USD").
     *
     * @return Result<List<Order>> with OPEN status only, or ExchangeError on failure.
     */
    suspend fun getOpenOrders(productId: String): Result<List<Order>>

    /**
     * Fetches a specific order by ID.
     *
     * Returns order with current status (OPEN, FILLED, CANCELLED, etc.).
     * Used to check order status after placement or monitor fills.
     *
     * **Example:**
     * ```kotlin
     * val order = repository.getOrder("order-12345-67890").getOrThrow()
     * if (order.status == OrderStatus.FILLED) {
     *     println("Order filled at ${order.avgFilledPrice}")
     * }
     * ```
     *
     * @param orderId Exchange order ID.
     *
     * @return Result<Order> with current status, or ExchangeError on failure.
     */
    suspend fun getOrder(orderId: String): Result<Order>

    /**
     * Places a bracket order: entry + take-profit + stop-loss in one operation.
     *
     * @param productId Trading pair (e.g., "BTC-USD")
     * @param side Order side (BUY for long, SELL for short)
     * @param size Position size in base currency
     * @param entryPrice Limit price for entry order
     * @param takeProfit Price target for profit taking
     * @param stopLoss Price level for stop loss
     *
     * @return Result<Order> entry order on success, or ExchangeError on failure
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
