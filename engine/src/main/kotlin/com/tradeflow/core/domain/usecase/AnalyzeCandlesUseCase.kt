package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.TradingConfig
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

class AnalyzeCandlesUseCase {

    operator fun invoke(candles: List<Candle>): Indicators {

        // Calculate candle duration from timestamps (auto-detect timeframe)
        val candleDuration = if (candles.size >= 2) {
            Duration.between(candles[0].timestamp, candles[1].timestamp)
        } else {
            Duration.ofHours(TradingConfig.Technical.DEFAULT_CANDLE_DURATION_HOURS.toLong())
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
        val smaIndicator = SMAIndicator(closePrice, TradingConfig.Technical.SMA_PERIOD)

        val smaValue = smaIndicator.getValue(series.endIndex).doubleValue()
        val smaPreviousIndex =
            (series.endIndex - TradingConfig.Technical.SMA_PREVIOUS_LOOKBACK).coerceAtLeast(0)
        val smaPreviousValue = smaIndicator.getValue(smaPreviousIndex).doubleValue()

        val adxValue =
            ADXIndicator(series, TradingConfig.Technical.ADX_PERIOD).getValue(series.endIndex)
                .doubleValue()
        val atrValue =
            ATRIndicator(series, TradingConfig.Technical.ATR_PERIOD).getValue(series.endIndex)
                .doubleValue()

        // RSI calculation (momentum filter)
        val rsiIndicator = RSIIndicator(closePrice, TradingConfig.Technical.RSI_PERIOD)
        val rsiValue = rsiIndicator.getValue(series.endIndex).doubleValue()

        // Volume indicators
        val volumeIndicator = VolumeIndicator(series)
        val volumeSmaIndicator =
            SMAIndicator(volumeIndicator, TradingConfig.Technical.VOLUME_SMA_PERIOD)

        val currentVolumeValue = volumeIndicator.getValue(series.endIndex).doubleValue()
        val volumeSmaValue = volumeSmaIndicator.getValue(series.endIndex).doubleValue()
        val volumeRatio =
            if (volumeSmaValue > 0) currentVolumeValue / volumeSmaValue else TradingConfig.Technical.VOLUME_RATIO_DEFAULT_FALLBACK

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

