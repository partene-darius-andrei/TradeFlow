package com.tradeflow.core.domain.indicator

import com.tradeflow.core.domain.model.Candle
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.adx.ADXIndicator
import java.time.ZoneId

class ADXCalculator {

    fun calculate(candles: List<Candle>, period: Int): Double {
        require(candles.size >= period * 2) {
            "Need at least ${period * 2} candles for ADX calculation, got ${candles.size}"
        }

        val series = BaseBarSeriesBuilder()
            .withName("ADX-Series")
            .build()

        candles.forEach { candle ->
            series.addBar(
                candle.timestamp.atZone(ZoneId.systemDefault()),
                candle.open.toDouble(),
                candle.high.toDouble(),
                candle.low.toDouble(),
                candle.close.toDouble(),
                candle.volume.toDouble()
            )
        }

        val adx = ADXIndicator(series, period)
        val lastValue = adx.getValue(series.endIndex)

        return lastValue.doubleValue()
    }
}
