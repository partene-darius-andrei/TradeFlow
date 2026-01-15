package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.config.OptimizationConfig
import com.tradeflow.core.domain.StrategyConfig
import java.math.BigDecimal
import kotlin.random.Random

data class Chromosome(
    val config: StrategyConfig
) {
    companion object {
        fun random(random: Random, ranges: OptimizationConfig = OptimizationConfig()): Chromosome {
            val r = ranges.ranges
            return Chromosome(
                StrategyConfig(
                    adxTrendThreshold = random.nextDouble(r.adxTrendThreshold.start, r.adxTrendThreshold.endInclusive),
                    adxRangeThreshold = random.nextDouble(r.adxRangeThreshold.start, r.adxRangeThreshold.endInclusive),
                    confirmationCandles = random.nextInt(r.confirmationCandles.first, r.confirmationCandles.last + 1),
                    trendPositionPercent = random.nextDouble(r.trendPositionPercent.start, r.trendPositionPercent.endInclusive).bd(),
                    stopLossAtrMultiplier = random.nextDouble(r.stopLossAtrMultiplier.start, r.stopLossAtrMultiplier.endInclusive).bd(),
                    takeProfitAtrMultiplier = random.nextDouble(r.takeProfitAtrMultiplier.start, r.takeProfitAtrMultiplier.endInclusive).bd(),
                    leverage = random.nextDouble(r.leverage.start, r.leverage.endInclusive).bd()
                )
            )
        }
    }
}

private fun Double.bd(): BigDecimal = BigDecimal(this.toString())

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
    private val config: OptimizationConfig = OptimizationConfig()
) {
    private val populationSize: Int = config.ga.populationSize
    private val generations: Int = config.ga.generations
    private val mutationRate: Double = config.ga.mutationRate
    private val eliteRatio: Double = config.ga.eliteRatio

    fun optimize(
        fitnessFunction: (StrategyConfig) -> Double,
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
                individual.fitness = fitnessFunction(individual.chromosome.config)
            }

            population = population.sortedByDescending { it.fitness }

            val stats = GenerationStats(
                generation = gen,
                bestFitness = population.first().fitness,
                avgFitness = population.map { it.fitness }.average(),
                worstFitness = population.last().fitness
            )
            evolutionHistory.add(stats)

            if (gen % config.ga.reportInterval == 0) {
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
        val cfg = champion.chromosome.config
        println("  ADX Trend Threshold:       ${cfg.adxTrendThreshold}")
        println("  ADX Range Threshold:       ${cfg.adxRangeThreshold}")
        println("  Stop Loss ATR Multiplier:  ${cfg.stopLossAtrMultiplier}")
        println("  Take Profit ATR Multiplier: ${cfg.takeProfitAtrMultiplier}")
        println("  Trend Position %:          ${(cfg.trendPositionPercent.toDouble() * 100).let { "%.2f".format(it) }}%")
        println("  Confirmation Candles:      ${cfg.confirmationCandles}")
        println("  Leverage:                  ${cfg.leverage}x")
        println("=".repeat(80))

        return OptimizationResult(
            champion = champion.chromosome,
            fitness = champion.fitness,
            history = evolutionHistory
        )
    }

    private fun initializePopulation(random: Random): List<Individual> {
        return List(populationSize) {
            Individual(Chromosome.random(random, config))
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
        val tournamentSize = config.ga.tournamentSize
        val tournament = (0 until tournamentSize).map {
            population[random.nextInt(population.size)]
        }
        return tournament.maxByOrNull { it.fitness }!!
    }

    private fun crossover(parent1: Chromosome, parent2: Chromosome, random: Random): Chromosome {
        val p1 = parent1.config
        val p2 = parent2.config

        return Chromosome(
            StrategyConfig(
                adxTrendThreshold = if (random.nextBoolean()) p1.adxTrendThreshold else p2.adxTrendThreshold,
                adxRangeThreshold = if (random.nextBoolean()) p1.adxRangeThreshold else p2.adxRangeThreshold,
                confirmationCandles = if (random.nextBoolean()) p1.confirmationCandles else p2.confirmationCandles,
                trendPositionPercent = if (random.nextBoolean()) p1.trendPositionPercent else p2.trendPositionPercent,
                stopLossAtrMultiplier = if (random.nextBoolean()) p1.stopLossAtrMultiplier else p2.stopLossAtrMultiplier,
                takeProfitAtrMultiplier = if (random.nextBoolean()) p1.takeProfitAtrMultiplier else p2.takeProfitAtrMultiplier,
                leverage = if (random.nextBoolean()) p1.leverage else p2.leverage
            )
        )
    }

    private fun mutate(chromosome: Chromosome, random: Random): Chromosome {
        val p = chromosome.config
        val ranges = config.ranges
        val mutations = config.mutations

        return when (random.nextInt(7)) {
            0 -> Chromosome(p.copy(
                adxTrendThreshold = (p.adxTrendThreshold + random.nextDouble(mutations.adx.start, mutations.adx.endInclusive))
                    .coerceIn(ranges.adxTrendThreshold.start, ranges.adxTrendThreshold.endInclusive)
            ))
            1 -> Chromosome(p.copy(
                adxRangeThreshold = (p.adxRangeThreshold + random.nextDouble(mutations.adxRange.start, mutations.adxRange.endInclusive))
                    .coerceIn(ranges.adxRangeThreshold.start, ranges.adxRangeThreshold.endInclusive)
            ))
            2 -> Chromosome(p.copy(
                stopLossAtrMultiplier = (p.stopLossAtrMultiplier.toDouble() + random.nextDouble(mutations.stopLoss.start, mutations.stopLoss.endInclusive))
                    .coerceIn(ranges.stopLossAtrMultiplier.start, ranges.stopLossAtrMultiplier.endInclusive).bd()
            ))
            3 -> Chromosome(p.copy(
                takeProfitAtrMultiplier = (p.takeProfitAtrMultiplier.toDouble() + random.nextDouble(mutations.takeProfit.start, mutations.takeProfit.endInclusive))
                    .coerceIn(ranges.takeProfitAtrMultiplier.start, ranges.takeProfitAtrMultiplier.endInclusive).bd()
            ))
            4 -> Chromosome(p.copy(
                trendPositionPercent = (p.trendPositionPercent.toDouble() + random.nextDouble(mutations.position.start, mutations.position.endInclusive))
                    .coerceIn(ranges.trendPositionPercent.start, ranges.trendPositionPercent.endInclusive).bd()
            ))
            5 -> Chromosome(p.copy(
                confirmationCandles = (p.confirmationCandles + random.nextInt(mutations.confirmation.first, mutations.confirmation.last + 1))
                    .coerceIn(ranges.confirmationCandles.first, ranges.confirmationCandles.last)
            ))
            else -> Chromosome(p.copy(
                leverage = (p.leverage.toDouble() + random.nextDouble(-1.0, 1.0))
                    .coerceIn(ranges.leverage.start, ranges.leverage.endInclusive).bd()
            ))
        }
    }
}
