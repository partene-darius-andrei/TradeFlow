package com.tradeflow.backtesting.data

import com.tradeflow.core.domain.model.Candle
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

enum class NoiseLevel(
    val priceVariance: Double,
    val volumeVariance: Double,
    val flashEventProbability: Double,
    val gapEventProbability: Double,
    val wickExtensionProbability: Double
) {
    NONE(0.0, 0.0, 0.0, 0.0, 0.0),
    LOW(0.001, 0.15, 0.001, 0.005, 0.02),
    MEDIUM(0.003, 0.30, 0.005, 0.015, 0.05),
    HIGH(0.005, 0.50, 0.015, 0.030, 0.10),
    EXTREME(0.010, 0.80, 0.030, 0.050, 0.20)
}

object CandleNoiseInjector {

    fun injectNoise(
        candles: List<Candle>,
        noiseLevel: NoiseLevel = NoiseLevel.MEDIUM,
        seed: Long? = null
    ): List<Candle> {
        if (noiseLevel == NoiseLevel.NONE) return candles

        val random = seed?.let { Random(it) } ?: Random.Default
        val noisyCandles = mutableListOf<Candle>()

        candles.forEachIndexed { index, candle ->
            var noisyCandle = candle

            noisyCandle = applyPriceNoise(noisyCandle, noiseLevel.priceVariance, random)

            noisyCandle = applyVolumeNoise(noisyCandle, noiseLevel.volumeVariance, random)

            if (random.nextDouble() < noiseLevel.flashEventProbability) {
                noisyCandle = applyFlashEvent(noisyCandle, random)
            }

            if (index > 0 && random.nextDouble() < noiseLevel.gapEventProbability) {
                noisyCandle = applyGapEvent(noisyCandles.last(), noisyCandle, random)
            }

            if (random.nextDouble() < noiseLevel.wickExtensionProbability) {
                noisyCandle = applyWickExtension(noisyCandle, random)
            }

            noisyCandles.add(noisyCandle)
        }

        return noisyCandles
    }

    private fun applyPriceNoise(candle: Candle, variance: Double, random: Random): Candle {
        val openNoise = 1.0 + randomGaussian(random) * variance
        val highNoise = 1.0 + random.nextDouble(0.0, variance)
        val lowNoise = 1.0 - random.nextDouble(0.0, variance)
        val closeNoise = 1.0 + randomGaussian(random) * variance

        val newOpen = (candle.open.toDouble() * openNoise).toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        val newClose = (candle.close.toDouble() * closeNoise).toBigDecimal().setScale(2, RoundingMode.HALF_UP)

        val tempHigh = (candle.high.toDouble() * highNoise).toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        val tempLow = (candle.low.toDouble() * lowNoise).toBigDecimal().setScale(2, RoundingMode.HALF_UP)

        val newHigh = maxOf(newOpen, newClose, tempHigh)
        val newLow = minOf(newOpen, newClose, tempLow)

        return candle.copy(
            open = newOpen,
            high = newHigh,
            low = newLow,
            close = newClose
        )
    }

    private fun applyVolumeNoise(candle: Candle, variance: Double, random: Random): Candle {
        val volumeMultiplier = 1.0 + randomGaussian(random) * variance
        val newVolume = (candle.volume.toDouble() * volumeMultiplier)
            .coerceAtLeast(0.0)
            .toBigDecimal()
            .setScale(8, RoundingMode.HALF_UP)

        return candle.copy(volume = newVolume)
    }

    private fun applyFlashEvent(candle: Candle, random: Random): Candle {
        val flashMagnitude = random.nextDouble(0.01, 0.05)
        val isFlashCrash = random.nextBoolean()

        return if (isFlashCrash) {
            val flashLow = (candle.low.toDouble() * (1.0 - flashMagnitude))
                .toBigDecimal()
                .setScale(2, RoundingMode.HALF_UP)
            candle.copy(low = flashLow)
        } else {
            val flashHigh = (candle.high.toDouble() * (1.0 + flashMagnitude))
                .toBigDecimal()
                .setScale(2, RoundingMode.HALF_UP)
            candle.copy(high = flashHigh)
        }
    }

    private fun applyGapEvent(previousCandle: Candle, currentCandle: Candle, random: Random): Candle {
        val gapSize = random.nextDouble(0.002, 0.010)
        val isGapUp = random.nextBoolean()

        val gapMultiplier = if (isGapUp) 1.0 + gapSize else 1.0 - gapSize

        return currentCandle.copy(
            open = (previousCandle.close.toDouble() * gapMultiplier).toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            high = (currentCandle.high.toDouble() * gapMultiplier).toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            low = (currentCandle.low.toDouble() * gapMultiplier).toBigDecimal().setScale(2, RoundingMode.HALF_UP),
            close = (currentCandle.close.toDouble() * gapMultiplier).toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        )
    }

    private fun applyWickExtension(candle: Candle, random: Random): Candle {
        val wickExtension = random.nextDouble(0.005, 0.020)
        val extendHigh = random.nextBoolean()

        return if (extendHigh) {
            val newHigh = (candle.high.toDouble() * (1.0 + wickExtension))
                .toBigDecimal()
                .setScale(2, RoundingMode.HALF_UP)
            candle.copy(high = newHigh)
        } else {
            val newLow = (candle.low.toDouble() * (1.0 - wickExtension))
                .toBigDecimal()
                .setScale(2, RoundingMode.HALF_UP)
            candle.copy(low = newLow)
        }
    }

    private fun randomGaussian(random: Random): Double {
        var u1: Double
        var u2: Double
        do {
            u1 = random.nextDouble()
            u2 = random.nextDouble()
        } while (u1 <= 0.0)

        return kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * kotlin.math.PI * u2)
    }
}
