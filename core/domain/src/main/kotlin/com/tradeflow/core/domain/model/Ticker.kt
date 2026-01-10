package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Real-time market ticker showing current price, bid/ask spread, and 24-hour volume.
 *
 * **Purpose:** Provides snapshot of current market conditions for a trading pair.
 * Used to get current price for position sizing and order placement.
 *
 * **Example Ticker (BTC-USD):**
 * ```
 * Product: BTC-USD
 * Price: $95,123.45 (last trade price)
 * Bid: $95,100.00 (highest buy order)
 * Ask: $95,150.00 (lowest sell order)
 * Spread: $50 (ask - bid = 0.053% of price)
 * Volume 24h: 1,234.56 BTC (24-hour trading volume)
 * Timestamp: 2025-01-10T12:34:56Z
 * ```
 *
 * **Price vs Bid vs Ask:**
 * - **Price:** Last executed trade price (most recent market transaction)
 * - **Bid:** Highest price someone is willing to BUY at (best bid)
 * - **Ask:** Lowest price someone is willing to SELL at (best ask)
 * - **Spread:** Difference between ask and bid (ask - bid)
 *
 * **Bid-Ask Spread:**
 * The spread represents market liquidity and transaction cost:
 * - **Narrow spread ($10-50 for BTC):** Highly liquid, low transaction cost
 * - **Wide spread ($100+ for BTC):** Illiquid, high transaction cost
 *
 * If you place a MARKET order:
 * - BUY: Executes at ask price (pay slightly more)
 * - SELL: Executes at bid price (receive slightly less)
 *
 * **24-Hour Volume:**
 * Total BTC traded in the last 24 hours. Indicates market activity:
 * - High volume (1000+ BTC): Active market, good liquidity
 * - Low volume (< 100 BTC): Quiet market, potential liquidity issues
 *
 * **Usage in Position Sizing:**
 * ```kotlin
 * val ticker = exchangeRepository.getCurrentPrice("BTC-USD").getOrThrow()
 * val currentPrice = ticker.price
 * val positionSizeUsd = portfolio.totalEquityUsd * BigDecimal("0.0523")
 * val positionSizeBtc = positionSizeUsd / currentPrice
 * ```
 *
 * **Why Use Ticker Instead of Candle:**
 * - Ticker: Real-time current price (millisecond freshness)
 * - Candle: Historical price (4-hour granularity in this system)
 * - For order placement, use ticker for most current price
 * - For technical analysis, use candles for historical patterns
 *
 * **Thread Safety:**
 * Immutable data class - thread-safe for reading.
 *
 * @property productId Trading pair identifier (e.g., "BTC-USD").
 *           Identifies which market this ticker is for.
 *
 * @property price Last trade price (most recent executed trade).
 *           This is the "current price" used for position sizing.
 *           Unit: Quote currency per base currency (USD per BTC).
 *
 * @property bid Highest buy order price in the order book.
 *           Price at which you can SELL immediately (market sell).
 *           Unit: USD per BTC.
 *
 * @property ask Lowest sell order price in the order book.
 *           Price at which you can BUY immediately (market buy).
 *           Unit: USD per BTC.
 *
 * @property volume24h Total volume traded in the last 24 hours.
 *           Indicates market activity and liquidity.
 *           Unit: Base currency (BTC for BTC-USD pair).
 *
 * @property timestamp When this ticker snapshot was captured.
 *           Used to check data freshness and avoid stale prices.
 *
 * @see ExchangeRepository.getCurrentPrice for how this is fetched
 */
data class Ticker(
    val productId: String,
    val price: BigDecimal,
    val bid: BigDecimal,
    val ask: BigDecimal,
    val volume24h: BigDecimal,
    val timestamp: Instant
)
