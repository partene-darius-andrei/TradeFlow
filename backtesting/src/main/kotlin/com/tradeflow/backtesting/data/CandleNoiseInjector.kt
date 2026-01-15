package com.tradeflow.backtesting.data

import com.tradeflow.backtesting.config.NoiseConfig
import com.tradeflow.backtesting.config.NoiseProfile
import com.tradeflow.core.domain.model.Candle
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

private fun BigDecimal.toUsd() = this.setScale(2, RoundingMode.HALF_UP)

enum class NoiseLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    EXTREME;

    fun toNoiseProfile(config: NoiseConfig): NoiseProfile {
        return when (this) {
            NONE -> NoiseProfile(0.0, 0.0, 0.0, 0.0, 0.0)
            LOW -> config.noiseLevelLow
            MEDIUM -> config.noiseLevelMedium
            HIGH -> config.noiseLevelHigh
            EXTREME -> config.noiseLevelExtreme
        }
    }
}

object CandleNoiseInjector {

    fun injectNoise(
        candles: List<Candle>,
        noiseLevel: NoiseLevel = NoiseLevel.MEDIUM,
        config: NoiseConfig = NoiseConfig.default(),
        seed: Long? = null
    ): List<Candle> {
        if (noiseLevel == NoiseLevel.NONE) return candles

        val profile = noiseLevel.toNoiseProfile(config)
        val random = seed?.let { Random(it) } ?: Random.Default
        val noisyCandles = mutableListOf<Candle>()

        candles.forEachIndexed { index, candle ->
            var noisyCandle = candle

            noisyCandle = applyPriceNoise(noisyCandle, profile.priceVariance, random)

            noisyCandle = applyVolumeNoise(noisyCandle, profile.volumeVariance, random)

            if (random.nextDouble() < profile.flashEventProbability) {
                noisyCandle = applyFlashEvent(noisyCandle, config, random)
            }

            if (index > 0 && random.nextDouble() < profile.gapEventProbability) {
                noisyCandle = applyGapEvent(noisyCandles.last(), noisyCandle, config, random)
            }

            if (random.nextDouble() < profile.wickExtensionProbability) {
                noisyCandle = applyWickExtension(noisyCandle, config, random)
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

        val newOpen = (candle.open.toDouble() * openNoise).toBigDecimal().toUsd()
        val newClose = (candle.close.toDouble() * closeNoise).toBigDecimal().toUsd()

        val tempHigh = (candle.high.toDouble() * highNoise).toBigDecimal().toUsd()
        val tempLow = (candle.low.toDouble() * lowNoise).toBigDecimal().toUsd()

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

    private fun applyFlashEvent(candle: Candle, config: NoiseConfig, random: Random): Candle {
        val flashMagnitude = random.nextDouble(config.flashEventMagnitudeRange.start, config.flashEventMagnitudeRange.endInclusive)
        val isFlashCrash = random.nextBoolean()

        return if (isFlashCrash) {
            val flashLow = (candle.low.toDouble() * (1.0 - flashMagnitude))
                .toBigDecimal()
                .toUsd()
            candle.copy(low = flashLow)
        } else {
            val flashHigh = (candle.high.toDouble() * (1.0 + flashMagnitude))
                .toBigDecimal()
                .toUsd()
            candle.copy(high = flashHigh)
        }
    }

    private fun applyGapEvent(previousCandle: Candle, currentCandle: Candle, config: NoiseConfig, random: Random): Candle {
        val gapSize = random.nextDouble(config.gapSizeRange.start, config.gapSizeRange.endInclusive)
        val isGapUp = random.nextBoolean()

        val gapMultiplier = if (isGapUp) 1.0 + gapSize else 1.0 - gapSize

        return currentCandle.copy(
            open = (previousCandle.close.toDouble() * gapMultiplier).toBigDecimal().toUsd(),
            high = (currentCandle.high.toDouble() * gapMultiplier).toBigDecimal().toUsd(),
            low = (currentCandle.low.toDouble() * gapMultiplier).toBigDecimal().toUsd(),
            close = (currentCandle.close.toDouble() * gapMultiplier).toBigDecimal().toUsd()
        )
    }

    private fun applyWickExtension(candle: Candle, config: NoiseConfig, random: Random): Candle {
        val wickExtension = random.nextDouble(config.wickExtensionRange.start, config.wickExtensionRange.endInclusive)
        val extendHigh = random.nextBoolean()

        return if (extendHigh) {
            val newHigh = (candle.high.toDouble() * (1.0 + wickExtension))
                .toBigDecimal()
                .toUsd()
            candle.copy(high = newHigh)
        } else {
            val newLow = (candle.low.toDouble() * (1.0 - wickExtension))
                .toBigDecimal()
                .toUsd()
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
