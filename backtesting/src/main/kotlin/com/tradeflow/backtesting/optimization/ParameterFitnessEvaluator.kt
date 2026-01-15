package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.config.BacktestConfig
import com.tradeflow.backtesting.config.OptimizationConfig
import com.tradeflow.backtesting.engine.BacktestEngine
import com.tradeflow.backtesting.engine.BacktestResult
import com.tradeflow.core.domain.StrategyConfig
import com.tradeflow.core.domain.model.Candle

class ParameterFitnessEvaluator(
    private val candles: List<Candle>,
    private val backtestConfig: BacktestConfig = BacktestConfig(),
    private val optimizationConfig: OptimizationConfig = OptimizationConfig()
) {
    private val fitness = optimizationConfig.fitness

    fun evaluate(config: StrategyConfig): Double {
        val silentConfig = backtestConfig.copy(silent = true)
        val engine = BacktestEngine(silentConfig, config)
        val result = engine.execute(candles)
        return calculateFitness(result)
    }

    private fun calculateFitness(result: BacktestResult): Double {
        val normalizedSharpe = (result.sharpeRatio / fitness.sharpeNormalizationFactor).coerceIn(-1.0, 1.0)
        val normalizedReturn = (result.pnlPercent / fitness.returnNormalizationFactor).coerceIn(-1.0, 1.0)
        val normalizedDrawdown = 1.0 - result.maxDrawdown

        val compositeFitness = fitness.sharpeWeight * normalizedSharpe +
            fitness.returnWeight * normalizedReturn +
            fitness.drawdownPenalty * normalizedDrawdown

        return compositeFitness
    }
}
