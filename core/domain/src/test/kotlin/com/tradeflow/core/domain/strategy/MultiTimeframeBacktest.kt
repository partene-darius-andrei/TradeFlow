package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.*
import com.tradeflow.core.domain.usecase.MultiTimeframeDecisionEngine
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.util.BinanceDataLoader
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.sqrt

class MultiTimeframeBacktest {

    @Test
    fun `multi-timeframe hierarchical filtering - 1h-15m-5m-1m`() = runBlocking {
        println("\n🌐 MULTI-TIMEFRAME BACKTEST (Hierarchical Filtering)")
        println("=".repeat(90))
        println("Testing 1h → 15m → 5m → 1m confluence for higher win rate")
        println("=".repeat(90))
        println()

        val config = TradingConfig(
            strategy = StrategyParameters(
                confirmationCandles = 2,
                adxTrendThreshold = 18.0,
                adxRangeThreshold = 14.0,
                stopLossAtrMultiplier = BigDecimal("3.0"),
                takeProfitAtrMultiplier = BigDecimal("9.0"),
                trendPositionPercent = BigDecimal("0.05"),
                gridPositionPercentPerLevel = BigDecimal("0.08"),
                leverage = BigDecimal("2.0")
            ),
            risk = RiskParameters(),
            technical = TechnicalParameters(
                minVolumeRatio = 1.0,
                smaPeriod = 100
            ),
            execution = ExecutionParameters(),
            profile = RiskProfile.BALANCED
        )

        println("⏳ Fetching multi-timeframe data in parallel...")
        val candles1h = async {
            BinanceDataLoader.fetchHistoricalCandles("BTCUSDT", "1h", 800)
        }
        val candles15m = async {
            BinanceDataLoader.fetchHistoricalCandles("BTCUSDT", "15m", 1000)
        }
        val candles5m = async {
            BinanceDataLoader.fetchHistoricalCandles("BTCUSDT", "5m", 1000)
        }
        val candles1m = async {
            BinanceDataLoader.fetchHistoricalCandles("BTCUSDT", "1m", 1000)
        }

        val all1h = candles1h.await()
        val all15m = candles15m.await()
        val all5m = candles5m.await()
        val all1m = candles1m.await()

        println("✅ Data loaded:")
        println("  1h:  ${all1h.size} candles")
        println("  15m: ${all15m.size} candles")
        println("  5m:  ${all5m.size} candles")
        println("  1m:  ${all1m.size} candles")
        println()

        val prime1h = all1h.take(200)
        val prime15m = all15m.take(200)
        val prime5m = all5m.take(200)
        val prime1m = all1m.take(200)

        val test5m = all5m.drop(200)

        println("Test Configuration:")
        println("  Base Timeframe:       5m (execution)")
        println("  Test Period:          ${test5m.size} candles (~${test5m.size * 5 / 60} hours)")
        println("  ADX Trend:            ${config.strategy.adxTrendThreshold}")
        println("  ADX Range:            ${config.strategy.adxRangeThreshold}")
        println("  Stop Loss:            ${config.strategy.stopLossAtrMultiplier}× ATR")
        println("  Take Profit:          ${config.strategy.takeProfitAtrMultiplier}× ATR")
        println("  Risk/Reward:          3:1")
        println()

        val engine = MultiTimeframeDecisionEngine(config)

        val initialCapital = BigDecimal("500.00")
        var equity = initialCapital
        val openTrades = mutableListOf<NoSafeguardsBacktestTest.Trade>()
        val closedTrades = mutableListOf<NoSafeguardsBacktestTest.Trade>()
        val equityCurve = mutableListOf<BigDecimal>()

        println("🔬 SIMULATING ${test5m.size} CANDLES (Multi-Timeframe Analysis)...")
        println("-".repeat(90))

        test5m.forEachIndexed { index, candle5m ->
            val history5m = (prime5m + test5m.take(index + 1)).takeLast(200)

            val index1h = (200 + (index / 12)).coerceAtMost(all1h.size - 1)
            val history1h = all1h.take(index1h + 1).takeLast(200)

            val index15m = (200 + (index / 3)).coerceAtMost(all15m.size - 1)
            val history15m = all15m.take(index15m + 1).takeLast(200)

            val index1m = (200 + (index * 5)).coerceAtMost(all1m.size - 1)
            val history1m = all1m.take(index1m + 1).takeLast(200)

            if (history1h.size < 100 || history15m.size < 100 || history5m.size < 100 || history1m.size < 100) {
                equityCurve.add(equity)
                return@forEachIndexed
            }

            val mtfCandles = MultiTimeframeDecisionEngine.MultiTimeframeCandles(
                candles1h = history1h,
                candles15m = history15m,
                candles5m = history5m,
                candles1m = history1m,
                currentPrice = candle5m.close
            )

            val decision = engine.execute(mtfCandles)

            openTrades.filter { it.isOpen }.forEach { trade ->
                val hitStopLoss = when (trade.direction) {
                    OrderSide.BUY -> candle5m.low <= trade.stopLoss
                    OrderSide.SELL -> candle5m.high >= trade.stopLoss
                }

                val hitTakeProfit = when (trade.direction) {
                    OrderSide.BUY -> candle5m.high >= trade.takeProfit
                    OrderSide.SELL -> candle5m.low <= trade.takeProfit
                }

                if (hitStopLoss) {
                    trade.exitCandle = index
                    trade.exitPrice = trade.stopLoss
                    trade.exitReason = "Stop Loss"
                    closedTrades.add(trade)
                    val pnl = trade.calculatePnl()
                    val pnlUsd = equity * pnl * config.strategy.trendPositionPercent
                    equity += pnlUsd

                    if (closedTrades.size <= 20) {
                        println("  Trade #${closedTrades.size}: ${trade.direction} CLOSED @ ${trade.exitPrice?.setScale(2, RoundingMode.HALF_UP)} (SL) | " +
                            "PnL: ${(pnl.toDouble() * 100).let { "%.2f".format(it) }}% | Equity: \$${equity.setScale(2, RoundingMode.HALF_UP)}")
                    }
                } else if (hitTakeProfit) {
                    trade.exitCandle = index
                    trade.exitPrice = trade.takeProfit
                    trade.exitReason = "Take Profit"
                    closedTrades.add(trade)
                    val pnl = trade.calculatePnl()
                    val pnlUsd = equity * pnl * config.strategy.trendPositionPercent
                    equity += pnlUsd

                    if (closedTrades.size <= 20) {
                        println("  Trade #${closedTrades.size}: ${trade.direction} CLOSED @ ${trade.exitPrice?.setScale(2, RoundingMode.HALF_UP)} (TP) | " +
                            "PnL: ${(pnl.toDouble() * 100).let { "%.2f".format(it) }}% | Equity: \$${equity.setScale(2, RoundingMode.HALF_UP)}")
                    }
                }
            }

            openTrades.removeAll { !it.isOpen }

            when (decision) {
                is Decision.Trend -> {
                    val newTrade = NoSafeguardsBacktestTest.Trade(
                        entryCandle = index,
                        direction = decision.direction,
                        entryPrice = decision.entryPrice,
                        stopLoss = decision.stopLoss,
                        takeProfit = decision.takeProfit
                    )
                    openTrades.add(newTrade)

                    val totalTrades = closedTrades.size + openTrades.size
                    if (totalTrades <= 20) {
                        println("  Trade #${totalTrades}: ${decision.direction} OPENED @ ${decision.entryPrice.setScale(2, RoundingMode.HALF_UP)} | " +
                            "SL: ${decision.stopLoss.setScale(2, RoundingMode.HALF_UP)} | " +
                            "TP: ${decision.takeProfit.setScale(2, RoundingMode.HALF_UP)}")
                    }
                }
                else -> {}
            }

            equityCurve.add(equity)

            if ((index + 1) % 200 == 0) {
                println("  Progress: ${index + 1}/${test5m.size} candles | " +
                    "Open: ${openTrades.size} | Closed: ${closedTrades.size} | " +
                    "Equity: \$${equity.setScale(2, RoundingMode.HALF_UP)}")
            }
        }

        openTrades.filter { it.isOpen }.forEach { trade ->
            trade.exitCandle = test5m.size - 1
            trade.exitPrice = test5m.last().close
            trade.exitReason = "Market Close"
            closedTrades.add(trade)
            val pnl = trade.calculatePnl()
            val pnlUsd = equity * pnl * config.strategy.trendPositionPercent
            equity += pnlUsd
        }

        val finalEquity = equity
        val totalPnl = finalEquity - initialCapital
        val pnlPercent = (totalPnl / initialCapital).toDouble() * 100

        val winningTrades = closedTrades.filter { it.calculatePnl() > BigDecimal.ZERO }
        val losingTrades = closedTrades.filter { it.calculatePnl() <= BigDecimal.ZERO }
        val winRate = if (closedTrades.isNotEmpty()) (winningTrades.size.toDouble() / closedTrades.size * 100) else 0.0

        val avgWin = if (winningTrades.isNotEmpty()) {
            winningTrades.map { it.calculatePnl().toDouble() * 100 }.average()
        } else 0.0

        val avgLoss = if (losingTrades.isNotEmpty()) {
            losingTrades.map { it.calculatePnl().toDouble() * 100 }.average()
        } else 0.0

        val totalWins = winningTrades.sumOf { it.calculatePnl().toDouble() }
        val totalLosses = losingTrades.sumOf { it.calculatePnl().toDouble() }.let { kotlin.math.abs(it) }
        val profitFactor = if (totalLosses > 0.0) totalWins / totalLosses else 0.0

        val returns = mutableListOf<Double>()
        for (i in 1 until equityCurve.size) {
            val ret = (equityCurve[i] - equityCurve[i - 1])
                .divide(equityCurve[i - 1], 6, RoundingMode.HALF_UP)
                .toDouble()
            returns.add(ret)
        }
        val avgReturn = if (returns.isNotEmpty()) returns.average() else 0.0
        val stdDev = if (returns.size > 1) {
            sqrt(returns.map { (it - avgReturn) * (it - avgReturn) }.average())
        } else 0.0
        val sharpeRatio = if (stdDev > 0.0) (avgReturn / stdDev) * sqrt(365.0 * 288.0) else 0.0

        var maxDrawdown = 0.0
        var peak = initialCapital
        equityCurve.forEach { eq ->
            if (eq > peak) peak = eq
            val dd = if (peak > BigDecimal.ZERO) {
                ((peak - eq).divide(peak, 4, RoundingMode.HALF_UP).toDouble() * 100)
            } else 0.0
            if (dd > maxDrawdown) maxDrawdown = dd
        }

        val stopLossExits = closedTrades.count { it.exitReason == "Stop Loss" }
        val takeProfitExits = closedTrades.count { it.exitReason == "Take Profit" }
        val marketCloseExits = closedTrades.count { it.exitReason == "Market Close" }

        println()
        println("=".repeat(90))
        println("📊 MULTI-TIMEFRAME RESULTS")
        println("=".repeat(90))
        println("Initial Capital:  \$${initialCapital}")
        println("Final Equity:     \$${finalEquity.setScale(2, RoundingMode.HALF_UP)}")
        println("Total PnL:        ${if (totalPnl >= BigDecimal.ZERO) "+" else ""}${totalPnl.setScale(2, RoundingMode.HALF_UP)} (${if (pnlPercent >= 0) "+" else ""}${"%.2f".format(pnlPercent)}%)")
        println()
        println("Total Trades:     ${closedTrades.size}")
        println("Winning Trades:   ${winningTrades.size}")
        println("Losing Trades:    ${losingTrades.size}")
        println("Win Rate:         ${"%.1f".format(winRate)}%")
        println()
        if (winningTrades.isNotEmpty()) println("Avg Win:          ${"%.2f".format(avgWin)}%")
        if (losingTrades.isNotEmpty()) println("Avg Loss:         ${"%.2f".format(avgLoss)}%")
        println("Profit Factor:    ${"%.2f".format(profitFactor)}")
        println("Sharpe Ratio:     ${"%.2f".format(sharpeRatio)}")
        println("Max Drawdown:     ${"%.2f".format(maxDrawdown)}%")
        println()
        println("Exit Reasons:")
        if (closedTrades.isNotEmpty()) {
            println("  Stop Loss:      $stopLossExits (${"%.0f".format(stopLossExits.toDouble() / closedTrades.size * 100)}%)")
            println("  Take Profit:    $takeProfitExits (${"%.0f".format(takeProfitExits.toDouble() / closedTrades.size * 100)}%)")
            println("  Market Close:   $marketCloseExits (${"%.0f".format(marketCloseExits.toDouble() / closedTrades.size * 100)}%)")
        }
        println("=".repeat(90))

        println()
        println("📈 COMPARISON TO SINGLE TIMEFRAME:")
        println("  1h alone:         30.9% win rate, 181 trades, -0.96% PnL")
        println("  Multi-TF:         ${"%.1f".format(winRate)}% win rate, ${closedTrades.size} trades, ${"%.2f".format(pnlPercent)}% PnL")
        println()

        when {
            winRate >= 48.0 && closedTrades.size >= 30 -> {
                println("✅ BREAKTHROUGH! Multi-timeframe filtering works!")
                println("   Win rate improved from 30.9% → ${"%.1f".format(winRate)}%")
                println("   Trade quality: ${takeProfitExits} wins vs ${stopLossExits} losses")
                println("   → This is the edge we needed!")
            }
            winRate >= 45.0 && closedTrades.size >= 20 -> {
                println("⚠️  PROMISING! Win rate improved, needs refinement")
                println("   Better than single timeframe but not profitable yet")
                println("   → Consider adding more filters or adjusting thresholds")
            }
            closedTrades.size < 10 -> {
                println("⚠️  TOO CONSERVATIVE! Multi-TF filtering too strict")
                println("   Only ${closedTrades.size} trades (need 30+ for statistical significance)")
                println("   → Loosen confirmation requirements")
            }
            else -> {
                println("❌ NOT WORKING - Multi-TF didn't improve performance")
                println("   Win rate: ${"%.1f".format(winRate)}% (target: 48%+)")
                println("   → May need different approach or indicators")
            }
        }
        println("=".repeat(90))
    }
}
