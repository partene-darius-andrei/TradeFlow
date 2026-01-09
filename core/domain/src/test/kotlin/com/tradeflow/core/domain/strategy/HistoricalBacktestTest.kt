package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.util.BinanceDataLoader
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoricalBacktestTest {

    @Test
    fun `fetch sample data to verify API connectivity`() {
        val candles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "1h",
            limit = 10
        )

        println("Sample candles:")
        candles.take(3).forEach { candle ->
            println("${candle.timestamp} | O: ${candle.open} H: ${candle.high} L: ${candle.low} C: ${candle.close}")
        }

        assertEquals(candles.size, 10)
        assertTrue(candles.all { it.close > BigDecimal.ZERO })
    }
}
