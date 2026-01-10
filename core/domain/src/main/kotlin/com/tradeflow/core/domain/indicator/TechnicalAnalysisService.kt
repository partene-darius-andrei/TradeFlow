package com.tradeflow.core.domain.indicator

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
import javax.inject.Inject

class TechnicalAnalysisService @Inject constructor() {

    data class Indicators(
        val sma200: BigDecimal,
        val sma200Previous: BigDecimal,
        val adx: Double,
        val atr: BigDecimal
    ) {
        fun isSmaRising(): Boolean = sma200 > sma200Previous
        fun isSmaFalling(): Boolean = sma200 < sma200Previous
    }

    fun calculateAll(candles: List<Candle>, smaPeriod: Int = 200, adxPeriod: Int = 14, atrPeriod: Int = 14): Indicators {
        require(candles.isNotEmpty()) { "Candle list cannot be empty" }

        val series = BaseBarSeriesBuilder().withName("TradeFlow-Series").build()

        candles.forEach { candle ->
            validateCandle(candle)

            val bar = BaseBar(
                Duration.ofMinutes(240), // H4
                candle.timestamp,
                candle.timestamp.plus(Duration.ofHours(4)),
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
