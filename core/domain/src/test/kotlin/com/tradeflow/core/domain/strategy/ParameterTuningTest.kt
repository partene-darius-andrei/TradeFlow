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

data class TuningResult(
    val volumeThreshold: Double,
    val confirmationCandles: Int,
    val adxTrendThreshold: Double,
    val finalEquity: BigDecimal,
    val totalPnL: BigDecimal,
    val pnlPercent: Double,
    val trades: Int,
    val winRate: Double,
    val sharpeRatio: Double,
    val maxDrawdown: Double,
    val score: Double
) {
    fun printSummary() {
        val pnlSign = if (totalPnL >= BigDecimal.ZERO) "+" else ""
        println("Vol: ${volumeThreshold}x | Confirm: $confirmationCandles | ADX: $adxTrendThreshold | " +
                "PnL: $pnlSign${pnlPercent.toBigDecimal().setScale(1, RoundingMode.HALF_UP)}% | " +
                "Trades: $trades | Win: ${"%.0f".format(winRate)}% | " +
                "Sharpe: ${"%.2f".format(sharpeRatio)} | " +
                "DD: ${"%.1f".format(maxDrawdown)}% | " +
                "Score: ${"%.3f".format(score)}")
    }
}

class ParameterTuningTest {

    @Test
    fun `comprehensive parameter sweep for profitability`() = runBlocking {
        println("\n🎯 COMPREHENSIVE PARAMETER TUNING")
        println("=".repeat(90))
        println("Testing combinations to find profitable configuration")
        println("=".repeat(90))
        println()

        val volumeThresholds = listOf(0.8, 1.0, 1.2, 1.5)
        val confirmationCandlesOptions = listOf(2, 3, 4)
        val adxTrendThresholds = listOf(12.0, 15.69, 18.0, 20.0)

        val results = mutableListOf<TuningResult>()
        var testCount = 0
        val totalTests = volumeThresholds.size * confirmationCandlesOptions.size * adxTrendThresholds.size

        println("Testing ${totalTests} parameter combinations...\n")

        volumeThresholds.forEach { volumeThreshold ->
            confirmationCandlesOptions.forEach { confirmationCandles ->
                adxTrendThresholds.forEach { adxTrendThreshold ->
                    testCount++

                    if (testCount % 5 == 1) {
                        println("Progress: $testCount/$totalTests...")
                    }

                    val result = testConfiguration(
                        volumeThreshold = volumeThreshold,
                        confirmationCandles = confirmationCandles,
                        adxTrendThreshold = adxTrendThreshold
                    )
                    results.add(result)
                }
            }
        }

        println("\n📊 TOP 10 CONFIGURATIONS (by Score)")
        println("=".repeat(90))
        println("Score = (Sharpe × 0.4) + (Return% × 0.3) + (WinRate × 0.2) - (Drawdown% × 0.1)")
        println("-".repeat(90))

        results.sortedByDescending { it.score }
            .take(10)
            .forEachIndexed { index, result ->
                print("#${index + 1}  ")
                result.printSummary()
            }

        println("\n🏆 BEST BY PROFITABILITY (Return %)")
        println("=".repeat(90))
        val bestProfit = results.maxByOrNull { it.pnlPercent }
        bestProfit?.printSummary()

        println("\n🛡️  BEST BY SHARPE RATIO")
        println("=".repeat(90))
        val bestSharpe = results.filter { it.trades >= 5 }.maxByOrNull { it.sharpeRatio }
        bestSharpe?.printSummary()

        println("\n📈 BEST BY TRADE FREQUENCY")
        println("=".repeat(90))
        val bestFrequency = results.maxByOrNull { it.trades }
        bestFrequency?.printSummary()

        println("\n💎 RECOMMENDED CONFIGURATION")
        println("=".repeat(90))
        // Best overall score with minimum 5 trades
        val recommended = results.filter { it.trades >= 5 }.maxByOrNull { it.score }
        if (recommended != null) {
            println("Volume Threshold:     ${recommended.volumeThreshold}x")
            println("Confirmation Candles: ${recommended.confirmationCandles}")
            println("ADX Trend Threshold:  ${recommended.adxTrendThreshold}")
            println()
            println("Expected Performance:")
            println("  Return:      ${if (recommended.pnlPercent >= 0) "+" else ""}${"%.1f".format(recommended.pnlPercent)}%")
            println("  Trades:      ${recommended.trades}")
            println("  Win Rate:    ${"%.0f".format(recommended.winRate)}%")
            println("  Sharpe:      ${"%.2f".format(recommended.sharpeRatio)}")
            println("  Max DD:      ${"%.1f".format(recommended.maxDrawdown)}%")
        } else {
            println("No configuration with >= 5 trades found. All tested configs are too conservative.")
        }
        println("=".repeat(90))
    }

    private suspend fun testConfiguration(
        volumeThreshold: Double,
        confirmationCandles: Int,
        adxTrendThreshold: Double
    ): TuningResult {
        val customConfig = TradingConfig(
            strategy = StrategyParameters(
                confirmationCandles = confirmationCandles,
                adxTrendThreshold = adxTrendThreshold,
                adxRangeThreshold = adxTrendThreshold - 3.0,  // Keep 3.0 gap
                stopLossAtrMultiplier = BigDecimal("8.30"),
                takeProfitAtrMultiplier = BigDecimal("22.53"),
                trendPositionPercent = BigDecimal("0.0523"),
                gridPositionPercentPerLevel = BigDecimal("0.0710"),
                leverage = BigDecimal("2.0")
            ),
            risk = RiskParameters(),
            technical = TechnicalParameters(
                minVolumeRatio = volumeThreshold
            ),
            execution = ExecutionParameters(),
            profile = RiskProfile.BALANCED
        )

        val initialCapital = BigDecimal("500.00")
        val exchange = SimulatedExchange(
            initialUsd = initialCapital,
            tradingConfig = customConfig
        )

        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = customConfig
        )

        val orchestrator = ExecuteTradingCycleUseCase(
            exchangeRepository = exchange,
            makeDecisionUseCase = engine,
            config = customConfig,
            trailingStopManager = com.tradeflow.core.domain.risk.TrailingStopManager(customConfig)
        )

        // Load 4h candles (66 days of trading)
        val allCandles = BinanceDataLoader.fetchHistoricalCandles(interval = "4h", limit = 400)
        val primeHistory = allCandles.take(200)
        val simulationDays = allCandles.drop(200)

        exchange.setHistory(primeHistory)

        var highWaterMark = initialCapital
        val equityCurve = mutableListOf<BigDecimal>()
        var tradeCount = 0
        var winCount = 0
        var previousPosition: com.tradeflow.core.domain.model.PerpetualPosition? = null

        simulationDays.forEach { candle ->
            exchange.advanceTime(candle)

            val currentEquity = exchange.getTotalEquity()
            equityCurve.add(currentEquity)

            val cycleResult = orchestrator.runCycle("BTC-USD", highWaterMark)
            highWaterMark = cycleResult.updatedHighWaterMark

            // Track trades
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

        // Calculate Sharpe ratio
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

        // Calculate max drawdown
        var maxDrawdown = 0.0
        var peak = initialCapital
        equityCurve.forEach { equity ->
            if (equity > peak) peak = equity
            val dd = if (peak > BigDecimal.ZERO) {
                ((peak - equity).divide(peak, 4, RoundingMode.HALF_UP).toDouble() * 100)
            } else 0.0
            if (dd > maxDrawdown) maxDrawdown = dd
        }

        // Calculate composite score
        // Prioritize: Sharpe (40%), Return (30%), Win Rate (20%), penalize Drawdown (10%)
        val normalizedSharpe = (sharpeRatio / 3.0).coerceIn(-1.0, 2.0)
        val normalizedReturn = (pnlPercent / 50.0).coerceIn(-1.0, 2.0)
        val normalizedWinRate = (winRate / 100.0)
        val drawdownPenalty = (maxDrawdown / 100.0)

        val score = (normalizedSharpe * 0.4) + (normalizedReturn * 0.3) + (normalizedWinRate * 0.2) - (drawdownPenalty * 0.1)

        return TuningResult(
            volumeThreshold = volumeThreshold,
            confirmationCandles = confirmationCandles,
            adxTrendThreshold = adxTrendThreshold,
            finalEquity = finalEquity,
            totalPnL = totalPnL,
            pnlPercent = pnlPercent,
            trades = tradeCount,
            winRate = winRate,
            sharpeRatio = sharpeRatio,
            maxDrawdown = maxDrawdown,
            score = score
        )
    }

    @Test
    fun `quick validation - test one promising config`() = runBlocking {
        println("\n⚡ QUICK VALIDATION TEST")
        println("=".repeat(90))
        println("Testing single promising configuration")
        println("=".repeat(90))
        println()

        // Based on diagnostic findings, test relaxed volume filter
        val result = testConfiguration(
            volumeThreshold = 1.0,  // Relaxed from 1.2x
            confirmationCandles = 3,  // Reduced from 4
            adxTrendThreshold = 15.69  // Keep current
        )

        println("Configuration:")
        println("  Volume Threshold:     1.0x (was 1.2x)")
        println("  Confirmation Candles: 3 (was 4)")
        println("  ADX Trend Threshold:  15.69 (unchanged)")
        println()
        println("Results:")
        result.printSummary()
        println()

        if (result.pnlPercent > 0 && result.trades >= 5) {
            println("✅ PROFITABLE! This configuration shows promise.")
        } else if (result.trades < 5) {
            println("⚠️  Too few trades (${result.trades}) for statistical significance.")
        } else {
            println("❌ Unprofitable. Need to adjust parameters further.")
        }
        println("=".repeat(90))
    }
}
