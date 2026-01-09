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
        val adx: Double,
        val atr: BigDecimal
    )

    fun calculateAll(candles: List<Candle>, smaPeriod: Int = 200, adxPeriod: Int = 14, atrPeriod: Int = 14): Indicators {
        val series = BaseBarSeriesBuilder().withName("TradeFlow-Series").build()
        
        candles.forEach { candle ->
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
        
        val smaValue = SMAIndicator(closePrice, smaPeriod).getValue(series.endIndex).doubleValue()
        val adxValue = ADXIndicator(series, adxPeriod).getValue(series.endIndex).doubleValue()
        val atrValue = ATRIndicator(series, atrPeriod).getValue(series.endIndex).doubleValue()

        return Indicators(
            sma200 = BigDecimal.valueOf(smaValue),
            adx = adxValue,
            atr = BigDecimal.valueOf(atrValue)
        )
    }
}
