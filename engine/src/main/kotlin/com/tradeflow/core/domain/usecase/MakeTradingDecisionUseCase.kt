package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.StrategyConfig
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import java.math.BigDecimal

class MakeTradingDecisionUseCase(
    private val config: StrategyConfig = StrategyConfig()
) {

    private val analyzeCandlesUseCase = AnalyzeCandlesUseCase(config)
    private val modeDecisionUseCase = ModeDecisionUseCase(config)
    private val tradingSignalUseCase = TradingSignalUseCase(config)

    operator fun invoke(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        if (candles.size < StrategyConfig.MIN_CANDLES_REQUIRED) {
            return Decision.Wait("Not enough candles: ${candles.size}/${StrategyConfig.MIN_CANDLES_REQUIRED}")
        }

        val indicators = analyzeCandlesUseCase(candles)

        // Skip if indicators contain NaN (insufficient data)
        if (indicators.adx.isNaN() || indicators.rsi.isNaN() || indicators.volumeRatio.isNaN()) {
            return Decision.Wait("Indicators contain NaN (insufficient data)")
        }

        // Determine mode with hysteresis
        val modeResult = modeDecisionUseCase(indicators)

        // If mode is not confirmed, wait
        if (!modeResult.isConfirmed) {
            return Decision.Wait(modeResult.waitReason ?: "Waiting for mode confirmation")
        }

        // Create signal based on confirmed mode
        return when (modeResult.mode) {
            ModeDecisionUseCase.Mode.TREND -> tradingSignalUseCase.createTrendSignal(currentPrice, indicators)
            ModeDecisionUseCase.Mode.RANGE -> tradingSignalUseCase.createRangeSignal(currentPrice, indicators)
        }
    }
}

