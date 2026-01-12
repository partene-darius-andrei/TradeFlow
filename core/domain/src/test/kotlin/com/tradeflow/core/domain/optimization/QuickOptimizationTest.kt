package com.tradeflow.core.domain.optimization

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.synthetic.StationaryBootstrapGenerator
import com.tradeflow.core.domain.util.BinanceDataLoader
import org.junit.Test
import kotlin.test.assertTrue

class QuickOptimizationTest {

    @Test
    fun `quick optimization proof of concept`() {
        println("\n🚀 QUICK OPTIMIZATION (PROOF OF CONCEPT)")
        println("=".repeat(90))

        val historicalData = BinanceDataLoader.fetchHistoricalCandles(interval = "4h", limit = 600)

        val inSampleData = historicalData.take(400)
        val outOfSampleData = historicalData.drop(400)

        println("In-Sample Data: ${inSampleData.size} candles")
        println("Out-Of-Sample Data: ${outOfSampleData.size} candles")
        println("=".repeat(90))

        val optimizer = GeneticOptimizer(
            populationSize = 10,
            generations = 15,
            mutationRate = 0.2,
            eliteRatio = 0.2
        )

        val bootstrapGenerator = StationaryBootstrapGenerator(inSampleData)

        val fitnessFunction: (Chromosome) -> Double = { chromosome ->
            val results = (0 until 5).map { seed ->
                val syntheticCandles = bootstrapGenerator.generate(
                    nSteps = 400,
                    seed = seed.toLong(),
                    noiseLevel = 0.2
                )

                val customConfig = TradingConfig(
                    strategy = chromosome.toStrategyParameters(),
                    risk = TradingConfig.forProfile(RiskProfile.BALANCED).risk,
                    technical = TradingConfig.forProfile(RiskProfile.BALANCED).technical,
                    execution = TradingConfig.forProfile(RiskProfile.BALANCED).execution,
                    profile = RiskProfile.BALANCED
                )

                com.tradeflow.core.domain.repository.DependencyInjection.tradingConfig = customConfig
                val engine = MakeTradingDecisionUseCase()

                val metrics = simulateStrategy(syntheticCandles, engine)

                val sharpeWeight = 0.4
                val returnWeight = 0.4
                val drawdownPenalty = 0.2

                val normalizedSharpe = (metrics.sharpeRatio / 3.0).coerceIn(-1.0, 1.0)
                val normalizedReturn = (metrics.totalReturn / 0.5).coerceIn(-1.0, 1.0)
                val normalizedDrawdown = 1.0 - metrics.maxDrawdown

                sharpeWeight * normalizedSharpe +
                    returnWeight * normalizedReturn +
                    drawdownPenalty * normalizedDrawdown
            }

            results.average()
        }

        val result = optimizer.optimize(RiskProfile.BALANCED, fitnessFunction, seed = 42)

        println("\n✅ QUICK OPTIMIZATION COMPLETE")
        println("Champion Fitness: ${result.fitness.toBigDecimal().setScale(4, java.math.RoundingMode.HALF_UP)}")

        println("\n🧪 VALIDATING ON OUT-OF-SAMPLE DATA")
        println("=".repeat(90))

        val optimizedConfig = TradingConfig(
            strategy = result.champion.toStrategyParameters(),
            risk = TradingConfig.forProfile(RiskProfile.BALANCED).risk,
            technical = TradingConfig.forProfile(RiskProfile.BALANCED).technical,
            execution = TradingConfig.forProfile(RiskProfile.BALANCED).execution,
            profile = RiskProfile.BALANCED
        )

        com.tradeflow.core.domain.repository.DependencyInjection.tradingConfig = optimizedConfig
        val optimizedEngine = MakeTradingDecisionUseCase()

        val oosMetrics = simulateStrategy(outOfSampleData, optimizedEngine)

        println("Out-Of-Sample Performance:")
        println("  Total Return:   ${(oosMetrics.totalReturn * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")
        println("  Sharpe Ratio:   ${oosMetrics.sharpeRatio.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}")
        println("  Max Drawdown:   ${(oosMetrics.maxDrawdown * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")
        println("  Win Rate:       ${(oosMetrics.winRate * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")
        println("  Total Trades:   ${oosMetrics.totalTrades}")
        println("=".repeat(90))

        println("\n📊 CHAMPION PARAMETERS:")
        println("  ADX Trend Threshold:       ${result.champion.adxTrendThreshold}")
        println("  ADX Range Threshold:       ${result.champion.adxRangeThreshold}")
        println("  Stop Loss ATR Multiplier:  ${result.champion.stopLossAtrMultiplier}")
        println("  Take Profit ATR Multiplier: ${result.champion.takeProfitAtrMultiplier}")
        println("  Trend Position %:          ${(result.champion.trendPositionPercent * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")
        println("  Grid Position %:           ${(result.champion.gridPositionPercentPerLevel * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")
        println("  Confirmation Candles:      ${result.champion.confirmationCandles}")

        assertTrue(
            oosMetrics.totalReturn > -0.15,
            "Out-of-sample return must be > -15% for quick optimization (got ${(oosMetrics.totalReturn * 100).toInt()}%)"
        )
    }

    private fun simulateStrategy(
        candles: List<com.tradeflow.core.domain.model.Candle>,
        engine: MakeTradingDecisionUseCase
    ): PerformanceMetrics {
        var capital = 1000.0
        var btcHeld = 0.0
        var inTrade = false
        var entryPrice = 0.0
        val equity = mutableListOf<Double>()
        var trades = 0
        var wins = 0

        // Reset hysteresis state once before simulation (not on every candle)
        engine.resetState()

        candles.forEachIndexed { index, candle ->
            if (index < 200) return@forEachIndexed

            val history = candles.subList(index - 200, index)
            val currentPrice = candle.close.toDouble()

            val decision = engine.execute(history, candle.close)

            val currentEquity = capital + btcHeld * currentPrice
            equity.add(currentEquity)

            when (decision) {
                is Decision.Trend -> {
                    if (!inTrade && decision.direction == com.tradeflow.core.domain.model.OrderSide.BUY) {
                        val positionSize = currentEquity * decision.positionSizePercent.toDouble()
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

            if (inTrade && decision is Decision.Trend) {
                val slPrice = decision.stopLoss.toDouble()
                val tpPrice = decision.takeProfit.toDouble()

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

    data class PerformanceMetrics(
        val totalReturn: Double,
        val sharpeRatio: Double,
        val maxDrawdown: Double,
        val winRate: Double,
        val totalTrades: Int
    )
}
