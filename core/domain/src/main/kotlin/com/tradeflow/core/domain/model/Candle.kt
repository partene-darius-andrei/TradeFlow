package com.tradeflow.core.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * OHLCV (Open/High/Low/Close/Volume) candlestick bar representing price action over a time period.
 *
 * Candles are the fundamental unit of technical analysis. Each candle summarizes all trading
 * activity during a specific time period (e.g., 4 hours, 1 day).
 *
 * **OHLC Interpretation:**
 * - **Open:** First trade price when the candle period began
 * - **High:** Highest price reached during the candle period
 * - **Low:** Lowest price reached during the candle period
 * - **Close:** Last trade price when the candle period ended
 *
 * **Example (4-hour BTC candle):**
 * ```
 * Timestamp: 2025-01-10 08:00:00 UTC
 * Open: $95,000 (price at 08:00)
 * High: $97,000 (peak during 08:00-12:00)
 * Low: $94,500 (trough during 08:00-12:00)
 * Close: $96,500 (price at 12:00)
 * Volume: 150.5 BTC (total BTC traded during period)
 * ```
 *
 * **Candle Color (bullish vs bearish):**
 * - **Green/Bullish:** close > open (price rose during period)
 * - **Red/Bearish:** close < open (price fell during period)
 * - **Doji:** close ≈ open (indecision)
 *
 * **Usage in Technical Analysis:**
 * ```kotlin
 * val candles = repository.getCandles("BTC-USD", Granularity.FOUR_HOUR, 250)
 * val indicators = technicalService.calculateAll(candles)
 * ```
 *
 * @property timestamp Start time of the candle period (UTC).
 *           For a 4H candle starting at 08:00, timestamp = 2025-01-10T08:00:00Z.
 *
 * @property open Opening price (first trade) when the candle period began.
 *           Must be positive. Unit: Quote currency (e.g., USD for BTC-USD).
 *
 * @property high Highest price reached during the candle period.
 *           Must be >= open, close, low. Unit: Quote currency.
 *
 * @property low Lowest price reached during the candle period.
 *           Must be <= open, close, high. Unit: Quote currency.
 *
 * @property close Closing price (last trade) when the candle period ended.
 *           Must be positive. Unit: Quote currency.
 *
 * @property volume Total volume traded during the candle period.
 *           Unit: Base currency (e.g., BTC for BTC-USD pair).
 *           Can be zero (no trades during period, rare in liquid markets).
 *
 * @see TechnicalAnalysisService for how candles are used to calculate indicators
 * @see Granularity for available candle timeframes
 */
data class Candle(
    val timestamp: Instant,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal
)

/**
 * Candle timeframe (granularity) defining the duration of each candlestick.
 *
 * The granularity determines how much price action is aggregated into a single candle.
 * Shorter timeframes = more candles, more detail, more noise.
 * Longer timeframes = fewer candles, smoother trends, slower signals.
 *
 * **Trade-offs by Timeframe:**
 *
 * **Short Timeframes (1M, 5M, 15M):**
 * - Pros: Fast signals, good for scalping
 * - Cons: Lots of noise, many false signals, high trading frequency
 *
 * **Medium Timeframes (1H, 4H):**
 * - Pros: Balance of signal quality and frequency
 * - Cons: Slower than short timeframes, still some noise
 * - **4H is the default for this system** (good for swing trading crypto)
 *
 * **Long Timeframes (1D, 1W):**
 * - Pros: Clean trends, high-quality signals, low noise
 * - Cons: Very slow signals, miss short-term opportunities
 *
 * **Example:**
 * - ONE_MINUTE: Each candle = 1 minute of trading (60 seconds)
 * - FOUR_HOUR: Each candle = 4 hours of trading (14,400 seconds)
 * - ONE_DAY: Each candle = 24 hours of trading (86,400 seconds)
 *
 * **Usage:**
 * ```kotlin
 * val candles = repository.getCandles(
 *     productId = "BTC-USD",
 *     granularity = Granularity.FOUR_HOUR,  // 4-hour candles
 *     limit = 250  // Get 250 candles = 1000 hours of data
 * )
 * ```
 *
 * **Common Crypto Timeframes:**
 * - Day trading: 5M, 15M, 1H
 * - Swing trading: 4H, 1D (this system's default)
 * - Position trading: 1D, 1W
 *
 * @property seconds Duration of each candle in seconds
 */
enum class Granularity(val seconds: Long) {
    ONE_MINUTE(60),
    FIVE_MINUTE(300),
    FIFTEEN_MINUTE(900),
    THIRTY_MINUTE(1800),
    ONE_HOUR(3600),
    TWO_HOUR(7200),
    FOUR_HOUR(14400),
    SIX_HOUR(21600),
    ONE_DAY(86400)
}
