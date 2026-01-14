package com.tradeflow.backtesting.optimization

import kotlin.random.Random

data class Chromosome(
    val params: TradingParameters
) {
    companion object {
        fun random(random: Random): Chromosome {
            return Chromosome(TradingParameters.random(random))
        }

        fun fromTradingParameters(params: TradingParameters): Chromosome {
            return Chromosome(params)
        }
    }
}

data class Individual(
    val chromosome: Chromosome,
    var fitness: Double = 0.0
)

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

class GeneticOptimizer(
    private val populationSize: Int = 50,
    private val generations: Int = 100,
    private val mutationRate: Double = 0.15,
    private val eliteRatio: Double = 0.1
) {

    fun optimize(
        fitnessFunction: (TradingParameters) -> Double,
        seed: Long = System.currentTimeMillis()
    ): OptimizationResult {
        val random = Random(seed)
        var population = initializePopulation(random)

        val evolutionHistory = mutableListOf<GenerationStats>()

        println("\n🧬 GENETIC ALGORITHM OPTIMIZATION")
        println("=".repeat(80))
        println("Population: $populationSize | Generations: $generations")
        println("Mutation Rate: ${(mutationRate * 100).toInt()}% | Elite Ratio: ${(eliteRatio * 100).toInt()}%")
        println("=".repeat(80))

        repeat(generations) { gen ->
            population.forEach { individual ->
                individual.fitness = fitnessFunction(individual.chromosome.params)
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
                println("Gen $gen | Best: ${"%.4f".format(stats.bestFitness)} | " +
                    "Avg: ${"%.4f".format(stats.avgFitness)} | " +
                    "Worst: ${"%.4f".format(stats.worstFitness)}")
            }

            if (gen < generations - 1) {
                population = evolvePopulation(population, random)
            }
        }

        val champion = population.first()

        println("\n🏆 OPTIMIZATION COMPLETE")
        println("=".repeat(80))
        println("Champion Fitness: ${champion.fitness}")
        println("\nOptimal Parameters:")
        println("  ADX Trend Threshold:       ${champion.chromosome.params.adxTrendThreshold}")
        println("  ADX Range Threshold:       ${champion.chromosome.params.adxRangeThreshold}")
        println("  Stop Loss ATR Multiplier:  ${champion.chromosome.params.stopLossAtrMultiplier}")
        println("  Take Profit ATR Multiplier: ${champion.chromosome.params.takeProfitAtrMultiplier}")
        println("  Trend Position %:          ${(champion.chromosome.params.trendPositionPercent * 100).let { "%.2f".format(it) }}%")
        println("  Confirmation Candles:      ${champion.chromosome.params.confirmationCandles}")
        println("=".repeat(80))

        return OptimizationResult(
            champion = champion.chromosome,
            fitness = champion.fitness,
            history = evolutionHistory
        )
    }

    private fun initializePopulation(random: Random): List<Individual> {
        return List(populationSize) {
            Individual(Chromosome.random(random))
        }
    }

    private fun evolvePopulation(
        population: List<Individual>,
        random: Random
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
                child = mutate(child, random)
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
        val p1 = parent1.params
        val p2 = parent2.params

        return Chromosome(
            TradingParameters(
                adxTrendThreshold = if (random.nextBoolean()) p1.adxTrendThreshold else p2.adxTrendThreshold,
                adxRangeThreshold = if (random.nextBoolean()) p1.adxRangeThreshold else p2.adxRangeThreshold,
                confirmationCandles = if (random.nextBoolean()) p1.confirmationCandles else p2.confirmationCandles,
                trendPositionPercent = if (random.nextBoolean()) p1.trendPositionPercent else p2.trendPositionPercent,
                stopLossAtrMultiplier = if (random.nextBoolean()) p1.stopLossAtrMultiplier else p2.stopLossAtrMultiplier,
                takeProfitAtrMultiplier = if (random.nextBoolean()) p1.takeProfitAtrMultiplier else p2.takeProfitAtrMultiplier
            )
        )
    }

    private fun mutate(chromosome: Chromosome, random: Random): Chromosome {
        val p = chromosome.params

        return when (random.nextInt(6)) {
            0 -> Chromosome(p.copy(
                adxTrendThreshold = (p.adxTrendThreshold + random.nextDouble(-2.0, 2.0)).coerceIn(15.0, 30.0)
            ))
            1 -> Chromosome(p.copy(
                adxRangeThreshold = (p.adxRangeThreshold + random.nextDouble(-0.3, 0.3)).coerceIn(0.5, 2.0)
            ))
            2 -> Chromosome(p.copy(
                stopLossAtrMultiplier = (p.stopLossAtrMultiplier + random.nextDouble(-2.0, 2.0)).coerceIn(3.0, 20.0)
            ))
            3 -> Chromosome(p.copy(
                takeProfitAtrMultiplier = (p.takeProfitAtrMultiplier + random.nextDouble(-3.0, 3.0)).coerceIn(5.0, 40.0)
            ))
            4 -> Chromosome(p.copy(
                trendPositionPercent = (p.trendPositionPercent + random.nextDouble(-0.01, 0.01)).coerceIn(0.01, 0.15)
            ))
            else -> Chromosome(p.copy(
                confirmationCandles = (p.confirmationCandles + random.nextInt(-1, 2)).coerceIn(1, 5)
            ))
        }
    }
}
