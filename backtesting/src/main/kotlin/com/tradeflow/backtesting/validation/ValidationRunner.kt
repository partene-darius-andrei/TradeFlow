package com.tradeflow.backtesting.validation

import com.tradeflow.backtesting.config.BacktestConfig
import com.tradeflow.backtesting.config.ValidationConfig
import com.tradeflow.backtesting.data.BinanceDataLoader
import com.tradeflow.backtesting.data.RandomPeriodGenerator
import com.tradeflow.backtesting.engine.BacktestEngine
import com.tradeflow.backtesting.engine.BacktestResult
import com.tradeflow.backtesting.util.toDateString
import com.tradeflow.core.domain.StrategyConfig

class ValidationRunner(
    private val backtestConfig: BacktestConfig = BacktestConfig(),
    private val strategyConfig: StrategyConfig = StrategyConfig()
) {

    fun runBacktests(periods: List<Pair<Long, Long>>): List<Pair<Pair<Long, Long>, BacktestResult>> {
        val results = mutableListOf<Pair<Pair<Long, Long>, BacktestResult>>()
        val engine = BacktestEngine(backtestConfig, strategyConfig)
        val numPeriods = periods.size

        periods.forEachIndexed { index, period ->
            try {
                val days = ((period.second - period.first) / (1000 * 60 * 60 * 24)).toInt()
                print("  [${index + 1}/$numPeriods] ${period.first.toDateString()} to ${period.second.toDateString()} ($days days)... ")

                val candles = BinanceDataLoader.fetchPeriodData(period, backtestConfig.interval)
                val result = engine.execute(candles)

                println("✓ (PnL: ${if (result.pnlPercent >= 0) "+" else ""}${"%.2f".format(result.pnlPercent)}%, " +
                    "WR: ${"%.1f".format(result.winRate)}%, Trades: ${result.trades.size})")

                results.add(period to result)

            } catch (e: Exception) {
                println("✗ Error: ${e.message}")
            }
        }

        return results
    }

    fun generatePeriods(validationConfig: ValidationConfig, numPeriods: Int, minDays: Int, maxDays: Int): List<Pair<Long, Long>> {
        println("📅 GENERATING RANDOM HISTORICAL PERIODS")
        println("─".repeat(90))
        val periods = RandomPeriodGenerator.generateRandomPeriods(
            config = validationConfig,
            count = numPeriods,
            minDurationDays = minDays,
            maxDurationDays = maxDays
        )

        periods.forEachIndexed { index, (start, end) ->
            val days = ((end - start) / (1000 * 60 * 60 * 24)).toInt()
            println("  ${index + 1}. ${start.toDateString()} to ${end.toDateString()} ($days days)")
        }
        println()
        return periods
    }
}
