package com.tradeflow.backtesting

import com.tradeflow.backtesting.config.BacktestConfig
import com.tradeflow.backtesting.config.GeneticAlgorithmConfig
import com.tradeflow.backtesting.config.OptimizationConfig
import com.tradeflow.backtesting.config.PeriodConfig
import com.tradeflow.backtesting.config.ValidationConfig
import com.tradeflow.backtesting.data.BinanceDataLoader
import com.tradeflow.backtesting.data.RandomPeriodGenerator
import com.tradeflow.backtesting.optimization.GeneticOptimizer
import com.tradeflow.backtesting.optimization.OptimizationResult
import com.tradeflow.backtesting.optimization.ParameterFitnessEvaluator
import com.tradeflow.core.domain.StrategyConfig
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

data class RunResult(
    val runNumber: Int,
    val fitness: Double,
    val config: StrategyConfig,
    val durationMs: Long
)

fun main() {
    println("\n🚀 MASSIVE OPTIMIZATION - 50 ROUNDS")
    println("=".repeat(100))

    val targetMinutesPerRun = 2.0
    var currentPopulation = 24
    var currentGenerations = 30
    var currentPeriods = 5

    val allResults = mutableListOf<RunResult>()

    repeat(50) { runIndex ->
        val runNumber = runIndex + 1
        println("\n" + "▓".repeat(100))
        println("🔥 ROUND $runNumber/50")
        println("▓".repeat(100))
        println("Population: $currentPopulation | Generations: $currentGenerations | Periods: $currentPeriods")
        println()

        val durationMs = measureTimeMillis {
            val result = runSingleOptimization(
                population = currentPopulation,
                generations = currentGenerations,
                numPeriods = currentPeriods,
                runNumber = runNumber
            )

            allResults.add(RunResult(
                runNumber = runNumber,
                fitness = result.fitness,
                config = result.champion,
                durationMs = 0
            ))
        }

        allResults[runIndex] = allResults[runIndex].copy(durationMs = durationMs)

        val durationMinutes = durationMs / 60_000.0
        println("\n⏱️  Round $runNumber completed in ${"%.2f".format(durationMinutes)} minutes")
        println("   Fitness: ${"%.4f".format(allResults[runIndex].fitness)}")

        // Dynamically scale up if too fast
        if (runNumber <= 3 && durationMinutes < targetMinutesPerRun) {
            val scaleFactor = (targetMinutesPerRun / durationMinutes).coerceIn(1.0, 2.0)
            currentPopulation = (currentPopulation * scaleFactor).toInt().coerceAtMost(50)
            currentGenerations = (currentGenerations * scaleFactor).toInt().coerceAtMost(50)
            currentPeriods = (currentPeriods * 1.5).toInt().coerceAtMost(10)

            println("   ⚡ Scaling up: Pop=$currentPopulation, Gen=$currentGenerations, Periods=$currentPeriods")
        }

        // Progress report every 10 rounds
        if (runNumber % 10 == 0) {
            printProgressReport(allResults)
        }
    }

    printFinalReport(allResults)
}

fun runSingleOptimization(
    population: Int,
    generations: Int,
    numPeriods: Int,
    runNumber: Int
): OptimizationResult {
    val backtestConfig = BacktestConfig(silent = true)
    val optimizationConfig = OptimizationConfig(
        ga = GeneticAlgorithmConfig(
            populationSize = population,
            generations = generations,
            mutationRate = 0.15,
            eliteRatio = 0.1,
            tournamentSize = 3,
            reportInterval = generations / 4
        )
    )
    val validationConfig = ValidationConfig(
        period = PeriodConfig(
            defaultNumPeriods = numPeriods,
            minPeriodDays = 45,
            maxPeriodDays = 90,
            lookbackBuffer = 200,
            seed = runNumber,
            trainTestRatio = 0.7
        )
    )

    val seed = System.currentTimeMillis() + runNumber * 1000

    val periods = RandomPeriodGenerator.generateRandomPeriods(
        config = validationConfig,
        count = validationConfig.period.defaultNumPeriods,
        seed = seed
    )

    val candles = runBlocking {
        periods.mapNotNull { period ->
            try {
                BinanceDataLoader.fetchPeriodData(period, backtestConfig.interval)
            } catch (e: Exception) {
                null
            }
        }.flatten()
    }

    if (candles.isEmpty()) {
        throw IllegalStateException("No data fetched for round $runNumber")
    }

    val optimizer = GeneticOptimizer(optimizationConfig)
    val evaluator = ParameterFitnessEvaluator(candles, backtestConfig, optimizationConfig)

    return optimizer.optimize(
        fitnessFunction = { params -> evaluator.evaluate(params) },
        seed = seed
    )
}

fun printProgressReport(results: List<RunResult>) {
    val completed = results.size
    val bestSoFar = results.maxByOrNull { it.fitness }!!
    val avgFitness = results.map { it.fitness }.average()
    val avgDuration = results.map { it.durationMs / 60_000.0 }.average()

    println("\n" + "━".repeat(100))
    println("📊 PROGRESS REPORT ($completed/50 rounds complete)")
    println("━".repeat(100))
    println("Best Fitness So Far:     ${"%.4f".format(bestSoFar.fitness)} (Round ${bestSoFar.runNumber})")
    println("Average Fitness:         ${"%.4f".format(avgFitness)}")
    println("Average Duration:        ${"%.2f".format(avgDuration)} minutes/round")
    println("Estimated Time Left:     ${"%.1f".format(avgDuration * (50 - completed))} minutes")
    println("━".repeat(100))
}

fun printFinalReport(results: List<RunResult>) {
    val champion = results.maxByOrNull { it.fitness }!!
    val top10 = results.sortedByDescending { it.fitness }.take(10)
    val totalTime = results.sumOf { it.durationMs } / 60_000.0

    println("\n" + "█".repeat(100))
    println("🏆 FINAL REPORT - 50 ROUNDS COMPLETE")
    println("█".repeat(100))
    println()

    println("📈 STATISTICS")
    println("─".repeat(100))
    println("Total Runtime:           ${"%.1f".format(totalTime)} minutes (${"%.2f".format(totalTime / 60)} hours)")
    println("Total Evaluations:       ${results.size}")
    println("Best Fitness:            ${"%.4f".format(champion.fitness)} (Round ${champion.runNumber})")
    println("Worst Fitness:           ${"%.4f".format(results.minOf { it.fitness })}")
    println("Average Fitness:         ${"%.4f".format(results.map { it.fitness }.average())}")
    println("Std Dev Fitness:         ${"%.4f".format(calculateStdDev(results.map { it.fitness }))}")
    println()

    println("🥇 TOP 10 CONFIGURATIONS")
    println("─".repeat(100))
    top10.forEachIndexed { index, result ->
        println("${index + 1}. Round ${result.runNumber} | Fitness: ${"%.4f".format(result.fitness)} | " +
            "Duration: ${"%.1f".format(result.durationMs / 60_000.0)}m")
    }
    println()

    println("👑 CHAMPION PARAMETERS (Round ${champion.runNumber})")
    println("─".repeat(100))
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
    println("█".repeat(100))
    println()
    println("💡 Copy these parameters to StrategyConfig.kt for production use")
    println("⚠️  Always validate on fresh data before deploying!")
    println()
}

fun calculateStdDev(values: List<Double>): Double {
    val mean = values.average()
    val variance = values.map { (it - mean) * (it - mean) }.average()
    return kotlin.math.sqrt(variance)
}
