package com.tradeflow.backtesting.config

import com.tradeflow.backtesting.data.NoiseLevel

data class NoiseProfile(
    val priceVariance: Double,
    val volumeVariance: Double,
    val flashEventProbability: Double,
    val gapEventProbability: Double,
    val wickExtensionProbability: Double,
    val flashEventMagnitudeRange: ClosedRange<Double> = 0.01..0.05,
    val gapSizeRange: ClosedRange<Double> = 0.002..0.010,
    val wickExtensionRange: ClosedRange<Double> = 0.005..0.020
)

data class NoiseConfig(
    val profiles: Map<NoiseLevel, NoiseProfile> = mapOf(
        NoiseLevel.NONE to NoiseProfile(
            priceVariance = 0.0,
            volumeVariance = 0.0,
            flashEventProbability = 0.0,
            gapEventProbability = 0.0,
            wickExtensionProbability = 0.0
        ),
        NoiseLevel.LOW to NoiseProfile(
            priceVariance = 0.001,
            volumeVariance = 0.15,
            flashEventProbability = 0.001,
            gapEventProbability = 0.005,
            wickExtensionProbability = 0.02
        ),
        NoiseLevel.MEDIUM to NoiseProfile(
            priceVariance = 0.003,
            volumeVariance = 0.30,
            flashEventProbability = 0.005,
            gapEventProbability = 0.015,
            wickExtensionProbability = 0.05
        ),
        NoiseLevel.HIGH to NoiseProfile(
            priceVariance = 0.005,
            volumeVariance = 0.50,
            flashEventProbability = 0.015,
            gapEventProbability = 0.030,
            wickExtensionProbability = 0.10
        ),
        NoiseLevel.EXTREME to NoiseProfile(
            priceVariance = 0.010,
            volumeVariance = 0.80,
            flashEventProbability = 0.030,
            gapEventProbability = 0.050,
            wickExtensionProbability = 0.20
        )
    )
)
