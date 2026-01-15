package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Indicators
import org.ta4j.core.BaseBar
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.ATRIndicator
import org.ta4j.core.indicators.RSIIndicator
import org.ta4j.core.indicators.adx.ADXIndicator
import org.ta4j.core.indicators.averages.SMAIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.indicators.helpers.VolumeIndicator
import org.ta4j.core.indicators.volume.ChaikinMoneyFlowIndicator
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator
import org.ta4j.core.num.DecimalNum
import java.math.BigDecimal
import java.time.Duration

class AnalyzeCandlesUseCase(
    private val config: com.tradeflow.core.domain.StrategyConfig = com.tradeflow.core.domain.StrategyConfig()
) {

    operator fun invoke(candles: List<Candle>): Indicators {

        // Calculate candle duration from timestamps (auto-detect timeframe)
        val candleDuration = if (candles.size >= 2) {
            Duration.between(candles[0].timestamp, candles[1].timestamp)
        } else {
            Duration.ofHours(com.tradeflow.core.domain.StrategyConfig.DEFAULT_CANDLE_DURATION_HOURS.toLong())
        }

        val series = BaseBarSeriesBuilder().build()

        candles.forEach { candle ->

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
        val smaIndicator = SMAIndicator(closePrice, config.smaPeriod.default.toInt())

        val smaValue = smaIndicator.getValue(series.endIndex).doubleValue()
        val smaPreviousIndex =
            (series.endIndex - config.smaPreviousLookback.default.toInt()).coerceAtLeast(0)
        val smaPreviousValue = smaIndicator.getValue(smaPreviousIndex).doubleValue()

        val adxValue =
            ADXIndicator(series, config.adxPeriod.default.toInt()).getValue(series.endIndex)
                .doubleValue()
        val atrValue =
            ATRIndicator(series, config.atrPeriod.default.toInt()).getValue(series.endIndex)
                .doubleValue()

        // RSI calculation (momentum filter)
        val rsiIndicator = RSIIndicator(closePrice, config.rsiPeriod.default.toInt())
        val rsiValue = rsiIndicator.getValue(series.endIndex).doubleValue()

        // Volume indicators
        val volumeIndicator = VolumeIndicator(series)
        val volumeSmaIndicator =
            SMAIndicator(volumeIndicator, config.volumeSmaPeriod.default.toInt())

        val currentVolumeValue = volumeIndicator.getValue(series.endIndex).doubleValue()
        val volumeSmaValue = volumeSmaIndicator.getValue(series.endIndex).doubleValue()
        val volumeRatio =
            if (volumeSmaValue > 0) currentVolumeValue / volumeSmaValue else com.tradeflow.core.domain.StrategyConfig.VOLUME_RATIO_DEFAULT_FALLBACK

        // OBV (On-Balance Volume)
        val obvIndicator = OnBalanceVolumeIndicator(series)
        val obvValue = obvIndicator.getValue(series.endIndex).doubleValue()

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
        )
    }
}

