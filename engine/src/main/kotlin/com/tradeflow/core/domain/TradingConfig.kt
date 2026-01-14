package com.tradeflow.core.domain

import java.math.BigDecimal

private fun String.bd(): BigDecimal = BigDecimal(this)

object StrategyConfig {
    var confirmationCandles: Int = 3
    var adxTrendThreshold: Double = 20.0
    var adxRangeThreshold: Double = 1.0
    var stopLossAtrMultiplier: BigDecimal = "10.0".bd()
    var takeProfitAtrMultiplier: BigDecimal = "20.0".bd()
    var trendPositionPercent: BigDecimal = "0.075".bd()
    var leverage: BigDecimal = "3.0".bd()

    // Signal quality filters (constants - don't need to change)
    const val RSI_LONG_BLOCK_THRESHOLD: Double = 30.0
    const val RSI_SHORT_BLOCK_THRESHOLD: Double = 70.0
}

object TradingConfig {

    object Technical {
        const val SMA_PERIOD: Int = 200
        const val ADX_PERIOD: Int = 14
        const val ATR_PERIOD: Int = 14
        const val RSI_PERIOD: Int = 14
        const val VOLUME_SMA_PERIOD: Int = 20
        const val MIN_VOLUME_RATIO: Double = 1.2
        const val MIN_CANDLES_REQUIRED: Int = 200
        const val SMA_PREVIOUS_LOOKBACK: Int = 10
        const val PNL_PRECISION_DECIMAL_PLACES: Int = 6
        const val DEFAULT_CANDLE_DURATION_HOURS: Int = 4
        const val VOLUME_RATIO_DEFAULT_FALLBACK: Double = 1.0
    }
}
