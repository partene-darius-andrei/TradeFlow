package com.tradeflow.backtesting.config

import com.tradeflow.backtesting.data.NoiseLevel
import java.math.BigDecimal

data class BacktestConfig(
    val initialCapital: BigDecimal = "500.00".toBigDecimal(),
    val entryFeeRate: BigDecimal = "0.0005".toBigDecimal(),
    val exitFeeRate: BigDecimal = "0.0002".toBigDecimal(),
    val exitSlippageRate: BigDecimal = "0.0005".toBigDecimal(),
    val loops: Int = 100,
    val primeSize: Int = 300,
    val lookbackWindow: Int = 200,
    val minCandlesRequired: Int = 200,
    val noiseLevel: NoiseLevel = NoiseLevel.NONE
)
