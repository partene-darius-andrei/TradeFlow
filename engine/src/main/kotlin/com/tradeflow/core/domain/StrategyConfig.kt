package com.tradeflow.core.domain

import kotlin.random.Random

data class StrategyConfig(
    val confirmationCandles: Param = Param(default = 3.0, min = 1.0, max = 5.0),
    val adxTrendThreshold: Param = Param(default = 25.782492479530777, min = 15.0, max = 30.0),
    val adxRangeThreshold: Param = Param(default = 1.9514741656281005, min = 0.5, max = 2.0),
    val stopLossAtrMultiplier: Param = Param(default = 1.0, min = 3.0, max = 20.0),
    val takeProfitAtrMultiplier: Param = Param(default = 2.0, min = 5.0, max = 40.0),
    val trendPositionPercent: Param = Param(default = 0.013717556606683175, min = 0.01, max = 0.15),
    val leverage: Param = Param(default = 3.863343975917127, min = 1.0, max = 10.0),
    val rangeEntryMultiplier: Param = Param(default = 0.7689869740896329, min = 0.3, max = 1.0),
    val rangeStopMultiplier: Param = Param(default = 2.5127263893683947, min = 1.5, max = 3.0),
    val rangeRsiMidpoint: Param = Param(default = 55.0, min = 45.0, max = 55.0),
    val smaPeriod: Param = Param(default = 233.0, min = 150.0, max = 250.0),
    val adxPeriod: Param = Param(default = 18.0, min = 10.0, max = 20.0),
    val atrPeriod: Param = Param(default = 14.0, min = 10.0, max = 20.0),
    val rsiPeriod: Param = Param(default = 18.0, min = 10.0, max = 20.0),
    val volumeSmaPeriod: Param = Param(default = 23.0, min = 10.0, max = 30.0),
    val minVolumeRatio: Param = Param(default = 2.0, min = 0.5, max = 2.0),
    val rsiLongBlockThreshold: Param = Param(default = 32.0142270461333, min = 25.0, max = 35.0),
    val rsiShortBlockThreshold: Param = Param(default = 66.95229008675221, min = 65.0, max = 75.0),
    val smaPreviousLookback: Param = Param(default = 6.0, min = 5.0, max = 20.0)
) {
    data class Param(
        var default: Double,
        val min: Double,
        val max: Double
    ) {
        val range: ClosedRange<Double> = min..max
        private val rangeSize: Double = max - min

        fun randomized(): Param = Param(
            default = Random.nextDouble(min, max),
            min = min,
            max = max
        )

        fun mutated(random: Random): Param {
            val mutationDelta = random.nextDouble(-rangeSize * 0.15, rangeSize * 0.15)
            return Param(
                default = (default + mutationDelta).coerceIn(min, max),
                min = min,
                max = max
            )
        }
    }

    companion object {
        const val MIN_CANDLES_REQUIRED: Int = 200
        const val PNL_PRECISION_DECIMAL_PLACES: Int = 6
        const val DEFAULT_CANDLE_DURATION_HOURS: Int = 4
        const val VOLUME_RATIO_DEFAULT_FALLBACK: Double = 1.0

        fun randomInRanges(): StrategyConfig {
            val template = StrategyConfig()
            return StrategyConfig(
                confirmationCandles = template.confirmationCandles.randomized(),
                adxTrendThreshold = template.adxTrendThreshold.randomized(),
                adxRangeThreshold = template.adxRangeThreshold.randomized(),
                stopLossAtrMultiplier = template.stopLossAtrMultiplier.randomized(),
                takeProfitAtrMultiplier = template.takeProfitAtrMultiplier.randomized(),
                trendPositionPercent = template.trendPositionPercent.randomized(),
                leverage = template.leverage.randomized(),
                rangeEntryMultiplier = template.rangeEntryMultiplier.randomized(),
                rangeStopMultiplier = template.rangeStopMultiplier.randomized(),
                rangeRsiMidpoint = template.rangeRsiMidpoint.randomized(),
                smaPeriod = template.smaPeriod.randomized(),
                adxPeriod = template.adxPeriod.randomized(),
                atrPeriod = template.atrPeriod.randomized(),
                rsiPeriod = template.rsiPeriod.randomized(),
                volumeSmaPeriod = template.volumeSmaPeriod.randomized(),
                minVolumeRatio = template.minVolumeRatio.randomized(),
                rsiLongBlockThreshold = template.rsiLongBlockThreshold.randomized(),
                rsiShortBlockThreshold = template.rsiShortBlockThreshold.randomized(),
                smaPreviousLookback = template.smaPreviousLookback.randomized()
            )
        }
    }
}
