package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.config.BacktestConfig
import com.tradeflow.backtesting.config.OptimizationConfig
import com.tradeflow.backtesting.config.ValidationConfig
import com.tradeflow.backtesting.data.BinanceDataLoader
import com.tradeflow.backtesting.data.RandomPeriodGenerator
import com.tradeflow.core.domain.StrategyConfig
import com.tradeflow.core.domain.model.Candle
import kotlinx.coroutines.runBlocking

class ParameterOptimizer {

    companion object {
        fun run() {
            ParameterOptimizer()
        }
    }


    private val backtestConfig = BacktestConfig()
    private val optimizationConfig = OptimizationConfig()
    private val validationConfig = ValidationConfig()
    private val seed = System.currentTimeMillis()

    init {
        execute()
    }

    private fun execute() {
        printHeader()

        val periods = generatePeriods()
        val datasets = runBlocking { fetchDatasets(periods) }

        if (datasets.isEmpty()) {
            println("\n❌ No data fetched. Aborting.")
            return
        }

        val candles = combineDatasets(datasets)
        val result = runOptimization(candles)
        reportResults(result, candles)
        nextSteps()
    }

    private fun printHeader() {
        println("\n🧬 RUN OPTIMIZATION - RANDOM PERIODS MODE")
        println("=".repeat(90))
        println("Purpose: Find optimal parameters using genetic algorithm on random historical data")
        println("=".repeat(90))
        println("\nConfiguration:")
        println("  Mode:              Random Periods")
        println("  Random Periods:    ${validationConfig.period.defaultNumPeriods}")
        println("  Population Size:   ${optimizationConfig.ga.populationSize}")
        println("  Generations:       ${optimizationConfig.ga.generations}")
        println("  Random Seed:       $seed")
        println()
    }

    private fun generatePeriods(): List<Pair<Long, Long>> {
        println("📅 GENERATING RANDOM HISTORICAL PERIODS")
        println("─".repeat(90))
        val periods = RandomPeriodGenerator.generateRandomPeriods(
            config = validationConfig,
            count = validationConfig.period.defaultNumPeriods,
            seed = seed
        )
        println()
        return periods
    }

    private suspend fun fetchDatasets(
        periods: List<Pair<Long, Long>>
    ): List<Pair<Pair<Long, Long>, List<Candle>>> {
        println("📡 FETCHING DATA FROM BINANCE")
        println("─".repeat(90))
        val datasets = mutableListOf<Pair<Pair<Long, Long>, List<Candle>>>()

        periods.forEachIndexed { index, period ->
            try {
                print("  Fetching period ${index + 1}/${periods.size}... ")
                val candles = BinanceDataLoader.fetchPeriodData(period)
                println("✓ (${candles.size} 1m candles)")
                datasets.add(Pair(period, candles))
            } catch (e: Exception) {
                println("✗ Error: ${e.message}")
            }
        }
        return datasets
    }

    private fun combineDatasets(datasets: List<Pair<Pair<Long, Long>, List<Candle>>>): List<Candle> {
        val candles = datasets.flatMap { it.second }

        println("\n📊 COMBINED DATASET")
        println("─".repeat(90))
        println("  Total candles (${backtestConfig.executionIntervalMinutes}m):  ${candles.size}")
        println("  Total days:                ${candles.size / (24 * 60 / backtestConfig.executionIntervalMinutes)}")
        println("  Trading decisions every:   ${backtestConfig.tradingIntervalMinutes}m")
        println()

        return candles
    }

    private fun runOptimization(candles: List<Candle>): OptimizationResult {
        println("🧬 RUNNING GENETIC OPTIMIZATION")
        println("─".repeat(90))
        println("This will take several minutes...")
        println()

        val optimizer = GeneticOptimizer(optimizationConfig)
        val evaluator = ParameterFitnessEvaluator(
            candles,
            backtestConfig,
            optimizationConfig
        )

        return optimizer.optimize(
            fitnessFunction = { params -> evaluator.evaluate(params) },
            seed = seed
        )
    }

    private fun reportResults(result: OptimizationResult, candles: List<Candle>) {
        println("\n✅ OPTIMIZATION COMPLETE")
        println("=".repeat(90))
        println("Champion Fitness: ${"%.4f".format(result.fitness)}")
        println()

        printChampionParameters(result.champion.config)
        printComparison(result, candles)
    }

    private fun printChampionParameters(champion: StrategyConfig) {
        println("📋 OPTIMIZED PARAMETERS")
        println("─".repeat(90))
        println("  adxTrendThreshold:       ${champion.adxTrendThreshold}")
        println("  adxRangeThreshold:       ${champion.adxRangeThreshold}")
        println("  confirmationCandles:     ${champion.confirmationCandles}")
        println("  trendPositionPercent:    ${champion.trendPositionPercent} (${"%.2f".format(champion.trendPositionPercent.toDouble() * 100)}%)")
        println("  stopLossAtrMultiplier:   ${champion.stopLossAtrMultiplier}")
        println("  takeProfitAtrMultiplier: ${champion.takeProfitAtrMultiplier}")
        println("  leverage:                ${champion.leverage}x")
        println()
    }

    private fun printComparison(result: OptimizationResult, candles: List<Candle>) {
        val evaluator = ParameterFitnessEvaluator(
            candles,
            backtestConfig,
            optimizationConfig
        )

        val baselineFitness = evaluator.evaluate(StrategyConfig())
        val improvement = ((result.fitness - baselineFitness) / baselineFitness.coerceAtLeast(0.0001)) * 100

        println("📈 COMPARISON")
        println("─".repeat(90))
        println("  Baseline Fitness (current):  ${"%.4f".format(baselineFitness)}")
        println("  Champion Fitness (evolved):  ${"%.4f".format(result.fitness)}")
        println("  Improvement:                 ${if (improvement >= 0) "+" else ""}${"%.2f".format(improvement)}%")
        println()

        when {
            improvement > validationConfig.significance.significantImprovement -> {
                println("✅ SIGNIFICANT IMPROVEMENT - Consider updating TradingConfig with these parameters")
            }
            improvement > 0.0 -> {
                println("⚠️  MARGINAL IMPROVEMENT - Test thoroughly before deploying")
            }
            else -> {
                println("❌ NO IMPROVEMENT - Current parameters are already optimal for this dataset")
            }
        }
    }

    private fun nextSteps() {
        println()
        println("💡 NEXT STEPS")
        println("─".repeat(90))
        println("1. Run RunValidation with these parameters on fresh random periods")
        println("2. Verify out-of-sample performance (win rate > 52%, Sharpe > 1.0)")
        println("3. If validated, update TradingConfig.kt with optimized values")
        println("4. Paper trade for 30 days before going live")
        println()
        println("⚠️  WARNING: Optimized parameters may overfit to this historical data")
        println("⚠️  Always validate on unseen data before deployment")
        println("=".repeat(90))
        println()
    }
}
