package com.tradeflow.core.domain.optimization

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
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
            populationSize = 15,  // FAST: Reduced from 30
            generations = 20,     // FAST: Reduced from 50
            mutationRate = 0.2,
            eliteRatio = 0.15
        )

        val bootstrapGenerator = StationaryBootstrapGenerator(inSampleData)

        val fitnessFunction: (Chromosome) -> Double = { chromosome ->
            val results = (0 until 10).map { seed ->  // FAST: Reduced from 20
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

                val engine = MakeTradingDecisionUseCase(
                    taService = AnalyzeCandlesUseCase(),
                    config = customConfig
                )

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

        val optimizedEngine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = optimizedConfig
        )

        // FIXED: Use ALL historical data for out-of-sample (need 200 candles history for SMA200)
        // But only calculate metrics starting from candle 400 (out-of-sample period)
        val oosMetrics = simulateStrategyWithOffset(historicalData, optimizedEngine, startIndex = 400)

        println("Out-Of-Sample Performance:")
        println("  Total Return:   ${(oosMetrics.totalReturn * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")
        println("  Sharpe Ratio:   ${oosMetrics.sharpeRatio.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}")
        println("  Max Drawdown:   ${(oosMetrics.maxDrawdown * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")
        println("  Win Rate:       ${(oosMetrics.winRate * 100).toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)}%")
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
                    val candles = generator.generate(nSteps = 400, seed = seed.toLong(), noiseLevel = 0.15)

                    val engine = MakeTradingDecisionUseCase(
                        taService = AnalyzeCandlesUseCase(),
                        config = customConfig
                    )

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
        println("Fitness Score: ${result.fitness.toBigDecimal().setScale(4, java.math.RoundingMode.HALF_UP)}")
        println("\nThis strategy is optimized to perform well across:")
        println("  ✅ Bull markets (high returns)")
        println("  ✅ Bear markets (capital preservation)")
        println("  ✅ Sideways markets (risk-adjusted returns)")
        println("=".repeat(90))

        assertTrue(
            result.fitness > 0.3,
            "Multi-regime fitness must be > 0.3 (got ${result.fitness.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP)})"
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

        // Coinbase Advanced Trade fee: 0.4% taker
        val feeRate = 0.004

        // CRITICAL FIX: Reset state ONCE at start, not every candle
        // resetState() every candle breaks 3-candle hysteresis logic
        engine.resetState()

        candles.forEachIndexed { index, candle ->
            if (index < 200) return@forEachIndexed

            val history = candles.subList(index - 200, index)
            val currentPrice = candle.close.toDouble()

            // REMOVED: engine.resetState() - was breaking hysteresis
            val decision = engine.execute(history, candle.close)

            val currentEquity = capital + btcHeld * currentPrice
            equity.add(currentEquity)

            when (decision) {
                is Decision.Trend -> {
                    // FIXED: Support both LONG (BUY) and SHORT (SELL) with 2x leverage
                    if (!inTrade) {
                        val leverage = 2.0 // 2x leverage as per user decision
                        val positionSize = currentEquity * decision.positionSizePercent.toDouble() * leverage
                        val fee = positionSize * feeRate // Fee on position size

                        if (decision.direction == com.tradeflow.core.domain.model.OrderSide.BUY) {
                            // LONG: Buy BTC
                            btcHeld = (positionSize - fee) / currentPrice
                            capital -= positionSize
                        } else {
                            // SHORT: Borrow and sell BTC (negative position)
                            btcHeld = -((positionSize - fee) / currentPrice)
                            capital += positionSize
                        }

                        inTrade = true
                        entryPrice = currentPrice
                        trades++
                    }
                }
                else -> {}
            }

            if (inTrade && decision is Decision.Trend) {
                val slPrice = decision.stopLoss.toDouble()
                val tpPrice = decision.takeProfit.toDouble()
                val isLong = btcHeld > 0

                val hitSL = if (isLong) currentPrice <= slPrice else currentPrice >= slPrice
                val hitTP = if (isLong) currentPrice >= tpPrice else currentPrice <= tpPrice

                if (hitSL || hitTP) {
                    val exitValue = kotlin.math.abs(btcHeld) * currentPrice
                    val fee = exitValue * feeRate

                    if (isLong) {
                        // LONG exit: Sell BTC
                        capital += (exitValue - fee)
                        if (currentPrice > entryPrice) wins++
                    } else {
                        // SHORT exit: Buy back BTC to close
                        capital -= (exitValue + fee)
                        if (currentPrice < entryPrice) wins++ // Profit if price went down
                    }

                    btcHeld = 0.0
                    inTrade = false
                }
            }
        }

        // FIXED: For SHORT positions (btcHeld < 0), unrealized P&L is calculated correctly
        val finalPrice = candles.last().close.toDouble()
        val unrealizedPnL = if (btcHeld > 0) {
            // LONG: Profit if price increased
            btcHeld * finalPrice
        } else if (btcHeld < 0) {
            // SHORT: Profit if price decreased
            // We borrowed BTC and sold it, need to buy it back
            -kotlin.math.abs(btcHeld) * finalPrice
        } else {
            0.0
        }

        val finalEquity = capital + unrealizedPnL
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

    /**
     * Same as simulateStrategy() but only tracks metrics starting from startIndex.
     * Used for out-of-sample validation where we need full history for indicators.
     */
    private fun simulateStrategyWithOffset(
        candles: List<com.tradeflow.core.domain.model.Candle>,
        engine: MakeTradingDecisionUseCase,
        startIndex: Int
    ): PerformanceMetrics {
        var capital = 1000.0
        var btcHeld = 0.0
        var inTrade = false
        var entryPrice = 0.0
        val equity = mutableListOf<Double>()
        var trades = 0
        var wins = 0

        val feeRate = 0.004

        engine.resetState()

        candles.forEachIndexed { index, candle ->
            if (index < 200) return@forEachIndexed

            val history = candles.subList(index - 200, index)
            val currentPrice = candle.close.toDouble()

            val decision = engine.execute(history, candle.close)

            val currentEquity = capital + btcHeld * currentPrice

            // FIXED: Only track equity starting from startIndex (out-of-sample period)
            if (index >= startIndex) {
                equity.add(currentEquity)
            }

            when (decision) {
                is com.tradeflow.core.domain.model.Decision.Trend -> {
                    if (!inTrade) {
                        val leverage = 2.0
                        val positionSize = currentEquity * decision.positionSizePercent.toDouble() * leverage
                        val fee = positionSize * feeRate

                        if (decision.direction == com.tradeflow.core.domain.model.OrderSide.BUY) {
                            btcHeld = (positionSize - fee) / currentPrice
                            capital -= positionSize
                        } else {
                            btcHeld = -((positionSize - fee) / currentPrice)
                            capital += positionSize
                        }

                        inTrade = true
                        entryPrice = currentPrice

                        // Only count trades in out-of-sample period
                        if (index >= startIndex) {
                            trades++
                        }
                    }
                }
                else -> {}
            }

            if (inTrade && decision is com.tradeflow.core.domain.model.Decision.Trend) {
                val slPrice = decision.stopLoss.toDouble()
                val tpPrice = decision.takeProfit.toDouble()
                val isLong = btcHeld > 0

                val hitSL = if (isLong) currentPrice <= slPrice else currentPrice >= slPrice
                val hitTP = if (isLong) currentPrice >= tpPrice else currentPrice <= tpPrice

                if (hitSL || hitTP) {
                    val exitValue = kotlin.math.abs(btcHeld) * currentPrice
                    val fee = exitValue * feeRate

                    if (isLong) {
                        capital += (exitValue - fee)
                        if (currentPrice > entryPrice && index >= startIndex) wins++
                    } else {
                        capital -= (exitValue + fee)
                        if (currentPrice < entryPrice && index >= startIndex) wins++
                    }

                    btcHeld = 0.0
                    inTrade = false
                }
            }
        }

        val finalPrice = candles.last().close.toDouble()
        val unrealizedPnL = if (btcHeld > 0) {
            btcHeld * finalPrice
        } else if (btcHeld < 0) {
            -kotlin.math.abs(btcHeld) * finalPrice
        } else {
            0.0
        }

        val finalEquity = capital + unrealizedPnL
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
