package com.tradeflow.core.domain.synthetic

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.indicator.TechnicalAnalysisService
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.strategy.TradingDecisionEngine
import com.tradeflow.core.domain.util.BinanceDataLoader
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertTrue

class StressTestSuite {

    data class PerformanceMetrics(
        val totalReturn: Double,
        val sharpeRatio: Double,
        val maxDrawdown: Double,
        val winRate: Double,
        val totalTrades: Int
    )

    @Test
    fun `stress test strategy across 1000 alternate timelines`() {
        val taService = TechnicalAnalysisService()
        val config = TradingConfig.forProfile(RiskProfile.BALANCED)
        val engine = TradingDecisionEngine(taService, config)

        val historicalCandles = BinanceDataLoader.fetchHistoricalCandles(interval = "4h", limit = 500)
        val generator = StationaryBootstrapGenerator(historicalCandles)

        val results = mutableListOf<PerformanceMetrics>()

        println("\n🔬 MULTIVERSE STRESS TEST")
        println("=".repeat(80))
        println("Testing strategy across 1000 alternate Bitcoin timelines")
        println("Generator: ${generator.getName()}")
        println("=".repeat(80))

        repeat(1000) { iteration ->
            val noiseLevel = (iteration / 1000.0) * 0.5
            val syntheticCandles = generator.generate(
                nSteps = 400,
                seed = iteration.toLong(),
                noiseLevel = noiseLevel
            )

            engine.resetState()

            val metrics = simulateStrategy(syntheticCandles, engine)
            results.add(metrics)

            if (iteration % 100 == 0) {
                println("Timeline #$iteration | Noise: ${(noiseLevel * 100).toInt()}% | " +
                    "Return: ${(metrics.totalReturn * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}% | " +
                    "Sharpe: ${metrics.sharpeRatio.toBigDecimal().setScale(2, RoundingMode.HALF_UP)} | " +
                    "MaxDD: ${(metrics.maxDrawdown * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}%")
            }
        }

        println("\n📊 AGGREGATE STATISTICS")
        println("=".repeat(80))

        val avgReturn = results.map { it.totalReturn }.average()
        val avgSharpe = results.map { it.sharpeRatio }.average()
        val avgDrawdown = results.map { it.maxDrawdown }.average()
        val worstDrawdown = results.maxOf { it.maxDrawdown }
        val bestReturn = results.maxOf { it.totalReturn }
        val worstReturn = results.minOf { it.totalReturn }
        val profitableTimelines = results.count { it.totalReturn > 0 }

        println("Average Return:       ${(avgReturn * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}%")
        println("Best Return:          ${(bestReturn * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}%")
        println("Worst Return:         ${(worstReturn * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}%")
        println("Average Sharpe:       ${avgSharpe.toBigDecimal().setScale(2, RoundingMode.HALF_UP)}")
        println("Average Max Drawdown: ${(avgDrawdown * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}%")
        println("Worst Max Drawdown:   ${(worstDrawdown * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}%")
        println("Profitable Timelines: $profitableTimelines/1000 (${(profitableTimelines / 10.0).toInt()}%)")
        println("=".repeat(80))

        assertTrue(
            profitableTimelines >= 550,
            "Strategy must be profitable in at least 55% of alternate timelines (got ${profitableTimelines / 10}%)"
        )

        assertTrue(
            worstDrawdown < 0.25,
            "Worst drawdown must be < 25% (got ${(worstDrawdown * 100).toInt()}%)"
        )
    }

    @Test
    fun `stress test with jump diffusion black swan events`() {
        val taService = TechnicalAnalysisService()
        val config = TradingConfig.forProfile(RiskProfile.BALANCED)
        val engine = TradingDecisionEngine(taService, config)

        val generator = JumpDiffusionGenerator(
            jumpIntensity = 0.10,
            jumpMean = -0.05,
            jumpStdDev = 0.08
        )

        val results = mutableListOf<PerformanceMetrics>()

        println("\n💥 BLACK SWAN STRESS TEST")
        println("=".repeat(80))
        println("Testing strategy with high-frequency jump events (crashes/pumps)")
        println("Jump Intensity: 10% per period | Mean: -5% | StdDev: 8%")
        println("=".repeat(80))

        repeat(500) { iteration ->
            val syntheticCandles = generator.generate(
                nSteps = 400,
                seed = iteration.toLong(),
                noiseLevel = iteration / 500.0
            )

            engine.resetState()

            val metrics = simulateStrategy(syntheticCandles, engine)
            results.add(metrics)

            if (iteration % 50 == 0) {
                println("Timeline #$iteration | " +
                    "Return: ${(metrics.totalReturn * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}% | " +
                    "MaxDD: ${(metrics.maxDrawdown * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}%")
            }
        }

        val catastrophicFailures = results.count { it.maxDrawdown > 0.20 }
        val profitableTimelines = results.count { it.totalReturn > 0 }

        println("\n📊 BLACK SWAN RESILIENCE")
        println("=".repeat(80))
        println("Profitable Timelines: $profitableTimelines/500 (${(profitableTimelines / 5.0).toInt()}%)")
        println("Catastrophic Failures (DD > 20%): $catastrophicFailures/500 (${(catastrophicFailures / 5.0).toInt()}%)")
        println("=".repeat(80))

        assertTrue(
            catastrophicFailures < 100,
            "Catastrophic failures must be < 20% (got ${(catastrophicFailures / 5.0).toInt()}%)"
        )
    }

    private fun simulateStrategy(
        candles: List<com.tradeflow.core.domain.model.Candle>,
        engine: TradingDecisionEngine
    ): PerformanceMetrics {
        var capital = 1000.0
        var btcHeld = 0.0
        var inTrade = false
        var entryPrice = 0.0
        val returns = mutableListOf<Double>()
        val equity = mutableListOf<Double>()
        var trades = 0
        var wins = 0

        candles.forEachIndexed { index, candle ->
            if (index < 200) return@forEachIndexed

            val history = candles.subList(index - 200, index)
            val currentPrice = candle.close.toDouble()

            val decision = engine.evaluate(history, candle.close)

            val currentEquity = capital + btcHeld * currentPrice
            equity.add(currentEquity)

            when (decision) {
                is Decision.Trend -> {
                    if (!inTrade && decision.direction == com.tradeflow.core.domain.model.OrderSide.BUY) {
                        val positionSize = currentEquity * 0.05
                        btcHeld = positionSize / currentPrice
                        capital -= positionSize
                        inTrade = true
                        entryPrice = currentPrice
                        trades++
                    }
                }
                is Decision.Defense -> {
                    if (inTrade) {
                        val exitValue = btcHeld * currentPrice
                        capital += exitValue
                        if (currentPrice > entryPrice) wins++
                        btcHeld = 0.0
                        inTrade = false
                    }
                }
                else -> {}
            }

            if (inTrade) {
                val slPrice = decision.let { it as? Decision.Trend }?.stopLoss?.toDouble() ?: 0.0
                val tpPrice = decision.let { it as? Decision.Trend }?.takeProfit?.toDouble() ?: Double.MAX_VALUE

                if (currentPrice <= slPrice || currentPrice >= tpPrice) {
                    val exitValue = btcHeld * currentPrice
                    capital += exitValue
                    if (currentPrice > entryPrice) wins++
                    btcHeld = 0.0
                    inTrade = false
                }
            }
        }

        val finalEquity = capital + btcHeld * candles.last().close.toDouble()
        val totalReturn = (finalEquity / 1000.0) - 1.0

        val equityReturns = equity.zipWithNext { a, b -> (b - a) / a }
        val sharpe = if (equityReturns.isNotEmpty()) {
            val avgReturn = equityReturns.average()
            val stdDev = kotlin.math.sqrt(equityReturns.map { (it - avgReturn) * (it - avgReturn) }.average())
            if (stdDev > 0) avgReturn / stdDev * kotlin.math.sqrt(252.0) else 0.0
        } else 0.0

        val maxDrawdown = calculateMaxDrawdown(equity)
        val winRate = if (trades > 0) wins.toDouble() / trades else 0.0

        return PerformanceMetrics(
            totalReturn = totalReturn,
            sharpeRatio = sharpe,
            maxDrawdown = maxDrawdown,
            winRate = winRate,
            totalTrades = trades
        )
    }

    private fun calculateMaxDrawdown(equity: List<Double>): Double {
        var maxDD = 0.0
        var peak = equity.firstOrNull() ?: 1000.0

        equity.forEach { value ->
            if (value > peak) peak = value
            val dd = (peak - value) / peak
            if (dd > maxDD) maxDD = dd
        }

        return maxDD
    }
}
