package com.tradeflow.backtesting.validation

import com.tradeflow.backtesting.config.ValidationConfig
import com.tradeflow.backtesting.engine.BacktestResult
import com.tradeflow.core.domain.StrategyConfig
import kotlin.math.sqrt

class ValidationReporter(
    private val validationConfig: ValidationConfig = ValidationConfig()
) {

    fun printCurrentParameters(strategyConfig: StrategyConfig) {
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

    fun printAggregatedResults(results: List<Pair<Pair<Long, Long>, BacktestResult>>) {
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
            profitableRuns >= validationConfig.consistency.highThreshold && avgPnl > validationConfig.consistency.highPnLThreshold -> "✅ HIGHLY CONSISTENT - Strategy is robust"
            profitableRuns >= validationConfig.consistency.threshold && avgPnl > validationConfig.consistency.pnLThreshold -> "✅ CONSISTENT - Good edge across periods"
            profitableRuns >= validationConfig.consistency.moderateThreshold && avgPnl > validationConfig.consistency.moderatePnLThreshold -> "⚠️  MODERATELY CONSISTENT - Needs improvement"
            else -> "❌ INCONSISTENT - Strategy lacks robustness"
        }
        println()
        println("Assessment: $consistency")
        println()

        println("💡 RECOMMENDATIONS")
        println("─".repeat(90))
        when {
            overallWinRate >= validationConfig.edge.strongWinRate && avgPnl > validationConfig.edge.strongPnL && profitableRuns >= validationConfig.edge.strongProfitable -> {
                println("✅ Strategy shows strong edge across diverse market conditions")
                println("✅ Ready for paper trading with real-time data")
                println("✅ Monitor performance for 30 days before going live")
            }
            overallWinRate >= validationConfig.edge.promiseWinRate && avgPnl > validationConfig.edge.promisePnL && profitableRuns >= validationConfig.edge.promiseProfitable -> {
                println("⚠️  Strategy shows promise but needs refinement")
                println("⚠️  Consider running RunOptimization to optimize configuration")
                println("⚠️  Analyze losing periods to identify weaknesses")
            }
            totalTrades < validationConfig.significance.minTrades -> {
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

    fun printNextSteps(results: List<Pair<Pair<Long, Long>, BacktestResult>>) {
        println("💡 NEXT STEPS")
        println("─".repeat(90))
        val avgPnl = results.map { it.second.pnlPercent }.average()
        val overallWinRate = results.map { it.second.winRate }.average()

        when {
            avgPnl > validationConfig.significance.wellPerformingPnL && overallWinRate > validationConfig.significance.wellPerformingWinRate -> {
                println("✅ Parameters are performing well")
                println("   → Proceed to paper trading")
            }
            avgPnl > validationConfig.significance.marginalPnL && overallWinRate > validationConfig.significance.marginalWinRate -> {
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

    private fun printPerformanceMetrics(result: BacktestResult) {
        println("PnL:      ${"%.2f".format(result.pnlPercent)}%")
        println("Win Rate: ${"%.1f".format(result.winRate)}%")
        println("Sharpe:   ${"%.2f".format(result.sharpeRatio)}")
        println("Trades:   ${result.trades.size}")
    }

    private fun calculateStdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
}
