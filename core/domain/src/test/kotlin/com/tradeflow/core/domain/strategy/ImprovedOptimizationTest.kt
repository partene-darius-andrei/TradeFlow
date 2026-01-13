package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.*
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.usecase.ExecuteTradingCycleUseCase
import com.tradeflow.core.domain.simulator.SimulatedExchange
import com.tradeflow.core.domain.util.BinanceDataLoader
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.sqrt
import kotlin.random.Random

data class ExtendedChromosome(
    val adxTrendThreshold: Double,
    val adxRangeThreshold: Double,
    val stopLossAtrMultiplier: Double,
    val takeProfitAtrMultiplier: Double,
    val trendPositionPercent: Double,
    val gridPositionPercentPerLevel: Double,
    val confirmationCandles: Int,
    val volumeThreshold: Double  // NEW: This was missing!
) {
    fun toConfig(): TradingConfig {
        return TradingConfig(
            strategy = StrategyParameters(
                adxTrendThreshold = adxTrendThreshold,
                adxRangeThreshold = adxRangeThreshold,
                stopLossAtrMultiplier = BigDecimal(stopLossAtrMultiplier.toString()),
                takeProfitAtrMultiplier = BigDecimal(takeProfitAtrMultiplier.toString()),
                trendPositionPercent = BigDecimal(trendPositionPercent.toString()),
                gridPositionPercentPerLevel = BigDecimal(gridPositionPercentPerLevel.toString()),
                confirmationCandles = confirmationCandles,
                leverage = BigDecimal("2.0")
            ),
            risk = RiskParameters(),
            technical = TechnicalParameters(
                minVolumeRatio = volumeThreshold
            ),
            execution = ExecutionParameters(),
            profile = RiskProfile.BALANCED
        )
    }

    companion object {
        fun random(random: Random): ExtendedChromosome {
            return ExtendedChromosome(
                adxTrendThreshold = random.nextDouble(12.0, 25.0),
                adxRangeThreshold = random.nextDouble(8.0, 15.0),
                stopLossAtrMultiplier = random.nextDouble(5.0, 15.0),
                takeProfitAtrMultiplier = random.nextDouble(10.0, 30.0),
                trendPositionPercent = random.nextDouble(0.02, 0.10),
                gridPositionPercentPerLevel = random.nextDouble(0.03, 0.12),
                confirmationCandles = random.nextInt(1, 5),
                volumeThreshold = random.nextDouble(0.8, 1.8)
            )
        }

        fun crossover(parent1: ExtendedChromosome, parent2: ExtendedChromosome, random: Random): ExtendedChromosome {
            return ExtendedChromosome(
                adxTrendThreshold = if (random.nextBoolean()) parent1.adxTrendThreshold else parent2.adxTrendThreshold,
                adxRangeThreshold = if (random.nextBoolean()) parent1.adxRangeThreshold else parent2.adxRangeThreshold,
                stopLossAtrMultiplier = if (random.nextBoolean()) parent1.stopLossAtrMultiplier else parent2.stopLossAtrMultiplier,
                takeProfitAtrMultiplier = if (random.nextBoolean()) parent1.takeProfitAtrMultiplier else parent2.takeProfitAtrMultiplier,
                trendPositionPercent = if (random.nextBoolean()) parent1.trendPositionPercent else parent2.trendPositionPercent,
                gridPositionPercentPerLevel = if (random.nextBoolean()) parent1.gridPositionPercentPerLevel else parent2.gridPositionPercentPerLevel,
                confirmationCandles = if (random.nextBoolean()) parent1.confirmationCandles else parent2.confirmationCandles,
                volumeThreshold = if (random.nextBoolean()) parent1.volumeThreshold else parent2.volumeThreshold
            )
        }

        fun mutate(chromosome: ExtendedChromosome, random: Random): ExtendedChromosome {
            return when (random.nextInt(8)) {
                0 -> chromosome.copy(adxTrendThreshold = (chromosome.adxTrendThreshold + random.nextDouble(-2.0, 2.0)).coerceIn(10.0, 30.0))
                1 -> chromosome.copy(adxRangeThreshold = (chromosome.adxRangeThreshold + random.nextDouble(-1.0, 1.0)).coerceIn(5.0, 18.0))
                2 -> chromosome.copy(stopLossAtrMultiplier = (chromosome.stopLossAtrMultiplier + random.nextDouble(-2.0, 2.0)).coerceIn(3.0, 20.0))
                3 -> chromosome.copy(takeProfitAtrMultiplier = (chromosome.takeProfitAtrMultiplier + random.nextDouble(-3.0, 3.0)).coerceIn(8.0, 40.0))
                4 -> chromosome.copy(trendPositionPercent = (chromosome.trendPositionPercent + random.nextDouble(-0.01, 0.01)).coerceIn(0.01, 0.15))
                5 -> chromosome.copy(gridPositionPercentPerLevel = (chromosome.gridPositionPercentPerLevel + random.nextDouble(-0.01, 0.01)).coerceIn(0.01, 0.15))
                6 -> chromosome.copy(confirmationCandles = (chromosome.confirmationCandles + random.nextInt(-1, 2)).coerceIn(1, 6))
                else -> chromosome.copy(volumeThreshold = (chromosome.volumeThreshold + random.nextDouble(-0.2, 0.2)).coerceIn(0.5, 2.0))
            }
        }
    }
}

class ImprovedOptimizationTest {

    @Test
    fun `genetic algorithm with volume threshold - real data`() = runBlocking {
        println("\n🧬 IMPROVED GENETIC OPTIMIZATION")
        println("=".repeat(90))
        println("Optimizing 8 parameters (including volume threshold) on real historical data")
        println("=".repeat(90))
        println()

        // Load REAL historical data (not synthetic)
        val allCandles = BinanceDataLoader.fetchHistoricalCandles(interval = "4h", limit = 600)
        val inSample = allCandles.take(400)      // First 400 for training
        val outOfSample = allCandles.drop(400)   // Last 200 for validation

        println("In-Sample:      ${inSample.size} candles (training)")
        println("Out-Of-Sample:  ${outOfSample.size} candles (validation)")
        println()

        val populationSize = 30
        val generations = 50
        val mutationRate = 0.20
        val eliteRatio = 0.15

        println("Population:     $populationSize")
        println("Generations:    $generations")
        println("Mutation Rate:  ${(mutationRate * 100).toInt()}%")
        println("Elite Ratio:    ${(eliteRatio * 100).toInt()}%")
        println("=".repeat(90))
        println()

        val random = Random(42)
        var population = List(populationSize) { ExtendedChromosome.random(random) }

        // Fitness function: Evaluate on in-sample data
        val fitnessFunction = { chromosome: ExtendedChromosome ->
            evaluateFitness(chromosome, inSample)
        }

        println("🔬 EVOLUTION IN PROGRESS...")
        println("-".repeat(90))

        repeat(generations) { gen ->
            // Evaluate fitness for all individuals
            val fitnesses = population.map { fitnessFunction(it) }
            val sortedPopulation = population.zip(fitnesses).sortedByDescending { it.second }

            if (gen % 5 == 0 || gen == generations - 1) {
                val best = sortedPopulation.first().second
                val avg = fitnesses.average()
                val worst = sortedPopulation.last().second
                println("Gen ${gen.toString().padStart(2)}: Best=${"%.3f".format(best)} | Avg=${"%.3f".format(avg)} | Worst=${"%.3f".format(worst)}")
            }

            if (gen < generations - 1) {
                // Elitism: Keep top performers
                val eliteCount = (populationSize * eliteRatio).toInt()
                val elite = sortedPopulation.take(eliteCount).map { it.first }

                // Generate offspring
                val offspring = mutableListOf<ExtendedChromosome>()
                offspring.addAll(elite)

                while (offspring.size < populationSize) {
                    val parent1 = sortedPopulation[random.nextInt(sortedPopulation.size / 2)].first
                    val parent2 = sortedPopulation[random.nextInt(sortedPopulation.size / 2)].first
                    var child = ExtendedChromosome.crossover(parent1, parent2, random)

                    if (random.nextDouble() < mutationRate) {
                        child = ExtendedChromosome.mutate(child, random)
                    }

                    offspring.add(child)
                }

                population = offspring
            } else {
                // Final generation - get champion
                population = sortedPopulation.map { it.first }
            }
        }

        val champion = population.first()
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
        println("  Stop Loss ATR Multiplier:  ${"%.2f".format(champion.stopLossAtrMultiplier)}")
        println("  Take Profit ATR Multiplier: ${"%.2f".format(champion.takeProfitAtrMultiplier)}")
        println("  Trend Position %:          ${"%.2f".format(champion.trendPositionPercent * 100)}%")
        println("  Grid Position %:           ${"%.2f".format(champion.gridPositionPercentPerLevel * 100)}%")
        println("  Confirmation Candles:      ${champion.confirmationCandles}")
        println("  Volume Threshold:          ${"%.2f".format(champion.volumeThreshold)}x")
        println()

        // Validate on out-of-sample data
        println("🧪 OUT-OF-SAMPLE VALIDATION")
        println("=".repeat(90))
        val oosMetrics = evaluateDetailed(champion, outOfSample)

        val pnlSign = if (oosMetrics.totalPnL >= BigDecimal.ZERO) "+" else ""
        println("Final Equity:    ${oosMetrics.finalEquity.setScale(2, RoundingMode.HALF_UP)} USD")
        println("Total PnL:       $pnlSign${oosMetrics.totalPnL.setScale(2, RoundingMode.HALF_UP)} USD ($pnlSign${"%.2f".format(oosMetrics.pnlPercent)}%)")
        println("Total Trades:    ${oosMetrics.trades}")
        println("Win Rate:        ${"%.0f".format(oosMetrics.winRate)}%")
        println("Sharpe Ratio:    ${"%.2f".format(oosMetrics.sharpeRatio)}")
        println("Max Drawdown:    ${"%.2f".format(oosMetrics.maxDrawdown)}%")
        println("=".repeat(90))

        if (oosMetrics.pnlPercent > 0 && oosMetrics.trades >= 5) {
            println("\n✅ SUCCESS! Out-of-sample profitable with statistical significance.")
        } else if (oosMetrics.trades < 5) {
            println("\n⚠️  Too few trades (${oosMetrics.trades}) for validation.")
        } else {
            println("\n❌ Unprofitable on out-of-sample data.")
        }
    }

    private fun evaluateFitness(chromosome: ExtendedChromosome, candles: List<com.tradeflow.core.domain.model.Candle>): Double = runBlocking {
        val metrics = evaluateDetailed(chromosome, candles)

        // Fitness function that heavily penalizes 0 trades
        if (metrics.trades == 0) return@runBlocking -10.0

        // Composite score:
        // - Sharpe ratio (40%): Risk-adjusted returns
        // - Total return (30%): Profitability
        // - Win rate (20%): Consistency
        // - Penalize drawdown (10%): Risk management
        // - Bonus for trade frequency (5-20 trades is ideal)

        val normalizedSharpe = (metrics.sharpeRatio / 2.0).coerceIn(-2.0, 2.0)
        val normalizedReturn = (metrics.pnlPercent / 30.0).coerceIn(-2.0, 2.0)
        val normalizedWinRate = (metrics.winRate / 100.0)
        val drawdownPenalty = (metrics.maxDrawdown / 100.0)

        // Trade frequency bonus: Peak at 10-15 trades, penalty for too few or too many
        val tradeBonus = when {
            metrics.trades < 5 -> -0.5  // Too conservative
            metrics.trades in 5..20 -> 0.3  // Good
            else -> 0.0  // Too aggressive (overtrading)
        }

        return@runBlocking (normalizedSharpe * 0.4) + (normalizedReturn * 0.3) + (normalizedWinRate * 0.2) - (drawdownPenalty * 0.1) + tradeBonus
    }

    private data class PerformanceMetrics(
        val finalEquity: BigDecimal,
        val totalPnL: BigDecimal,
        val pnlPercent: Double,
        val trades: Int,
        val winRate: Double,
        val sharpeRatio: Double,
        val maxDrawdown: Double
    )

    private suspend fun evaluateDetailed(
        chromosome: ExtendedChromosome,
        candles: List<com.tradeflow.core.domain.model.Candle>
    ): PerformanceMetrics {
        val config = chromosome.toConfig()
        val initialCapital = BigDecimal("500.00")
        val exchange = SimulatedExchange(
            initialUsd = initialCapital,
            tradingConfig = config
        )

        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = config
        )

        val orchestrator = ExecuteTradingCycleUseCase(
            exchangeRepository = exchange,
            makeDecisionUseCase = engine,
            config = config,
            trailingStopManager = com.tradeflow.core.domain.risk.TrailingStopManager(config)
        )

        val primeHistory = candles.take(200)
        val simulationCandles = candles.drop(200)

        exchange.setHistory(primeHistory)

        var highWaterMark = initialCapital
        val equityCurve = mutableListOf<BigDecimal>()
        var tradeCount = 0
        var winCount = 0
        var previousPosition: com.tradeflow.core.domain.model.PerpetualPosition? = null

        simulationCandles.forEach { candle ->
            exchange.advanceTime(candle)

            val currentEquity = exchange.getTotalEquity()
            equityCurve.add(currentEquity)

            val cycleResult = orchestrator.runCycle("BTC-USD", highWaterMark)
            highWaterMark = cycleResult.updatedHighWaterMark

            val currentPosition = exchange.getPerpetualPosition("BTC-USD").getOrNull()
            if (previousPosition != null && currentPosition == null) {
                tradeCount++
                if (previousPosition.unrealizedPnl > BigDecimal.ZERO) winCount++
            }
            previousPosition = currentPosition
        }

        val finalEquity = exchange.getTotalEquity()
        val totalPnL = finalEquity - initialCapital
        val pnlPercent = if (initialCapital > BigDecimal.ZERO) {
            (totalPnL.divide(initialCapital, 6, RoundingMode.HALF_UP).toDouble() * 100)
        } else 0.0

        val winRate = if (tradeCount > 0) (winCount.toDouble() / tradeCount * 100) else 0.0

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
        val sharpeRatio = if (stdDev > 0.0) (avgReturn / stdDev) * sqrt(365.0 * 6.0) else 0.0

        // Max drawdown
        var maxDrawdown = 0.0
        var peak = initialCapital
        equityCurve.forEach { equity ->
            if (equity > peak) peak = equity
            val dd = if (peak > BigDecimal.ZERO) {
                ((peak - equity).divide(peak, 4, RoundingMode.HALF_UP).toDouble() * 100)
            } else 0.0
            if (dd > maxDrawdown) maxDrawdown = dd
        }

        return PerformanceMetrics(
            finalEquity = finalEquity,
            totalPnL = totalPnL,
            pnlPercent = pnlPercent,
            trades = tradeCount,
            winRate = winRate,
            sharpeRatio = sharpeRatio,
            maxDrawdown = maxDrawdown
        )
    }
}
