package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Candle
import org.ta4j.core.BaseBar
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.ATRIndicator
import org.ta4j.core.indicators.adx.ADXIndicator
import org.ta4j.core.indicators.averages.SMAIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.num.DecimalNum
import java.math.BigDecimal
import java.time.Duration

/**
 * Service for calculating technical analysis indicators using the ta4j library.
 *
 * This service calculates three core indicators in a **single pass** over the candle data:
 * 1. **SMA (Simple Moving Average):** Trend direction and strength
 * 2. **ADX (Average Directional Index):** Trend vs range classification
 * 3. **ATR (Average True Range):** Volatility measurement for stop placement
 *
 * **Single-Pass Optimization:**
 * All three indicators are calculated from the same ta4j BarSeries to avoid redundant
 * candle conversions. This is more efficient than calculating each indicator separately.
 *
 * **ta4j Integration:**
 * ta4j is a technical analysis library that provides battle-tested implementations of
 * common indicators. We use it to avoid implementing complex indicator math ourselves
 * (ADX in particular is notoriously tricky to implement correctly).
 *
 * **Usage in Trading Decision Engine:**
 * ```kotlin
 * val service = AnalyzeCandlesUseCase()
 * val indicators = service.calculateAll(
 *     candles = recentCandles,
 *     smaPeriod = 200,
 *     adxPeriod = 14,
 *     atrPeriod = 14
 * )
 *
 * // Use indicators for mode detection
 * val mode = if (indicators.adx > 20.0) TREND else RANGE
 *
 * // Use ATR for stop placement
 * val stopDistance = indicators.atr * BigDecimal("10.0")  // 10× ATR stop
 * ```
 *
 * **Thread Safety:**
 * This service is stateless and thread-safe. Each call creates a new ta4j BarSeries
 * and calculates indicators from scratch.
 *
 * @see Indicators for the result object containing all calculated indicator values
 */
class AnalyzeCandlesUseCase constructor() {

    /**
     * Result object containing all calculated technical indicators.
     *
     * This data class bundles the three core indicators (SMA, ADX, ATR) plus historical
     * SMA data for trend direction analysis.
     *
     * **Indicator Interpretation:**
     *
     * **SMA (Simple Moving Average):**
     * - Long-term trend baseline (typically 200-period)
     * - Price above SMA = bullish trend bias
     * - Price below SMA = bearish trend bias
     * - SMA rising = strengthening uptrend
     * - SMA falling = strengthening downtrend
     *
     * **ADX (Average Directional Index):**
     * - Measures trend STRENGTH (not direction!)
     * - ADX < 20: Weak/absent trend (use RANGE mode)
     * - ADX 20-25: Emerging trend
     * - ADX 25-50: Strong trend (use TREND mode)
     * - ADX > 50: Very strong trend (rare in crypto)
     *
     * **ATR (Average True Range):**
     * - Measures volatility in price units (e.g., dollars for BTC)
     * - Used for volatility-adaptive stop placement
     * - High ATR = volatile market → wider stops needed
     * - Low ATR = quiet market → can use tighter stops
     * - Multiplied by a constant for stop distance (e.g., 10× ATR)
     *
     * **Example:**
     * ```kotlin
     * val indicators = service.calculateAll(candles)
     *
     * println("SMA200: ${indicators.sma200}")
     * println("ADX: ${indicators.adx}")
     * println("ATR: ${indicators.atr}")
     *
     * if (indicators.isSmaRising()) {
     *     println("Uptrend strengthening")
     * }
     * ```
     *
     * @property sma200 Current Simple Moving Average value (typically 200-period).
     *           Unit: Same as price (e.g., dollars for BTC/USD).
     *
     * @property sma200Previous Historical SMA value from 10 candles ago.
     *           Used to determine SMA slope (rising vs falling).
     *           Unit: Same as price.
     *
     * @property adx Current Average Directional Index value.
     *           Unit: Dimensionless (0-100 scale, though rarely exceeds 60).
     *           **Critical for mode switching:** High ADX triggers TREND mode.
     *
     * @property atr Current Average True Range value.
     *           Unit: Same as price (e.g., dollars for BTC/USD).
     *           **Critical for stop placement:** Multiplied by stopLossAtrMultiplier.
     */
    data class Indicators(
        val sma200: BigDecimal,
        val sma200Previous: BigDecimal,
        val adx: Double,
        val atr: BigDecimal
    ) {
        /**
         * Returns true if the SMA is rising (uptrend strengthening).
         *
         * Compares current SMA to SMA from 10 candles ago. Rising SMA indicates
         * trend momentum is accelerating upward.
         *
         * @return true if sma200 > sma200Previous
         */
        fun isSmaRising(): Boolean = sma200 > sma200Previous

        /**
         * Returns true if the SMA is falling (downtrend strengthening).
         *
         * Compares current SMA to SMA from 10 candles ago. Falling SMA indicates
         * trend momentum is accelerating downward.
         *
         * @return true if sma200 < sma200Previous
         */
        fun isSmaFalling(): Boolean = sma200 < sma200Previous
    }

    /**
     * Calculates all technical indicators (SMA, ADX, ATR) in a single pass.
     *
     * This method converts the candle list to a ta4j BarSeries and calculates all three
     * indicators efficiently. The ta4j library handles the complex math internally.
     *
     * **Single-Pass Optimization:**
     * All indicators are calculated from the same BarSeries to avoid redundant conversions.
     * This is significantly faster than calling separate methods for each indicator.
     *
     * **Candle Requirements:**
     * - Minimum candles: max(smaPeriod, adxPeriod, atrPeriod) for stable results
     * - Typically need 200+ candles (for 200-period SMA)
     * - ADX needs extra candles for internal smoothing (~2× adxPeriod)
     *
     * **ta4j Internals:**
     * - **SMAIndicator:** Simple average of closing prices over last N candles
     * - **ADXIndicator:** Complex calculation involving +DI, -DI, and smoothed DX
     * - **ATRIndicator:** Exponentially smoothed average of true ranges
     *
     * **Edge Cases:**
     * - Insufficient candles: Results will be unstable (check candle count before calling)
     * - Invalid OHLC data: Throws IllegalArgumentException (validated via validateCandle)
     * - Empty candle list: Throws IllegalArgumentException
     *
     * **Example:**
     * ```kotlin
     * val candles = repository.getCandles("BTC-USD", granularity = FOUR_HOUR, limit = 250)
     * val indicators = service.calculateAll(
     *     candles = candles.getOrThrow(),
     *     smaPeriod = 200,
     *     adxPeriod = 14,
     *     atrPeriod = 14
     * )
     * ```
     *
     * @param candles List of OHLCV candles in chronological order (oldest first).
     *                Must not be empty. All OHLCV values must be positive and valid.
     *
     * @param smaPeriod Number of candles to use for SMA calculation.
     *                  Default: 200 (long-term trend).
     *                  Common values: 50 (short-term), 100 (medium-term), 200 (long-term).
     *
     * @param adxPeriod Number of candles to use for ADX calculation.
     *                  Default: 14 (standard Wilder specification).
     *                  **Note:** ADX internally uses double smoothing, so effective lag is ~adxPeriod/2.
     *
     * @param atrPeriod Number of candles to use for ATR calculation.
     *                  Default: 14 (standard Wilder specification).
     *
     * @return Indicators object containing SMA, ADX, and ATR values
     *
     * @throws IllegalArgumentException if candles list is empty or contains invalid OHLCV data
     *
     * @see validateCandle for OHLCV validation rules
     */
    fun calculateAll(candles: List<Candle>, smaPeriod: Int = 200, adxPeriod: Int = 14, atrPeriod: Int = 14): Indicators {
        require(candles.isNotEmpty()) { "Candle list cannot be empty" }

        // Calculate candle duration from timestamps (auto-detect timeframe)
        val candleDuration = if (candles.size >= 2) {
            Duration.between(candles[0].timestamp, candles[1].timestamp)
        } else {
            Duration.ofHours(4) // Default to 4H if only one candle
        }

        val series = BaseBarSeriesBuilder().withName("TradeFlow-Series").build()

        candles.forEach { candle ->
            validateCandle(candle)

            val bar = BaseBar(
                candleDuration, // Auto-detected from candle spacing
                candle.timestamp,
                candle.timestamp.plus(candleDuration),
                DecimalNum.valueOf(candle.open),
                DecimalNum.valueOf(candle.high),
                DecimalNum.valueOf(candle.low),
                DecimalNum.valueOf(candle.close),
                DecimalNum.valueOf(candle.volume),
                DecimalNum.valueOf(0),
                0L
            )
            series.addBar(bar)
        }

        val closePrice = ClosePriceIndicator(series)
        val smaIndicator = SMAIndicator(closePrice, smaPeriod)

        val smaValue = smaIndicator.getValue(series.endIndex).doubleValue()
        val smaPreviousIndex = (series.endIndex - 10).coerceAtLeast(0)
        val smaPreviousValue = smaIndicator.getValue(smaPreviousIndex).doubleValue()

        val adxValue = ADXIndicator(series, adxPeriod).getValue(series.endIndex).doubleValue()
        val atrValue = ATRIndicator(series, atrPeriod).getValue(series.endIndex).doubleValue()

        return Indicators(
            sma200 = BigDecimal.valueOf(smaValue),
            sma200Previous = BigDecimal.valueOf(smaPreviousValue),
            adx = adxValue,
            atr = BigDecimal.valueOf(atrValue)
        )
    }

    /**
     * Validates OHLCV (Open/High/Low/Close/Volume) candle data for correctness.
     *
     * This prevents invalid data from corrupting indicator calculations. ta4j can
     * produce garbage output if fed invalid OHLCV data (e.g., high < low).
     *
     * **Validation Rules:**
     * 1. **Positive Prices:** All OHLC values must be > 0
     * 2. **Non-negative Volume:** Volume must be >= 0 (zero volume is allowed)
     * 3. **High is Highest:** high >= open, high >= close, high >= low
     * 4. **Low is Lowest:** low <= open, low <= close
     *
     * **Why These Rules:**
     * - Prices can never be zero or negative in real markets
     * - High must be the highest price during the candle period
     * - Low must be the lowest price during the candle period
     * - Volume can be zero (e.g., no trades during the period)
     *
     * **Example Invalid Candles:**
     * - High < Close: Impossible (close can't exceed high)
     * - Low > Open: Impossible (open can't be below low)
     * - Negative Price: Nonsensical
     *
     * @param candle The candle to validate
     *
     * @throws IllegalArgumentException if any validation rule is violated
     */
    private fun validateCandle(candle: Candle) {
        require(candle.open > BigDecimal.ZERO) { "Open price must be positive: ${candle.open}" }
        require(candle.high > BigDecimal.ZERO) { "High price must be positive: ${candle.high}" }
        require(candle.low > BigDecimal.ZERO) { "Low price must be positive: ${candle.low}" }
        require(candle.close > BigDecimal.ZERO) { "Close price must be positive: ${candle.close}" }
        require(candle.volume >= BigDecimal.ZERO) { "Volume cannot be negative: ${candle.volume}" }

        require(candle.high >= candle.open) { "High (${candle.high}) must be >= open (${candle.open})" }
        require(candle.high >= candle.close) { "High (${candle.high}) must be >= close (${candle.close})" }
        require(candle.high >= candle.low) { "High (${candle.high}) must be >= low (${candle.low})" }

        require(candle.low <= candle.open) { "Low (${candle.low}) must be <= open (${candle.open})" }
        require(candle.low <= candle.close) { "Low (${candle.low}) must be <= close (${candle.close})" }
    }
}
