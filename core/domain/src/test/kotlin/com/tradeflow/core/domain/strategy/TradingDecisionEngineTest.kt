package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.indicator.ADXCalculator
import com.tradeflow.core.domain.indicator.ATRCalculator
import com.tradeflow.core.domain.indicator.SMACalculator
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TradingDecisionEngineTest {

    private lateinit var smaCalculator: SMACalculator
    private lateinit var adxCalculator: ADXCalculator
    private lateinit var atrCalculator: ATRCalculator
    private lateinit var engine: TradingDecisionEngine

    private val testConfig = StrategyConfig(
        smaPeriod = 200,
        adxPeriod = 14,
        atrPeriod = 14,
        adxTrendThreshold = 25.0,
        adxRangeThreshold = 25.0,
        stopLossAtrMultiplier = BigDecimal("3.0"),
        takeProfitAtrMultiplier = BigDecimal("6.0")
    )

    @Before
    fun setup() {
        smaCalculator = mockk()
        adxCalculator = mockk()
        atrCalculator = mockk()
        engine = TradingDecisionEngine(smaCalculator, adxCalculator, atrCalculator, testConfig)
    }

    private fun createCandles(count: Int): List<Candle> {
        return (0 until count).map { i ->
            Candle(
                timestamp = Instant.now().plusSeconds(i * 60L),
                open = BigDecimal("50000"),
                high = BigDecimal("51000"),
                low = BigDecimal("49000"),
                close = BigDecimal("50500"),
                volume = BigDecimal("100")
            )
        }
    }

    @Test
    fun `evaluate returns Defense when price below SMA200`() {
        val candles = createCandles(200)
        val currentPrice = BigDecimal("40000")
        val sma200 = BigDecimal("50000")

        every { smaCalculator.calculate(candles, 200) } returns sma200
        every { adxCalculator.calculate(candles, 14) } returns 30.0
        every { atrCalculator.calculate(candles, 14) } returns BigDecimal("500")

        val decision = engine.evaluate(candles, currentPrice)

        assertTrue(decision is Decision.Defense)
        assertEquals(sma200, (decision as Decision.Defense).sma200)
        assertEquals(currentPrice, decision.currentPrice)
    }

    @Test
    fun `evaluate returns Trend after 3 consecutive high ADX readings`() {
        val candles = createCandles(200)
        val currentPrice = BigDecimal("55000")
        val sma200 = BigDecimal("50000")
        val atr = BigDecimal("500")

        every { smaCalculator.calculate(candles, 200) } returns sma200
        every { adxCalculator.calculate(candles, 14) } returns 30.0
        every { atrCalculator.calculate(candles, 14) } returns atr

        val decision1 = engine.evaluate(candles, currentPrice)
        assertTrue(decision1 is Decision.Wait)
        assertEquals(1, (decision1 as Decision.Wait).confirmationCount)

        val decision2 = engine.evaluate(candles, currentPrice)
        assertTrue(decision2 is Decision.Wait)
        assertEquals(2, (decision2 as Decision.Wait).confirmationCount)

        val decision3 = engine.evaluate(candles, currentPrice)
        assertTrue(decision3 is Decision.Trend)
        assertEquals(currentPrice, (decision3 as Decision.Trend).entryPrice)
    }

    @Test
    fun `evaluate returns Range after 3 consecutive low ADX readings`() {
        val candles = createCandles(200)
        val currentPrice = BigDecimal("55000")
        val sma200 = BigDecimal("50000")
        val atr = BigDecimal("500")

        every { smaCalculator.calculate(candles, 200) } returns sma200
        every { adxCalculator.calculate(candles, 14) } returns 20.0
        every { atrCalculator.calculate(candles, 14) } returns atr

        val decision1 = engine.evaluate(candles, currentPrice)
        assertTrue(decision1 is Decision.Wait)

        val decision2 = engine.evaluate(candles, currentPrice)
        assertTrue(decision2 is Decision.Wait)

        val decision3 = engine.evaluate(candles, currentPrice)
        assertTrue(decision3 is Decision.Range)
        assertEquals(currentPrice, (decision3 as Decision.Range).centerPrice)
    }

    @Test
    fun `evaluate resets hysteresis when ADX changes direction mid-confirmation`() {
        val candles = createCandles(200)
        val currentPrice = BigDecimal("55000")
        val sma200 = BigDecimal("50000")
        val atr = BigDecimal("500")

        every { smaCalculator.calculate(candles, 200) } returns sma200
        every { atrCalculator.calculate(candles, 14) } returns atr

        every { adxCalculator.calculate(candles, 14) } returns 30.0
        val decision1 = engine.evaluate(candles, currentPrice)
        assertTrue(decision1 is Decision.Wait)
        assertEquals(1, (decision1 as Decision.Wait).confirmationCount)

        every { adxCalculator.calculate(candles, 14) } returns 30.0
        val decision2 = engine.evaluate(candles, currentPrice)
        assertTrue(decision2 is Decision.Wait)
        assertEquals(2, (decision2 as Decision.Wait).confirmationCount)

        every { adxCalculator.calculate(candles, 14) } returns 20.0
        val decision3 = engine.evaluate(candles, currentPrice)
        assertTrue(decision3 is Decision.Wait)
        assertEquals(1, (decision3 as Decision.Wait).confirmationCount)
    }

    @Test
    fun `evaluate stays in current mode when ADX is in neutral zone`() {
        val candles = createCandles(200)
        val currentPrice = BigDecimal("55000")
        val sma200 = BigDecimal("50000")
        val atr = BigDecimal("500")

        every { smaCalculator.calculate(candles, 200) } returns sma200
        every { atrCalculator.calculate(candles, 14) } returns atr
        every { adxCalculator.calculate(candles, 14) } returns 30.0

        val decision1 = engine.evaluate(candles, currentPrice)
        val decision2 = engine.evaluate(candles, currentPrice)
        val decision3 = engine.evaluate(candles, currentPrice)
        assertTrue(decision3 is Decision.Trend)

        every { adxCalculator.calculate(candles, 14) } returns 25.0
        val decision4 = engine.evaluate(candles, currentPrice)
        assertTrue(decision4 is Decision.Trend)
    }

    @Test
    fun `evaluate calculates correct stop loss and take profit for Trend`() {
        val candles = createCandles(200)
        val currentPrice = BigDecimal("50000")
        val sma200 = BigDecimal("45000")
        val atr = BigDecimal("1000")

        every { smaCalculator.calculate(candles, 200) } returns sma200
        every { adxCalculator.calculate(candles, 14) } returns 30.0
        every { atrCalculator.calculate(candles, 14) } returns atr

        engine.evaluate(candles, currentPrice)
        engine.evaluate(candles, currentPrice)
        val decision = engine.evaluate(candles, currentPrice)

        assertTrue(decision is Decision.Trend)
        val trendDecision = decision as Decision.Trend

        val expectedStopLoss = currentPrice - (atr * BigDecimal("3.0"))
        val expectedTakeProfit = currentPrice + (atr * BigDecimal("6.0"))

        assertEquals(expectedStopLoss, trendDecision.stopLoss)
        assertEquals(expectedTakeProfit, trendDecision.takeProfit)
    }

    @Test
    fun `evaluate requires minimum candles`() {
        val candles = createCandles(100)
        val currentPrice = BigDecimal("50000")

        val exception = runCatching {
            engine.evaluate(candles, currentPrice)
        }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("Need at least 200 candles") == true)
    }

    @Test
    fun `evaluate immediately switches to Defense when price drops below SMA200`() {
        val candles = createCandles(200)
        val currentPrice = BigDecimal("55000")
        val sma200 = BigDecimal("50000")
        val atr = BigDecimal("500")

        every { smaCalculator.calculate(candles, 200) } returns sma200
        every { adxCalculator.calculate(candles, 14) } returns 30.0
        every { atrCalculator.calculate(candles, 14) } returns atr

        engine.evaluate(candles, currentPrice)
        engine.evaluate(candles, currentPrice)
        val trendDecision = engine.evaluate(candles, currentPrice)
        assertTrue(trendDecision is Decision.Trend)

        val lowPrice = BigDecimal("45000")
        val defenseDecision = engine.evaluate(candles, lowPrice)

        assertTrue(defenseDecision is Decision.Defense)
        assertEquals(lowPrice, (defenseDecision as Decision.Defense).currentPrice)
    }
}
