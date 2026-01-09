package com.tradeflow.core.domain.indicator

import com.tradeflow.core.domain.model.Candle
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.ATRIndicator
import java.math.BigDecimal
import java.time.ZoneId

class ATRCalculator {

    fun calculate(candles: List<Candle>, period: Int): BigDecimal {
        require(candles.size >= period) {
            "Need at least $period candles for ATR calculation, got ${candles.size}"
        }

        val series = BaseBarSeriesBuilder()
            .withName("ATR-Series")
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

        val atr = ATRIndicator(series, period)
        val lastValue = atr.getValue(series.endIndex)

        return BigDecimal(lastValue.toString())
    }
}
