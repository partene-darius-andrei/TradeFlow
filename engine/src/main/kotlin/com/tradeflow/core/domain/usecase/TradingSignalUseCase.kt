package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.StrategyConfig
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.Indicators
import com.tradeflow.core.domain.model.Order
import java.math.BigDecimal
import java.math.RoundingMode

class TradingSignalUseCase(
    private val config: StrategyConfig = StrategyConfig()
) {

    private fun BigDecimal.toUsd() = this.setScale(2, RoundingMode.HALF_UP)

    private fun checkVolume(indicators: Indicators): Decision.Wait? {
        if (indicators.volumeRatio < config.minVolumeRatio.default) {
            return Decision.Wait(
                "Volume ${indicators.volumeRatio.toBigDecimal().toUsd()}x below required ${config.minVolumeRatio.default}x threshold"
            )
        }
        return null
    }

    private fun checkRsiTrend(indicators: Indicators, isLong: Boolean): Decision.Wait? {
        val rsiBlocksTrade = if (isLong) {
            indicators.rsi < config.rsiLongBlockThreshold.default
        } else {
            indicators.rsi > config.rsiShortBlockThreshold.default
        }

        if (rsiBlocksTrade) {
            val direction = if (isLong) "LONG" else "SHORT"
            val condition = if (isLong) "extreme bearish" else "extreme bullish"
            return Decision.Wait(
                "RSI ${indicators.rsi.toBigDecimal().setScale(1, RoundingMode.HALF_UP)} blocks $direction ($condition)"
            )
        }

        return null
    }

    private fun checkRsiRange(indicators: Indicators, direction: Order.Side): Decision.Wait? {
        val rsiValid = when (direction) {
            Order.Side.BUY -> indicators.rsi > config.rangeRsiMidpoint.default
            Order.Side.SELL -> indicators.rsi < config.rangeRsiMidpoint.default
        }

        if (!rsiValid) {
            val operator = if (direction == Order.Side.BUY) ">" else "<"
            val directionStr = if (direction == Order.Side.BUY) "LONG" else "SHORT"
            val reason = "RSI must be $operator ${config.rangeRsiMidpoint.default} for $directionStr"
            return Decision.Wait(
                "RSI ${indicators.rsi.toBigDecimal().setScale(1, RoundingMode.HALF_UP)} blocks $directionStr ($reason)"
            )
        }

        return null
    }

    fun createTrendSignal(currentPrice: BigDecimal, indicators: Indicators): Decision {
        val isLong = currentPrice >= indicators.sma200
        val direction = if (isLong) Order.Side.BUY else Order.Side.SELL

        checkRsiTrend(indicators, isLong)?.let { return it }
        checkVolume(indicators)?.let { return it }

        // Calculate stop loss and take profit based on direction
        val sl = if (isLong) {
            currentPrice - (indicators.atr * config.stopLossAtrMultiplier.default.toBigDecimal())
        } else {
            currentPrice + (indicators.atr * config.stopLossAtrMultiplier.default.toBigDecimal())
        }

        val tp = if (isLong) {
            currentPrice + (indicators.atr * config.takeProfitAtrMultiplier.default.toBigDecimal())
        } else {
            currentPrice - (indicators.atr * config.takeProfitAtrMultiplier.default.toBigDecimal())
        }

        return Decision.Trend(
            direction = direction,
            entryPrice = currentPrice,
            stopLoss = sl,
            takeProfit = tp
        )
    }

    fun createRangeSignal(currentPrice: BigDecimal, indicators: Indicators): Decision {
        val sma200 = indicators.sma200
        val atr = indicators.atr

        val deviation = currentPrice - sma200
        val atrMultiple = (deviation.abs().divide(atr, 4, RoundingMode.HALF_UP)).toDouble()

        if (atrMultiple < config.rangeEntryMultiplier.default) {
            return Decision.Wait("Price too close to mean (${atrMultiple}× ATR < ${config.rangeEntryMultiplier.default}× threshold)")
        }

        val direction = when {
            currentPrice < sma200 -> Order.Side.BUY
            currentPrice > sma200 -> Order.Side.SELL
            else -> return Decision.Wait("Price at mean")
        }

        checkRsiRange(indicators, direction)?.let { return it }
        checkVolume(indicators)?.let { return it }

        val stopLoss = when (direction) {
            Order.Side.BUY -> currentPrice - (atr * config.rangeStopMultiplier.default.toBigDecimal())
            Order.Side.SELL -> currentPrice + (atr * config.rangeStopMultiplier.default.toBigDecimal())
        }

        return Decision.Range(
            direction = direction,
            entryPrice = currentPrice,
            stopLoss = stopLoss,
            takeProfit = sma200,
            meanPrice = sma200
        )
    }
}
