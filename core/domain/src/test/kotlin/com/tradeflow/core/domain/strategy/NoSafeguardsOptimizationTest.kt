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

data class OptimizedChromosome(
    val adxTrendThreshold: Double,
    val adxRangeThreshold: Double,
    val stopLossAtrMultiplier: Double,
    val takeProfitAtrMultiplier: Double,
    val trendPositionPercent: Double,
    val confirmationCandles: Int,
    val volumeThreshold: Double,
    val smaPeriod: Int
) {
    fun toConfig(): TradingConfig {
        return TradingConfig(
            strategy = StrategyParameters(
                adxTrendThreshold = adxTrendThreshold,
                adxRangeThreshold = adxRangeThreshold,
                stopLossAtrMultiplier = BigDecimal(stopLossAtrMultiplier.toString()),
                takeProfitAtrMultiplier = BigDecimal(takeProfitAtrMultiplier.toString()),
                trendPositionPercent = BigDecimal(trendPositionPercent.toString()),
                gridPositionPercentPerLevel = BigDecimal((trendPositionPercent * 1.5).toString()),
                confirmationCandles = confirmationCandles,
                leverage = BigDecimal("2.0")
            ),
            risk = RiskParameters(),
            technical = TechnicalParameters(
                minVolumeRatio = volumeThreshold,
                smaPeriod = smaPeriod
            ),
            execution = ExecutionParameters(),
            profile = RiskProfile.BALANCED
        )
    }

    companion object {
        fun random(random: Random): OptimizedChromosome {
            return OptimizedChromosome(
                adxTrendThreshold = random.nextDouble(12.0, 25.0),
                adxRangeThreshold = random.nextDouble(8.0, 15.0),
                stopLossAtrMultiplier = random.nextDouble(3.0, 6.0),
                takeProfitAtrMultiplier = random.nextDouble(9.0, 18.0),
                trendPositionPercent = random.nextDouble(0.03, 0.08),
                confirmationCandles = random.nextInt(2, 5),
                volumeThreshold = random.nextDouble(0.8, 1.8),
                smaPeriod = listOf(20, 50, 100).random(random)
            )
        }

        fun crossover(parent1: OptimizedChromosome, parent2: OptimizedChromosome, random: Random): OptimizedChromosome {
            return OptimizedChromosome(
                adxTrendThreshold = if (random.nextBoolean()) parent1.adxTrendThreshold else parent2.adxTrendThreshold,
                adxRangeThreshold = if (random.nextBoolean()) parent1.adxRangeThreshold else parent2.adxRangeThreshold,
                stopLossAtrMultiplier = if (random.nextBoolean()) parent1.stopLossAtrMultiplier else parent2.stopLossAtrMultiplier,
                takeProfitAtrMultiplier = if (random.nextBoolean()) parent1.takeProfitAtrMultiplier else parent2.takeProfitAtrMultiplier,
                trendPositionPercent = if (random.nextBoolean()) parent1.trendPositionPercent else parent2.trendPositionPercent,
                confirmationCandles = if (random.nextBoolean()) parent1.confirmationCandles else parent2.confirmationCandles,
                volumeThreshold = if (random.nextBoolean()) parent1.volumeThreshold else parent2.volumeThreshold,
                smaPeriod = if (random.nextBoolean()) parent1.smaPeriod else parent2.smaPeriod
            )
        }

        fun mutate(chromosome: OptimizedChromosome, random: Random): OptimizedChromosome {
            return when (random.nextInt(8)) {
                0 -> chromosome.copy(adxTrendThreshold = (chromosome.adxTrendThreshold + random.nextDouble(-3.0, 3.0)).coerceIn(10.0, 30.0))
                1 -> chromosome.copy(adxRangeThreshold = (chromosome.adxRangeThreshold + random.nextDouble(-2.0, 2.0)).coerceIn(5.0, 18.0))
                2 -> chromosome.copy(stopLossAtrMultiplier = (chromosome.stopLossAtrMultiplier + random.nextDouble(-1.0, 1.0)).coerceIn(2.0, 8.0))
                3 -> chromosome.copy(takeProfitAtrMultiplier = (chromosome.takeProfitAtrMultiplier + random.nextDouble(-2.0, 2.0)).coerceIn(6.0, 25.0))
                4 -> chromosome.copy(trendPositionPercent = (chromosome.trendPositionPercent + random.nextDouble(-0.01, 0.01)).coerceIn(0.02, 0.10))
                5 -> chromosome.copy(confirmationCandles = (chromosome.confirmationCandles + random.nextInt(-1, 2)).coerceIn(1, 6))
                6 -> chromosome.copy(volumeThreshold = (chromosome.volumeThreshold + random.nextDouble(-0.2, 0.2)).coerceIn(0.5, 2.5))
                else -> chromosome.copy(smaPeriod = listOf(20, 50, 100).random(random))
            }
        }
    }
}

data class OptimizationMetrics(
    val finalEquity: BigDecimal,
    val totalPnl: BigDecimal,
    val pnlPercent: Double,
    val trades: Int,
    val winRate: Double,
    val sharpeRatio: Double,
    val maxDrawdown: Double,
    val avgWin: Double,
    val avgLoss: Double,
    val profitFactor: Double
)

class NoSafeguardsOptimizationTest {

    @Test
    fun `genetic algorithm - no safeguards - find profitable config`() = runBlocking {
        println("\n🧬 GENETIC OPTIMIZATION (NO SAFEGUARDS MODE)")
        println("=".repeat(90))
        println("Optimizing parameters with full trade execution (no isInTrade blocking)")
        println("=".repeat(90))
        println()

        // Load real historical data
        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "5m",
            limit = 1500
        )
        val inSample = allCandles.take(1000)
        val outOfSample = allCandles.drop(1000)

        println("Data Split:")
        println("  In-Sample:      ${inSample.size} candles (training)")
        println("  Out-Of-Sample:  ${outOfSample.size} candles (validation)")
        println()

        val populationSize = 40
        val generations = 30
        val mutationRate = 0.25
        val eliteRatio = 0.20

        println("Genetic Algorithm Config:")
        println("  Population:     $populationSize")
        println("  Generations:    $generations")
        println("  Mutation Rate:  ${(mutationRate * 100).toInt()}%")
        println("  Elite Ratio:    ${(eliteRatio * 100).toInt()}%")
        println("=".repeat(90))
        println()

        val random = Random(42)
        var population = List(populationSize) { OptimizedChromosome.random(random) }

        val fitnessFunction = { chromosome: OptimizedChromosome ->
            evaluateFitness(chromosome, inSample)
        }

        println("🔬 EVOLUTION IN PROGRESS...")
        println("-".repeat(90))

        var bestFitnessOverall = Double.NEGATIVE_INFINITY
        var bestChromosomeOverall: OptimizedChromosome? = null

        repeat(generations) { gen ->
            val fitnesses = population.map { fitnessFunction(it) }
            val sortedPopulation = population.zip(fitnesses).sortedByDescending { it.second }

            val best = sortedPopulation.first().second
            val avg = fitnesses.average()
            val worst = sortedPopulation.last().second

            if (best > bestFitnessOverall) {
                bestFitnessOverall = best
                bestChromosomeOverall = sortedPopulation.first().first
            }

            if (gen % 3 == 0 || gen == generations - 1) {
                println("Gen ${gen.toString().padStart(2)}: Best=${"%.3f".format(best)} | Avg=${"%.3f".format(avg)} | Worst=${"%.3f".format(worst)}")
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
            } else {
                population = sortedPopulation.map { it.first }
            }
        }

        val champion = bestChromosomeOverall ?: population.first()
        val championFitness = fitnessFunction(champion)

        println()
        println("=".repeat(90))
        println("🏆 CHAMPION FOUND")
        println("=".repeat(90))
        println("In-Sample Fitness: ${"%.3f".format(championFitness)}")
        println()
        println("Optimal Parameters:")
        println("  ADX Trend Threshold:       ${"%.2f".format(champion.adxTrendThreshold)}")
        println("  ADX Range Threshold:       ${"%.2f".format(champion.adxRangeThreshold)}")
        println("  Stop Loss ATR Multiplier:  ${"%.2f".format(champion.stopLossAtrMultiplier)}×")
        println("  Take Profit ATR Multiplier: ${"%.2f".format(champion.takeProfitAtrMultiplier)}×")
        println("  Risk/Reward Ratio:         ${"%.2f".format(champion.takeProfitAtrMultiplier / champion.stopLossAtrMultiplier)}:1")
        println("  Position Size:             ${"%.2f".format(champion.trendPositionPercent * 100)}%")
        println("  Confirmation Candles:      ${champion.confirmationCandles}")
        println("  Volume Threshold:          ${"%.2f".format(champion.volumeThreshold)}×")
        println("  SMA Period:                ${champion.smaPeriod}")
        println()

        // Detailed in-sample evaluation
        println("📊 IN-SAMPLE PERFORMANCE")
        println("=".repeat(90))
        val inSampleMetrics = evaluateDetailed(champion, inSample)
        printMetrics(inSampleMetrics)

        // Out-of-sample validation
        println()
        println("🧪 OUT-OF-SAMPLE VALIDATION")
        println("=".repeat(90))
        val oosMetrics = evaluateDetailed(champion, outOfSample)
        printMetrics(oosMetrics)

        println()
        println("=".repeat(90))
        if (oosMetrics.pnlPercent > 0 && oosMetrics.trades >= 10 && oosMetrics.winRate >= 50.0) {
            println("✅ SUCCESS! Out-of-sample PROFITABLE with good win rate!")
            println("   This configuration is ready for live testing.")
        } else if (oosMetrics.pnlPercent > 0 && oosMetrics.trades >= 5) {
            println("⚠️  Out-of-sample profitable but needs more validation (${oosMetrics.trades} trades, ${oosMetrics.winRate.toInt()}% win rate)")
        } else if (oosMetrics.trades < 5) {
            println("⚠️  Too few trades (${oosMetrics.trades}) for validation - may be overfitted to conservative strategy")
        } else {
            println("❌ Unprofitable on out-of-sample (${oosMetrics.pnlPercent.let { "%.2f".format(it) }}%)")
            println("   Try longer training period or different market conditions")
        }
        println("=".repeat(90))
    }

    private fun evaluateFitness(chromosome: OptimizedChromosome, candles: List<com.tradeflow.core.domain.model.Candle>): Double = runBlocking {
        val metrics = evaluateDetailed(chromosome, candles)

        // Penalize heavily for too few trades
        if (metrics.trades < 10) return@runBlocking -10.0 + (metrics.trades * 0.5)

        // Composite fitness score
        val returnScore = (metrics.pnlPercent / 20.0).coerceIn(-2.0, 3.0)
        val winRateScore = ((metrics.winRate - 50.0) / 25.0).coerceIn(-2.0, 2.0)
        val sharpeScore = (metrics.sharpeRatio / 2.0).coerceIn(-2.0, 2.0)
        val drawdownPenalty = (metrics.maxDrawdown / 50.0).coerceIn(0.0, 2.0)

        // Trade frequency bonus: 20-100 trades optimal
        val tradeFrequencyScore = when {
            metrics.trades < 20 -> -0.5
            metrics.trades in 20..100 -> 0.5
            else -> 0.0
        }

        // Profit factor bonus
        val profitFactorScore = (metrics.profitFactor - 1.0).coerceIn(-1.0, 2.0) * 0.3

        return@runBlocking (returnScore * 0.30) +
                           (winRateScore * 0.25) +
                           (sharpeScore * 0.20) +
                           (profitFactorScore * 0.15) -
                           (drawdownPenalty * 0.10) +
                           tradeFrequencyScore
    }

    private fun printMetrics(metrics: OptimizationMetrics) {
        val pnlSign = if (metrics.totalPnl >= BigDecimal.ZERO) "+" else ""
        println("Final Equity:    \$${metrics.finalEquity.setScale(2, RoundingMode.HALF_UP)}")
        println("Total PnL:       $pnlSign\$${metrics.totalPnl.setScale(2, RoundingMode.HALF_UP)} ($pnlSign${"%.2f".format(metrics.pnlPercent)}%)")
        println("Total Trades:    ${metrics.trades}")
        println("Win Rate:        ${"%.1f".format(metrics.winRate)}%")
        println("Sharpe Ratio:    ${"%.2f".format(metrics.sharpeRatio)}")
        println("Max Drawdown:    ${"%.2f".format(metrics.maxDrawdown)}%")
        if (metrics.avgWin != 0.0) println("Avg Win:         ${"%.2f".format(metrics.avgWin)}%")
        if (metrics.avgLoss != 0.0) println("Avg Loss:        ${"%.2f".format(metrics.avgLoss)}%")
        println("Profit Factor:   ${"%.2f".format(metrics.profitFactor)}")
    }

    private suspend fun evaluateDetailed(
        chromosome: OptimizedChromosome,
        candles: List<com.tradeflow.core.domain.model.Candle>
    ): OptimizationMetrics {
        val config = chromosome.toConfig()
        val primeHistory = candles.take(maxOf(chromosome.smaPeriod, 200))
        val testCandles = candles.drop(maxOf(chromosome.smaPeriod, 200))

        if (testCandles.size < 50) {
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

            // Check exits
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

            // Execute new signals
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

        // Close remaining trades
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

        // Sharpe ratio
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

        // Max drawdown
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
