package com.tradeflow.backtesting.optimization

import com.tradeflow.backtesting.engine.BacktestEngine
import com.tradeflow.backtesting.engine.BacktestResult
import com.tradeflow.core.domain.TradingConfig
import com.tradeflow.core.domain.model.Candle
import java.math.BigDecimal

class ParameterFitnessEvaluator(
    private val candles1h: List<Candle>,
    private val candles15m: List<Candle>,
    private val sharpeWeight: Double = 0.4,
    private val returnWeight: Double = 0.4,
    private val drawdownPenalty: Double = 0.2
) {

    fun evaluate(params: TradingParameters): Double {
        val result = TradingConfig.withOverrides(
            adxTrendThreshold = params.adxTrendThreshold,
            adxRangeThreshold = params.adxRangeThreshold,
            confirmationCandles = params.confirmationCandles,
            trendPositionPercent = params.trendPositionPercent,
            stopLossAtrMultiplier = params.stopLossAtrMultiplier,
            takeProfitAtrMultiplier = params.takeProfitAtrMultiplier,
            leverage = params.leverage
        ) {
            val engine = BacktestEngine(initialCapital = BigDecimal("500.00"))
            engine.execute(candles1h, candles15m, verbose = false)
        }

        return calculateFitness(result)
    }

    private fun calculateFitness(result: BacktestResult): Double {
        val normalizedSharpe = (result.sharpeRatio / 3.0).coerceIn(-1.0, 1.0)
        val normalizedReturn = (result.pnlPercent / 50.0).coerceIn(-1.0, 1.0)
        val normalizedDrawdown = 1.0 - result.maxDrawdown

        val fitness = sharpeWeight * normalizedSharpe +
            returnWeight * normalizedReturn +
            drawdownPenalty * normalizedDrawdown

        return fitness
    }
}
