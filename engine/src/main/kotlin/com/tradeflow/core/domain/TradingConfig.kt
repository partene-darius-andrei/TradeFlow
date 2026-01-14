package com.tradeflow.core.domain

import com.tradeflow.core.domain.utils.bd
import java.math.BigDecimal

object TradingConfig {

    private val parameterOverrides = ThreadLocal<ParameterSetOverride?>()

    data class ParameterSetOverride(
        val adxTrendThreshold: Double?,
        val adxRangeThreshold: Double?,
        val confirmationCandles: Int?,
        val trendPositionPercent: BigDecimal?,
        val stopLossAtrMultiplier: BigDecimal?,
        val takeProfitAtrMultiplier: BigDecimal?,
        val leverage: BigDecimal?
    )

    fun <T> withOverrides(
        adxTrendThreshold: Double? = null,
        adxRangeThreshold: Double? = null,
        confirmationCandles: Int? = null,
        trendPositionPercent: Double? = null,
        stopLossAtrMultiplier: Double? = null,
        takeProfitAtrMultiplier: Double? = null,
        leverage: Double? = null,
        block: () -> T
    ): T {
        val overrides = ParameterSetOverride(
            adxTrendThreshold = adxTrendThreshold,
            adxRangeThreshold = adxRangeThreshold,
            confirmationCandles = confirmationCandles,
            trendPositionPercent = trendPositionPercent?.let { BigDecimal(it.toString()) },
            stopLossAtrMultiplier = stopLossAtrMultiplier?.let { BigDecimal(it.toString()) },
            takeProfitAtrMultiplier = takeProfitAtrMultiplier?.let { BigDecimal(it.toString()) },
            leverage = leverage?.let { BigDecimal(it.toString()) }
        )
        parameterOverrides.set(overrides)
        try {
            return block()
        } finally {
            parameterOverrides.remove()
        }
    }

    object Strategy {
        private const val CONFIRMATION_CANDLES_DEFAULT: Int = 3
        private const val ADX_TREND_THRESHOLD_DEFAULT: Double = 20.0
        private const val ADX_RANGE_THRESHOLD_DEFAULT: Double = 1.0
        private val STOP_LOSS_ATR_MULTIPLIER_DEFAULT: BigDecimal = "10.0".bd()
        private val TAKE_PROFIT_ATR_MULTIPLIER_DEFAULT: BigDecimal = "20.0".bd()
        private val TREND_POSITION_PERCENT_DEFAULT: BigDecimal = "0.05".bd()
        private val LEVERAGE_DEFAULT: BigDecimal = "1.0".bd()

        fun getConfirmationCandles(): Int =
            parameterOverrides.get()?.confirmationCandles ?: CONFIRMATION_CANDLES_DEFAULT

        fun getAdxTrendThreshold(): Double =
            parameterOverrides.get()?.adxTrendThreshold ?: ADX_TREND_THRESHOLD_DEFAULT

        fun getAdxRangeThreshold(): Double =
            parameterOverrides.get()?.adxRangeThreshold ?: ADX_RANGE_THRESHOLD_DEFAULT

        fun getStopLossAtrMultiplier(): BigDecimal =
            parameterOverrides.get()?.stopLossAtrMultiplier ?: STOP_LOSS_ATR_MULTIPLIER_DEFAULT

        fun getTakeProfitAtrMultiplier(): BigDecimal =
            parameterOverrides.get()?.takeProfitAtrMultiplier ?: TAKE_PROFIT_ATR_MULTIPLIER_DEFAULT

        fun getTrendPositionPercent(): BigDecimal =
            parameterOverrides.get()?.trendPositionPercent ?: TREND_POSITION_PERCENT_DEFAULT

        fun getLeverage(): BigDecimal =
            parameterOverrides.get()?.leverage ?: LEVERAGE_DEFAULT
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
