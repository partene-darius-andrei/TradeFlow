package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.TradingConfig
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal
import java.math.RoundingMode

enum class Mode {
    TREND,
    RANGE
}

class MakeTradingDecisionUseCase(
    private val analyzeCandlesUseCase: AnalyzeCandlesUseCase
) {

    private var lastMode: Mode = Mode.TREND

    private var confirmationCount = 0

    private var candidateMode: Mode? = null


    fun execute(candles: List<Candle>, currentPrice: BigDecimal): Decision {
        if (candles.size < TradingConfig.Technical.MIN_CANDLES_REQUIRED) {
            return Decision.Wait("Not enough candles: ${candles.size}/${TradingConfig.Technical.MIN_CANDLES_REQUIRED}")
        }

        val indicators = analyzeCandlesUseCase.calculateAll(
            candles,
            TradingConfig.Technical.SMA_PERIOD,
            TradingConfig.Technical.ADX_PERIOD,
            TradingConfig.Technical.ATR_PERIOD,
            TradingConfig.Technical.RSI_PERIOD,
            TradingConfig.Technical.VOLUME_SMA_PERIOD,
            TradingConfig.Technical.CMF_PERIOD
        )

        // Skip if indicators contain NaN (insufficient data)
        if (indicators.adx.isNaN() || indicators.rsi.isNaN() || indicators.volumeRatio.isNaN() || indicators.cmf.isNaN()) {
            return Decision.Wait("Indicators contain NaN (insufficient data)")
        }

        try {
            println("  [DECISION] Price: $currentPrice | SMA: ${indicators.sma200.setScale(0, java.math.RoundingMode.HALF_UP)} | ADX: ${indicators.adx.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} | ATR: ${indicators.atr.setScale(0, java.math.RoundingMode.HALF_UP)}")
            println("  [DECISION] RSI: ${indicators.rsi.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} | Volume: ${indicators.volumeRatio.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}x avg | CMF: ${indicators.cmf.toBigDecimal().setScale(3, java.math.RoundingMode.HALF_UP)}")
        } catch (e: Exception) {
            // Skip debug output if indicators contain NaN
        }

        // 1. Determine desired mode based on Trend Strength (ADX)
        val desiredMode = when {
            indicators.adx >= TradingConfig.Strategy.ADX_TREND_THRESHOLD -> {
                println("  [DECISION] ADX ${indicators.adx} >= ${TradingConfig.Strategy.ADX_TREND_THRESHOLD} → Wants TREND")
                Mode.TREND
            }
            indicators.adx <= TradingConfig.Strategy.ADX_RANGE_THRESHOLD -> {
                println("  [DECISION] ADX ${indicators.adx} <= ${TradingConfig.Strategy.ADX_RANGE_THRESHOLD} → Wants RANGE")
                Mode.RANGE
            }
            else -> {
                // ADX in neutral zone (between range and trend thresholds)
                // Stay in current mode to avoid whipsaw
                println("  [DECISION] ADX ${indicators.adx} in neutral zone (${TradingConfig.Strategy.ADX_RANGE_THRESHOLD}-${TradingConfig.Strategy.ADX_TREND_THRESHOLD}) → Stay in $lastMode")
                lastMode
            }
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
        if (confirmationCount >= TradingConfig.Strategy.CONFIRMATION_CANDLES) {
            // ✅ CONFIRMATION COMPLETE - switch to new mode
            lastMode = desiredMode
            candidateMode = null
            confirmationCount = 0
            return createDecision(lastMode, currentPrice, indicators)
        }

        // Still waiting for confirmation
        return Decision.Wait("Confirming mode switch to $desiredMode ($confirmationCount/${TradingConfig.Strategy.CONFIRMATION_CANDLES})")
    }


    private fun createDecision(mode: Mode, currentPrice: BigDecimal, indicators: AnalyzeCandlesUseCase.Indicators): Decision {
        return when (mode) {
            Mode.TREND -> {
                // Determine direction: LONG (BUY) if price > SMA200, SHORT (SELL) if price < SMA200
                val isLong = currentPrice >= indicators.sma200
                val direction = if (isLong) OrderSide.BUY else OrderSide.SELL

                // RSI Momentum Filter: Block only extreme opposite momentum
                // FIX: Relaxed from RSI > 50 to RSI > 30 for LONG (was blocking 90% of trades)
                // LONG blocked only if RSI < 30 (extreme bearish)
                // SHORT blocked only if RSI > 70 (extreme bullish)
                val rsiBlocksTrade = if (isLong) indicators.rsi < 30.0 else indicators.rsi > 70.0
                if (rsiBlocksTrade) {
                    val reason = if (isLong) "RSI < 30 (extreme bearish)" else "RSI > 70 (extreme bullish)"
                    println("  [DECISION] ❌ RSI filter: ${indicators.rsi.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP)} blocks ${if (isLong) "LONG" else "SHORT"} ($reason)")
                    return Decision.Wait("RSI ${indicators.rsi.toBigDecimal().setScale(1, RoundingMode.HALF_UP)} blocks ${if (isLong) "LONG" else "SHORT"} ($reason)")
                }

                // Volume Confirmation Filter: Volume must be significantly above average
                // Research: Volume > 1.5x improves breakout success from 39% to 65% (+26 percentage points)
                if (indicators.volumeRatio < TradingConfig.Technical.MIN_VOLUME_RATIO) {
                    println("  [DECISION] ❌ Volume filter: ${indicators.volumeRatio.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}x below required ${TradingConfig.Technical.MIN_VOLUME_RATIO}x threshold")
                    return Decision.Wait("Volume ${indicators.volumeRatio.toBigDecimal().setScale(2, RoundingMode.HALF_UP)}x below required ${TradingConfig.Technical.MIN_VOLUME_RATIO}x threshold")
                }

                // CMF Confirmation (optional, adds additional confidence layer)
                // CMF > 0.05 for LONG = money flowing in (bullish)
                // CMF < -0.05 for SHORT = money flowing out (bearish)
                val cmfConfirmsDirection = if (isLong) indicators.cmf > 0.05 else indicators.cmf < -0.05
                if (!cmfConfirmsDirection) {
                    try {
                        println("  [DECISION] ⚠️  CMF weak: ${indicators.cmf.toBigDecimal().setScale(3, java.math.RoundingMode.HALF_UP)} weakly supports ${if (isLong) "LONG" else "SHORT"} (not blocking, but lower confidence)")
                    } catch (e: Exception) {
                        // Skip debug output if CMF is NaN
                    }
                }

                // Calculate stop loss and take profit based on direction
                val sl = if (isLong) {
                    currentPrice - (indicators.atr * TradingConfig.Strategy.stopLossAtrMultiplier)
                } else {
                    currentPrice + (indicators.atr * TradingConfig.Strategy.stopLossAtrMultiplier)
                }

                val tp = if (isLong) {
                    currentPrice + (indicators.atr * TradingConfig.Strategy.takeProfitAtrMultiplier)
                } else {
                    currentPrice - (indicators.atr * TradingConfig.Strategy.takeProfitAtrMultiplier)
                }

                println("  [DECISION] → Final: IS LONG $isLong ${TradingConfig.Strategy.trendPositionPercent.multiply(BigDecimal("100"))}% | Entry: $currentPrice | SL: $sl | TP: $tp")

                Decision.Trend(
                    direction = direction,
                    entryPrice = currentPrice,
                    stopLoss = sl,
                    takeProfit = tp
                )
            }
            Mode.RANGE -> {
                Decision.Range
            }
        }
    }
}

