package com.tradeflow.core.domain.indicator

import com.tradeflow.core.domain.model.Candle
import org.ta4j.core.BaseBar
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.ATRIndicator
import org.ta4j.core.num.DecimalNum
import java.math.BigDecimal
import java.time.Duration
import javax.inject.Inject

class ATRCalculator @Inject constructor() {

    fun calculate(candles: List<Candle>, period: Int): BigDecimal {
        require(candles.size >= period) {
            "Need at least $period candles for ATR calculation, got ${candles.size}"
        }

        val series = BaseBarSeriesBuilder()
            .withName("ATR-Series")
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

        val atr = ATRIndicator(series, period)
        val lastValue = atr.getValue(series.endIndex)

        return BigDecimal(lastValue.toString())
    }
}
