package com.tradeflow.core.domain.config

import com.tradeflow.core.domain.model.Granularity

/**
 * Technical analysis indicator configuration parameters.
 *
 * These parameters control which indicators are calculated, their periods/lookback windows,
 * and the candle granularity (timeframe) for analysis.
 *
 * **Three Core Indicators:**
 * 1. **SMA (Simple Moving Average):** Trend baseline, price filter
 * 2. **ADX (Average Directional Index):** Trend strength measurement
 * 3. **ATR (Average True Range):** Volatility measurement
 *
 * **Usage in AnalyzeCandlesUseCase:**
 * ```kotlin
 * val params = TechnicalParameters(
 *     smaPeriod = 200,     // 200-period SMA (long-term trend)
 *     adxPeriod = 14,      // Standard 14-period ADX
 *     atrPeriod = 14       // Standard 14-period ATR
 * )
 * val analysis = technicalService.analyze(candles, params)
 * ```
 *
 * **Why These Indicators:**
 * - **SMA:** Simple, reliable trend filter. Price above SMA = uptrend bias.
 * - **ADX:** Quantifies trend strength without directional bias. High ADX = strong trend (up or down).
 * - **ATR:** Volatility-adaptive stop placement. ATR adjusts to market conditions automatically.
 *
 * @property smaPeriod Number of candles used to calculate Simple Moving Average.
 *           Example: 200 = average of last 200 closing prices.
 *           **Common Values:**
 *           - 50: Short-term trend
 *           - 200: Long-term trend (most common)
 *           - 100: Medium-term
 *           **Rationale:** 200-period SMA is the industry standard for identifying major trend direction.
 *           Default: 200 (long-term trend baseline).
 *
 * @property adxPeriod Number of candles used to calculate Average Directional Index.
 *           Example: 14 = ADX calculated over last 14 candles.
 *           **Technical Note:** ADX is a smoothed indicator (uses EMA internally), so it lags price by ~adxPeriod/2 candles.
 *           **Common Values:**
 *           - 14: Standard period (Wilder's original specification)
 *           - 7: More responsive but noisy
 *           - 21: Smoother but slower
 *           Default: 14 (standard J. Welles Wilder Jr. specification).
 *
 * @property atrPeriod Number of candles used to calculate Average True Range.
 *           Example: 14 = ATR is average of last 14 true ranges.
 *           **Technical Note:** ATR uses EMA smoothing, so it adapts quickly to volatility changes.
 *           **Common Values:**
 *           - 14: Standard period (Wilder's specification)
 *           - 10: More reactive to volatility spikes
 *           - 20: Smoother volatility estimate
 *           Default: 14 (standard Wilder specification).
 *
 * @property rsiPeriod Number of candles used to calculate Relative Strength Index.
 *           Example: 14 = RSI calculated over last 14 candles.
 *           **Technical Note:** RSI uses Wilder's smoothing (similar to EMA).
 *           **Common Values:**
 *           - 14: Standard period (Wilder's specification)
 *           - 7-10: More responsive, noisier
 *           - 21: Smoother, less responsive
 *           **Warmup Requirement:** RSI needs 150-250 bars for stable readings.
 *           Default: 14 (standard Wilder specification).
 *
 * @property volumeSmaPeriod Number of candles used to calculate volume moving average.
 *           Example: 20 = average volume over last 20 candles (80 hours for 4H timeframe).
 *           **Usage:** Baseline for calculating volumeRatio (current volume / average volume).
 *           **Rationale:** 20-period provides responsive measurement without excessive noise.
 *           Default: 20 candles (balanced lookback period).
 *
 * @property cmfPeriod Number of candles used to calculate Chaikin Money Flow.
 *           Example: 21 = CMF calculated over last 21 candles.
 *           **Technical Note:** CMF measures volume-weighted buying/selling pressure.
 *           **Common Values:**
 *           - 21: Standard period (recommended)
 *           - 10-15: More responsive
 *           - 30-40: Smoother
 *           Default: 21 (standard specification).
 *
 * @property minVolumeRatio Minimum volume ratio required for trade entry confirmation.
 *           Example: 1.5 = current volume must be 50% above 20-period average.
 *           **Rationale:** High volume confirms breakout validity.
 *           **Research:** Volume > 1.5x improves breakout success from 39% to 65%.
 *           Default: 1.5 (50% above average).
 *
 * @property smaLookbackCandles NOT CURRENTLY USED in the codebase.
 *           Legacy parameter, may be removed in future cleanup.
 *           Default: 10.
 *
 * @property minCandlesRequired Minimum number of candles required before technical analysis can produce valid results.
 *           Example: 200 = need at least 200 candles of history before SMA/ADX/ATR are reliable.
 *           **Rationale:**
 *           - SMA needs smaPeriod candles to avoid initial bias
 *           - ADX needs ~2× adxPeriod for stable readings (internal smoothing)
 *           - ATR needs atrPeriod for stable volatility estimate
 *           **Safety:** If fewer than minCandlesRequired candles available, strategy should wait or use safe defaults.
 *           Default: 200 (matches SMA period for consistency).
 *
 * @property barDurationMinutes Duration of each candle in MINUTES.
 *           Example: 240 = 4-hour candles (240 minutes).
 *           **Must Match Granularity:**
 *           - ONE_MINUTE → 1
 *           - FIVE_MINUTE → 5
 *           - ONE_HOUR → 60
 *           - FOUR_HOUR → 240 (default)
 *           - ONE_DAY → 1440
 *           **Usage:** Time-based calculations, candle fetching, backtest simulation.
 *           Default: 240 minutes (4 hours).
 *
 * @property granularity Candle timeframe for analysis.
 *           Example: FOUR_HOUR = analyze on 4-hour candles.
 *           **Trade-offs by Timeframe:**
 *           - Shorter (1H): More signals, more noise, faster reaction
 *           - Medium (4H): Balanced signal quality and frequency ⭐ DEFAULT
 *           - Longer (1D): Fewer signals, cleaner trends, slower reaction
 *           **Rationale:** 4-hour candles balance signal frequency with noise reduction.
 *           Default: FOUR_HOUR (balanced timeframe for crypto swing trading).
 *
 * @see AnalyzeCandlesUseCase for how these parameters are used to calculate indicators
 * @see Granularity for available candle timeframes
 */
data class TechnicalParameters(
    val smaPeriod: Int = 200,
    val adxPeriod: Int = 14,
    val atrPeriod: Int = 14,
    val rsiPeriod: Int = 14,
    val volumeSmaPeriod: Int = 20,
    val cmfPeriod: Int = 21,
    val minVolumeRatio: Double = 1.5,
    val smaLookbackCandles: Int = 10,
    val minCandlesRequired: Int = 200,
    val barDurationMinutes: Int = 240,
    val granularity: Granularity = Granularity.FOUR_HOUR
)
