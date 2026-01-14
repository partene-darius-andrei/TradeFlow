package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.data.BinanceDataLoader
import com.tradeflow.backtesting.data.RandomPeriodGenerator
import com.tradeflow.backtesting.data.RandomPeriod
import com.tradeflow.backtesting.engine.BacktestEngine
import com.tradeflow.core.domain.TradingConfig
import com.tradeflow.core.domain.model.Candle
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class ParameterOptimizer {

    suspend fun run(args: Array<String>) {
        val mode = args.getOrNull(0) ?: "random-periods"

        when (mode) {
            "random-periods" -> runRandomPeriodOptimization(args)
            "train-test-split" -> runTrainTestOptimization(args)
            "help", "--help", "-h" -> showUsage()
            else -> {
                println("❌ Unknown mode: $mode")
                showUsage()
            }
        }
    }

    private suspend fun runRandomPeriodOptimization(args: Array<String>) {
        println("\n🧬 RUN OPTIMIZATION - RANDOM PERIODS MODE")
        println("=".repeat(90))
        println("Purpose: Find optimal parameters using genetic algorithm on random historical data")
        println("=".repeat(90))

        val numPeriods = args.getOrNull(1)?.toIntOrNull() ?: 5
        val populationSize = args.getOrNull(2)?.toIntOrNull() ?: 50
        val generations = args.getOrNull(3)?.toIntOrNull() ?: 100
        val seed = args.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis()

        println("\nConfiguration:")
        println("  Mode:              Random Periods")
        println("  Random Periods:    $numPeriods")
        println("  Population Size:   $populationSize")
        println("  Generations:       $generations")
        println("  Random Seed:       $seed")
        println()

        println("📅 GENERATING RANDOM HISTORICAL PERIODS")
        println("─".repeat(90))
        val periods = RandomPeriodGenerator.generateRandomPeriods(
            count = numPeriods,
            minDurationDays = 60,
            maxDurationDays = 180,
            seed = seed
        )

        periods.forEachIndexed { index, period ->
            println("  ${index + 1}. ${period.description}")
        }
        println()

        println("📡 FETCHING DATA FROM BINANCE")
        println("─".repeat(90))
        val datasets = mutableListOf<Triple<RandomPeriod, List<Candle>, List<Candle>>>()

        periods.forEachIndexed { index, period ->
            try {
                print("  Fetching period ${index + 1}/$numPeriods... ")
                val (candles1h, candles15m) = fetchDataForPeriod(period)
                println("✓ (${candles1h.size} 1h + ${candles15m.size} 15m candles)")
                datasets.add(Triple(period, candles1h, candles15m))
            } catch (e: Exception) {
                println("✗ Error: ${e.message}")
            }
        }

        if (datasets.isEmpty()) {
            println("\n❌ No data fetched. Aborting.")
            return
        }

        val allCandles1h = datasets.flatMap { it.second }
        val allCandles15m = datasets.flatMap { it.third }

        println("\n📊 COMBINED DATASET")
        println("─".repeat(90))
        println("  Total 1h candles:  ${allCandles1h.size}")
        println("  Total 15m candles: ${allCandles15m.size}")
        println("  Total days:        ${allCandles1h.size / 24}")
        println()

        println("🧬 RUNNING GENETIC OPTIMIZATION")
        println("─".repeat(90))
        println("This will take several minutes...")
        println()

        val optimizer = GeneticOptimizer(
            populationSize = populationSize,
            generations = generations,
            mutationRate = 0.15,
            eliteRatio = 0.1
        )

        val evaluator = ParameterFitnessEvaluator(allCandles1h, allCandles15m)

        val result = optimizer.optimize(
            fitnessFunction = { params -> evaluator.evaluate(params) },
            seed = seed
        )

        println("\n✅ OPTIMIZATION COMPLETE")
        println("=".repeat(90))
        println("Champion Fitness: ${"%.4f".format(result.fitness)}")
        println()
        println("📋 OPTIMIZED PARAMETERS")
        println("─".repeat(90))
        val champion = result.champion.params
        println("  adxTrendThreshold:       ${champion.adxTrendThreshold}")
        println("  adxRangeThreshold:       ${champion.adxRangeThreshold}")
        println("  confirmationCandles:     ${champion.confirmationCandles}")
        println("  trendPositionPercent:    ${champion.trendPositionPercent} (${(champion.trendPositionPercent * 100).let { "%.2f".format(it) }}%)")
        println("  stopLossAtrMultiplier:   ${champion.stopLossAtrMultiplier}")
        println("  takeProfitAtrMultiplier: ${champion.takeProfitAtrMultiplier}")
        println("  leverage:                ${champion.leverage}x")
        println()

        val baselineFitness = evaluator.evaluate(TradingParameters.current())
        val improvement = ((result.fitness - baselineFitness) / baselineFitness.coerceAtLeast(0.0001)) * 100

        println("📈 COMPARISON")
        println("─".repeat(90))
        println("  Baseline Fitness (current):  ${"%.4f".format(baselineFitness)}")
        println("  Champion Fitness (evolved):  ${"%.4f".format(result.fitness)}")
        println("  Improvement:                 ${if (improvement >= 0) "+" else ""}${"%.2f".format(improvement)}%")
        println()

        if (improvement > 10.0) {
            println("✅ SIGNIFICANT IMPROVEMENT - Consider updating TradingConfig with these parameters")
        } else if (improvement > 0.0) {
            println("⚠️  MARGINAL IMPROVEMENT - Test thoroughly before deploying")
        } else {
            println("❌ NO IMPROVEMENT - Current parameters are already optimal for this dataset")
        }

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

    private fun runTrainTestOptimization(args: Array<String>) {
        println("\n🧬 RUN OPTIMIZATION - TRAIN/TEST SPLIT MODE")
        println("=".repeat(90))
        println("Purpose: Find optimal parameters using train/test split on recent historical data")
        println("=".repeat(90))

        val candles1hLimit = args.getOrNull(1)?.toIntOrNull() ?: 1000
        val candles15mLimit = args.getOrNull(2)?.toIntOrNull() ?: 4000
        val trainTestRatio = args.getOrNull(3)?.toDoubleOrNull() ?: 0.7
        val populationSize = args.getOrNull(4)?.toIntOrNull() ?: 50
        val generations = args.getOrNull(5)?.toIntOrNull() ?: 100

        println("\nConfiguration:")
        println("  Mode:              Train/Test Split")
        println("  1h Candles:        $candles1hLimit")
        println("  15m Candles:       $candles15mLimit")
        println("  Train/Test Ratio:  ${(trainTestRatio * 100).toInt()}% / ${((1 - trainTestRatio) * 100).toInt()}%")
        println("  Population Size:   $populationSize")
        println("  Generations:       $generations")
        println()

        println("📥 LOADING HISTORICAL DATA FROM BINANCE")
        val all1h = BinanceDataLoader.fetchHistoricalCandles(interval = "1h", limit = candles1hLimit)
        val all15m = BinanceDataLoader.fetchHistoricalCandles(interval = "15m", limit = candles15mLimit)

        println("Loaded ${all1h.size} 1h candles and ${all15m.size} 15m candles")

        val splitPoint1h = (all1h.size * trainTestRatio).toInt()
        val splitPoint15m = (all15m.size * trainTestRatio).toInt()

        val train1h = all1h.take(splitPoint1h)
        val train15m = all15m.take(splitPoint15m)

        val test1h = all1h.drop(splitPoint1h)
        val test15m = all15m.drop(splitPoint15m)

        println("\nTraining Set: ${train1h.size} 1h candles, ${train15m.size} 15m candles (${(trainTestRatio * 100).toInt()}%)")
        println("Validation Set: ${test1h.size} 1h candles, ${test15m.size} 15m candles (${((1 - trainTestRatio) * 100).toInt()}%)")

        println("\n🧬 RUNNING GENETIC OPTIMIZATION ON TRAINING DATA")
        println("─".repeat(90))

        val optimizer = GeneticOptimizer(
            populationSize = populationSize,
            generations = generations,
            mutationRate = 0.15,
            eliteRatio = 0.1
        )

        val trainEvaluator = ParameterFitnessEvaluator(train1h, train15m)

        val result = optimizer.optimize(
            fitnessFunction = { params -> trainEvaluator.evaluate(params) },
            seed = System.currentTimeMillis()
        )

        println("\n🧪 VALIDATING ON OUT-OF-SAMPLE DATA")
        println("─".repeat(90))

        val optimizedParams = result.champion.params
        val oosResult = TradingConfig.withOverrides(
            adxTrendThreshold = optimizedParams.adxTrendThreshold,
            adxRangeThreshold = optimizedParams.adxRangeThreshold,
            confirmationCandles = optimizedParams.confirmationCandles,
            trendPositionPercent = optimizedParams.trendPositionPercent,
            stopLossAtrMultiplier = optimizedParams.stopLossAtrMultiplier,
            takeProfitAtrMultiplier = optimizedParams.takeProfitAtrMultiplier,
            leverage = optimizedParams.leverage
        ) {
            val engine = BacktestEngine(initialCapital = BigDecimal("500.00"))
            engine.execute(test1h, test15m, verbose = false)
        }

        val baselineResult = TradingConfig.withOverrides() {
            val engine = BacktestEngine(initialCapital = BigDecimal("500.00"))
            engine.execute(test1h, test15m, verbose = false)
        }

        println("\n📊 OUT-OF-SAMPLE VALIDATION RESULTS")
        println("=".repeat(90))
        println("%-30s | %-15s | %-15s".format("Metric", "Optimized", "Baseline"))
        println("-".repeat(90))
        println("%-30s | %-15s | %-15s".format("Total Return", "${"%.2f".format(oosResult.pnlPercent)}%", "${"%.2f".format(baselineResult.pnlPercent)}%"))
        println("%-30s | %-15s | %-15s".format("Win Rate", "${"%.2f".format(oosResult.winRate)}%", "${"%.2f".format(baselineResult.winRate)}%"))
        println("%-30s | %-15s | %-15s".format("Sharpe Ratio", "${"%.2f".format(oosResult.sharpeRatio)}", "${"%.2f".format(baselineResult.sharpeRatio)}"))
        println("%-30s | %-15s | %-15s".format("Max Drawdown", "${"%.2f".format(oosResult.maxDrawdown)}%", "${"%.2f".format(baselineResult.maxDrawdown)}%"))
        println("%-30s | %-15s | %-15s".format("Total Trades", "${oosResult.trades.size}", "${baselineResult.trades.size}"))
        println("%-30s | %-15s | %-15s".format("Profit Factor", "${"%.2f".format(oosResult.profitFactor)}", "${"%.2f".format(baselineResult.profitFactor)}"))
        println("=".repeat(90))

        val improvement = oosResult.pnlPercent - baselineResult.pnlPercent
        val improvementIndicator = if (improvement > 0) "✅" else "⚠️"
        println("\n$improvementIndicator Improvement vs Baseline: ${"%.2f".format(improvement)}%")
        println()
    }

    private suspend fun fetchDataForPeriod(period: RandomPeriod): Pair<List<Candle>, List<Candle>> = coroutineScope {
        val required1h = RandomPeriodGenerator.calculateRequiredCandles(period, "1h")
        val required15m = RandomPeriodGenerator.calculateRequiredCandles(period, "15m")

        val candles1h = async {
            BinanceDataLoader.fetchHistoricalCandles(
                symbol = "BTCUSDT",
                interval = "1h",
                startTime = period.startTime,
                endTime = period.endTime,
                limit = required1h.coerceAtMost(1000)
            )
        }

        val candles15m = async {
            BinanceDataLoader.fetchHistoricalCandles(
                symbol = "BTCUSDT",
                interval = "15m",
                startTime = period.startTime,
                endTime = period.endTime,
                limit = required15m.coerceAtMost(1000)
            )
        }

        Pair(candles1h.await(), candles15m.await())
    }

    private fun showUsage() {
        println("""
            |
            |Usage: RunOptimization [mode] [options]
            |
            |Modes:
            |  random-periods        Optimize on random historical periods (default)
            |  train-test-split      Optimize using train/test split on recent data
            |
            |Random Periods Mode:
            |  ./gradlew :backtesting:run -PmainClass=com.tradeflow.backtesting.RunOptimizationKt \
            |    --args="random-periods [numPeriods] [populationSize] [generations] [seed]"
            |
            |  Default: 5 periods, 50 population, 100 generations, random seed
            |
            |Train/Test Split Mode:
            |  ./gradlew :backtesting:run -PmainClass=com.tradeflow.backtesting.RunOptimizationKt \
            |    --args="train-test-split [1hLimit] [15mLimit] [trainRatio] [populationSize] [generations]"
            |
            |  Default: 1000 1h candles, 4000 15m candles, 70% train, 50 population, 100 generations
            |
        """.trimMargin())
    }
}
