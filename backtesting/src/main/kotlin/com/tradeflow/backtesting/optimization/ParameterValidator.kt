package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.data.BinanceDataLoader
import com.tradeflow.backtesting.data.RandomPeriodGenerator
import com.tradeflow.backtesting.data.RandomPeriod
import com.tradeflow.backtesting.engine.BacktestEngine
import com.tradeflow.backtesting.engine.BacktestResult
import com.tradeflow.core.domain.TradingConfig
import com.tradeflow.core.domain.model.Candle
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import kotlin.math.sqrt

class ParameterValidator {

    suspend fun run(args: Array<String>) {
        println("\n🧪 RUN VALIDATION - VALIDATE CURRENT PARAMETERS")
        println("=".repeat(90))
        println("Purpose: Validate current parameters on random historical data (no optimization)")
        println("=".repeat(90))

        val numPeriods = args.getOrNull(0)?.toIntOrNull() ?: 10
        val minDays = args.getOrNull(1)?.toIntOrNull() ?: 60
        val maxDays = args.getOrNull(2)?.toIntOrNull() ?: 180
        val seed = args.getOrNull(3)?.toLongOrNull()

        println("\nConfiguration:")
        println("  Random Periods:    $numPeriods")
        println("  Period Range:      $minDays - $maxDays days")
        println("  Random Seed:       ${seed ?: "Random"}")
        println()

        printCurrentParameters()
        val periods = generatePeriods(numPeriods, minDays, maxDays, seed)
        val results = runBacktests(periods, numPeriods)

        if (results.isEmpty()) {
            println("\n❌ No results collected. Aborting.")
            return
        }

        printAggregatedResults(results)
        printNextSteps(results)
    }

    private fun printCurrentParameters() {
        println("📋 CURRENT TRADING PARAMETERS")
        println("─".repeat(90))
        println("  ADX Trend Threshold:     ${TradingConfig.Strategy.getAdxTrendThreshold()}")
        println("  ADX Range Threshold:     ${TradingConfig.Strategy.getAdxRangeThreshold()}")
        println("  Confirmation Candles:    ${TradingConfig.Strategy.getConfirmationCandles()}")
        println("  Trend Position %:        ${(TradingConfig.Strategy.getTrendPositionPercent().toDouble() * 100).let { "%.2f".format(it) }}%")
        println("  Stop Loss ATR Mult:      ${TradingConfig.Strategy.getStopLossAtrMultiplier()}")
        println("  Take Profit ATR Mult:    ${TradingConfig.Strategy.getTakeProfitAtrMultiplier()}")
        println()
    }

    private fun generatePeriods(numPeriods: Int, minDays: Int, maxDays: Int, seed: Long?): List<RandomPeriod> {
        println("📅 GENERATING RANDOM HISTORICAL PERIODS")
        println("─".repeat(90))
        val periods = RandomPeriodGenerator.generateRandomPeriods(
            count = numPeriods,
            minDurationDays = minDays,
            maxDurationDays = maxDays,
            seed = seed
        )

        periods.forEachIndexed { index, period ->
            println("  ${index + 1}. ${period.description}")
        }
        println()
        return periods
    }

    private suspend fun runBacktests(periods: List<RandomPeriod>, numPeriods: Int): List<Pair<RandomPeriod, BacktestResult>> {
        println("📡 FETCHING DATA & RUNNING BACKTESTS")
        println("─".repeat(90))

        val results = mutableListOf<Pair<RandomPeriod, BacktestResult>>()
        val engine = BacktestEngine(initialCapital = BigDecimal("500.00"))

        periods.forEachIndexed { index, period ->
            try {
                print("  [${index + 1}/$numPeriods] ${period.description}... ")

                val (candles1h, candles15m) = fetchPeriodData(period)
                val result = engine.execute(candles1h, candles15m, primeSize = 300, verbose = false)

                println("✓ (PnL: ${if (result.pnlPercent >= 0) "+" else ""}${"%.2f".format(result.pnlPercent)}%, " +
                    "WR: ${"%.1f".format(result.winRate)}%, Trades: ${result.trades.size})")

                results.add(period to result)

            } catch (e: Exception) {
                println("✗ Error: ${e.message}")
            }
        }

        return results
    }

    private suspend fun fetchPeriodData(period: RandomPeriod): Pair<List<Candle>, List<Candle>> = coroutineScope {
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

    private fun calculateStdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    private fun printAggregatedResults(results: List<Pair<RandomPeriod, BacktestResult>>) {
        if (results.isEmpty()) {
            println("⚠️  No results to aggregate")
            return
        }

        println()
        println("█".repeat(90))
        println("🎯 AGGREGATED RESULTS ACROSS ALL PERIODS")
        println("█".repeat(90))
        println()

        val allResults = results.map { it.second }
        val profitableRuns = allResults.count { it.pnlPercent > 0.0 }
        val breakEvenRuns = allResults.count { it.pnlPercent == 0.0 }
        val losingRuns = allResults.count { it.pnlPercent < 0.0 }

        val avgPnl = allResults.map { it.pnlPercent }.average()
        val avgWinRate = allResults.map { it.winRate }.average()
        val avgTrades = allResults.map { it.trades.size }.average()
        val avgSharpe = allResults.map { it.sharpeRatio }.average()
        val avgMaxDD = allResults.map { it.maxDrawdown }.average()

        val bestRun = results.maxByOrNull { it.second.pnlPercent }
        val worstRun = results.minByOrNull { it.second.pnlPercent }

        val totalTrades = allResults.sumOf { it.trades.size }
        val totalWins = allResults.sumOf { it.winningTrades.size }
        val totalLosses = allResults.sumOf { it.losingTrades.size }
        val overallWinRate = if (totalTrades > 0) (totalWins.toDouble() / totalTrades * 100) else 0.0

        println("📈 PERFORMANCE SUMMARY")
        println("─".repeat(90))
        println("Total Runs:           ${results.size}")
        println("Profitable:           $profitableRuns (${"%.0f".format(profitableRuns.toDouble() / results.size * 100)}%)")
        println("Break-even:           $breakEvenRuns (${"%.0f".format(breakEvenRuns.toDouble() / results.size * 100)}%)")
        println("Losing:               $losingRuns (${"%.0f".format(losingRuns.toDouble() / results.size * 100)}%)")
        println()
        println("Avg PnL:              ${"%.2f".format(avgPnl)}%")
        println("Avg Win Rate:         ${"%.1f".format(avgWinRate)}%")
        println("Avg Trades/Run:       ${"%.0f".format(avgTrades)}")
        println("Avg Sharpe Ratio:     ${"%.2f".format(avgSharpe)}")
        println("Avg Max Drawdown:     ${"%.2f".format(avgMaxDD)}%")
        println()
        println("Total Trades:         $totalTrades")
        println("Total Wins:           $totalWins")
        println("Total Losses:         $totalLosses")
        println("Overall Win Rate:     ${"%.1f".format(overallWinRate)}%")
        println()

        println("🏆 BEST PERFORMANCE")
        println("─".repeat(90))
        bestRun?.let { (period, result) ->
            println("Period:   ${period.description}")
            println("PnL:      ${"%.2f".format(result.pnlPercent)}%")
            println("Win Rate: ${"%.1f".format(result.winRate)}%")
            println("Sharpe:   ${"%.2f".format(result.sharpeRatio)}")
            println("Trades:   ${result.trades.size}")
        }
        println()

        println("📉 WORST PERFORMANCE")
        println("─".repeat(90))
        worstRun?.let { (period, result) ->
            println("Period:   ${period.description}")
            println("PnL:      ${"%.2f".format(result.pnlPercent)}%")
            println("Win Rate: ${"%.1f".format(result.winRate)}%")
            println("Sharpe:   ${"%.2f".format(result.sharpeRatio)}")
            println("Trades:   ${result.trades.size}")
        }
        println()

        println("🎲 CONSISTENCY ANALYSIS")
        println("─".repeat(90))
        val pnlStdDev = calculateStdDev(allResults.map { it.pnlPercent })
        val winRateStdDev = calculateStdDev(allResults.map { it.winRate })
        println("PnL Std Dev:          ${"%.2f".format(pnlStdDev)}%")
        println("Win Rate Std Dev:     ${"%.2f".format(winRateStdDev)}%")

        val consistency = when {
            profitableRuns >= 8 && avgPnl > 3.0 -> "✅ HIGHLY CONSISTENT - Strategy is robust"
            profitableRuns >= 7 && avgPnl > 1.0 -> "✅ CONSISTENT - Good edge across periods"
            profitableRuns >= 5 && avgPnl > 0.0 -> "⚠️  MODERATELY CONSISTENT - Needs improvement"
            else -> "❌ INCONSISTENT - Strategy lacks robustness"
        }
        println()
        println("Assessment: $consistency")
        println()

        println("💡 RECOMMENDATIONS")
        println("─".repeat(90))
        when {
            overallWinRate >= 55.0 && avgPnl > 3.0 && profitableRuns >= 8 -> {
                println("✅ Strategy shows strong edge across diverse market conditions")
                println("✅ Ready for paper trading with real-time data")
                println("✅ Monitor performance for 30 days before going live")
            }
            overallWinRate >= 50.0 && avgPnl > 0.0 && profitableRuns >= 6 -> {
                println("⚠️  Strategy shows promise but needs refinement")
                println("⚠️  Consider running RunOptimization to optimize configuration")
                println("⚠️  Analyze losing periods to identify weaknesses")
            }
            totalTrades < 200 -> {
                println("⚠️  Insufficient trade sample size ($totalTrades total)")
                println("⚠️  Extend test periods or adjust filters to generate more trades")
                println("⚠️  Need at least 200-300 trades for statistical significance")
            }
            else -> {
                println("❌ Strategy needs significant improvement")
                println("❌ Run RunOptimization to find better configuration")
                println("❌ Review core logic, indicators, and risk parameters")
            }
        }

        println("█".repeat(90))
        println()
    }

    private fun printNextSteps(results: List<Pair<RandomPeriod, BacktestResult>>) {
        println("💡 NEXT STEPS")
        println("─".repeat(90))
        val avgPnl = results.map { it.second.pnlPercent }.average()
        val overallWinRate = results.map { it.second.winRate }.average()

        when {
            avgPnl > 3.0 && overallWinRate > 55.0 -> {
                println("✅ Parameters are performing well")
                println("   → Proceed to paper trading")
            }
            avgPnl > 0.0 && overallWinRate > 50.0 -> {
                println("⚠️  Parameters are marginal")
                println("   → Consider running RunOptimization to optimize")
            }
            else -> {
                println("❌ Parameters are underperforming")
                println("   → Run RunOptimization to find better configuration")
                println("   → Usage: ./gradlew :backtesting:run -PmainClass=com.tradeflow.backtesting.RunOptimizationKt")
            }
        }
        println("=".repeat(90))
        println()
    }
}
