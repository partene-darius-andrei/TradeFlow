package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.indicator.ADXCalculator
import com.tradeflow.core.domain.indicator.ATRCalculator
import com.tradeflow.core.domain.indicator.SMACalculator
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import java.math.BigDecimal
import javax.inject.Inject

class TradingDecisionEngine @Inject constructor(
    private val smaCalculator: SMACalculator,
    private val adxCalculator: ADXCalculator,
    private val atrCalculator: ATRCalculator,
    private val config: StrategyConfig = StrategyConfig()
) : DecisionEngine {

    private var currentMode: Mode = Mode.DEFENSE
    private var modeConfirmationCount: Int = 0
    private var candidateMode: Mode? = null

    private enum class Mode {
        DEFENSE, TREND, RANGE
    }

    override fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        require(candles.size >= config.smaPeriod) {
            "Need at least ${config.smaPeriod} candles, got ${candles.size}"
        }

        val sma200 = smaCalculator.calculate(candles, config.smaPeriod)
        val adx = adxCalculator.calculate(candles, config.adxPeriod)
        val atr = atrCalculator.calculate(candles, config.atrPeriod)

        if (currentPrice < sma200) {
            resetHysteresis()
            currentMode = Mode.DEFENSE
            return Decision.Defense(
                reason = "Price below SMA200",
                sma200 = sma200,
                currentPrice = currentPrice
            )
        }

        val desiredMode = when {
            adx >= config.adxTrendThreshold -> Mode.TREND
            adx <= config.adxRangeThreshold -> Mode.RANGE
            else -> currentMode
        }

        if (desiredMode == currentMode) {
            resetHysteresis()
            return createDecisionForMode(currentMode, currentPrice, sma200, adx, atr)
        }

        if (candidateMode == null || candidateMode != desiredMode) {
            candidateMode = desiredMode
            modeConfirmationCount = 1
        } else {
            modeConfirmationCount++
        }

        if (modeConfirmationCount >= 3) {
            currentMode = candidateMode!!
            resetHysteresis()
            return createDecisionForMode(currentMode, currentPrice, sma200, adx, atr)
        }

        return Decision.Wait(
            reason = "Confirming mode switch to ${candidateMode?.name} ($modeConfirmationCount/3)",
            currentMode = currentMode.name,
            candidateMode = candidateMode?.name ?: "NONE",
            confirmationCount = modeConfirmationCount
        )
    }

    private fun createDecisionForMode(
        mode: Mode,
        currentPrice: BigDecimal,
        sma200: BigDecimal,
        adx: Double,
        atr: BigDecimal
    ): Decision {
        return when (mode) {
            Mode.DEFENSE -> Decision.Defense(
                reason = "Defensive mode confirmed",
                sma200 = sma200,
                currentPrice = currentPrice
            )
            Mode.TREND -> {
                val stopLoss = currentPrice - (atr * config.stopLossAtrMultiplier)
                val takeProfit = currentPrice + (atr * config.takeProfitAtrMultiplier)
                Decision.Trend(
                    entryPrice = currentPrice,
                    stopLoss = stopLoss,
                    takeProfit = takeProfit,
                    positionSizePercent = config.trendPositionPercent,
                    adx = adx,
                    atr = atr
                )
            }
            Mode.RANGE -> {
                val gridSpacing = (atr * config.minGridSpacing).max(BigDecimal("0.01"))
                Decision.Range(
                    centerPrice = currentPrice,
                    gridSpacing = gridSpacing,
                    positionSizePercentPerLevel = config.gridPositionPercentPerLevel,
                    levels = 5,
                    adx = adx,
                    atr = atr
                )
            }
        }
    }

    private fun resetHysteresis() {
        candidateMode = null
        modeConfirmationCount = 0
    }
}
