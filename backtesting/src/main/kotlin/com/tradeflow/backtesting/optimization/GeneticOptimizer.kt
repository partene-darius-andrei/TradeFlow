package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.config.OptimizationConfig
import com.tradeflow.core.domain.StrategyConfig
import kotlinx.coroutines.*
import kotlin.random.Random

data class Individual(
    val config: StrategyConfig,
    var fitness: Double = 0.0
)

data class GenerationStats(
    val generation: Int,
    val bestFitness: Double,
    val avgFitness: Double,
    val worstFitness: Double
)

data class OptimizationResult(
    val champion: StrategyConfig,
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
    ): OptimizationResult = runBlocking {
        val random = Random(seed)
        var population = initializePopulation(random)

        val evolutionHistory = mutableListOf<GenerationStats>()

        println("\n🧬 GENETIC ALGORITHM OPTIMIZATION (PARALLEL)")
        println("=".repeat(80))
        println("Population: $populationSize | Generations: $generations")
        println("Mutation Rate: ${(mutationRate * 100).toInt()}% | Elite Ratio: ${(eliteRatio * 100).toInt()}%")
        println("CPU Cores: ${Runtime.getRuntime().availableProcessors()}")
        println("=".repeat(80))

        repeat(generations) { gen ->
            population = evaluatePopulationParallel(population, fitnessFunction)
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
        val cfg = champion.config
        println("  confirmationCandles:         ${cfg.confirmationCandles.default.toInt()}")
        println("  adxTrendThreshold:           ${cfg.adxTrendThreshold.default}")
        println("  adxRangeThreshold:           ${cfg.adxRangeThreshold.default}")
        println("  stopLossAtrMultiplier:       ${cfg.stopLossAtrMultiplier.default}")
        println("  takeProfitAtrMultiplier:     ${cfg.takeProfitAtrMultiplier.default}")
        println("  trendPositionPercent:        ${cfg.trendPositionPercent.default} (${"%.2f".format(cfg.trendPositionPercent.default * 100)}%)")
        println("  leverage:                    ${cfg.leverage.default}x")
        println("  rangeEntryMultiplier:        ${cfg.rangeEntryMultiplier.default}")
        println("  rangeStopMultiplier:         ${cfg.rangeStopMultiplier.default}")
        println("  rangeRsiMidpoint:            ${cfg.rangeRsiMidpoint.default}")
        println("  smaPeriod:                   ${cfg.smaPeriod.default.toInt()}")
        println("  adxPeriod:                   ${cfg.adxPeriod.default.toInt()}")
        println("  atrPeriod:                   ${cfg.atrPeriod.default.toInt()}")
        println("  rsiPeriod:                   ${cfg.rsiPeriod.default.toInt()}")
        println("  volumeSmaPeriod:             ${cfg.volumeSmaPeriod.default.toInt()}")
        println("  minVolumeRatio:              ${cfg.minVolumeRatio.default}")
        println("  rsiLongBlockThreshold:       ${cfg.rsiLongBlockThreshold.default}")
        println("  rsiShortBlockThreshold:      ${cfg.rsiShortBlockThreshold.default}")
        println("  smaPreviousLookback:         ${cfg.smaPreviousLookback.default.toInt()}")
        println("=".repeat(80))

        OptimizationResult(
            champion = champion.config,
            fitness = champion.fitness,
            history = evolutionHistory
        )
    }

    private suspend fun evaluatePopulationParallel(
        population: List<Individual>,
        fitnessFunction: (StrategyConfig) -> Double
    ): List<Individual> = coroutineScope {
        population.map { individual ->
            async(Dispatchers.Default) {
                individual.fitness = fitnessFunction(individual.config)
                individual
            }
        }.awaitAll()
    }

    private fun initializePopulation(random: Random): List<Individual> {
        return List(populationSize) {
            Individual(StrategyConfig.randomInRanges())
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

            var child = crossover(parent1.config, parent2.config, random)

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

    private fun crossover(parent1: StrategyConfig, parent2: StrategyConfig, random: Random): StrategyConfig {
        return StrategyConfig(
            confirmationCandles = if (random.nextBoolean()) parent1.confirmationCandles.copy() else parent2.confirmationCandles.copy(),
            adxTrendThreshold = if (random.nextBoolean()) parent1.adxTrendThreshold.copy() else parent2.adxTrendThreshold.copy(),
            adxRangeThreshold = if (random.nextBoolean()) parent1.adxRangeThreshold.copy() else parent2.adxRangeThreshold.copy(),
            stopLossAtrMultiplier = if (random.nextBoolean()) parent1.stopLossAtrMultiplier.copy() else parent2.stopLossAtrMultiplier.copy(),
            takeProfitAtrMultiplier = if (random.nextBoolean()) parent1.takeProfitAtrMultiplier.copy() else parent2.takeProfitAtrMultiplier.copy(),
            trendPositionPercent = if (random.nextBoolean()) parent1.trendPositionPercent.copy() else parent2.trendPositionPercent.copy(),
            leverage = if (random.nextBoolean()) parent1.leverage.copy() else parent2.leverage.copy(),
            rangeEntryMultiplier = if (random.nextBoolean()) parent1.rangeEntryMultiplier.copy() else parent2.rangeEntryMultiplier.copy(),
            rangeStopMultiplier = if (random.nextBoolean()) parent1.rangeStopMultiplier.copy() else parent2.rangeStopMultiplier.copy(),
            rangeRsiMidpoint = if (random.nextBoolean()) parent1.rangeRsiMidpoint.copy() else parent2.rangeRsiMidpoint.copy(),
            smaPeriod = if (random.nextBoolean()) parent1.smaPeriod.copy() else parent2.smaPeriod.copy(),
            adxPeriod = if (random.nextBoolean()) parent1.adxPeriod.copy() else parent2.adxPeriod.copy(),
            atrPeriod = if (random.nextBoolean()) parent1.atrPeriod.copy() else parent2.atrPeriod.copy(),
            rsiPeriod = if (random.nextBoolean()) parent1.rsiPeriod.copy() else parent2.rsiPeriod.copy(),
            volumeSmaPeriod = if (random.nextBoolean()) parent1.volumeSmaPeriod.copy() else parent2.volumeSmaPeriod.copy(),
            minVolumeRatio = if (random.nextBoolean()) parent1.minVolumeRatio.copy() else parent2.minVolumeRatio.copy(),
            rsiLongBlockThreshold = if (random.nextBoolean()) parent1.rsiLongBlockThreshold.copy() else parent2.rsiLongBlockThreshold.copy(),
            rsiShortBlockThreshold = if (random.nextBoolean()) parent1.rsiShortBlockThreshold.copy() else parent2.rsiShortBlockThreshold.copy(),
            smaPreviousLookback = if (random.nextBoolean()) parent1.smaPreviousLookback.copy() else parent2.smaPreviousLookback.copy()
        )
    }

    private fun mutate(config: StrategyConfig, random: Random): StrategyConfig {
        val p = config.copy()
        val allParams = listOf(
            p::confirmationCandles, p::adxTrendThreshold, p::adxRangeThreshold,
            p::stopLossAtrMultiplier, p::takeProfitAtrMultiplier, p::trendPositionPercent,
            p::leverage, p::rangeEntryMultiplier, p::rangeStopMultiplier, p::rangeRsiMidpoint,
            p::smaPeriod, p::adxPeriod, p::atrPeriod, p::rsiPeriod, p::volumeSmaPeriod, p::minVolumeRatio,
            p::rsiLongBlockThreshold, p::rsiShortBlockThreshold, p::smaPreviousLookback
        )

        val paramToMutate = allParams.random(random)
        val originalParam = paramToMutate.get()
        val mutatedParam = originalParam.mutated(random)

        return when (paramToMutate.name) {
            "confirmationCandles" -> p.copy(confirmationCandles = mutatedParam)
            "adxTrendThreshold" -> p.copy(adxTrendThreshold = mutatedParam)
            "adxRangeThreshold" -> p.copy(adxRangeThreshold = mutatedParam)
            "stopLossAtrMultiplier" -> p.copy(stopLossAtrMultiplier = mutatedParam)
            "takeProfitAtrMultiplier" -> p.copy(takeProfitAtrMultiplier = mutatedParam)
            "trendPositionPercent" -> p.copy(trendPositionPercent = mutatedParam)
            "leverage" -> p.copy(leverage = mutatedParam)
            "rangeEntryMultiplier" -> p.copy(rangeEntryMultiplier = mutatedParam)
            "rangeStopMultiplier" -> p.copy(rangeStopMultiplier = mutatedParam)
            "rangeRsiMidpoint" -> p.copy(rangeRsiMidpoint = mutatedParam)
            "smaPeriod" -> p.copy(smaPeriod = mutatedParam)
            "adxPeriod" -> p.copy(adxPeriod = mutatedParam)
            "atrPeriod" -> p.copy(atrPeriod = mutatedParam)
            "rsiPeriod" -> p.copy(rsiPeriod = mutatedParam)
            "volumeSmaPeriod" -> p.copy(volumeSmaPeriod = mutatedParam)
            "minVolumeRatio" -> p.copy(minVolumeRatio = mutatedParam)
            "rsiLongBlockThreshold" -> p.copy(rsiLongBlockThreshold = mutatedParam)
            "rsiShortBlockThreshold" -> p.copy(rsiShortBlockThreshold = mutatedParam)
            "smaPreviousLookback" -> p.copy(smaPreviousLookback = mutatedParam)
            else -> p
        }
    }
}
