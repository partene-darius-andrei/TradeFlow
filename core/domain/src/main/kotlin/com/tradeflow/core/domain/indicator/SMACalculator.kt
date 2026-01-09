package com.tradeflow.core.domain.indicator

import com.tradeflow.core.domain.model.Candle
import org.ta4j.core.BaseBar
import org.ta4j.core.BaseBarSeriesBuilder
import org.ta4j.core.indicators.SMAIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import java.math.BigDecimal
import java.time.Duration
import java.time.ZoneId

class SMACalculator {

    fun calculate(candles: List<Candle>, period: Int): BigDecimal {
        require(candles.size >= period) {
            "Need at least $period candles for SMA calculation, got ${candles.size}"
        }

        val series = BaseBarSeriesBuilder()
            .withName("SMA-Series")
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

        val closePrice = ClosePriceIndicator(series)
        val sma = SMAIndicator(closePrice, period)

        val lastValue = sma.getValue(series.endIndex)
        return BigDecimal(lastValue.toString())
    }
}
