package com.tradeflow.core.domain.indicator

import com.tradeflow.core.domain.model.Candle
import org.ta4j.core.BaseBar
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.adx.ADXIndicator
import java.time.Duration
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
            val bar = BaseBar(
                Duration.ofMinutes(1),
                candle.timestamp.atZone(ZoneId.systemDefault()),
                candle.open,
                candle.high,
                candle.low,
                candle.close,
                candle.volume
            )
            series.addBar(bar)
        }

        val adx = ADXIndicator(series, period)
        val lastValue = adx.getValue(series.endIndex)

        return lastValue.doubleValue()
    }
}
