package com.tradeflow.core.domain.synthetic

import com.tradeflow.core.domain.util.BinanceDataLoader
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratorValidationTest {

    @Test
    fun `stationary bootstrap generator produces valid candles`() {
        val historicalData = BinanceDataLoader.fetchHistoricalCandles(interval = "4h", limit = 300)
        val generator = StationaryBootstrapGenerator(historicalData)

        val syntheticCandles = generator.generate(
            nSteps = 100,
            seed = 42,
            noiseLevel = 0.2
        )

        assertEquals(100, syntheticCandles.size, "Should generate requested number of candles")

        syntheticCandles.forEach { candle ->
            assertTrue(candle.high >= candle.low, "High must be >= Low")
            assertTrue(candle.high >= candle.open, "High must be >= Open")
            assertTrue(candle.high >= candle.close, "High must be >= Close")
            assertTrue(candle.low <= candle.open, "Low must be <= Open")
            assertTrue(candle.low <= candle.close, "Low must be <= Close")
            assertTrue(candle.volume > java.math.BigDecimal.ZERO, "Volume must be positive")
        }

        println("✅ Bootstrap Generator: Generated ${syntheticCandles.size} valid candles")
        println("   Price Range: ${syntheticCandles.minOf { it.low }} - ${syntheticCandles.maxOf { it.high }}")
    }

    @Test
    fun `jump diffusion generator produces realistic volatility`() {
        val generator = JumpDiffusionGenerator(
            config = GenerationConfig(),
            jumpIntensity = 0.05
        )

        val syntheticCandles = generator.generate(
            nSteps = 200,
            seed = 123,
            noiseLevel = 0.1
        )

        assertEquals(200, syntheticCandles.size, "Should generate requested number of candles")

        val returns = syntheticCandles.zipWithNext { prev, curr ->
            kotlin.math.ln(curr.close.toDouble() / prev.close.toDouble())
        }

        val avgReturn = returns.average()
        val stdDev = kotlin.math.sqrt(returns.map { (it - avgReturn) * (it - avgReturn) }.average())

        println("✅ Jump Diffusion Generator: Generated ${syntheticCandles.size} candles")
        println("   Average Return: ${(avgReturn * 100).toBigDecimal().setScale(4, java.math.RoundingMode.HALF_UP)}%")
        println("   Volatility (StdDev): ${(stdDev * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")

        assertTrue(stdDev > 0, "Volatility should be positive")
        assertTrue(stdDev < 0.5, "Volatility should be realistic (< 50%)")
    }

    @Test
    fun `generators are deterministic with same seed`() {
        val historicalData = BinanceDataLoader.fetchHistoricalCandles(interval = "4h", limit = 300)
        val generator1 = StationaryBootstrapGenerator(historicalData)
        val generator2 = StationaryBootstrapGenerator(historicalData)

        val candles1 = generator1.generate(nSteps = 50, seed = 999, noiseLevel = 0.1)
        val candles2 = generator2.generate(nSteps = 50, seed = 999, noiseLevel = 0.1)

        assertEquals(candles1.size, candles2.size, "Same seed should produce same length")

        candles1.zip(candles2).forEach { (c1, c2) ->
            assertEquals(c1.close, c2.close, "Same seed should produce identical candles")
        }

        println("✅ Determinism Test: Same seed produces identical output")
    }

    @Test
    fun `noise level controls deviation from base pattern`() {
        val historicalData = BinanceDataLoader.fetchHistoricalCandles(interval = "4h", limit = 300)
        val generator = StationaryBootstrapGenerator(historicalData)

        val lowNoise = generator.generate(nSteps = 100, seed = 42, noiseLevel = 0.0)
        val highNoise = generator.generate(nSteps = 100, seed = 42, noiseLevel = 0.5)

        val lowNoiseReturns = lowNoise.zipWithNext { prev, curr ->
            kotlin.math.ln(curr.close.toDouble() / prev.close.toDouble())
        }

        val highNoiseReturns = highNoise.zipWithNext { prev, curr ->
            kotlin.math.ln(curr.close.toDouble() / prev.close.toDouble())
        }

        val lowVolatility = kotlin.math.sqrt(lowNoiseReturns.map { it * it }.average())
        val highVolatility = kotlin.math.sqrt(highNoiseReturns.map { it * it }.average())

        println("✅ Noise Control Test:")
        println("   Low Noise (0.0) Volatility:  ${(lowVolatility * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")
        println("   High Noise (0.5) Volatility: ${(highVolatility * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")

        assertTrue(highVolatility >= lowVolatility * 0.8, "Higher noise should produce comparable or higher volatility")
    }
}
