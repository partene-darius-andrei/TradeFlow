package com.tradeflow.core.domain.indicator

import com.tradeflow.core.domain.model.Candle
import org.ta4j.core.BaseBar
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.adx.ADXIndicator
import org.ta4j.core.num.DecimalNum
import java.time.Duration
import javax.inject.Inject

class ADXCalculator @Inject constructor() {

    fun calculate(candles: List<Candle>, period: Int): Double {
        require(candles.size >= period * 2) {
            "Need at least ${period * 2} candles for ADX calculation, got ${candles.size}"
        }

        val series = BaseBarSeriesBuilder()
            .withName("ADX-Series")
            .build()

        candles.forEach { candle ->
            val beginTime = candle.timestamp
            val endTime = candle.timestamp.plus(Duration.ofMinutes(1))
            val bar = BaseBar(
                Duration.ofMinutes(1),
                beginTime,
                endTime,
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

        val adx = ADXIndicator(series, period)
        val lastValue = adx.getValue(series.endIndex)

        return lastValue.doubleValue()
    }
}
