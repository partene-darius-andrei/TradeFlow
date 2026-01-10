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
    private var lastMode: Mode = Mode.RANGE
    private var confirmationCount = 0
    private var candidateMode: Mode? = null

    private enum class Mode { TREND, RANGE }

    override fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        if (candles.size < config.smaPeriod) {
            return Decision.Wait("Not enough candles: ${candles.size}/${config.smaPeriod}")
        }

        val indicators = taService.calculateAll(candles, config.smaPeriod, config.adxPeriod, config.atrPeriod)

        println("  [DECISION] Price: $currentPrice | SMA: ${indicators.sma200.setScale(0, java.math.RoundingMode.HALF_UP)} | ADX: ${indicators.adx.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} | ATR: ${indicators.atr.setScale(0, java.math.RoundingMode.HALF_UP)}")

        // 1. Determine desired mode based on Trend Strength (ADX)
        val desiredMode = when {
            indicators.adx >= config.adxTrendThreshold -> {
                println("  [DECISION] ADX ${indicators.adx} >= ${config.adxTrendThreshold} → Wants TREND")
                Mode.TREND
            }
            indicators.adx <= config.adxRangeThreshold -> {
                println("  [DECISION] ADX ${indicators.adx} <= ${config.adxRangeThreshold} → Wants RANGE")
                Mode.RANGE
            }
            else -> {
                println("  [DECISION] ADX ${indicators.adx} in neutral zone (${config.adxRangeThreshold}-${config.adxTrendThreshold}) → Stay in $lastMode")
                lastMode
            }
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
            Mode.TREND -> {
                val sl = currentPrice - (indicators.atr * config.stopLossAtrMultiplier)
                val tp = currentPrice + (indicators.atr * config.takeProfitAtrMultiplier)
                println("  [DECISION] → Final: TREND ${config.trendPositionPercent.multiply(BigDecimal("100"))}% | Entry: $currentPrice | SL: $sl | TP: $tp")
                Decision.Trend(
                    direction = OrderSide.BUY,
                    entryPrice = currentPrice,
                    stopLoss = currentPrice - (indicators.atr * config.stopLossAtrMultiplier),
                    takeProfit = currentPrice + (indicators.atr * config.takeProfitAtrMultiplier),
                    positionSizePercent = config.trendPositionPercent,
                    adx = indicators.adx,
                    atr = indicators.atr
                )
            }
            Mode.RANGE -> {
                val spacing = (indicators.atr * config.minGridSpacing).max(BigDecimal("0.01"))
                println("  [DECISION] → Final: RANGE 3 levels | Spacing: $$spacing | ${config.gridPositionPercentPerLevel.multiply(BigDecimal("100"))}% per level")
                Decision.Range(
                    gridSpacing = spacing,
                    levels = 3,
                    positionSizePercentPerLevel = config.gridPositionPercentPerLevel,
                    adx = indicators.adx,
                    atr = indicators.atr
                )
            }
        }
    }

    private fun resetHysteresis(mode: Mode) {
        lastMode = mode
        candidateMode = null
        confirmationCount = 0
    }
}
