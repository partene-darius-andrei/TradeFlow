package com.tradeflow.core.domain.optimization

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.StrategyParameters
import com.tradeflow.core.domain.config.TradingConfig
import java.math.BigDecimal
import kotlin.random.Random

data class Chromosome(
    val adxTrendThreshold: Double,
    val adxRangeThreshold: Double,
    val stopLossAtrMultiplier: Double,
    val takeProfitAtrMultiplier: Double,
    val trendPositionPercent: Double,
    val gridPositionPercentPerLevel: Double,
    val confirmationCandles: Int
) {
    fun toStrategyParameters(): StrategyParameters {
        return StrategyParameters(
            adxTrendThreshold = adxTrendThreshold,
            adxRangeThreshold = adxRangeThreshold,
            stopLossAtrMultiplier = BigDecimal(stopLossAtrMultiplier.toString()),
            takeProfitAtrMultiplier = BigDecimal(takeProfitAtrMultiplier.toString()),
            trendPositionPercent = BigDecimal(trendPositionPercent.toString()),
            gridPositionPercentPerLevel = BigDecimal(gridPositionPercentPerLevel.toString()),
            confirmationCandles = confirmationCandles
        )
    }

    companion object {
        fun random(random: Random, profile: RiskProfile): Chromosome {
            val baseConfig = profile.createConfig().strategy

            return Chromosome(
                adxTrendThreshold = baseConfig.adxTrendThreshold + random.nextDouble(-5.0, 5.0),
                adxRangeThreshold = baseConfig.adxRangeThreshold + random.nextDouble(-0.5, 0.5),
                stopLossAtrMultiplier = baseConfig.stopLossAtrMultiplier.toDouble() + random.nextDouble(-3.0, 3.0),
                takeProfitAtrMultiplier = baseConfig.takeProfitAtrMultiplier.toDouble() + random.nextDouble(-5.0, 5.0),
                trendPositionPercent = (baseConfig.trendPositionPercent.toDouble() + random.nextDouble(-0.02, 0.02)).coerceIn(0.01, 0.15),
                gridPositionPercentPerLevel = (baseConfig.gridPositionPercentPerLevel.toDouble() + random.nextDouble(-0.02, 0.02)).coerceIn(0.01, 0.15),
                confirmationCandles = (baseConfig.confirmationCandles + random.nextInt(-1, 2)).coerceIn(1, 5)
            )
        }
    }
}

data class Individual(
    val chromosome: Chromosome,
    var fitness: Double = 0.0
)

class GeneticOptimizer(
    private val populationSize: Int = 50,
    private val generations: Int = 100,
    private val mutationRate: Double = 0.15,
    private val eliteRatio: Double = 0.1
) {

    fun optimize(
        profile: RiskProfile,
        fitnessFunction: (Chromosome) -> Double,
        seed: Long = System.currentTimeMillis()
    ): OptimizationResult {
        val random = Random(seed)
        var population = initializePopulation(random, profile)

        val evolutionHistory = mutableListOf<GenerationStats>()

        println("\n🧬 GENETIC ALGORITHM OPTIMIZATION")
        println("=".repeat(80))
        println("Population: $populationSize | Generations: $generations")
        println("Mutation Rate: ${(mutationRate * 100).toInt()}% | Elite Ratio: ${(eliteRatio * 100).toInt()}%")
        println("=".repeat(80))

        repeat(generations) { gen ->
            population.forEach { individual ->
                individual.fitness = fitnessFunction(individual.chromosome)
            }

            population = population.sortedByDescending { it.fitness }

            val stats = GenerationStats(
                generation = gen,
                bestFitness = population.first().fitness,
                avgFitness = population.map { it.fitness }.average(),
                worstFitness = population.last().fitness
            )
            evolutionHistory.add(stats)

            if (gen % 10 == 0) {
                println("Gen $gen | Best: ${stats.bestFitness.toBigDecimal().setScale(4)} | " +
                    "Avg: ${stats.avgFitness.toBigDecimal().setScale(4)} | " +
                    "Worst: ${stats.worstFitness.toBigDecimal().setScale(4)}")
            }

            if (gen < generations - 1) {
                population = evolvePopulation(population, random, profile)
            }
        }

        val champion = population.first()

        println("\n🏆 OPTIMIZATION COMPLETE")
        println("=".repeat(80))
        println("Champion Fitness: ${champion.fitness}")
        println("\nOptimal Parameters:")
        println("  ADX Trend Threshold:       ${champion.chromosome.adxTrendThreshold}")
        println("  ADX Range Threshold:       ${champion.chromosome.adxRangeThreshold}")
        println("  Stop Loss ATR Multiplier:  ${champion.chromosome.stopLossAtrMultiplier}")
        println("  Take Profit ATR Multiplier: ${champion.chromosome.takeProfitAtrMultiplier}")
        println("  Trend Position %:          ${(champion.chromosome.trendPositionPercent * 100).toBigDecimal().setScale(2)}%")
        println("  Grid Position %:           ${(champion.chromosome.gridPositionPercentPerLevel * 100).toBigDecimal().setScale(2)}%")
        println("  Confirmation Candles:      ${champion.chromosome.confirmationCandles}")
        println("=".repeat(80))

        return OptimizationResult(
            champion = champion.chromosome,
            fitness = champion.fitness,
            history = evolutionHistory
        )
    }

    private fun initializePopulation(random: Random, profile: RiskProfile): List<Individual> {
        return List(populationSize) {
            Individual(Chromosome.random(random, profile))
        }
    }

    private fun evolvePopulation(
        population: List<Individual>,
        random: Random,
        profile: RiskProfile
    ): List<Individual> {
        val eliteCount = (populationSize * eliteRatio).toInt()
        val elite = population.take(eliteCount)

        val offspring = mutableListOf<Individual>()
        offspring.addAll(elite)

        while (offspring.size < populationSize) {
            val parent1 = tournamentSelection(population, random)
            val parent2 = tournamentSelection(population, random)

            var child = crossover(parent1.chromosome, parent2.chromosome, random)

            if (random.nextDouble() < mutationRate) {
                child = mutate(child, random, profile)
            }

            offspring.add(Individual(child))
        }

        return offspring
    }

    private fun tournamentSelection(population: List<Individual>, random: Random): Individual {
        val tournamentSize = 3
        val tournament = (0 until tournamentSize).map {
            population[random.nextInt(population.size)]
        }
        return tournament.maxByOrNull { it.fitness }!!
    }

    private fun crossover(parent1: Chromosome, parent2: Chromosome, random: Random): Chromosome {
        return Chromosome(
            adxTrendThreshold = if (random.nextBoolean()) parent1.adxTrendThreshold else parent2.adxTrendThreshold,
            adxRangeThreshold = if (random.nextBoolean()) parent1.adxRangeThreshold else parent2.adxRangeThreshold,
            stopLossAtrMultiplier = if (random.nextBoolean()) parent1.stopLossAtrMultiplier else parent2.stopLossAtrMultiplier,
            takeProfitAtrMultiplier = if (random.nextBoolean()) parent1.takeProfitAtrMultiplier else parent2.takeProfitAtrMultiplier,
            trendPositionPercent = if (random.nextBoolean()) parent1.trendPositionPercent else parent2.trendPositionPercent,
            gridPositionPercentPerLevel = if (random.nextBoolean()) parent1.gridPositionPercentPerLevel else parent2.gridPositionPercentPerLevel,
            confirmationCandles = if (random.nextBoolean()) parent1.confirmationCandles else parent2.confirmationCandles
        )
    }

    private fun mutate(chromosome: Chromosome, random: Random, profile: RiskProfile): Chromosome {
        return when (random.nextInt(7)) {
            0 -> chromosome.copy(adxTrendThreshold = chromosome.adxTrendThreshold + random.nextDouble(-2.0, 2.0))
            1 -> chromosome.copy(adxRangeThreshold = chromosome.adxRangeThreshold + random.nextDouble(-0.3, 0.3))
            2 -> chromosome.copy(stopLossAtrMultiplier = (chromosome.stopLossAtrMultiplier + random.nextDouble(-2.0, 2.0)).coerceAtLeast(3.0))
            3 -> chromosome.copy(takeProfitAtrMultiplier = (chromosome.takeProfitAtrMultiplier + random.nextDouble(-3.0, 3.0)).coerceAtLeast(5.0))
            4 -> chromosome.copy(trendPositionPercent = (chromosome.trendPositionPercent + random.nextDouble(-0.01, 0.01)).coerceIn(0.01, 0.15))
            5 -> chromosome.copy(gridPositionPercentPerLevel = (chromosome.gridPositionPercentPerLevel + random.nextDouble(-0.01, 0.01)).coerceIn(0.01, 0.15))
            else -> chromosome.copy(confirmationCandles = (chromosome.confirmationCandles + random.nextInt(-1, 2)).coerceIn(1, 5))
        }
    }
}

data class GenerationStats(
    val generation: Int,
    val bestFitness: Double,
    val avgFitness: Double,
    val worstFitness: Double
)

data class OptimizationResult(
    val champion: Chromosome,
    val fitness: Double,
    val history: List<GenerationStats>
)
