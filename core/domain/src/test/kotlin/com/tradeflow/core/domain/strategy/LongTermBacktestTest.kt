package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.StrategyParameters
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.util.BinanceDataLoader
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertTrue

class LongTermBacktestTest {

    @Test
    fun `backtest 2-7 years with optimized parameters and SHORT support`() {
        println("\n🔬 LONG-TERM BACKTEST: 2.7 YEARS (1000 DAILY CANDLES)")
        println("=".repeat(90))

        // Use ORIGINAL optimized parameters from first FAST optimization
        // (ADX 21.13, SL 9.85, TP 22.5, confirmationCandles 4)
        val optimizedStrategy = StrategyParameters(
            confirmationCandles = 4,
            adxTrendThreshold = 21.13560737380287,
            adxRangeThreshold = 1.1306769699275305,
            stopLossAtrMultiplier = BigDecimal("9.854876016062633"),
            takeProfitAtrMultiplier = BigDecimal("22.0"),
            trendPositionPercent = BigDecimal("0.0523"),
            gridPositionPercentPerLevel = BigDecimal("0.0710")
        )

        val config = TradingConfig(
            strategy = optimizedStrategy,
            risk = TradingConfig.forProfile(RiskProfile.BALANCED).risk,
            technical = TradingConfig.forProfile(RiskProfile.BALANCED).technical,
            execution = TradingConfig.forProfile(RiskProfile.BALANCED).execution,
            profile = RiskProfile.BALANCED
        )

        com.tradeflow.core.domain.repository.DependencyInjection.tradingConfig = config
        val engine = MakeTradingDecisionUseCase()

        // Fetch ~2.7 years of daily candles (1000 candles max from Binance)
        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "1d",
            limit = 1000
        )

        println("Loaded ${allCandles.size} daily candles")
        println("Period: ${allCandles.first().timestamp} to ${allCandles.last().timestamp}")
        println("Starting Price: ${allCandles.first().close}")
        println("Ending Price: ${allCandles.last().close}")
        println("=".repeat(90))

        // Simulate trading with 2x leverage
        val metrics = simulateStrategyWithLeverage(allCandles, engine, leverage = 2.0)

        println("\n📊 LONG-TERM PERFORMANCE METRICS")
        println("=".repeat(90))
        println("Total Return:       ${(metrics.totalReturn * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}%")
        println("Sharpe Ratio:       ${metrics.sharpeRatio.toBigDecimal().setScale(2, RoundingMode.HALF_UP)}")
        println("Max Drawdown:       ${(metrics.maxDrawdown * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}%")
        println("Win Rate:           ${(metrics.winRate * 100).toBigDecimal().setScale(2, RoundingMode.HALF_UP)}%")
        println("Total Trades:       ${metrics.totalTrades}")
        println("Profitable Trades:  ${metrics.winningTrades}")
        println("Losing Trades:      ${metrics.losingTrades}")
        println("=".repeat(90))

        // Success criteria for 2.7 year backtest
        assertTrue(
            metrics.totalReturn > -0.20,
            "Total return must be > -20% over 2.7 years (got ${(metrics.totalReturn * 100).toInt()}%)"
        )

        assertTrue(
            metrics.maxDrawdown < 0.30,
            "Max drawdown must be < 30% (got ${(metrics.maxDrawdown * 100).toInt()}%)"
        )

        assertTrue(
            metrics.totalTrades >= 20,
            "Should have at least 20 trades over 2.7 years (got ${metrics.totalTrades})"
        )
    }

    private fun simulateStrategyWithLeverage(
        candles: List<com.tradeflow.core.domain.model.Candle>,
        engine: MakeTradingDecisionUseCase,
        leverage: Double
    ): PerformanceMetrics {
        var capital = 1000.0
        var btcHeld = 0.0
        var inTrade = false
        var entryPrice = 0.0
        val equity = mutableListOf<Double>()
        var trades = 0
        var wins = 0
        var losses = 0

        val feeRate = 0.004 // Coinbase Advanced Trade: 0.4%

        engine.resetState()

        candles.forEachIndexed { index, candle ->
            if (index < 200) return@forEachIndexed

            val history = candles.subList(index - 200, index)
            val currentPrice = candle.close.toDouble()

            val decision = engine.execute(history, candle.close)

            // Calculate current equity (with SHORT positions handled correctly)
            val currentEquity = capital + btcHeld * currentPrice
            equity.add(currentEquity)

            when (decision) {
                is Decision.Trend -> {
                    if (!inTrade) {
                        val positionSize = currentEquity * decision.positionSizePercent.toDouble() * leverage
                        val fee = positionSize * feeRate

                        if (decision.direction == OrderSide.BUY) {
                            // LONG: Buy BTC
                            btcHeld = (positionSize - fee) / currentPrice
                            capital -= positionSize
                        } else {
                            // SHORT: Borrow and sell BTC
                            btcHeld = -((positionSize - fee) / currentPrice)
                            capital += positionSize
                        }

                        inTrade = true
                        entryPrice = currentPrice
                        trades++
                    }
                }
                is Decision.Defense -> {
                    // Legacy: Close any open position
                    if (inTrade) {
                        val exitValue = kotlin.math.abs(btcHeld) * currentPrice
                        val fee = exitValue * feeRate
                        val isLong = btcHeld > 0

                        if (isLong) {
                            capital += (exitValue - fee)
                            if (currentPrice > entryPrice) wins++ else losses++
                        } else {
                            capital -= (exitValue + fee)
                            if (currentPrice < entryPrice) wins++ else losses++
                        }

                        btcHeld = 0.0
                        inTrade = false
                    }
                }
                else -> {}
            }

            // Check SL/TP
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
                        capital += (exitValue - fee)
                        if (currentPrice > entryPrice) wins++ else losses++
                    } else {
                        capital -= (exitValue + fee)
                        if (currentPrice < entryPrice) wins++ else losses++
                    }

                    btcHeld = 0.0
                    inTrade = false
                }
            }
        }

        // Final equity calculation (handle SHORT positions)
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
            totalTrades = trades,
            winningTrades = wins,
            losingTrades = losses
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
        val totalTrades: Int,
        val winningTrades: Int,
        val losingTrades: Int
    )
}
