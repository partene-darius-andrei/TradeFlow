package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.*
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.util.BinanceDataLoader
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal

/**
 * RAW STRATEGY FREQUENCY TEST
 *
 * Bypasses ExecuteTradingCycleUseCase entirely to measure pure strategy signal frequency.
 * No "one position at a time" restriction - shows raw decision output.
 *
 * Purpose: Prove that the strategy DOES generate signals, but ExecuteTradingCycleUseCase
 * blocks them with the `isInTrade` check.
 */
class RawStrategyFrequencyTest {

    @Test
    fun `raw strategy signals - ultra aggressive 5m - NO execution layer`() = runBlocking {
        println("\n🔬 RAW STRATEGY FREQUENCY (No Execution Layer)")
        println("=".repeat(90))
        println("Testing PURE decision output without ExecuteTradingCycleUseCase blocking")
        println("=".repeat(90))
        println()

        // Ultra-aggressive config (same as UltraAggressiveTest)
        val config = TradingConfig(
            strategy = StrategyParameters(
                confirmationCandles = 1,
                adxTrendThreshold = 5.0,
                adxRangeThreshold = 3.0,
                stopLossAtrMultiplier = BigDecimal("2.0"),
                takeProfitAtrMultiplier = BigDecimal("6.0"),
                trendPositionPercent = BigDecimal("0.10"),
                gridPositionPercentPerLevel = BigDecimal("0.15"),
                leverage = BigDecimal("3.0")
            ),
            risk = RiskParameters(),
            technical = TechnicalParameters(
                minVolumeRatio = 0.1,
                smaPeriod = 20
            ),
            execution = ExecutionParameters(),
            profile = RiskProfile.BALANCED
        )

        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "5m",
            limit = 1500
        )

        val primeHistory = allCandles.take(20)
        val testCandles = allCandles.drop(20)

        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = config
        )

        var waitCount = 0
        var trendCount = 0
        var rangeCount = 0

        println("Configuration:")
        println("  Volume Threshold:     ${config.technical.minVolumeRatio}x")
        println("  Confirmation Candles: ${config.strategy.confirmationCandles}")
        println("  ADX Trend Threshold:  ${config.strategy.adxTrendThreshold}")
        println("  ADX Range Threshold:  ${config.strategy.adxRangeThreshold}")
        println()
        println("Testing ${testCandles.size} candles...")
        println("-".repeat(90))

        testCandles.forEachIndexed { index, candle ->
            val history = (primeHistory + testCandles.take(index + 1)).takeLast(200)
            val decision = engine.execute(history, candle.close)

            when (decision) {
                is Decision.Wait -> waitCount++
                is Decision.Trend -> {
                    trendCount++
                    if (trendCount <= 10) {
                        println("  Candle $index: TREND ${decision.direction} @ ${candle.close} | " +
                            "Stop: ${decision.stopLoss} | Target: ${decision.takeProfit}")
                    }
                }
                is Decision.Range -> {
                    rangeCount++
                    if (rangeCount <= 10) {
                        println("  Candle $index: RANGE ${decision.levels} levels")
                    }
                }
            }
        }

        val totalCandles = testCandles.size
        val signalCount = trendCount + rangeCount
        val signalRate = if (totalCandles > 0) (signalCount * 100) / totalCandles else 0

        println()
        println("=".repeat(90))
        println("📊 RAW STRATEGY OUTPUT")
        println("=".repeat(90))
        println("Total Candles:   $totalCandles")
        println("WAIT Decisions:  $waitCount (${if (totalCandles > 0) (waitCount * 100) / totalCandles else 0}%)")
        println("TREND Signals:   $trendCount (${if (totalCandles > 0) (trendCount * 100) / totalCandles else 0}%)")
        println("RANGE Signals:   $rangeCount (${if (totalCandles > 0) (rangeCount * 100) / totalCandles else 0}%)")
        println()
        println("SIGNAL RATE:     $signalRate% (TREND + RANGE)")
        println("=".repeat(90))

        if (signalCount > 100) {
            println("\n✅ Strategy generates FREQUENT signals ($signalCount total)")
            println("   Problem: ExecuteTradingCycleUseCase blocks with 'isInTrade' check")
            println("   Solution: Allow multiple concurrent positions OR close trades faster")
        } else if (signalCount > 20) {
            println("\n⚠️  Strategy generates MODERATE signals ($signalCount total)")
            println("   Still being blocked by execution layer")
        } else {
            println("\n❌ Strategy still too conservative even at raw level ($signalCount signals)")
        }
    }

    @Test
    fun `raw strategy signals - 15m candles - realistic aggressive config`() = runBlocking {
        println("\n🔬 RAW STRATEGY FREQUENCY (15m Candles)")
        println("=".repeat(90))
        println("=".repeat(90))
        println()

        // Realistic aggressive config for 15m
        val config = TradingConfig(
            strategy = StrategyParameters(
                confirmationCandles = 1,
                adxTrendThreshold = 10.0,
                adxRangeThreshold = 8.0,
                stopLossAtrMultiplier = BigDecimal("3.0"),
                takeProfitAtrMultiplier = BigDecimal("9.0"),
                trendPositionPercent = BigDecimal("0.05"),
                gridPositionPercentPerLevel = BigDecimal("0.08"),
                leverage = BigDecimal("2.0")
            ),
            risk = RiskParameters(),
            technical = TechnicalParameters(
                minVolumeRatio = 0.8,
                smaPeriod = 50
            ),
            execution = ExecutionParameters(),
            profile = RiskProfile.BALANCED
        )

        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "15m",
            limit = 1000
        )

        val primeHistory = allCandles.take(50)
        val testCandles = allCandles.drop(50)

        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = config
        )

        var waitCount = 0
        var trendCount = 0
        var rangeCount = 0

        println("Configuration:")
        println("  Timeframe:            15m")
        println("  Volume Threshold:     ${config.technical.minVolumeRatio}x")
        println("  Confirmation Candles: ${config.strategy.confirmationCandles}")
        println("  ADX Trend Threshold:  ${config.strategy.adxTrendThreshold}")
        println("  ADX Range Threshold:  ${config.strategy.adxRangeThreshold}")
        println("  SMA Period:           ${config.technical.smaPeriod}")
        println()
        println("Testing ${testCandles.size} candles...")
        println("-".repeat(90))

        testCandles.forEachIndexed { index, candle ->
            val history = (primeHistory + testCandles.take(index + 1)).takeLast(200)
            val decision = engine.execute(history, candle.close)

            when (decision) {
                is Decision.Wait -> waitCount++
                is Decision.Trend -> {
                    trendCount++
                    if (trendCount <= 10) {
                        println("  Candle $index: TREND ${decision.direction}")
                    }
                }
                is Decision.Range -> {
                    rangeCount++
                    if (rangeCount <= 10) {
                        println("  Candle $index: RANGE")
                    }
                }
            }
        }

        val totalCandles = testCandles.size
        val signalCount = trendCount + rangeCount
        val signalRate = if (totalCandles > 0) (signalCount * 100) / totalCandles else 0

        println()
        println("=".repeat(90))
        println("📊 RAW STRATEGY OUTPUT (15m)")
        println("=".repeat(90))
        println("Total Candles:   $totalCandles")
        println("WAIT Decisions:  $waitCount (${if (totalCandles > 0) (waitCount * 100) / totalCandles else 0}%)")
        println("TREND Signals:   $trendCount (${if (totalCandles > 0) (trendCount * 100) / totalCandles else 0}%)")
        println("RANGE Signals:   $rangeCount (${if (totalCandles > 0) (rangeCount * 100) / totalCandles else 0}%)")
        println()
        println("SIGNAL RATE:     $signalRate% (TREND + RANGE)")
        println("=".repeat(90))

        if (signalCount > 50) {
            println("\n✅ 15m timeframe generates FREQUENT signals ($signalCount total)")
        } else if (signalCount > 10) {
            println("\n⚠️  15m timeframe generates MODERATE signals ($signalCount total)")
        } else {
            println("\n❌ Still too conservative ($signalCount signals)")
        }
    }
}
