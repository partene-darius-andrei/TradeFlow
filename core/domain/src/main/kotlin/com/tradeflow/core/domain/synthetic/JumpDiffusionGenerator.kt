package com.tradeflow.core.domain.synthetic

import com.tradeflow.core.domain.model.Candle
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.math.ln as naturalLog
import kotlin.math.cos
import kotlin.random.Random

fun Random.nextGaussian(): Double {
    val u1 = this.nextDouble()
    val u2 = this.nextDouble()
    return sqrt(-2.0 * naturalLog(u1)) * cos(2.0 * kotlin.math.PI * u2)
}

class JumpDiffusionGenerator(
    private val config: GenerationConfig = GenerationConfig(),
    private val jumpIntensity: Double = 0.05,
    private val jumpMean: Double = -0.02,
    private val jumpStdDev: Double = 0.03,
    private val volatilityOfVolatility: Double = 0.3
) : MarketGenerator {

    override fun getName(): String = "JumpDiffusion"

    override fun generate(nSteps: Int, seed: Long, noiseLevel: Double): List<Candle> {
        val random = Random(seed)
        val candles = mutableListOf<Candle>()

        var currentPrice = config.startPrice.toDouble()
        var currentTime = config.startTime
        var currentVol = config.volatilityAnnualized

        val dt = config.intervalMinutes / (365.0 * 24.0 * 60.0)
        val adjustedJumpIntensity = jumpIntensity * (1.0 + noiseLevel)

        repeat(nSteps) {
            val dW = random.nextGaussian()
            val dV = random.nextGaussian()

            val jump = if (random.nextDouble() < adjustedJumpIntensity * dt) {
                jumpMean + jumpStdDev * random.nextGaussian()
            } else {
                0.0
            }

            val drift = config.drift - 0.5 * currentVol * currentVol
            val diffusion = currentVol * sqrt(dt) * dW
            val logReturn = drift * dt + diffusion + jump

            currentVol = (currentVol + volatilityOfVolatility * currentVol * sqrt(dt) * dV)
                .coerceIn(0.1, 2.0)

            currentPrice *= exp(logReturn)

            val intrabarVol = currentVol * sqrt(dt) * (1.0 + noiseLevel * 0.5)
            val high = currentPrice * exp(intrabarVol * 2.0)
            val low = currentPrice * exp(-intrabarVol * 2.0)
            val open = low + random.nextDouble() * (high - low)
            val close = low + random.nextDouble() * (high - low)
            val volume = 100.0 * exp(random.nextGaussian() * 0.5)

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
