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

/**
 * AGGRESSIVE OPTIMIZATION: MAKE IT TRADE
 *
 * Philosophy:
 * - Remove conservative filters that block trades
 * - Use 15m candles for high frequency
 * - Prioritize LEARNING over capital preservation
 * - Accept losses as data for optimization
 * - Trade NOW, optimize LATER
 */
class AggressiveOptimizationTest {

    @Test
    fun `aggressive 15m optimization - remove safeguards`() = runBlocking {
        println("\n⚡ AGGRESSIVE 15-MINUTE OPTIMIZATION")
        println("=".repeat(90))
        println("GOAL: Make the system TRADE. Remove conservative filters.")
        println("=".repeat(90))
        println()

        // 15-minute candles: 1000 candles = ~10 days of trading
        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "15m",
            limit = 1400
        )

        val inSample = allCandles.take(1000)       // First 1000 for training
        val outOfSample = allCandles.drop(1000)    // Last 400 for validation

        println("Timeframe:      15-minute candles")
        println("In-Sample:      ${inSample.size} candles (~10 days)")
        println("Out-Of-Sample:  ${outOfSample.size} candles (~4 days)")
        println()

        // AGGRESSIVE parameter space
        val configs = listOf(
            // Ultra-aggressive: Almost no filters
            AggressiveConfig(
                volumeThreshold = 0.5,    // Very low
                confirmationCandles = 1,   // Instant reaction
                adxTrendThreshold = 10.0,  // Lower bar
                stopLossAtr = 3.0,         // Tight stops
                takeProfitAtr = 9.0,       // 3:1 reward
                positionPercent = 0.08     // 8% per trade
            ),
            // Aggressive: Minimal filters
            AggressiveConfig(
                volumeThreshold = 0.7,
                confirmationCandles = 1,
                adxTrendThreshold = 12.0,
                stopLossAtr = 4.0,
                takeProfitAtr = 12.0,
                positionPercent = 0.06
            ),
            // Moderate-Aggressive: Some filtering
            AggressiveConfig(
                volumeThreshold = 0.9,
                confirmationCandles = 2,
                adxTrendThreshold = 15.0,
                stopLossAtr = 5.0,
                takeProfitAtr = 15.0,
                positionPercent = 0.05
            ),
            // Balanced-Aggressive: Slightly conservative
            AggressiveConfig(
                volumeThreshold = 1.0,
                confirmationCandles = 2,
                adxTrendThreshold = 18.0,
                stopLossAtr = 6.0,
                takeProfitAtr = 18.0,
                positionPercent = 0.04
            )
        )

        println("Testing ${configs.size} aggressive configurations...")
        println("-".repeat(90))
        println()

        val results = configs.map { config ->
            val metrics = evaluateConfig(config, inSample)
            Pair(config, metrics)
        }

        // Sort by trade frequency first, then profitability
        val sorted = results.sortedWith(
            compareByDescending<Pair<AggressiveConfig, Metrics>> { it.second.trades }
                .thenByDescending { it.second.pnlPercent }
        )

        println("📊 RESULTS (sorted by trade frequency)")
        println("=".repeat(90))
        sorted.forEachIndexed { index, (config, metrics) ->
            val pnlSign = if (metrics.pnlPercent >= 0) "+" else ""
            println("#${index + 1}: Vol=${config.volumeThreshold}x Conf=${config.confirmationCandles} ADX=${config.adxTrendThreshold}")
            println("      Trades: ${metrics.trades} | PnL: $pnlSign${"%.2f".format(metrics.pnlPercent)}% | " +
                "WinRate: ${"%.0f".format(metrics.winRate)}% | Sharpe: ${"%.2f".format(metrics.sharpeRatio)}")
            println()
        }

        // Pick best by trade frequency (must have >= 5 trades)
        val champion = sorted.firstOrNull { it.second.trades >= 5 } ?: sorted.first()

        println("🏆 CHAMPION (Most Active)")
        println("=".repeat(90))
        println("Volume Threshold:     ${champion.first.volumeThreshold}x")
        println("Confirmation Candles: ${champion.first.confirmationCandles}")
        println("ADX Threshold:        ${champion.first.adxTrendThreshold}")
        println("Stop Loss:            ${champion.first.stopLossAtr}× ATR")
        println("Take Profit:          ${champion.first.takeProfitAtr}× ATR")
        println("Position Size:        ${"%.1f".format(champion.first.positionPercent * 100)}%")
        println()
        println("In-Sample Performance:")
        println("  Trades:      ${champion.second.trades}")
        println("  PnL:         ${if (champion.second.pnlPercent >= 0) "+" else ""}${"%.2f".format(champion.second.pnlPercent)}%")
        println("  Win Rate:    ${"%.0f".format(champion.second.winRate)}%")
        println("  Sharpe:      ${"%.2f".format(champion.second.sharpeRatio)}")
        println("  Max DD:      ${"%.2f".format(champion.second.maxDrawdown)}%")
        println()

        // Validate on out-of-sample
        println("🧪 OUT-OF-SAMPLE VALIDATION")
        println("=".repeat(90))
        val oosMetrics = evaluateConfig(champion.first, outOfSample)

        val oosPnlSign = if (oosMetrics.pnlPercent >= 0) "+" else ""
        println("Trades:      ${oosMetrics.trades}")
        println("PnL:         $oosPnlSign${"%.2f".format(oosMetrics.pnlPercent)}%")
        println("Win Rate:    ${"%.0f".format(oosMetrics.winRate)}%")
        println("Sharpe:      ${"%.2f".format(oosMetrics.sharpeRatio)}")
        println("Max DD:      ${"%.2f".format(oosMetrics.maxDrawdown)}%")
        println("=".repeat(90))

        if (oosMetrics.trades > 0) {
            println("\n✅ SUCCESS! System is trading actively.")
            if (oosMetrics.pnlPercent > 0) {
                println("   PROFITABLE on out-of-sample! This config shows promise.")
            } else {
                println("   Unprofitable but TRADING. Now we have data to optimize.")
            }
        } else {
            println("\n❌ Still no trades. Market conditions may be incompatible.")
        }

        println("\n📝 NEXT STEPS:")
        if (champion.second.pnlPercent > 0) {
            println("  1. Fine-tune this configuration with genetic algorithm")
            println("  2. Backtest on longer periods (1+ month)")
            println("  3. Deploy to paper trading")
        } else {
            println("  1. Analyze losing trades to identify patterns")
            println("  2. Adjust stop/target ratios")
            println("  3. Test different entry filters (RSI, CMF, etc)")
        }
    }

    @Test
    fun `genetic optimization on 15m - aggressive parameters`() = runBlocking {
        println("\n🧬 GENETIC ALGORITHM - 15M AGGRESSIVE")
        println("=".repeat(90))
        println("Optimizing on 15-minute candles with reduced safeguards")
        println("=".repeat(90))
        println()

        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "15m",
            limit = 1400
        )

        val inSample = allCandles.take(1000)

        val populationSize = 20
        val generations = 30
        val mutationRate = 0.25
        val eliteRatio = 0.20

        println("Population:  $populationSize")
        println("Generations: $generations")
        println("Mutation:    ${(mutationRate * 100).toInt()}%")
        println("=".repeat(90))
        println()

        val random = Random(42)

        // Initialize with aggressive ranges
        var population = List(populationSize) {
            AggressiveConfig(
                volumeThreshold = random.nextDouble(0.5, 1.5),
                confirmationCandles = random.nextInt(1, 4),
                adxTrendThreshold = random.nextDouble(8.0, 20.0),
                stopLossAtr = random.nextDouble(2.0, 8.0),
                takeProfitAtr = random.nextDouble(6.0, 24.0),
                positionPercent = random.nextDouble(0.03, 0.10)
            )
        }

        println("🔬 EVOLUTION (Fitness = Trades × (1 + Return/10) - Drawdown/20)")
        println("-".repeat(90))

        repeat(generations) { gen ->
            val fitnesses = population.map { config ->
                val metrics = evaluateConfig(config, inSample)
                // Fitness: Prioritize trades, then profitability, penalize drawdown
                val tradePriority = metrics.trades.toDouble()
                val returnBonus = metrics.pnlPercent / 10.0
                val drawdownPenalty = metrics.maxDrawdown / 20.0
                tradePriority * (1.0 + returnBonus) - drawdownPenalty
            }

            val sorted = population.zip(fitnesses).sortedByDescending { it.second }

            if (gen % 5 == 0 || gen == generations - 1) {
                val best = sorted.first()
                val bestMetrics = evaluateConfig(best.first, inSample)
                println("Gen ${gen.toString().padStart(2)}: Fitness=${"%.2f".format(best.second)} | " +
                    "Trades=${bestMetrics.trades} | PnL=${"%.2f".format(bestMetrics.pnlPercent)}%")
            }

            if (gen < generations - 1) {
                val eliteCount = (populationSize * eliteRatio).toInt()
                val elite = sorted.take(eliteCount).map { it.first }

                val offspring = mutableListOf<AggressiveConfig>()
                offspring.addAll(elite)

                while (offspring.size < populationSize) {
                    val parent1 = sorted[random.nextInt(sorted.size / 2)].first
                    val parent2 = sorted[random.nextInt(sorted.size / 2)].first
                    var child = crossover(parent1, parent2, random)

                    if (random.nextDouble() < mutationRate) {
                        child = mutate(child, random)
                    }

                    offspring.add(child)
                }

                population = offspring
            } else {
                population = sorted.map { it.first }
            }
        }

        val champion = population.first()
        val championMetrics = evaluateConfig(champion, inSample)

        println()
        println("=".repeat(90))
        println("🏆 CHAMPION")
        println("=".repeat(90))
        println("Volume Threshold:     ${champion.volumeThreshold}x")
        println("Confirmation Candles: ${champion.confirmationCandles}")
        println("ADX Threshold:        ${champion.adxTrendThreshold}")
        println("Stop Loss:            ${champion.stopLossAtr}× ATR")
        println("Take Profit:          ${champion.takeProfitAtr}× ATR")
        println("Position Size:        ${"%.1f".format(champion.positionPercent * 100)}%")
        println()
        println("Performance:")
        println("  Trades:   ${championMetrics.trades}")
        println("  PnL:      ${if (championMetrics.pnlPercent >= 0) "+" else ""}${"%.2f".format(championMetrics.pnlPercent)}%")
        println("  Win Rate: ${"%.0f".format(championMetrics.winRate)}%")
        println("  Sharpe:   ${"%.2f".format(championMetrics.sharpeRatio)}")
        println("=".repeat(90))
    }

    private data class AggressiveConfig(
        val volumeThreshold: Double,
        val confirmationCandles: Int,
        val adxTrendThreshold: Double,
        val stopLossAtr: Double,
        val takeProfitAtr: Double,
        val positionPercent: Double
    )

    private data class Metrics(
        val trades: Int,
        val pnlPercent: Double,
        val winRate: Double,
        val sharpeRatio: Double,
        val maxDrawdown: Double
    )

    private suspend fun evaluateConfig(config: AggressiveConfig, candles: List<com.tradeflow.core.domain.model.Candle>): Metrics {
        val tradingConfig = TradingConfig(
            strategy = StrategyParameters(
                confirmationCandles = config.confirmationCandles,
                adxTrendThreshold = config.adxTrendThreshold,
                adxRangeThreshold = (config.adxTrendThreshold - 3.0).coerceAtLeast(5.0),
                stopLossAtrMultiplier = BigDecimal(config.stopLossAtr.toString()),
                takeProfitAtrMultiplier = BigDecimal(config.takeProfitAtr.toString()),
                trendPositionPercent = BigDecimal(config.positionPercent.toString()),
                gridPositionPercentPerLevel = BigDecimal((config.positionPercent * 1.5).toString()),
                leverage = BigDecimal("2.0")
            ),
            risk = RiskParameters(
                maxDrawdownPercent = 0.30  // Increased to 30% for aggressive testing
            ),
            technical = TechnicalParameters(
                minVolumeRatio = config.volumeThreshold,
                smaPeriod = 50  // Reduced from 200 for faster signals on 15m
            ),
            execution = ExecutionParameters(),
            profile = RiskProfile.BALANCED
        )

        val initialCapital = BigDecimal("500.00")
        val exchange = SimulatedExchange(
            initialUsd = initialCapital,
            tradingConfig = tradingConfig
        )

        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = tradingConfig
        )

        val orchestrator = ExecuteTradingCycleUseCase(
            exchangeRepository = exchange,
            makeDecisionUseCase = engine,
            config = tradingConfig,
            trailingStopManager = com.tradeflow.core.domain.risk.TrailingStopManager(tradingConfig)
        )

        // Need 50 candles for SMA50, then trade on remainder
        if (candles.size < 60) {
            // Not enough data
            return Metrics(0, 0.0, 0.0, 0.0, 0.0)
        }

        val primeHistory = candles.take(50)
        val simulationCandles = candles.drop(50)

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
        val pnlPercent = (totalPnL.divide(initialCapital, 6, RoundingMode.HALF_UP).toDouble() * 100)

        val winRate = if (tradeCount > 0) (winCount.toDouble() / tradeCount * 100) else 0.0

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
        val sharpeRatio = if (stdDev > 0.0) (avgReturn / stdDev) * sqrt(365.0 * 96.0) else 0.0  // 96 = 15m candles/day

        var maxDrawdown = 0.0
        var peak = initialCapital
        equityCurve.forEach { equity ->
            if (equity > peak) peak = equity
            val dd = ((peak - equity).divide(peak, 4, RoundingMode.HALF_UP).toDouble() * 100)
            if (dd > maxDrawdown) maxDrawdown = dd
        }

        return Metrics(
            trades = tradeCount,
            pnlPercent = pnlPercent,
            winRate = winRate,
            sharpeRatio = sharpeRatio,
            maxDrawdown = maxDrawdown
        )
    }

    private fun crossover(p1: AggressiveConfig, p2: AggressiveConfig, random: Random) = AggressiveConfig(
        volumeThreshold = if (random.nextBoolean()) p1.volumeThreshold else p2.volumeThreshold,
        confirmationCandles = if (random.nextBoolean()) p1.confirmationCandles else p2.confirmationCandles,
        adxTrendThreshold = if (random.nextBoolean()) p1.adxTrendThreshold else p2.adxTrendThreshold,
        stopLossAtr = if (random.nextBoolean()) p1.stopLossAtr else p2.stopLossAtr,
        takeProfitAtr = if (random.nextBoolean()) p1.takeProfitAtr else p2.takeProfitAtr,
        positionPercent = if (random.nextBoolean()) p1.positionPercent else p2.positionPercent
    )

    private fun mutate(config: AggressiveConfig, random: Random) = when (random.nextInt(6)) {
        0 -> config.copy(volumeThreshold = (config.volumeThreshold + random.nextDouble(-0.2, 0.2)).coerceIn(0.3, 2.0))
        1 -> config.copy(confirmationCandles = (config.confirmationCandles + random.nextInt(-1, 2)).coerceIn(1, 5))
        2 -> config.copy(adxTrendThreshold = (config.adxTrendThreshold + random.nextDouble(-3.0, 3.0)).coerceIn(5.0, 25.0))
        3 -> config.copy(stopLossAtr = (config.stopLossAtr + random.nextDouble(-1.0, 1.0)).coerceIn(2.0, 10.0))
        4 -> config.copy(takeProfitAtr = (config.takeProfitAtr + random.nextDouble(-3.0, 3.0)).coerceIn(4.0, 30.0))
        else -> config.copy(positionPercent = (config.positionPercent + random.nextDouble(-0.02, 0.02)).coerceIn(0.02, 0.15))
    }
}
