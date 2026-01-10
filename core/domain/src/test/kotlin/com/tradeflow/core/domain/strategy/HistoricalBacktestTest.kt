package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.indicator.TechnicalAnalysisService
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.util.BinanceDataLoader
import org.junit.Test
import java.math.BigDecimal
import kotlin.random.Random
import kotlin.test.assertTrue

/**
 * Monte Carlo style validation: runs the strategy 100 times over random historical periods
 * to see how it behaves across different market regimes.
 */
class HistoricalBacktestTest {

    private val taService = TechnicalAnalysisService()
    private val config = TradingConfig.forProfile(RiskProfile.BALANCED)
    private val engine = TradingDecisionEngine(taService, config)

    @Test
    fun `monte carlo strategy behavior analysis`() {
        // 1. Fetch 1000 daily candles (~2.7 years of history)
        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "1d",
            limit = 1000
        )
        println("Loaded ${allCandles.size} BTC/USDT daily candles for Monte Carlo analysis")

        val windowSize = 200 // SMA200 requirement
        val iterations = 100
        
        var totalDefense = 0
        var totalTrend = 0
        var totalRange = 0
        var totalWait = 0

        println("\n--- Monte Carlo Report (100 Random Samples) ---")

        repeat(iterations) { i ->
            // Pick a random point in time that has at least 200 candles of history
            val randomIndex = Random.nextInt(windowSize, allCandles.size)

            val history = allCandles.subList(randomIndex - windowSize, randomIndex)
            val currentPrice = allCandles[randomIndex].close
            val date = allCandles[randomIndex].timestamp

            // Reset engine state for each independent sample
            (engine as? TradingDecisionEngine)?.resetState()

            val decision = engine.evaluate(history, currentPrice)

            val mode = when(decision) {
                is Decision.Defense -> { totalDefense++; "DEFENSE" }
                is Decision.Trend -> { totalTrend++; "TREND" }
                is Decision.Range -> { totalRange++; "RANGE" }
                is Decision.Wait -> { totalWait++; "WAIT" }
            }
            
            // Print every 10th sample to keep logs clean but representative
            if (i % 10 == 0) {
                println("Sample #$i | $date | Price: $currentPrice | Mode: $mode")
            }
        }

        println("\nFinal Regime Distribution across 100 random samples:")
        println("DEFENSE: $totalDefense (${(totalDefense * 100.0 / iterations).toInt()}%)")
        println("TREND:   $totalTrend (${(totalTrend * 100.0 / iterations).toInt()}%)")
        println("RANGE:   $totalRange (${(totalRange * 100.0 / iterations).toInt()}%)")
        println("WAIT:    $totalWait (${(totalWait * 100.0 / iterations).toInt()}%)")

        // Validation: Ensure the strategy actually switches regimes
        assertTrue(totalDefense + totalTrend + totalRange + totalWait == iterations, "All iterations should produce a decision")
        assertTrue(totalDefense > 0, "Strategy should have encountered some bearish periods in 1000 days")
        assertTrue(totalTrend > 0 || totalRange > 0, "Strategy should have encountered some bullish/neutral periods")
    }
}
