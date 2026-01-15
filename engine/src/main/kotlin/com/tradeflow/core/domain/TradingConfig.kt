package com.tradeflow.core.domain

import java.math.BigDecimal

data class StrategyConfig(
    val confirmationCandles: Int = 1,
    val adxTrendThreshold: Double = 17.0,
    val adxRangeThreshold: Double = 1.0,
    val stopLossAtrMultiplier: BigDecimal = "0.5".bd(),
    val takeProfitAtrMultiplier: BigDecimal = "1.5".bd(),
    val trendPositionPercent: BigDecimal = "0.05".bd(),
    val leverage: BigDecimal = "3".bd(),

    val rangeEntryMultiplier: Double = 0.5,
    val rangeStopMultiplier: Double = 2.0,
    val rangeRsiMidpoint: Double = 50.0
) {
    companion object {
        const val RSI_LONG_BLOCK_THRESHOLD: Double = 30.0
        const val RSI_SHORT_BLOCK_THRESHOLD: Double = 70.0
    }
}

private fun String.bd(): BigDecimal = BigDecimal(this)

object TradingConfig {

    object Technical {
        const val SMA_PERIOD: Int = 200
        const val ADX_PERIOD: Int = 14
        const val ATR_PERIOD: Int = 14
        const val RSI_PERIOD: Int = 14
        const val VOLUME_SMA_PERIOD: Int = 20
        const val MIN_VOLUME_RATIO: Double = 1.0
        const val MIN_CANDLES_REQUIRED: Int = 200
        const val SMA_PREVIOUS_LOOKBACK: Int = 10
        const val PNL_PRECISION_DECIMAL_PLACES: Int = 6
        const val DEFAULT_CANDLE_DURATION_HOURS: Int = 4
        const val VOLUME_RATIO_DEFAULT_FALLBACK: Double = 1.0
    }
}
