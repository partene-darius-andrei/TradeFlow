package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.config.BacktestConfig
import com.tradeflow.backtesting.config.OptimizationConfig
import com.tradeflow.backtesting.config.ValidationConfig
import com.tradeflow.backtesting.data.BinanceDataLoader
import com.tradeflow.backtesting.data.RandomPeriodGenerator
import com.tradeflow.core.domain.StrategyConfig
import com.tradeflow.core.domain.model.Candle

class ParameterOptimizer {

    data class OptimizerConfig(
        val numPeriods: Int,
        val populationSize: Int,
        val generations: Int,
        val seed: Long,
        val backtestConfig: BacktestConfig,
        val optimizationConfig: OptimizationConfig,
        val validationConfig: ValidationConfig
    )

    data class CombinedDataset(
        val candles1h: List<Candle>,
        val candles30m: List<Candle>,
        val candles15m: List<Candle>,
        val candles5m: List<Candle>,
        val candles1m: List<Candle>
    )

    suspend fun run(args: Array<String>) {
        val config = parseConfiguration(args)
        printHeader(config)

        val periods = generatePeriods(config)
        val datasets = fetchDatasets(periods, config.numPeriods)

        if (datasets.isEmpty()) {
            println("\n❌ No data fetched. Aborting.")
            return
        }

        val combinedData = combineDatasets(datasets)
        val result = runOptimization(combinedData, config)
        reportResults(result, combinedData, config)
        nextSteps()
    }

    private fun parseConfiguration(args: Array<String>): OptimizerConfig {
        val numPeriods = args.getOrNull(1)?.toIntOrNull() ?: 5
        val populationSize = args.getOrNull(2)?.toIntOrNull() ?: 50
        val generations = args.getOrNull(3)?.toIntOrNull() ?: 100
        val seed = args.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis()

        return OptimizerConfig(
            numPeriods = numPeriods,
            populationSize = populationSize,
            generations = generations,
            seed = seed,
            backtestConfig = BacktestConfig(),
            optimizationConfig = OptimizationConfig(),
            validationConfig = ValidationConfig()
        )
    }

    private fun printHeader(config: OptimizerConfig) {
        println("\n🧬 RUN OPTIMIZATION - RANDOM PERIODS MODE")
        println("=".repeat(90))
        println("Purpose: Find optimal parameters using genetic algorithm on random historical data")
        println("=".repeat(90))
        println("\nConfiguration:")
        println("  Mode:              Random Periods")
        println("  Random Periods:    ${config.numPeriods}")
        println("  Population Size:   ${config.populationSize}")
        println("  Generations:       ${config.generations}")
        println("  Random Seed:       ${config.seed}")
        println()
    }

    private fun generatePeriods(config: OptimizerConfig): List<Pair<Long, Long>> {
        println("📅 GENERATING RANDOM HISTORICAL PERIODS")
        println("─".repeat(90))
        val periods = RandomPeriodGenerator.generateRandomPeriods(
            config = config.validationConfig,
            count = config.numPeriods,
            seed = config.seed
        )
        println()
        return periods
    }

    private suspend fun fetchDatasets(
        periods: List<Pair<Long, Long>>,
        numPeriods: Int
    ): List<Pair<Pair<Long, Long>, BinanceDataLoader.MultiTimeframeData>> {
        println("📡 FETCHING DATA FROM BINANCE")
        println("─".repeat(90))
        val datasets = mutableListOf<Pair<Pair<Long, Long>, BinanceDataLoader.MultiTimeframeData>>()

        periods.forEachIndexed { index, period ->
            try {
                print("  Fetching period ${index + 1}/$numPeriods... ")
                val data = BinanceDataLoader.fetchPeriodData(period)
                println("✓ (${data.candles1h.size} 1h + ${data.candles30m.size} 30m + ${data.candles15m.size} 15m + ${data.candles5m.size} 5m + ${data.candles1m.size} 1m)")
                datasets.add(Pair(period, data))
            } catch (e: Exception) {
                println("✗ Error: ${e.message}")
            }
        }
        return datasets
    }

    private fun combineDatasets(datasets: List<Pair<Pair<Long, Long>, BinanceDataLoader.MultiTimeframeData>>): CombinedDataset {
        val allCandles1h = datasets.flatMap { it.second.candles1h }
        val allCandles30m = datasets.flatMap { it.second.candles30m }
        val allCandles15m = datasets.flatMap { it.second.candles15m }
        val allCandles5m = datasets.flatMap { it.second.candles5m }
        val allCandles1m = datasets.flatMap { it.second.candles1m }

        println("\n📊 COMBINED DATASET")
        println("─".repeat(90))
        println("  Total 1h candles:  ${allCandles1h.size}")
        println("  Total 30m candles: ${allCandles30m.size}")
        println("  Total 15m candles: ${allCandles15m.size}")
        println("  Total 5m candles:  ${allCandles5m.size}")
        println("  Total 1m candles:  ${allCandles1m.size}")
        println("  Total days:        ${allCandles1h.size / 24}")
        println()

        return CombinedDataset(allCandles1h, allCandles30m, allCandles15m, allCandles5m, allCandles1m)
    }

    private fun runOptimization(data: CombinedDataset, config: OptimizerConfig): OptimizationResult {
        println("🧬 RUNNING GENETIC OPTIMIZATION")
        println("─".repeat(90))
        println("This will take several minutes...")
        println()

        val optimizer = GeneticOptimizer(config.optimizationConfig)
        val evaluator = ParameterFitnessEvaluator(
            data.candles1h,
            data.candles30m,
            data.candles15m,
            data.candles5m,
            data.candles1m,
            config.backtestConfig,
            config.optimizationConfig
        )

        return optimizer.optimize(
            fitnessFunction = { params -> evaluator.evaluate(params) },
            seed = config.seed
        )
    }

    private fun reportResults(result: OptimizationResult, data: CombinedDataset, config: OptimizerConfig) {
        println("\n✅ OPTIMIZATION COMPLETE")
        println("=".repeat(90))
        println("Champion Fitness: ${"%.4f".format(result.fitness)}")
        println()

        printChampionParameters(result.champion.config)
        printComparison(result, data, config)
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

    private fun printComparison(result: OptimizationResult, data: CombinedDataset, config: OptimizerConfig) {
        val evaluator = ParameterFitnessEvaluator(
            data.candles1h,
            data.candles30m,
            data.candles15m,
            data.candles5m,
            data.candles1m,
            config.backtestConfig,
            config.optimizationConfig
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
            improvement > config.validationConfig.significance.significantImprovement -> {
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
