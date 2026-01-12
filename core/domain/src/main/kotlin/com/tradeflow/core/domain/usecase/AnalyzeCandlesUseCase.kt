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
     * This data class bundles the core indicators (SMA, ADX, ATR, RSI, Volume) plus historical
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
     * **RSI (Relative Strength Index):**
     * - Measures momentum strength (0-100 scale)
     * - RSI > 50: Bullish momentum (use for LONG confirmation)
     * - RSI < 50: Bearish momentum (use for SHORT confirmation)
     * - RSI > 70: Overbought (traditional interpretation, not used in trend-following)
     * - RSI < 30: Oversold (traditional interpretation, not used in trend-following)
     * - **TradeFlow uses RSI as MOMENTUM FILTER, not mean-reversion**
     *
     * **Volume Indicators:**
     * - **volumeRatio:** Current volume / 20-period average volume
     *   - Ratio > 1.5: High volume (confirms breakout)
     *   - Ratio < 1.0: Below average volume (weak signal)
     * - **OBV (On-Balance Volume):** Cumulative volume indicator
     *   - Rising OBV + rising price = strong uptrend
     *   - Falling OBV + rising price = divergence (bearish)
     * - **CMF (Chaikin Money Flow):** Volume-weighted indicator (-1 to +1)
     *   - CMF > 0.05: Money flowing into asset (bullish)
     *   - CMF < -0.05: Money flowing out of asset (bearish)
     *   - CMF near 0: Neutral flow
     *
     * **Example:**
     * ```kotlin
     * val indicators = service.calculateAll(candles)
     *
     * println("SMA200: ${indicators.sma200}")
     * println("ADX: ${indicators.adx}")
     * println("ATR: ${indicators.atr}")
     * println("RSI: ${indicators.rsi}")
     * println("Volume: ${indicators.volumeRatio}x average")
     *
     * if (indicators.isSmaRising() && indicators.rsi > 50.0) {
     *     println("Strong bullish momentum")
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
     *
     * @property rsi Current Relative Strength Index value.
     *           Unit: Dimensionless (0-100 scale).
     *           **Critical for momentum confirmation:** RSI > 50 confirms LONG, RSI < 50 confirms SHORT.
     *           Research shows RSI as momentum filter achieves 60-65% win rate on BTC.
     *
     * @property volumeSma 20-period simple moving average of volume.
     *           Used as baseline to calculate volumeRatio.
     *           Unit: Same as volume (e.g., BTC for BTC/USD).
     *
     * @property currentVolume Current candle's volume.
     *           Unit: Same as volume (e.g., BTC for BTC/USD).
     *
     * @property volumeRatio Current volume divided by 20-period average volume.
     *           Unit: Dimensionless ratio (1.0 = average, 1.5 = 50% above average).
     *           **Critical for breakout confirmation:** Ratio > 1.5 validates breakout.
     *           Research shows volume > 1.5x improves breakout success from 39% to 65%.
     *
     * @property obv Current On-Balance Volume value.
     *           Unit: Cumulative volume (dimensionless).
     *           Used for trend confirmation via divergence analysis.
     *
     * @property cmf Current Chaikin Money Flow value.
     *           Unit: Dimensionless (-1 to +1 scale).
     *           CMF > 0.05 confirms money flowing in (bullish).
     *           CMF < -0.05 confirms money flowing out (bearish).
     */
    data class Indicators(
        val sma200: BigDecimal,
        val sma200Previous: BigDecimal,
        val adx: Double,
        val atr: BigDecimal,
        val rsi: Double,
        val volumeSma: BigDecimal,
        val currentVolume: BigDecimal,
        val volumeRatio: Double,
        val obv: BigDecimal,
        val cmf: Double
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
     * Calculates all technical indicators (SMA, ADX, ATR, RSI, Volume) in a single pass.
     *
     * This method converts the candle list to a ta4j BarSeries and calculates all
     * indicators efficiently. The ta4j library handles the complex math internally.
     *
     * **Single-Pass Optimization:**
     * All indicators are calculated from the same BarSeries to avoid redundant conversions.
     * This is significantly faster than calling separate methods for each indicator.
     *
     * **Candle Requirements:**
     * - Minimum candles: max(smaPeriod, adxPeriod, atrPeriod, rsiPeriod, volumeSmaPeriod) for stable results
     * - Typically need 200+ candles (for 200-period SMA)
     * - ADX needs extra candles for internal smoothing (~2× adxPeriod)
     * - RSI needs 150-250 bars for stable warmup (check ta4j isStable())
     *
     * **ta4j Internals:**
     * - **SMAIndicator:** Simple average of closing prices over last N candles
     * - **ADXIndicator:** Complex calculation involving +DI, -DI, and smoothed DX
     * - **ATRIndicator:** Exponentially smoothed average of true ranges
     * - **RSIIndicator:** Relative Strength Index using Wilder's smoothing
     * - **OBVIndicator:** On-Balance Volume (cumulative volume indicator)
     * - **ChaikinMoneyFlowIndicator:** Volume-weighted accumulation/distribution
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
     *     atrPeriod = 14,
     *     rsiPeriod = 14,
     *     volumeSmaPeriod = 20,
     *     cmfPeriod = 21
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
     * @param rsiPeriod Number of candles to use for RSI calculation.
     *                  Default: 14 (standard Wilder specification).
     *                  **Note:** RSI needs 150-250 bars for stable warmup.
     *
     * @param volumeSmaPeriod Number of candles to use for volume SMA calculation.
     *                        Default: 20 (provides 80-hour lookback for 4H candles).
     *                        Used to calculate volumeRatio for breakout confirmation.
     *
     * @param cmfPeriod Number of candles to use for Chaikin Money Flow calculation.
     *                  Default: 21 (standard CMF period).
     *
     * @return Indicators object containing all calculated indicator values
     *
     * @throws IllegalArgumentException if candles list is empty or contains invalid OHLCV data
     *
     * @see validateCandle for OHLCV validation rules
     */
    fun calculateAll(
        candles: List<Candle>,
        smaPeriod: Int = 200,
        adxPeriod: Int = 14,
        atrPeriod: Int = 14,
        rsiPeriod: Int = 14,
        volumeSmaPeriod: Int = 20,
        cmfPeriod: Int = 21
    ): Indicators {
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

        // RSI calculation (momentum filter)
        val rsiIndicator = org.ta4j.core.indicators.RSIIndicator(closePrice, rsiPeriod)
        val rsiValue = rsiIndicator.getValue(series.endIndex).doubleValue()

        // Volume indicators
        val volumeIndicator = org.ta4j.core.indicators.helpers.VolumeIndicator(series)
        val volumeSmaIndicator = SMAIndicator(volumeIndicator, volumeSmaPeriod)

        val currentVolumeValue = volumeIndicator.getValue(series.endIndex).doubleValue()
        val volumeSmaValue = volumeSmaIndicator.getValue(series.endIndex).doubleValue()
        val volumeRatio = if (volumeSmaValue > 0) currentVolumeValue / volumeSmaValue else 1.0

        // OBV (On-Balance Volume)
        val obvIndicator = org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator(series)
        val obvValue = obvIndicator.getValue(series.endIndex).doubleValue()

        // CMF (Chaikin Money Flow)
        val cmfIndicator = org.ta4j.core.indicators.volume.ChaikinMoneyFlowIndicator(series, cmfPeriod)
        val cmfValue = cmfIndicator.getValue(series.endIndex).doubleValue()

        return Indicators(
            sma200 = BigDecimal.valueOf(smaValue),
            sma200Previous = BigDecimal.valueOf(smaPreviousValue),
            adx = adxValue,
            atr = BigDecimal.valueOf(atrValue),
            rsi = rsiValue,
            volumeSma = BigDecimal.valueOf(volumeSmaValue),
            currentVolume = BigDecimal.valueOf(currentVolumeValue),
            volumeRatio = volumeRatio,
            obv = BigDecimal.valueOf(obvValue),
            cmf = cmfValue
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
