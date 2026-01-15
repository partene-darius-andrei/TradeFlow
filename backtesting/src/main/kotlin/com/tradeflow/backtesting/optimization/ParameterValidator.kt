package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.config.BacktestConfig
import com.tradeflow.backtesting.config.ValidationConfig
import com.tradeflow.backtesting.data.BinanceDataLoader
import com.tradeflow.backtesting.data.RandomPeriodGenerator
import com.tradeflow.backtesting.engine.BacktestEngine
import com.tradeflow.backtesting.engine.BacktestResult
import com.tradeflow.core.domain.StrategyConfig
import kotlin.math.sqrt

class ParameterValidator(
    private val backtestConfig: BacktestConfig = BacktestConfig.default(),
    private val validationConfig: ValidationConfig = ValidationConfig.default(),
    private val strategyConfig: StrategyConfig = StrategyConfig.default()
) {

    suspend operator fun invoke() {

        println("\n🧪 RUN VALIDATION - VALIDATE CURRENT PARAMETERS")
        println("=".repeat(90))
        println("Purpose: Validate current parameters on random historical data (no optimization)")
        println("=".repeat(90))

        val numPeriods = backtestConfig.loops
        val minDays = validationConfig.minPeriodDays
        val maxDays = validationConfig.maxPeriodDays

        println("\nConfiguration:")
        println("  Random Periods:    $numPeriods")
        println("  Period Range:      $minDays - $maxDays days")
        println()

        printCurrentParameters()
        val periods = generatePeriods(numPeriods, minDays, maxDays)
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
        println("  ADX Trend Threshold:     ${strategyConfig.adxTrendThreshold}")
        println("  ADX Range Threshold:     ${strategyConfig.adxRangeThreshold}")
        println("  Confirmation Candles:    ${strategyConfig.confirmationCandles}")
        println("  Trend Position %:        ${(strategyConfig.trendPositionPercent.toDouble() * 100).let { "%.2f".format(it) }}%")
        println("  Stop Loss ATR Mult:      ${strategyConfig.stopLossAtrMultiplier}")
        println("  Take Profit ATR Mult:    ${strategyConfig.takeProfitAtrMultiplier}")
        println()
    }

    private fun generatePeriods(numPeriods: Int, minDays: Int, maxDays: Int): List<Pair<Long, Long>> {
        println("📅 GENERATING RANDOM HISTORICAL PERIODS")
        println("─".repeat(90))
        val periods = RandomPeriodGenerator.generateRandomPeriods(
            config = validationConfig,
            count = numPeriods,
            minDurationDays = minDays,
            maxDurationDays = maxDays
        )

        periods.forEachIndexed { index, (start, end) ->
            val startDate = java.time.Instant.ofEpochMilli(start).toString().substring(0, 10)
            val endDate = java.time.Instant.ofEpochMilli(end).toString().substring(0, 10)
            val days = ((end - start) / (1000 * 60 * 60 * 24)).toInt()
            println("  ${index + 1}. $startDate to $endDate ($days days)")
        }
        println()
        return periods
    }

    private suspend fun runBacktests(periods: List<Pair<Long, Long>>, numPeriods: Int): List<Pair<Pair<Long, Long>, BacktestResult>> {
        println("📡 FETCHING DATA & RUNNING BACKTESTS")
        println("─".repeat(90))

        val results = mutableListOf<Pair<Pair<Long, Long>, BacktestResult>>()
        val engine = BacktestEngine(backtestConfig, strategyConfig)

        periods.forEachIndexed { index, period ->
            try {
                val startDate = java.time.Instant.ofEpochMilli(period.first).toString().substring(0, 10)
                val endDate = java.time.Instant.ofEpochMilli(period.second).toString().substring(0, 10)
                val days = ((period.second - period.first) / (1000 * 60 * 60 * 24)).toInt()
                print("  [${index + 1}/$numPeriods] $startDate to $endDate ($days days)... ")

                val data = BinanceDataLoader.fetchPeriodData(period)
                val result = engine.execute(data.candles1h, data.candles30m, data.candles15m, data.candles5m, data.candles1m)

                println("✓ (PnL: ${if (result.pnlPercent >= 0) "+" else ""}${"%.2f".format(result.pnlPercent)}%, " +
                    "WR: ${"%.1f".format(result.winRate)}%, Trades: ${result.trades.size})")

                results.add(period to result)

            } catch (e: Exception) {
                println("✗ Error: ${e.message}")
            }
        }

        return results
    }

    private fun calculateStdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    private fun printPerformanceMetrics(result: BacktestResult) {
        println("PnL:      ${"%.2f".format(result.pnlPercent)}%")
        println("Win Rate: ${"%.1f".format(result.winRate)}%")
        println("Sharpe:   ${"%.2f".format(result.sharpeRatio)}")
        println("Trades:   ${result.trades.size}")
    }

    private fun printAggregatedResults(results: List<Pair<Pair<Long, Long>, BacktestResult>>) {
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
        bestRun?.let { (_, result) ->
            printPerformanceMetrics(result)
        }
        println()

        println("📉 WORST PERFORMANCE")
        println("─".repeat(90))
        worstRun?.let { (_, result) ->
            printPerformanceMetrics(result)
        }
        println()

        println("🎲 CONSISTENCY ANALYSIS")
        println("─".repeat(90))
        val pnlStdDev = calculateStdDev(allResults.map { it.pnlPercent })
        val winRateStdDev = calculateStdDev(allResults.map { it.winRate })
        println("PnL Std Dev:          ${"%.2f".format(pnlStdDev)}%")
        println("Win Rate Std Dev:     ${"%.2f".format(winRateStdDev)}%")

        val consistency = when {
            profitableRuns >= validationConfig.highConsistencyThreshold && avgPnl > validationConfig.highConsistencyPnLThreshold -> "✅ HIGHLY CONSISTENT - Strategy is robust"
            profitableRuns >= validationConfig.consistencyThreshold && avgPnl > validationConfig.consistencyPnLThreshold -> "✅ CONSISTENT - Good edge across periods"
            profitableRuns >= validationConfig.moderateConsistencyThreshold && avgPnl > validationConfig.moderateConsistencyPnLThreshold -> "⚠️  MODERATELY CONSISTENT - Needs improvement"
            else -> "❌ INCONSISTENT - Strategy lacks robustness"
        }
        println()
        println("Assessment: $consistency")
        println()

        println("💡 RECOMMENDATIONS")
        println("─".repeat(90))
        when {
            overallWinRate >= validationConfig.strongEdgeWinRateThreshold && avgPnl > validationConfig.strongEdgePnLThreshold && profitableRuns >= validationConfig.strongEdgeProfitableThreshold -> {
                println("✅ Strategy shows strong edge across diverse market conditions")
                println("✅ Ready for paper trading with real-time data")
                println("✅ Monitor performance for 30 days before going live")
            }
            overallWinRate >= validationConfig.promiseWinRateThreshold && avgPnl > validationConfig.promisePnLThreshold && profitableRuns >= validationConfig.promiseProfitableThreshold -> {
                println("⚠️  Strategy shows promise but needs refinement")
                println("⚠️  Consider running RunOptimization to optimize configuration")
                println("⚠️  Analyze losing periods to identify weaknesses")
            }
            totalTrades < validationConfig.minTradesForSignificance -> {
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

    private fun printNextSteps(results: List<Pair<Pair<Long, Long>, BacktestResult>>) {
        println("💡 NEXT STEPS")
        println("─".repeat(90))
        val avgPnl = results.map { it.second.pnlPercent }.average()
        val overallWinRate = results.map { it.second.winRate }.average()

        when {
            avgPnl > validationConfig.wellPerformingPnLThreshold && overallWinRate > validationConfig.wellPerformingWinRateThreshold -> {
                println("✅ Parameters are performing well")
                println("   → Proceed to paper trading")
            }
            avgPnl > validationConfig.marginalPnLThreshold && overallWinRate > validationConfig.marginalWinRateThreshold -> {
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
