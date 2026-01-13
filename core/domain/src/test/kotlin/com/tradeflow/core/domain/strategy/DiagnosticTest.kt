package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.util.BinanceDataLoader
import org.junit.Test

class DiagnosticTest {

    @Test
    fun `analyze why trades are blocked`() {
        val config = TradingConfig.forProfile(RiskProfile.BALANCED)
        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = config
        )

        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "4h",
            limit = 400
        )

        val primeHistory = allCandles.take(200)
        val testCandles = allCandles.drop(200).take(50)

        var waitCount = 0
        var trendCount = 0
        var rangeCount = 0

        val blockReasons = mutableMapOf<String, Int>()

        println("\n🔍 DIAGNOSTIC ANALYSIS: Why trades are blocked")
        println("=".repeat(90))
        println("Config: ADX Trend ≥ ${config.strategy.adxTrendThreshold}, Range ≤ ${config.strategy.adxRangeThreshold}, Confirmations: ${config.strategy.confirmationCandles}")
        println("=".repeat(90))

        testCandles.forEachIndexed { index, candle ->
            val history = (primeHistory + testCandles.take(index + 1)).takeLast(200)
            val decision = engine.execute(history, candle.close)

            when (decision) {
                is Decision.Wait -> {
                    waitCount++
                    val reason = decision.reason
                    blockReasons[reason] = blockReasons.getOrDefault(reason, 0) + 1

                    if (index < 10) {
                        println("Candle #${index + 1}: WAIT - $reason")
                    }
                }
                is Decision.Trend -> {
                    trendCount++
                    println("Candle #${index + 1}: ✅ TREND ${decision.direction} - SL: ${decision.stopLoss}, TP: ${decision.takeProfit}")
                }
                is Decision.Range -> {
                    rangeCount++
                    println("Candle #${index + 1}: ✅ RANGE - Grid levels: ${decision.levels}")
                }
            }
        }

        println("\n📊 RESULTS SUMMARY")
        println("=".repeat(90))
        println("WAIT:  $waitCount (${waitCount * 100 / testCandles.size}%)")
        println("TREND: $trendCount (${trendCount * 100 / testCandles.size}%)")
        println("RANGE: $rangeCount (${rangeCount * 100 / testCandles.size}%)")

        println("\n🚫 TOP BLOCKING REASONS:")
        println("=".repeat(90))
        blockReasons.entries
            .sortedByDescending { it.value }
            .take(5)
            .forEach { (reason, count) ->
                println("$count times (${count * 100 / waitCount}%): $reason")
            }
        println("=".repeat(90))
    }
}
