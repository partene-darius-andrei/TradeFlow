package com.tradeflow.backtesting.config

data class ValidationConfig(
    val defaultNumPeriods: Int = 20,
    val minPeriodDays: Int = 60,
    val maxPeriodDays: Int = 120,
    val lookbackBuffer: Int = 200,
    val seed: Int = 3,
    val trainTestRatio: Double = 0.7,

    val highConsistencyThreshold: Int = 8,
    val highConsistencyPnLThreshold: Double = 3.0,
    val consistencyThreshold: Int = 7,
    val consistencyPnLThreshold: Double = 1.0,
    val moderateConsistencyThreshold: Int = 5,
    val moderateConsistencyPnLThreshold: Double = 0.0,

    val strongEdgeWinRateThreshold: Double = 55.0,
    val strongEdgePnLThreshold: Double = 3.0,
    val strongEdgeProfitableThreshold: Int = 8,

    val promiseWinRateThreshold: Double = 50.0,
    val promisePnLThreshold: Double = 0.0,
    val promiseProfitableThreshold: Int = 6,

    val minTradesForSignificance: Int = 200,
    val wellPerformingPnLThreshold: Double = 3.0,
    val wellPerformingWinRateThreshold: Double = 55.0,
    val marginalPnLThreshold: Double = 0.0,
    val marginalWinRateThreshold: Double = 50.0,
    val significantImprovementThreshold: Double = 10.0
) {
    companion object {
        fun default(): ValidationConfig = ValidationConfig()
    }
}
