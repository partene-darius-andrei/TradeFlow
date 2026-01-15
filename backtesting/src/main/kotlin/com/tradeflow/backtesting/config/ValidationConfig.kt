package com.tradeflow.backtesting.config

data class PeriodConfig(
    val defaultNumPeriods: Int = 20,
    val minPeriodDays: Int = 60,
    val maxPeriodDays: Int = 120,
    val lookbackBuffer: Int = 200,
    val seed: Int = 3,
    val trainTestRatio: Double = 0.7
)

data class ConsistencyThresholds(
    val highThreshold: Int = 8,
    val highPnLThreshold: Double = 3.0,
    val threshold: Int = 7,
    val pnLThreshold: Double = 1.0,
    val moderateThreshold: Int = 5,
    val moderatePnLThreshold: Double = 0.0
)

data class EdgeDetectionThresholds(
    val strongWinRate: Double = 55.0,
    val strongPnL: Double = 3.0,
    val strongProfitable: Int = 8,
    val promiseWinRate: Double = 50.0,
    val promisePnL: Double = 0.0,
    val promiseProfitable: Int = 6
)

data class SignificanceThresholds(
    val minTrades: Int = 200,
    val wellPerformingPnL: Double = 3.0,
    val wellPerformingWinRate: Double = 55.0,
    val marginalPnL: Double = 0.0,
    val marginalWinRate: Double = 50.0,
    val significantImprovement: Double = 10.0
)

data class ValidationConfig(
    val period: PeriodConfig = PeriodConfig(),
    val consistency: ConsistencyThresholds = ConsistencyThresholds(),
    val edge: EdgeDetectionThresholds = EdgeDetectionThresholds(),
    val significance: SignificanceThresholds = SignificanceThresholds()
)
