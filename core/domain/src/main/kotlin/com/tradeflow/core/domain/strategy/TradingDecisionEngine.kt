package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.DecisionMode
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.indicator.TechnicalAnalysisService
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal
import javax.inject.Inject

class TradingDecisionEngine @Inject constructor(
    private val taService: TechnicalAnalysisService,
    private val config: TradingConfig
) : DecisionEngine {

    // Persistent state for hysteresis
    private var lastMode: Mode = Mode.valueOf(config.strategy.initialMode.name)
    private var confirmationCount = 0
    private var candidateMode: Mode? = null

    private enum class Mode { TREND, RANGE }

    fun resetState() {
        lastMode = Mode.valueOf(config.strategy.initialMode.name)
        candidateMode = null
        confirmationCount = 0
    }

    override fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        if (candles.size < config.technical.minCandlesRequired) {
            return Decision.Wait("Not enough candles: ${candles.size}/${config.technical.minCandlesRequired}")
        }

        val indicators = taService.calculateAll(candles, config.technical.smaPeriod, config.technical.adxPeriod, config.technical.atrPeriod)

        println("  [DECISION] Price: $currentPrice | SMA: ${indicators.sma200.setScale(0, java.math.RoundingMode.HALF_UP)} | ADX: ${indicators.adx.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} | ATR: ${indicators.atr.setScale(0, java.math.RoundingMode.HALF_UP)}")

        // 1. Determine desired mode based on Trend Strength (ADX)
        val desiredMode = when {
            indicators.adx >= config.strategy.adxTrendThreshold -> {
                println("  [DECISION] ADX ${indicators.adx} >= ${config.strategy.adxTrendThreshold} → Wants TREND")
                Mode.TREND
            }
            indicators.adx <= config.strategy.adxRangeThreshold -> {
                println("  [DECISION] ADX ${indicators.adx} <= ${config.strategy.adxRangeThreshold} → Wants RANGE")
                Mode.RANGE
            }
            else -> {
                println("  [DECISION] ADX ${indicators.adx} in neutral zone (${config.strategy.adxRangeThreshold}-${config.strategy.adxTrendThreshold}) → Stay in $lastMode")
                lastMode
            }
        }

        // 3. Apply Hysteresis
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

        if (confirmationCount >= config.strategy.confirmationCandles) {
            lastMode = desiredMode
            candidateMode = null
            confirmationCount = 0
            return createDecision(lastMode, currentPrice, indicators)
        }

        return Decision.Wait("Confirming mode switch to $desiredMode ($confirmationCount/${config.strategy.confirmationCandles})")
    }

    private fun createDecision(mode: Mode, currentPrice: BigDecimal, indicators: TechnicalAnalysisService.Indicators): Decision {
        return when (mode) {
            Mode.TREND -> {
                val sl = currentPrice - (indicators.atr * config.strategy.stopLossAtrMultiplier)
                val tp = currentPrice + (indicators.atr * config.strategy.takeProfitAtrMultiplier)
                println("  [DECISION] → Final: TREND ${config.strategy.trendPositionPercent.multiply(BigDecimal("100"))}% | Entry: $currentPrice | SL: $sl | TP: $tp")
                Decision.Trend(
                    direction = OrderSide.BUY,
                    entryPrice = currentPrice,
                    stopLoss = currentPrice - (indicators.atr * config.strategy.stopLossAtrMultiplier),
                    takeProfit = currentPrice + (indicators.atr * config.strategy.takeProfitAtrMultiplier),
                    positionSizePercent = config.strategy.trendPositionPercent,
                    adx = indicators.adx,
                    atr = indicators.atr
                )
            }
            Mode.RANGE -> {
                val spacing = (indicators.atr * config.strategy.minGridSpacingAtrMultiplier).max(config.strategy.minGridSpacingFloor)
                println("  [DECISION] → Final: RANGE ${config.strategy.gridLevels} levels | Spacing: $$spacing | ${config.strategy.gridPositionPercentPerLevel.multiply(BigDecimal("100"))}% per level")
                Decision.Range(
                    gridSpacing = spacing,
                    levels = config.strategy.gridLevels,
                    positionSizePercentPerLevel = config.strategy.gridPositionPercentPerLevel,
                    adx = indicators.adx,
                    atr = indicators.atr
                )
            }
        }
    }
}

