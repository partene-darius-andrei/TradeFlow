package com.tradeflow.core.domain.synthetic

import com.tradeflow.core.domain.model.Candle
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/**
 * Synthetic market data generator using Stationary Bootstrap resampling.
 *
 * Preserves statistical properties of historical data while generating new scenarios.
 * Unlike Jump Diffusion (which models price process), this resamples actual historical returns.
 *
 * Algorithm (Politis & Romano, 1994):
 * 1. Calculate log returns from historical candles
 * 2. Randomly select a starting return
 * 3. With probability p, jump to random new return (break block)
 * 4. Otherwise, continue to next sequential return (continue block)
 * 5. Reconstruct prices from resampled returns
 *
 * This preserves:
 * - Return distribution (fat tails, skewness)
 * - Autocorrelation structure
 * - Volatility clustering
 *
 * @property historicalData Real market data to resample from
 * @property config Generation parameters (start price, time, interval)
 */
class StationaryBootstrapGenerator(
    private val historicalData: List<Candle>,
    private val config: GenerationConfig = GenerationConfig()
) : MarketGenerator {

    override fun getName(): String = "StationaryBootstrap"

    /**
     * Generates synthetic data by resampling historical returns with block structure.
     *
     * The noiseLevel parameter controls expected block size:
     * - Low noise (0.0): Large blocks (10 returns) = more autocorrelation
     * - High noise (1.0): Small blocks (2 returns) = more randomness
     *
     * @param nSteps Number of candles to generate
     * @param seed Random seed for reproducibility
     * @param noiseLevel Block size control (0.0 = large blocks, 1.0 = small blocks)
     * @return List of synthetic candles preserving historical return characteristics
     * @throws IllegalArgumentException if historical data has fewer than 2 candles
     */
    override fun generate(nSteps: Int, seed: Long, noiseLevel: Double): List<Candle> {
        val random = Random(seed)
        val returns = calculateLogReturns(historicalData)

        if (returns.isEmpty()) {
            throw IllegalArgumentException("Historical data must have at least 2 candles")
        }

        val expectedBlockSize = (10.0 * (1.0 - noiseLevel) + 2.0 * noiseLevel).toInt().coerceAtLeast(2)
        val blockProbability = 1.0 / expectedBlockSize

        val syntheticReturns = mutableListOf<Double>()
        var currentIndex = random.nextInt(returns.size)

        while (syntheticReturns.size < nSteps) {
            syntheticReturns.add(returns[currentIndex])

            if (random.nextDouble() < blockProbability) {
                currentIndex = random.nextInt(returns.size)
            } else {
                currentIndex = (currentIndex + 1) % returns.size
            }
        }

        return reconstructCandles(syntheticReturns, config, seed)
    }

    /**
     * Calculates log returns from consecutive candles.
     * Log return = ln(price_t / price_{t-1})
     */
    private fun calculateLogReturns(candles: List<Candle>): List<Double> {
        return candles.zipWithNext { prev, curr ->
            ln(curr.close.toDouble() / prev.close.toDouble())
        }
    }

    /**
     * Reconstructs OHLCV candles from log returns.
     * Generates realistic intrabar price action (high/low/open/close) around the close price.
     */
    private fun reconstructCandles(
        returns: List<Double>,
        config: GenerationConfig,
        seed: Long
    ): List<Candle> {
        val random = Random(seed + 1)
        val candles = mutableListOf<Candle>()
        var currentPrice = config.startPrice.toDouble()
        var currentTime = config.startTime

        returns.forEach { logReturn ->
            currentPrice *= exp(logReturn)

            val high = currentPrice * (1.0 + random.nextDouble() * 0.005)
            val low = currentPrice * (1.0 - random.nextDouble() * 0.005)
            val open = low + random.nextDouble() * (high - low)
            val close = low + random.nextDouble() * (high - low)
            val volume = 100.0 + random.nextDouble() * 50.0

            candles.add(
                Candle(
                    timestamp = currentTime,
                    open = BigDecimal(open).setScale(2, RoundingMode.HALF_UP),
                    high = BigDecimal(high).setScale(2, RoundingMode.HALF_UP),
                    low = BigDecimal(low).setScale(2, RoundingMode.HALF_UP),
                    close = BigDecimal(close).setScale(2, RoundingMode.HALF_UP),
                    volume = BigDecimal(volume).setScale(2, RoundingMode.HALF_UP)
                )
            )

            currentTime = currentTime.plus(Duration.ofMinutes(config.intervalMinutes))
        }

        return candles
    }
}
