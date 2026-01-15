package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.config.BacktestConfig
import com.tradeflow.backtesting.config.ValidationConfig
import com.tradeflow.backtesting.validation.ValidationReporter
import com.tradeflow.backtesting.validation.ValidationRunner
import com.tradeflow.core.domain.StrategyConfig

class ParameterValidator(
    private val backtestConfig: BacktestConfig = BacktestConfig(),
    private val validationConfig: ValidationConfig = ValidationConfig(),
    private val strategyConfig: StrategyConfig = StrategyConfig()
) {
    private val runner = ValidationRunner(backtestConfig, strategyConfig)
    private val reporter = ValidationReporter(validationConfig)

    suspend operator fun invoke() {
        println("\n🧪 RUN VALIDATION - VALIDATE CURRENT PARAMETERS")
        println("=".repeat(90))
        println("Purpose: Validate current parameters on random historical data (no optimization)")
        println("=".repeat(90))

        val numPeriods = backtestConfig.loops
        val minDays = validationConfig.period.minPeriodDays
        val maxDays = validationConfig.period.maxPeriodDays

        println("\nConfiguration:")
        println("  Random Periods:    $numPeriods")
        println("  Period Range:      $minDays - $maxDays days")
        println()

        reporter.printCurrentParameters(strategyConfig)
        val periods = runner.generatePeriods(validationConfig, numPeriods, minDays, maxDays)

        println("📡 FETCHING DATA & RUNNING BACKTESTS")
        println("─".repeat(90))
        val results = runner.runBacktests(periods)

        if (results.isEmpty()) {
            println("\n❌ No results collected. Aborting.")
            return
        }

        reporter.printAggregatedResults(results)
        reporter.printNextSteps(results)
    }
}
