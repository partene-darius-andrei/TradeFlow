package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.config.BacktestConfig
import com.tradeflow.backtesting.engine.BacktestEngine
import com.tradeflow.backtesting.engine.BacktestResult
import com.tradeflow.core.domain.StrategyConfig
import com.tradeflow.core.domain.model.Candle

class ParameterFitnessEvaluator(
    private val candles1h: List<Candle>,
    private val candles15m: List<Candle>,
    private val config: BacktestConfig = BacktestConfig.default()
) {
    private val sharpeWeight: Double = config.sharpeWeight
    private val returnWeight: Double = config.returnWeight
    private val drawdownPenalty: Double = config.drawdownPenalty

    fun evaluate(params: TradingParameters): Double {
        params.applyTo(StrategyConfig)
        val engine = BacktestEngine(config)
        val result = engine.execute(candles1h, candles15m)
        return calculateFitness(result)
    }

    private fun calculateFitness(result: BacktestResult): Double {
        val normalizedSharpe = (result.sharpeRatio / config.sharpeNormalizationFactor).coerceIn(-1.0, 1.0)
        val normalizedReturn = (result.pnlPercent / config.returnNormalizationFactor).coerceIn(-1.0, 1.0)
        val normalizedDrawdown = 1.0 - result.maxDrawdown

        val fitness = sharpeWeight * normalizedSharpe +
            returnWeight * normalizedReturn +
            drawdownPenalty * normalizedDrawdown

        return fitness
    }
}
