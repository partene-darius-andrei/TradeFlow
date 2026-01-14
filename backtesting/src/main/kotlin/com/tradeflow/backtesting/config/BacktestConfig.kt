package com.tradeflow.backtesting.config

import com.tradeflow.backtesting.data.NoiseLevel
import java.math.BigDecimal

data class BacktestConfig(
    // ===== CAPITAL & FEES =====
    val initialCapital: BigDecimal = "500.00".toBigDecimal(),

    // ===== TRADING FEES =====
    val entryFeeRate: BigDecimal = "0.0005".toBigDecimal(),
    val exitFeeRate: BigDecimal = "0.0002".toBigDecimal(),
    val exitSlippageRate: BigDecimal = "0.0005".toBigDecimal(),

    // ===== BACKTEST SETUP =====
    val primeSize: Int = 300,
    val lookbackWindow: Int = 200,
    val minCandlesRequired: Int = 200,
    val noiseLevel: NoiseLevel = NoiseLevel.NONE,

    // ===== STRATEGY PARAMETER RANGES (for optimization) =====
    val adxTrendThresholdRange: ClosedRange<Double> = 15.0..30.0,
    val adxRangeThresholdRange: ClosedRange<Double> = 0.5..2.0,
    val confirmationCandlesRange: IntRange = 1..5,
    val trendPositionPercentRange: ClosedRange<Double> = 0.01..0.15,
    val stopLossAtrMultiplierRange: ClosedRange<Double> = 3.0..20.0,
    val takeProfitAtrMultiplierRange: ClosedRange<Double> = 5.0..40.0,
    val leverageRange: ClosedRange<Double> = 1.0..10.0,

    // ===== GENETIC ALGORITHM =====
    val populationSize: Int = 50,
    val generations: Int = 100,
    val mutationRate: Double = 0.15,
    val eliteRatio: Double = 0.1,
    val tournamentSize: Int = 3,
    val reportInterval: Int = 10,

    // ===== MUTATION RANGES =====
    val adxMutationRange: ClosedRange<Double> = -2.0..2.0,
    val adxRangeMutationRange: ClosedRange<Double> = -0.3..0.3,
    val slMutationRange: ClosedRange<Double> = -2.0..2.0,
    val tpMutationRange: ClosedRange<Double> = -3.0..3.0,
    val positionMutationRange: ClosedRange<Double> = -0.01..0.01,
    val confirmationMutationRange: IntRange = -1..2,

    // ===== DATA GENERATION =====
    val defaultNumPeriods: Int = 50,
    val minPeriodDays: Int = 60,
    val maxPeriodDays: Int = 180,
    val lookbackBuffer: Int = 200,
    val seed: Int = 3,
    val trainTestRatio: Double = 0.7,

    // ===== FITNESS EVALUATION =====
    val sharpeWeight: Double = 0.4,
    val returnWeight: Double = 0.4,
    val drawdownPenalty: Double = 0.2,
    val sharpeNormalizationFactor: Double = 3.0,
    val returnNormalizationFactor: Double = 50.0,

    // ===== NOISE INJECTION =====
    val noiseLevelLow: NoiseProfile = NoiseProfile(
        priceVariance = 0.001,
        volumeVariance = 0.15,
        flashEventProbability = 0.001,
        gapEventProbability = 0.005,
        wickExtensionProbability = 0.02
    ),
    val noiseLevelMedium: NoiseProfile = NoiseProfile(
        priceVariance = 0.003,
        volumeVariance = 0.30,
        flashEventProbability = 0.005,
        gapEventProbability = 0.015,
        wickExtensionProbability = 0.05
    ),
    val noiseLevelHigh: NoiseProfile = NoiseProfile(
        priceVariance = 0.005,
        volumeVariance = 0.50,
        flashEventProbability = 0.015,
        gapEventProbability = 0.030,
        wickExtensionProbability = 0.10
    ),
    val noiseLevelExtreme: NoiseProfile = NoiseProfile(
        priceVariance = 0.010,
        volumeVariance = 0.80,
        flashEventProbability = 0.030,
        gapEventProbability = 0.050,
        wickExtensionProbability = 0.20
    ),
    val flashEventMagnitudeRange: ClosedRange<Double> = 0.01..0.05,
    val gapSizeRange: ClosedRange<Double> = 0.002..0.010,
    val wickExtensionRange: ClosedRange<Double> = 0.005..0.020,

    // ===== VALIDATION THRESHOLDS =====
    val highConsistencyThreshold: Int = 8,
    val highConsistencyPnLThreshold: Double = 3.0,
    val consistencyThreshold: Int = 7,
    val consistencyPnLThreshold: Double = 1.0,
    val moderateConsistencyThreshold: Int = 5,
    val moderateConsistencyPnLThreshold: Double = 0.0,
    val strongEdgeWinRateThreshold: Double = 55.0,
    val strongEdgePnLThreshold: Double = 3.0,
    val strongEdgeProfitableThreshold: Int = 8,
    val promiseWinRateThreshold: Double = 50.0,
    val promisePnLThreshold: Double = 0.0,
    val promiseProfitableThreshold: Int = 6,
    val minTradesForSignificance: Int = 200,
    val wellPerformingPnLThreshold: Double = 3.0,
    val wellPerformingWinRateThreshold: Double = 55.0,
    val marginalPnLThreshold: Double = 0.0,
    val marginalWinRateThreshold: Double = 50.0,
    val significantImprovementThreshold: Double = 10.0
) {
    companion object {
        fun default(): BacktestConfig = BacktestConfig()
    }
}

data class NoiseProfile(
    val priceVariance: Double,
    val volumeVariance: Double,
    val flashEventProbability: Double,
    val gapEventProbability: Double,
    val wickExtensionProbability: Double
)
