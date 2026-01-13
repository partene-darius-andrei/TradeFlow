package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.util.BinanceDataLoader
import org.junit.Test

data class TimeframeResults(
    val interval: String,
    val totalCandles: Int,
    val waitCount: Int,
    val trendCount: Int,
    val rangeCount: Int,
    val blockReasons: Map<String, Int>
) {
    val signalRate: Int get() = if (totalCandles > 0) ((trendCount + rangeCount) * 100) / totalCandles else 0
    val waitRate: Int get() = if (totalCandles > 0) (waitCount * 100) / totalCandles else 0

    fun printSummary() {
        println("Timeframe: $interval")
        println("  Total Candles: $totalCandles")
        println("  WAIT:  $waitCount ($waitRate%)")
        println("  TREND: $trendCount (${if (totalCandles > 0) trendCount * 100 / totalCandles else 0}%)")
        println("  RANGE: $rangeCount (${if (totalCandles > 0) rangeCount * 100 / totalCandles else 0}%)")
        println("  Signal Rate: $signalRate%")
        println("  Top 3 Block Reasons:")
        if (waitCount > 0) {
            blockReasons.entries
                .sortedByDescending { it.value }
                .take(3)
                .forEach { (reason, count) ->
                    val shortened = if (reason.length > 60) reason.take(57) + "..." else reason
                    println("    - ${count}x (${"%.1f".format(count * 100.0 / waitCount)}%) $shortened")
                }
        } else {
            println("    - No WAIT signals (all candles produced trades)")
        }
        println()
    }
}

class TimeframeComparisonTest {

    @Test
    fun `compare different timeframes for optimal signal quality`() {
        println("\n⏰ TIMEFRAME COMPARISON ANALYSIS")
        println("=".repeat(90))
        println("Testing multiple timeframes to find optimal signal-to-noise ratio")
        println("=".repeat(90))
        println()

        val timeframes = listOf(
            "1h" to 1000,   // 1000 hours = ~41 days
            "4h" to 400,    // 400 candles = ~66 days
            "6h" to 300,    // 300 candles = ~75 days
            "12h" to 200,   // 200 candles = ~100 days
            "1d" to 150     // 150 candles = ~5 months
        )

        val results = mutableListOf<TimeframeResults>()

        timeframes.forEach { (interval, limit) ->
            val result = analyzeTimeframe(interval, limit)
            results.add(result)
            result.printSummary()
        }

        println("\n🏆 COMPARISON SUMMARY")
        println("=".repeat(90))
        println("%-10s | %-8s | %-8s | %-8s | %-12s".format("Timeframe", "Signal%", "WAIT%", "TREND%", "Top Block"))
        println("-".repeat(90))

        results.forEach { result ->
            val topBlock = result.blockReasons.entries
                .maxByOrNull { it.value }
                ?.key
                ?.take(12) ?: "None"

            println("%-10s | %6d%% | %6d%% | %6d%% | %s".format(
                result.interval,
                result.signalRate,
                result.waitRate,
                if (result.totalCandles > 0) result.trendCount * 100 / result.totalCandles else 0,
                topBlock
            ))
        }
        println("=".repeat(90))

        // Find optimal timeframe
        val optimal = results.maxByOrNull { it.signalRate }
        println("\n✅ OPTIMAL TIMEFRAME: ${optimal?.interval}")
        println("   Signal Rate: ${optimal?.signalRate}%")
        println("   TREND Signals: ${optimal?.trendCount} (${optimal?.let { if (it.totalCandles > 0) it.trendCount * 100 / it.totalCandles else 0 }}%)")
        println("   RANGE Signals: ${optimal?.rangeCount} (${optimal?.let { if (it.totalCandles > 0) it.rangeCount * 100 / it.totalCandles else 0 }}%)")
        println("=".repeat(90))
    }

    private fun analyzeTimeframe(interval: String, limit: Int): TimeframeResults {
        val config = TradingConfig.forProfile(RiskProfile.BALANCED)
        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = config
        )

        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = interval,
            limit = limit
        )

        val primeHistory = allCandles.take(200)
        val testCandles = allCandles.drop(200)

        var waitCount = 0
        var trendCount = 0
        var rangeCount = 0
        val blockReasons = mutableMapOf<String, Int>()

        testCandles.forEachIndexed { index, candle ->
            val history = (primeHistory + testCandles.take(index + 1)).takeLast(200)
            val decision = engine.execute(history, candle.close)

            when (decision) {
                is Decision.Wait -> {
                    waitCount++
                    val reason = decision.reason
                    blockReasons[reason] = blockReasons.getOrDefault(reason, 0) + 1
                }
                is Decision.Trend -> trendCount++
                is Decision.Range -> rangeCount++
            }
        }

        return TimeframeResults(
            interval = interval,
            totalCandles = testCandles.size,
            waitCount = waitCount,
            trendCount = trendCount,
            rangeCount = rangeCount,
            blockReasons = blockReasons
        )
    }

    @Test
    fun `deep dive 15h candle analysis`() {
        println("\n🔬 DEEP DIVE: 15-Hour Candle Analysis")
        println("=".repeat(90))

        // Binance doesn't support 15h directly, so we'll use 12h (closest available)
        val interval = "12h"
        println("NOTE: Binance doesn't support 15h intervals. Using 12h (closest available).")
        println()

        val config = TradingConfig.forProfile(RiskProfile.BALANCED)
        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = config
        )

        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = interval,
            limit = 300  // 300 candles × 12h = 150 days
        )

        val primeHistory = allCandles.take(200)
        val testCandles = allCandles.drop(200)

        var waitCount = 0
        var trendCount = 0
        var rangeCount = 0
        val blockReasons = mutableMapOf<String, Int>()

        println("Analyzing ${testCandles.size} candles at $interval interval...")
        println()

        testCandles.forEachIndexed { index, candle ->
            val history = (primeHistory + testCandles.take(index + 1)).takeLast(200)
            val decision = engine.execute(history, candle.close)

            when (decision) {
                is Decision.Wait -> {
                    waitCount++
                    val reason = decision.reason
                    blockReasons[reason] = blockReasons.getOrDefault(reason, 0) + 1
                }
                is Decision.Trend -> {
                    trendCount++
                    if (trendCount <= 5) {
                        println("TREND #$trendCount: ${decision.direction} | Entry: ${candle.close} | SL: ${decision.stopLoss} | TP: ${decision.takeProfit}")
                    }
                }
                is Decision.Range -> {
                    rangeCount++
                    if (rangeCount <= 5) {
                        println("RANGE #$rangeCount: ${decision.levels} levels | Spacing: ${decision.gridSpacing}")
                    }
                }
            }
        }

        println()
        println("📊 12-HOUR CANDLE RESULTS")
        println("=".repeat(90))
        println("Total Candles: ${testCandles.size}")
        println("WAIT:  $waitCount (${waitCount * 100 / testCandles.size}%)")
        println("TREND: $trendCount (${trendCount * 100 / testCandles.size}%)")
        println("RANGE: $rangeCount (${rangeCount * 100 / testCandles.size}%)")
        println("Signal Rate: ${(trendCount + rangeCount) * 100 / testCandles.size}%")
        println()
        println("🚫 TOP BLOCKING REASONS:")
        if (waitCount > 0) {
            blockReasons.entries
                .sortedByDescending { it.value }
                .take(5)
                .forEach { (reason, count) ->
                    println("  ${count}x (${count * 100 / waitCount}%): $reason")
                }
        } else {
            println("  No WAIT signals (all candles produced trades)")
        }
        println("=".repeat(90))
    }
}
