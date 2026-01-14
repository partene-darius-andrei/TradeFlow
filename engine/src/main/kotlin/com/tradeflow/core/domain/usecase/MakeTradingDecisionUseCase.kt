package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.StrategyConfig
import com.tradeflow.core.domain.TradingConfig
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.Indicators
import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal
import java.math.RoundingMode



class MakeTradingDecisionUseCase {

    private enum class Mode {
        TREND,
        RANGE
    }

    private fun BigDecimal.toUsd() = this.setScale(2, RoundingMode.HALF_UP)

    private var lastMode: Mode = Mode.TREND

    private var confirmationCount = 0

    private var candidateMode: Mode? = null

    private val analyzeCandlesUseCase = AnalyzeCandlesUseCase()

    operator fun invoke(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        if (candles.size < TradingConfig.Technical.MIN_CANDLES_REQUIRED) {
            return Decision.Wait("Not enough candles: ${candles.size}/${TradingConfig.Technical.MIN_CANDLES_REQUIRED}")
        }

        val indicators = analyzeCandlesUseCase(candles)

        // Skip if indicators contain NaN (insufficient data)
        if (indicators.adx.isNaN() || indicators.rsi.isNaN() || indicators.volumeRatio.isNaN()) {
            return Decision.Wait("Indicators contain NaN (insufficient data)")
        }

        // 1. Determine desired mode based on Trend Strength (ADX)
        val desiredMode = when {
            indicators.adx >= StrategyConfig.adxTrendThreshold -> Mode.TREND
            indicators.adx <= StrategyConfig.adxRangeThreshold -> Mode.RANGE
            else -> lastMode
        }

        // 2. Apply Hysteresis (require N consecutive confirmations before switching)
        if (desiredMode == lastMode) {
            // Already in desired mode, reset any pending switch
            candidateMode = null
            confirmationCount = 0
            return createDecision(lastMode, currentPrice, indicators)
        }

        // Mode change is desired, apply confirmation logic
        if (desiredMode != candidateMode) {
            // New candidate mode detected, start fresh confirmation
            candidateMode = desiredMode
            confirmationCount = 1
        } else {
            // Same candidate as before, increment confirmation count
            confirmationCount++
        }

        // Check if we have enough confirmations to switch
        val confirmationRequired = StrategyConfig.confirmationCandles
        if (confirmationCount >= confirmationRequired) {
            // ✅ CONFIRMATION COMPLETE - switch to new mode
            lastMode = desiredMode
            candidateMode = null
            confirmationCount = 0
            return createDecision(lastMode, currentPrice, indicators)
        }

        // Still waiting for confirmation
        return Decision.Wait("Confirming mode switch to $desiredMode ($confirmationCount/$confirmationRequired)")
    }


    private fun createDecision(mode: Mode, currentPrice: BigDecimal, indicators: Indicators): Decision {
        return when (mode) {
            Mode.TREND -> {
                // Determine direction: LONG (BUY) if price > SMA200, SHORT (SELL) if price < SMA200
                val isLong = currentPrice >= indicators.sma200
                val direction = if (isLong) OrderSide.BUY else OrderSide.SELL

                // RSI Momentum Filter: Block only extreme opposite momentum
                // FIX: Relaxed from RSI > 50 to RSI > 30 for LONG (was blocking 90% of trades)
                // LONG blocked only if RSI < 30 (extreme bearish)
                // SHORT blocked only if RSI > 70 (extreme bullish)
                val rsiBlocksTrade = if (isLong) {
                    indicators.rsi < StrategyConfig.RSI_LONG_BLOCK_THRESHOLD
                } else {
                    indicators.rsi > StrategyConfig.RSI_SHORT_BLOCK_THRESHOLD
                }
                if (rsiBlocksTrade) {
                    val reason = if (isLong) "RSI < 30 (extreme bearish)" else "RSI > 70 (extreme bullish)"
                    return Decision.Wait("RSI ${indicators.rsi.toBigDecimal().setScale(1, RoundingMode.HALF_UP)} blocks ${if (isLong) "LONG" else "SHORT"} ($reason)")
                }

                // Volume Confirmation Filter: Volume must be significantly above average
                // Research: Volume > 1.5x improves breakout success from 39% to 65% (+26 percentage points)
                if (indicators.volumeRatio < TradingConfig.Technical.MIN_VOLUME_RATIO) {
                    return Decision.Wait("Volume ${indicators.volumeRatio.toBigDecimal().toUsd()}x below required ${TradingConfig.Technical.MIN_VOLUME_RATIO}x threshold")
                }

                // Calculate stop loss and take profit based on direction
                val sl = if (isLong) {
                    currentPrice - (indicators.atr * StrategyConfig.stopLossAtrMultiplier)
                } else {
                    currentPrice + (indicators.atr * StrategyConfig.stopLossAtrMultiplier)
                }

                val tp = if (isLong) {
                    currentPrice + (indicators.atr * StrategyConfig.takeProfitAtrMultiplier)
                } else {
                    currentPrice - (indicators.atr * StrategyConfig.takeProfitAtrMultiplier)
                }

                Decision.Trend(
                    direction = direction,
                    entryPrice = currentPrice,
                    stopLoss = sl,
                    takeProfit = tp
                )
            }
            Mode.RANGE -> {
                val sma200 = indicators.sma200
                val atr = indicators.atr

                val deviation = currentPrice - sma200
                val atrMultiple = (deviation.abs().divide(atr, 4, RoundingMode.HALF_UP)).toDouble()

                if (atrMultiple < StrategyConfig.rangeEntryMultiplier) {
                    return Decision.Wait("Price too close to mean (${atrMultiple}× ATR < ${StrategyConfig.rangeEntryMultiplier}× threshold)")
                }

                val direction = when {
                    currentPrice < sma200 -> OrderSide.BUY
                    currentPrice > sma200 -> OrderSide.SELL
                    else -> return Decision.Wait("Price at mean")
                }

                val rsiValid = when (direction) {
                    OrderSide.BUY -> indicators.rsi > StrategyConfig.rangeRsiMidpoint
                    OrderSide.SELL -> indicators.rsi < StrategyConfig.rangeRsiMidpoint
                }
                if (!rsiValid) {
                    val operator = if (direction == OrderSide.BUY) ">" else "<"
                    val reason = "RSI must be $operator ${StrategyConfig.rangeRsiMidpoint} for ${if (direction == OrderSide.BUY) "LONG" else "SHORT"}"
                    return Decision.Wait("RSI ${indicators.rsi.toBigDecimal().setScale(1, RoundingMode.HALF_UP)} blocks ${if (direction == OrderSide.BUY) "LONG" else "SHORT"} ($reason)")
                }

                if (indicators.volumeRatio < TradingConfig.Technical.MIN_VOLUME_RATIO) {
                    return Decision.Wait("Volume ${indicators.volumeRatio.toBigDecimal().toUsd()}x below required ${TradingConfig.Technical.MIN_VOLUME_RATIO}x threshold")
                }

                val stopLoss = when (direction) {
                    OrderSide.BUY -> currentPrice - (atr * StrategyConfig.rangeStopMultiplier.toBigDecimal())
                    OrderSide.SELL -> currentPrice + (atr * StrategyConfig.rangeStopMultiplier.toBigDecimal())
                }

                Decision.Range(
                    direction = direction,
                    entryPrice = currentPrice,
                    stopLoss = stopLoss,
                    takeProfit = sma200,
                    meanPrice = sma200
                )
            }
        }
    }
}

