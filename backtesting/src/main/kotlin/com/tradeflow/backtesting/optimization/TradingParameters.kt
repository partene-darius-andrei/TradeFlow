package com.tradeflow.backtesting.optimization

import java.math.BigDecimal

data class TradingParameters(
    val adxTrendThreshold: Double = 20.0,
    val adxRangeThreshold: Double = 1.0,
    val confirmationCandles: Int = 3,
    val trendPositionPercent: Double = 0.05,
    val stopLossAtrMultiplier: Double = 10.0,
    val takeProfitAtrMultiplier: Double = 20.0,
    val leverage: Double = 1.0
) {
    init {
        require(adxTrendThreshold in 15.0..30.0) {
            "ADX trend threshold must be between 15.0 and 30.0, got $adxTrendThreshold"
        }
        require(adxRangeThreshold in 0.5..2.0) {
            "ADX range threshold must be between 0.5 and 2.0, got $adxRangeThreshold"
        }
        require(confirmationCandles in 1..5) {
            "Confirmation candles must be between 1 and 5, got $confirmationCandles"
        }
        require(trendPositionPercent in 0.01..0.15) {
            "Trend position percent must be between 0.01 and 0.15, got $trendPositionPercent"
        }
        require(stopLossAtrMultiplier in 3.0..20.0) {
            "Stop loss ATR multiplier must be between 3.0 and 20.0, got $stopLossAtrMultiplier"
        }
        require(takeProfitAtrMultiplier in 5.0..40.0) {
            "Take profit ATR multiplier must be between 5.0 and 40.0, got $takeProfitAtrMultiplier"
        }
        require(leverage in 1.0..10.0) {
            "Leverage must be between 1.0 and 10.0, got $leverage"
        }
    }

    fun toBigDecimal(value: Double): BigDecimal = BigDecimal(value.toString())

    companion object {
        fun current(): TradingParameters = TradingParameters()

        fun random(seed: kotlin.random.Random): TradingParameters {
            return TradingParameters(
                adxTrendThreshold = seed.nextDouble(15.0, 30.0),
                adxRangeThreshold = seed.nextDouble(0.5, 2.0),
                confirmationCandles = seed.nextInt(1, 6),
                trendPositionPercent = seed.nextDouble(0.01, 0.15),
                stopLossAtrMultiplier = seed.nextDouble(3.0, 20.0),
                takeProfitAtrMultiplier = seed.nextDouble(5.0, 40.0),
                leverage = seed.nextDouble(1.0, 10.0)
            )
        }
    }
}
