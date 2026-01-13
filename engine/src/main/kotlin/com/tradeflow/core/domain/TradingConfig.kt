package com.tradeflow.core.domain

import com.tradeflow.core.domain.utils.bd
import java.math.BigDecimal

object TradingConfig {

    object Strategy {
        const val CONFIRMATION_CANDLES: Int = 3
        const val ADX_TREND_THRESHOLD: Double = 20.0
        const val ADX_RANGE_THRESHOLD: Double = 1.0
        val stopLossAtrMultiplier: BigDecimal = "10.0".bd()
        val takeProfitAtrMultiplier: BigDecimal = "20.0".bd()
        val trendPositionPercent: BigDecimal = "0.05".bd()
    }

    object Technical {
        const val SMA_PERIOD: Int = 200
        const val ADX_PERIOD: Int = 14
        const val ATR_PERIOD: Int = 14
        const val RSI_PERIOD: Int = 14
        const val VOLUME_SMA_PERIOD: Int = 20
        const val CMF_PERIOD: Int = 21
        const val MIN_VOLUME_RATIO: Double = 1.2
        const val MIN_CANDLES_REQUIRED: Int = 200
    }
}
