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

    override fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        if (candles.size < config.smaPeriod) {
            return Decision.Wait("Not enough candles: ${candles.size}/${config.smaPeriod}")
        }

        val indicators = taService.calculateAll(candles, config.smaPeriod, config.adxPeriod, config.atrPeriod)

        return when {
            currentPrice < indicators.sma200 -> Decision.Defense(
                "Price below SMA200", currentPrice, indicators.sma200
            )
            indicators.adx >= config.adxTrendThreshold -> Decision.Trend(
                direction = OrderSide.BUY,
                entryPrice = currentPrice,
                stopLoss = currentPrice - (indicators.atr * config.stopLossAtrMultiplier),
                takeProfit = currentPrice + (indicators.atr * config.takeProfitAtrMultiplier),
                positionSize = currentPrice * config.trendPositionPercent,
                adx = indicators.adx,
                atr = indicators.atr
            )
            else -> Decision.Range(
                gridSpacing = (indicators.atr * config.minGridSpacing).max(BigDecimal("0.01")),
                levels = 5,
                positionSizePerLevel = currentPrice * config.gridPositionPercentPerLevel,
                adx = indicators.adx,
                atr = indicators.atr
            )
        }
    }
}
