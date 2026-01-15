package com.tradeflow.backtesting.config

data class OptimizationConfig(
    val ranges: OptimizationRanges = OptimizationRanges.default(),
    val ga: GeneticAlgorithmConfig = GeneticAlgorithmConfig.default(),
    val mutations: MutationRanges = MutationRanges.default(),
    val fitness: FitnessWeights = FitnessWeights.default()
) {
    companion object {
        fun default(): OptimizationConfig = OptimizationConfig()
    }
}

data class OptimizationRanges(
    val adxTrendThreshold: ClosedRange<Double> = 15.0..30.0,
    val adxRangeThreshold: ClosedRange<Double> = 0.5..2.0,
    val confirmationCandles: IntRange = 1..5,
    val trendPositionPercent: ClosedRange<Double> = 0.01..0.15,
    val stopLossAtrMultiplier: ClosedRange<Double> = 3.0..20.0,
    val takeProfitAtrMultiplier: ClosedRange<Double> = 5.0..40.0,
    val leverage: ClosedRange<Double> = 1.0..10.0
) {
    companion object {
        fun default(): OptimizationRanges = OptimizationRanges()
    }
}

data class GeneticAlgorithmConfig(
    val populationSize: Int = 20,
    val generations: Int = 30,
    val mutationRate: Double = 0.15,
    val eliteRatio: Double = 0.1,
    val tournamentSize: Int = 3,
    val reportInterval: Int = 10
) {
    companion object {
        fun default(): GeneticAlgorithmConfig = GeneticAlgorithmConfig()
    }
}

data class MutationRanges(
    val adx: ClosedRange<Double> = -2.0..2.0,
    val adxRange: ClosedRange<Double> = -0.3..0.3,
    val stopLoss: ClosedRange<Double> = -2.0..2.0,
    val takeProfit: ClosedRange<Double> = -3.0..3.0,
    val position: ClosedRange<Double> = -0.01..0.01,
    val confirmation: IntRange = -1..2
) {
    companion object {
        fun default(): MutationRanges = MutationRanges()
    }
}

data class FitnessWeights(
    val sharpeWeight: Double = 0.4,
    val returnWeight: Double = 0.4,
    val drawdownPenalty: Double = 0.2,
    val sharpeNormalizationFactor: Double = 3.0,
    val returnNormalizationFactor: Double = 50.0
) {
    companion object {
        fun default(): FitnessWeights = FitnessWeights()
    }
}
