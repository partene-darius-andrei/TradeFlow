package com.tradeflow.backtesting.config

data class OptimizationConfig(
    val ga: GeneticAlgorithmConfig = GeneticAlgorithmConfig(),
    val fitness: FitnessWeights = FitnessWeights()
)

data class GeneticAlgorithmConfig(
    val populationSize: Int = 16,
    val generations: Int = 20,
    val mutationRate: Double = 0.15,
    val eliteRatio: Double = 0.1,
    val tournamentSize: Int = 3,
    val reportInterval: Int = 5
)

data class FitnessWeights(
    val sharpeWeight: Double = 0.4,
    val returnWeight: Double = 0.4,
    val drawdownPenalty: Double = 0.2,
    val sharpeNormalizationFactor: Double = 3.0,
    val returnNormalizationFactor: Double = 50.0
)
