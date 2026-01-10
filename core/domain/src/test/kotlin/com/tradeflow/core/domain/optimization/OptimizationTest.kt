package com.tradeflow.core.domain.optimization

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.indicator.TechnicalAnalysisService
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.strategy.TradingDecisionEngine
import com.tradeflow.core.domain.synthetic.JumpDiffusionGenerator
import com.tradeflow.core.domain.synthetic.StationaryBootstrapGenerator
import com.tradeflow.core.domain.util.BinanceDataLoader
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertTrue

class OptimizationTest {

    @Test
    fun `walk-forward optimization with genetic algorithm`() {
        println("\n🔄 WALK-FORWARD OPTIMIZATION")
        println("=".repeat(90))

        val historicalData = BinanceDataLoader.fetchHistoricalCandles(interval = "4h", limit = 600)

        val inSampleData = historicalData.take(400)
        val outOfSampleData = historicalData.drop(400)

        println("In-Sample Data: ${inSampleData.size} candles")
        println("Out-Of-Sample Data: ${outOfSampleData.size} candles")
        println("=".repeat(90))

        val optimizer = GeneticOptimizer(
            populationSize = 30,
            generations = 50,
            mutationRate = 0.2,
            eliteRatio = 0.15
        )

        val bootstrapGenerator = StationaryBootstrapGenerator(inSampleData)

        val fitnessFunction: (Chromosome) -> Double = { chromosome ->
            val results = (0 until 20).map { seed ->
                val syntheticCandles = bootstrapGenerator.generate(
                    nSteps = 200,
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

                val taService = TechnicalAnalysisService()
                val engine = TradingDecisionEngine(taService, customConfig)

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

        println("\n✅ IN-SAMPLE OPTIMIZATION COMPLETE")
        println("Champion Fitness: ${result.fitness.toBigDecimal().setScale(4)}")

        println("\n🧪 VALIDATING ON OUT-OF-SAMPLE DATA")
        println("=".repeat(90))

        val optimizedConfig = TradingConfig(
            strategy = result.champion.toStrategyParameters(),
            risk = TradingConfig.forProfile(RiskProfile.BALANCED).risk,
            technical = TradingConfig.forProfile(RiskProfile.BALANCED).technical,
            execution = TradingConfig.forProfile(RiskProfile.BALANCED).execution,
            profile = RiskProfile.BALANCED
        )

        val taService = TechnicalAnalysisService()
        val optimizedEngine = TradingDecisionEngine(taService, optimizedConfig)

        val oosMetrics = simulateStrategy(outOfSampleData, optimizedEngine)

        println("Out-Of-Sample Performance:")
        println("  Total Return:   ${(oosMetrics.totalReturn * 100).toBigDecimal().setScale(2)}%")
        println("  Sharpe Ratio:   ${oosMetrics.sharpeRatio.toBigDecimal().setScale(2)}")
        println("  Max Drawdown:   ${(oosMetrics.maxDrawdown * 100).toBigDecimal().setScale(2)}%")
        println("  Win Rate:       ${(oosMetrics.winRate * 100).toBigDecimal().setScale(2)}%")
        println("  Total Trades:   ${oosMetrics.totalTrades}")
        println("=".repeat(90))

        assertTrue(
            oosMetrics.totalReturn > -0.10,
            "Out-of-sample return must be > -10% (got ${(oosMetrics.totalReturn * 100).toInt()}%)"
        )

        assertTrue(
            oosMetrics.maxDrawdown < 0.20,
            "Out-of-sample max drawdown must be < 20% (got ${(oosMetrics.maxDrawdown * 100).toInt()}%)"
        )
    }

    @Test
    fun `multi-regime optimization stress test`() {
        println("\n🌍 MULTI-REGIME OPTIMIZATION")
        println("=".repeat(90))

        val optimizer = GeneticOptimizer(
            populationSize = 40,
            generations = 60,
            mutationRate = 0.18
        )

        val bullMarketGenerator = JumpDiffusionGenerator(
            config = com.tradeflow.core.domain.synthetic.GenerationConfig(
                drift = 0.30,
                volatilityAnnualized = 0.60
            ),
            jumpIntensity = 0.03,
            jumpMean = 0.03
        )

        val bearMarketGenerator = JumpDiffusionGenerator(
            config = com.tradeflow.core.domain.synthetic.GenerationConfig(
                drift = -0.20,
                volatilityAnnualized = 0.90
            ),
            jumpIntensity = 0.08,
            jumpMean = -0.05,
            jumpStdDev = 0.10
        )

        val sidewaysGenerator = JumpDiffusionGenerator(
            config = com.tradeflow.core.domain.synthetic.GenerationConfig(
                drift = 0.02,
                volatilityAnnualized = 0.40
            ),
            jumpIntensity = 0.02
        )

        val fitnessFunction: (Chromosome) -> Double = { chromosome ->
            val customConfig = TradingConfig(
                strategy = chromosome.toStrategyParameters(),
                risk = TradingConfig.forProfile(RiskProfile.BALANCED).risk,
                technical = TradingConfig.forProfile(RiskProfile.BALANCED).technical,
                execution = TradingConfig.forProfile(RiskProfile.BALANCED).execution,
                profile = RiskProfile.BALANCED
            )

            val results = mutableListOf<Double>()

            listOf(
                "BULL" to bullMarketGenerator,
                "BEAR" to bearMarketGenerator,
                "SIDEWAYS" to sidewaysGenerator
            ).forEach { (regime, generator) ->
                (0 until 10).forEach { seed ->
                    val candles = generator.generate(nSteps = 250, seed = seed.toLong(), noiseLevel = 0.15)

                    val taService = TechnicalAnalysisService()
                    val engine = TradingDecisionEngine(taService, customConfig)

                    val metrics = simulateStrategy(candles, engine)

                    val fitness = when (regime) {
                        "BULL" -> 0.6 * metrics.totalReturn + 0.4 * (1.0 - metrics.maxDrawdown)
                        "BEAR" -> 0.8 * (1.0 - metrics.maxDrawdown) + 0.2 * maxOf(0.0, metrics.totalReturn)
                        else -> 0.5 * metrics.sharpeRatio / 2.0 + 0.5 * (1.0 - metrics.maxDrawdown)
                    }

                    results.add(fitness)
                }
            }

            results.average()
        }

        val result = optimizer.optimize(RiskProfile.BALANCED, fitnessFunction, seed = 123)

        println("\n🏆 MULTI-REGIME CHAMPION")
        println("=".repeat(90))
        println("Fitness Score: ${result.fitness.toBigDecimal().setScale(4)}")
        println("\nThis strategy is optimized to perform well across:")
        println("  ✅ Bull markets (high returns)")
        println("  ✅ Bear markets (capital preservation)")
        println("  ✅ Sideways markets (risk-adjusted returns)")
        println("=".repeat(90))

        assertTrue(
            result.fitness > 0.3,
            "Multi-regime fitness must be > 0.3 (got ${result.fitness.toBigDecimal().setScale(2)})"
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
        val equity = mutableListOf<Double>()
        var trades = 0
        var wins = 0

        candles.forEachIndexed { index, candle ->
            if (index < 200) return@forEachIndexed

            val history = candles.subList(index - 200, index)
            val currentPrice = candle.close.toDouble()

            engine.resetState()
            val decision = engine.evaluate(history, candle.close)

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
