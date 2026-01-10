package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.indicator.TechnicalAnalysisService
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal
import javax.inject.Inject

class TradingDecisionEngine @Inject constructor(
    private val taService: TechnicalAnalysisService,
    private val config: StrategyConfig = StrategyConfig()
) : DecisionEngine {

    // Persistent state for hysteresis
    private var lastMode: Mode = Mode.DEFENSE
    private var confirmationCount = 0
    private var candidateMode: Mode? = null

    private enum class Mode { DEFENSE, TREND, RANGE }

    override fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        if (candles.size < config.smaPeriod) {
            return Decision.Wait("Not enough candles: ${candles.size}/${config.smaPeriod}")
        }

        val indicators = taService.calculateAll(candles, config.smaPeriod, config.adxPeriod, config.atrPeriod)
        
        // Log indicators for visibility
        // println("DEBUG: Price: $currentPrice | SMA200: ${indicators.sma200} | ADX: ${indicators.adx} | ATR: ${indicators.atr}")

        // 1. Immediate Defense check (No hysteresis for capital protection)
        if (currentPrice < indicators.sma200) {
            resetHysteresis(Mode.DEFENSE)
            return Decision.Defense("Price below SMA200", currentPrice, indicators.sma200)
        }

        // 2. Determine desired mode based on Trend Strength (ADX)
        val desiredMode = when {
            indicators.adx >= config.adxTrendThreshold -> Mode.TREND
            indicators.adx <= config.adxRangeThreshold -> Mode.RANGE
            else -> lastMode // Stay in current mode if in the "neutral zone"
        }

        // 3. Apply Hysteresis (3-candle confirmation)
        if (desiredMode == lastMode) {
            candidateMode = null
            confirmationCount = 0
            return createDecision(lastMode, currentPrice, indicators)
        }

        if (desiredMode != candidateMode) {
            candidateMode = desiredMode
            confirmationCount = 1
        } else {
            confirmationCount++
        }

        if (confirmationCount >= 3) {
            lastMode = desiredMode
            candidateMode = null
            confirmationCount = 0
            return createDecision(lastMode, currentPrice, indicators)
        }

        return Decision.Wait("Confirming mode switch to $desiredMode ($confirmationCount/3)")
    }

    private fun createDecision(mode: Mode, currentPrice: BigDecimal, indicators: TechnicalAnalysisService.Indicators): Decision {
        return when (mode) {
            Mode.DEFENSE -> Decision.Defense("Above SMA but ADX neutral", currentPrice, indicators.sma200)
            Mode.TREND -> Decision.Trend(
                direction = OrderSide.BUY,
                entryPrice = currentPrice,
                stopLoss = currentPrice - (indicators.atr * config.stopLossAtrMultiplier),
                takeProfit = currentPrice + (indicators.atr * config.takeProfitAtrMultiplier),
                positionSizePercent = config.trendPositionPercent,
                adx = indicators.adx,
                atr = indicators.atr
            )
            Mode.RANGE -> Decision.Range(
                gridSpacing = (indicators.atr * config.minGridSpacing).max(BigDecimal("0.01")),
                levels = 10,
                positionSizePercentPerLevel = config.gridPositionPercentPerLevel,
                adx = indicators.adx,
                atr = indicators.atr
            )
        }
    }

    private fun resetHysteresis(mode: Mode) {
        lastMode = mode
        candidateMode = null
        confirmationCount = 0
    }
}
