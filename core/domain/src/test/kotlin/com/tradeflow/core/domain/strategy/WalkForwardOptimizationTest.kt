package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.*
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.util.BinanceDataLoader
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * WALK-FORWARD OPTIMIZATION
 *
 * Industry-standard approach to prevent overfitting:
 * 1. Split data into multiple windows
 * 2. Optimize on training window
 * 3. Test on following validation window
 * 4. Repeat for each window
 * 5. Average parameters that worked across ALL windows
 *
 * This ensures parameters generalize to unseen data.
 */
class WalkForwardOptimizationTest {

    data class WalkForwardWindow(
        val name: String,
        val trainStart: Int,
        val trainEnd: Int,
        val testStart: Int,
        val testEnd: Int
    )

    data class WindowResult(
        val window: WalkForwardWindow,
        val champion: OptimizedChromosome,
        val inSampleFitness: Double,
        val outOfSampleMetrics: OptimizationMetrics
    )

    @Test
    fun `walk forward optimization - 5 windows - robust parameters`() = runBlocking {
        println("\n🚶 WALK-FORWARD OPTIMIZATION")
        println("=".repeat(90))
        println("Industry-standard approach for robust parameter discovery")
        println("=".repeat(90))
        println()

        // Load all available data
        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "5m",
            limit = 1500
        )

        println("Total Data: ${allCandles.size} candles (~5 days of 5m data)")
        println()

        // Define walk-forward windows
        // Each window: train on 400 candles, test on next 200 candles
        val windows = listOf(
            WalkForwardWindow("Window 1", 0, 400, 400, 600),
            WalkForwardWindow("Window 2", 200, 600, 600, 800),
            WalkForwardWindow("Window 3", 400, 800, 800, 1000),
            WalkForwardWindow("Window 4", 600, 1000, 1000, 1200),
            WalkForwardWindow("Window 5", 800, 1200, 1200, 1400)
        )

        println("Walk-Forward Windows:")
        windows.forEach { window ->
            println("  ${window.name}: Train [${window.trainStart}-${window.trainEnd}] → Test [${window.testStart}-${window.testEnd}]")
        }
        println()

        val populationSize = 30  // Smaller for speed (5 windows × 30 = 150 total runs)
        val generations = 20     // Fewer gens per window
        val mutationRate = 0.25
        val eliteRatio = 0.20

        println("Genetic Algorithm Config (per window):")
        println("  Population:     $populationSize")
        println("  Generations:    $generations")
        println("  Mutation Rate:  ${(mutationRate * 100).toInt()}%")
        println("  Elite Ratio:    ${(eliteRatio * 100).toInt()}%")
        println()
        println("Total Optimizations: ${windows.size} windows")
        println("Total Configurations: ${populationSize * generations * windows.size}")
        println("=".repeat(90))
        println()

        val windowResults = mutableListOf<WindowResult>()

        // Optimize each window
        windows.forEachIndexed { windowIndex, window ->
            println("┌${"─".repeat(88)}┐")
            println("│ ${window.name.padEnd(86)} │")
            println("└${"─".repeat(88)}┘")
            println()

            val trainData = allCandles.subList(window.trainStart, window.trainEnd)
            val testData = allCandles.subList(window.testStart, window.testEnd)

            println("  Training:   ${trainData.size} candles")
            println("  Testing:    ${testData.size} candles")
            println()

            // Run genetic algorithm on this window
            val champion = optimizeWindow(trainData, populationSize, generations, mutationRate, eliteRatio)
            val inSampleFitness = evaluateFitness(champion, trainData)

            println()
            println("  🏆 Window Champion:")
            println("     ADX Trend:  ${"%.1f".format(champion.adxTrendThreshold)}")
            println("     Stop:       ${"%.1f".format(champion.stopLossAtrMultiplier)}× ATR")
            println("     Target:     ${"%.1f".format(champion.takeProfitAtrMultiplier)}× ATR (R:R ${"%.1f".format(champion.takeProfitAtrMultiplier / champion.stopLossAtrMultiplier)}:1)")
            println("     Volume:     ${"%.2f".format(champion.volumeThreshold)}×")
            println("     Confirm:    ${champion.confirmationCandles}")
            println("     Fitness:    ${"%.3f".format(inSampleFitness)}")
            println()

            // Test on out-of-sample
            val oosMetrics = evaluateDetailed(champion, testData)
            val pnlSign = if (oosMetrics.totalPnl >= BigDecimal.ZERO) "+" else ""

            println("  📊 Out-of-Sample Performance:")
            println("     Trades:     ${oosMetrics.trades}")
            println("     Win Rate:   ${"%.1f".format(oosMetrics.winRate)}%")
            println("     PnL:        $pnlSign${"%.2f".format(oosMetrics.pnlPercent)}%")
            println("     Sharpe:     ${"%.2f".format(oosMetrics.sharpeRatio)}")
            println()

            windowResults.add(WindowResult(window, champion, inSampleFitness, oosMetrics))

            if (windowIndex < windows.size - 1) {
                println()
            }
        }

        // Aggregate results
        println()
        println("=".repeat(90))
        println("📈 WALK-FORWARD SUMMARY")
        println("=".repeat(90))
        println()

        val allChampions = windowResults.map { it.champion }

        // Average parameters across all windows
        val avgAdxTrend = allChampions.map { it.adxTrendThreshold }.average()
        val avgAdxRange = allChampions.map { it.adxRangeThreshold }.average()
        val avgStopLoss = allChampions.map { it.stopLossAtrMultiplier }.average()
        val avgTakeProfit = allChampions.map { it.takeProfitAtrMultiplier }.average()
        val avgPositionSize = allChampions.map { it.trendPositionPercent }.average()
        val avgVolume = allChampions.map { it.volumeThreshold }.average()
        val mostCommonConfirm = allChampions.groupingBy { it.confirmationCandles }.eachCount().maxByOrNull { it.value }?.key ?: 3
        val mostCommonSma = allChampions.groupingBy { it.smaPeriod }.eachCount().maxByOrNull { it.value }?.key ?: 20

        println("Robust Parameters (Averaged Across Windows):")
        println("  ADX Trend Threshold:       ${"%.1f".format(avgAdxTrend)}")
        println("  ADX Range Threshold:       ${"%.1f".format(avgAdxRange)}")
        println("  Stop Loss:                 ${"%.1f".format(avgStopLoss)}× ATR")
        println("  Take Profit:               ${"%.1f".format(avgTakeProfit)}× ATR")
        println("  Risk/Reward Ratio:         ${"%.1f".format(avgTakeProfit / avgStopLoss)}:1")
        println("  Position Size:             ${"%.1f".format(avgPositionSize * 100)}%")
        println("  Volume Threshold:          ${"%.2f".format(avgVolume)}×")
        println("  Confirmation Candles:      $mostCommonConfirm")
        println("  SMA Period:                $mostCommonSma")
        println()

        // Out-of-sample results
        val totalTrades = windowResults.sumOf { it.outOfSampleMetrics.trades }
        val avgWinRate = if (windowResults.isNotEmpty()) {
            windowResults.map { it.outOfSampleMetrics.winRate }.average()
        } else 0.0
        val totalPnl = windowResults.sumOf { it.outOfSampleMetrics.pnlPercent }
        val avgPnl = if (windowResults.isNotEmpty()) totalPnl / windowResults.size else 0.0
        val avgSharpe = if (windowResults.isNotEmpty()) {
            windowResults.map { it.outOfSampleMetrics.sharpeRatio }.average()
        } else 0.0

        println("Aggregated Out-of-Sample Performance:")
        println("  Total Windows:       ${windowResults.size}")
        println("  Total Trades:        $totalTrades")
        println("  Avg Win Rate:        ${"%.1f".format(avgWinRate)}%")
        println("  Avg PnL per Window:  ${if (avgPnl >= 0) "+" else ""}${"%.2f".format(avgPnl)}%")
        println("  Total PnL:           ${if (totalPnl >= 0) "+" else ""}${"%.2f".format(totalPnl)}%")
        println("  Avg Sharpe:          ${"%.2f".format(avgSharpe)}")
        println()

        // Consistency check
        val profitableWindows = windowResults.count { it.outOfSampleMetrics.pnlPercent > 0 }
        val consistency = (profitableWindows.toDouble() / windowResults.size * 100)

        println("Consistency Metrics:")
        println("  Profitable Windows:  $profitableWindows / ${windowResults.size}")
        println("  Consistency Rate:    ${"%.0f".format(consistency)}%")
        println()

        // Show individual window breakdown
        println("Individual Window Results:")
        println("  " + "Window".padEnd(12) + "Trades".padEnd(10) + "Win%".padEnd(10) + "PnL%".padEnd(10) + "Sharpe")
        println("  " + "-".repeat(60))
        windowResults.forEach { result ->
            val winRateStr = if (result.outOfSampleMetrics.trades > 0) {
                "%.1f".format(result.outOfSampleMetrics.winRate)
            } else "N/A"
            val pnlStr = if (result.outOfSampleMetrics.pnlPercent >= 0) "+" else ""
            val pnlFormatted = "$pnlStr${"%.2f".format(result.outOfSampleMetrics.pnlPercent)}"

            println("  ${result.window.name.padEnd(12)}${result.outOfSampleMetrics.trades.toString().padEnd(10)}${winRateStr.padEnd(10)}${pnlFormatted.padEnd(10)}${"%.2f".format(result.outOfSampleMetrics.sharpeRatio)}")
        }

        println()
        println("=".repeat(90))

        // Final verdict
        when {
            totalPnl > 0 && totalTrades >= 50 && avgWinRate >= 50.0 -> {
                println("✅ SUCCESS! Robust profitable strategy discovered!")
                println("   Total: $totalTrades trades, ${avgWinRate.let { "%.1f".format(it) }}% win rate, ${totalPnl.let { "%.2f".format(it) }}% total return")
                println("   These parameters should generalize to live trading.")
            }
            totalTrades >= 30 && consistency >= 60.0 -> {
                println("⚠️  PROMISING! Strategy shows consistency across windows.")
                println("   ${consistency.toInt()}% of windows profitable, but needs improvement.")
                println("   Consider: longer timeframes or different market conditions.")
            }
            totalTrades < 10 -> {
                println("❌ INSUFFICIENT DATA! Strategy too conservative (<10 total trades).")
                println("   Try: Lower volume threshold or use longer timeframes (15m/1h).")
            }
            else -> {
                println("❌ UNPROFITABLE! Strategy doesn't generalize well.")
                println("   Total return: ${"%.2f".format(totalPnl)}%, consistency: ${consistency.toInt()}%")
                println("   Consider: Different timeframe, asset, or strategy approach.")
            }
        }
        println("=".repeat(90))
    }

    private fun optimizeWindow(
        candles: List<com.tradeflow.core.domain.model.Candle>,
        populationSize: Int,
        generations: Int,
        mutationRate: Double,
        eliteRatio: Double
    ): OptimizedChromosome = runBlocking {
        val random = Random.Default
        var population = List(populationSize) { OptimizedChromosome.random(random) }

        val fitnessFunction = { chromosome: OptimizedChromosome ->
            evaluateFitness(chromosome, candles)
        }

        var bestChromosomeOverall: OptimizedChromosome? = null
        var bestFitnessOverall = Double.NEGATIVE_INFINITY

        repeat(generations) { gen ->
            val fitnesses = population.map { fitnessFunction(it) }
            val sortedPopulation = population.zip(fitnesses).sortedByDescending { it.second }

            val best = sortedPopulation.first().second
            if (best > bestFitnessOverall) {
                bestFitnessOverall = best
                bestChromosomeOverall = sortedPopulation.first().first
            }

            if (gen % 5 == 0 || gen == generations - 1) {
                val avg = fitnesses.average()
                print("\r  Gen ${gen.toString().padStart(2)}: Best=${"%.3f".format(best)} Avg=${"%.3f".format(avg)}")
            }

            if (gen < generations - 1) {
                val eliteCount = (populationSize * eliteRatio).toInt()
                val elite = sortedPopulation.take(eliteCount).map { it.first }

                val offspring = mutableListOf<OptimizedChromosome>()
                offspring.addAll(elite)

                while (offspring.size < populationSize) {
                    val parent1 = sortedPopulation[random.nextInt(sortedPopulation.size / 2)].first
                    val parent2 = sortedPopulation[random.nextInt(sortedPopulation.size / 2)].first
                    var child = OptimizedChromosome.crossover(parent1, parent2, random)

                    if (random.nextDouble() < mutationRate) {
                        child = OptimizedChromosome.mutate(child, random)
                    }

                    offspring.add(child)
                }

                population = offspring
            }
        }

        return@runBlocking bestChromosomeOverall ?: population.first()
    }

    private fun evaluateFitness(chromosome: OptimizedChromosome, candles: List<com.tradeflow.core.domain.model.Candle>): Double = runBlocking {
        val metrics = evaluateDetailed(chromosome, candles)

        if (metrics.trades < 5) return@runBlocking -10.0 + (metrics.trades * 0.5)

        val returnScore = (metrics.pnlPercent / 20.0).coerceIn(-2.0, 3.0)
        val winRateScore = ((metrics.winRate - 50.0) / 25.0).coerceIn(-2.0, 2.0)
        val sharpeScore = (metrics.sharpeRatio / 2.0).coerceIn(-2.0, 2.0)
        val drawdownPenalty = (metrics.maxDrawdown / 50.0).coerceIn(0.0, 2.0)

        val tradeFrequencyScore = when {
            metrics.trades < 10 -> -0.3
            metrics.trades in 10..50 -> 0.3
            else -> 0.0
        }

        val profitFactorScore = (metrics.profitFactor - 1.0).coerceIn(-1.0, 2.0) * 0.3

        return@runBlocking (returnScore * 0.30) +
                           (winRateScore * 0.25) +
                           (sharpeScore * 0.20) +
                           (profitFactorScore * 0.15) -
                           (drawdownPenalty * 0.10) +
                           tradeFrequencyScore
    }

    private suspend fun evaluateDetailed(
        chromosome: OptimizedChromosome,
        candles: List<com.tradeflow.core.domain.model.Candle>
    ): OptimizationMetrics {
        val config = chromosome.toConfig()
        val primeHistory = candles.take(maxOf(chromosome.smaPeriod, 200))
        val testCandles = candles.drop(maxOf(chromosome.smaPeriod, 200))

        if (testCandles.size < 20) {
            return OptimizationMetrics(
                BigDecimal("500"), BigDecimal.ZERO, 0.0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
            )
        }

        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = config
        )

        val initialCapital = BigDecimal("500.00")
        var equity = initialCapital
        val openTrades = mutableListOf<NoSafeguardsBacktestTest.Trade>()
        val closedTrades = mutableListOf<NoSafeguardsBacktestTest.Trade>()
        val equityCurve = mutableListOf<BigDecimal>()

        testCandles.forEachIndexed { index, candle ->
            val history = (primeHistory + testCandles.take(index + 1)).takeLast(200)
            val decision = engine.execute(history, candle.close)

            openTrades.filter { it.isOpen }.forEach { trade ->
                val hitStopLoss = when (trade.direction) {
                    OrderSide.BUY -> candle.low <= trade.stopLoss
                    OrderSide.SELL -> candle.high >= trade.stopLoss
                }

                val hitTakeProfit = when (trade.direction) {
                    OrderSide.BUY -> candle.high >= trade.takeProfit
                    OrderSide.SELL -> candle.low <= trade.takeProfit
                }

                if (hitStopLoss) {
                    trade.exitCandle = index
                    trade.exitPrice = trade.stopLoss
                    trade.exitReason = "Stop Loss"
                    closedTrades.add(trade)
                    val pnl = trade.calculatePnl()
                    val pnlUsd = equity * pnl * config.strategy.trendPositionPercent
                    equity += pnlUsd
                } else if (hitTakeProfit) {
                    trade.exitCandle = index
                    trade.exitPrice = trade.takeProfit
                    trade.exitReason = "Take Profit"
                    closedTrades.add(trade)
                    val pnl = trade.calculatePnl()
                    val pnlUsd = equity * pnl * config.strategy.trendPositionPercent
                    equity += pnlUsd
                }
            }

            openTrades.removeAll { !it.isOpen }

            when (decision) {
                is Decision.Trend -> {
                    val newTrade = NoSafeguardsBacktestTest.Trade(
                        entryCandle = index,
                        direction = decision.direction,
                        entryPrice = decision.entryPrice,
                        stopLoss = decision.stopLoss,
                        takeProfit = decision.takeProfit
                    )
                    openTrades.add(newTrade)
                }
                else -> {}
            }

            equityCurve.add(equity)
        }

        openTrades.filter { it.isOpen }.forEach { trade ->
            trade.exitCandle = testCandles.size - 1
            trade.exitPrice = testCandles.last().close
            trade.exitReason = "Market Close"
            closedTrades.add(trade)
            val pnl = trade.calculatePnl()
            val pnlUsd = equity * pnl * config.strategy.trendPositionPercent
            equity += pnlUsd
        }

        val finalEquity = equity
        val totalPnl = finalEquity - initialCapital
        val pnlPercent = (totalPnl / initialCapital).toDouble() * 100

        val winningTrades = closedTrades.filter { it.calculatePnl() > BigDecimal.ZERO }
        val losingTrades = closedTrades.filter { it.calculatePnl() <= BigDecimal.ZERO }
        val winRate = if (closedTrades.isNotEmpty()) (winningTrades.size.toDouble() / closedTrades.size * 100) else 0.0

        val avgWin = if (winningTrades.isNotEmpty()) {
            winningTrades.map { it.calculatePnl().toDouble() * 100 }.average()
        } else 0.0

        val avgLoss = if (losingTrades.isNotEmpty()) {
            losingTrades.map { it.calculatePnl().toDouble() * 100 }.average()
        } else 0.0

        val totalWins = winningTrades.sumOf { it.calculatePnl().toDouble() }
        val totalLosses = losingTrades.sumOf { it.calculatePnl().toDouble() }.let { kotlin.math.abs(it) }
        val profitFactor = if (totalLosses > 0.0) totalWins / totalLosses else 0.0

        val returns = mutableListOf<Double>()
        for (i in 1 until equityCurve.size) {
            val ret = (equityCurve[i] - equityCurve[i - 1])
                .divide(equityCurve[i - 1], 6, RoundingMode.HALF_UP)
                .toDouble()
            returns.add(ret)
        }
        val avgReturn = if (returns.isNotEmpty()) returns.average() else 0.0
        val stdDev = if (returns.size > 1) {
            sqrt(returns.map { (it - avgReturn) * (it - avgReturn) }.average())
        } else 0.0
        val sharpeRatio = if (stdDev > 0.0) (avgReturn / stdDev) * sqrt(365.0 * 288.0) else 0.0

        var maxDrawdown = 0.0
        var peak = initialCapital
        equityCurve.forEach { eq ->
            if (eq > peak) peak = eq
            val dd = if (peak > BigDecimal.ZERO) {
                ((peak - eq).divide(peak, 4, RoundingMode.HALF_UP).toDouble() * 100)
            } else 0.0
            if (dd > maxDrawdown) maxDrawdown = dd
        }

        return OptimizationMetrics(
            finalEquity = finalEquity,
            totalPnl = totalPnl,
            pnlPercent = pnlPercent,
            trades = closedTrades.size,
            winRate = winRate,
            sharpeRatio = sharpeRatio,
            maxDrawdown = maxDrawdown,
            avgWin = avgWin,
            avgLoss = avgLoss,
            profitFactor = profitFactor
        )
    }
}
