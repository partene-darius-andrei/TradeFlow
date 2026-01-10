package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.indicator.TechnicalAnalysisService
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TradingDecisionEngineTest {

    private val taService = TechnicalAnalysisService()
    private val config = TradingConfig.forProfile(RiskProfile.BALANCED)
    private lateinit var engine: TradingDecisionEngine

    @Before
    fun setup() {
        engine = TradingDecisionEngine(taService, config)
        engine.resetState()
    }

    private fun createTrendCandles(): List<Candle> {
        val now = Instant.now().minus(Duration.ofDays(40))
        return (1..200).map { i ->
            val price = BigDecimal(50000 + i * 100)
            Candle(
                timestamp = now.plus(Duration.ofHours(i * 4L)),
                open = price,
                high = price + BigDecimal(50),
                low = price - BigDecimal(50),
                close = price,
                volume = BigDecimal(100)
            )
        }
    }

    private fun createDefenseCandles(): List<Candle> {
        val now = Instant.now().minus(Duration.ofDays(40))
        return (1..200).map { i ->
            val price = BigDecimal(100000)
            Candle(
                timestamp = now.plus(Duration.ofHours(i * 4L)),
                open = price,
                high = price,
                low = price,
                close = price,
                volume = BigDecimal(100)
            )
        }
    }

    @Test
    fun `evaluate Trend scenario`() {
        val candles = createTrendCandles()
        val currentPrice = candles.last().close

        // Engine requires 3 confirmations to switch to TREND mode due to hysteresis
        engine.evaluate(candles, currentPrice) // Confirmation 1
        engine.evaluate(candles, currentPrice) // Confirmation 2
        val decision = engine.evaluate(candles, currentPrice) // Confirmation 3 -> switches to TREND

        assertTrue(decision is Decision.Trend)
        assertEquals(OrderSide.BUY, (decision as Decision.Trend).direction)
        assertEquals(config.strategy.trendPositionPercent, decision.positionSizePercent)
    }

    @Test
    fun `evaluate Defense scenario`() {
        val candles = createDefenseCandles()
        val currentPrice = BigDecimal("10000") 
        
        val decision = engine.evaluate(candles, currentPrice)
        
        assertTrue(decision is Decision.Defense)
        assertTrue((decision as Decision.Defense).reason.contains("below SMA200"))
    }

    @Test
    fun `evaluate requires enough data`() {
        val candles = (1..50).map { 
            Candle(Instant.now(), BigDecimal("50000"), BigDecimal("50000"), BigDecimal("50000"), BigDecimal("50000"), BigDecimal(100)) 
        }
        
        val decision = engine.evaluate(candles, BigDecimal("50000"))
        assertTrue(decision is Decision.Wait)
    }
}
