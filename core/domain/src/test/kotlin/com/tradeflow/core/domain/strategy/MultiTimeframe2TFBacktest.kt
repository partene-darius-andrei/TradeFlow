package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.*
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.util.BinanceDataLoader
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.sqrt

class MultiTimeframe2TFBacktest {

    @Test
    fun `multi-timeframe 1h-15m confirmation filter`() = runBlocking {
        println("\n🌐 MULTI-TIMEFRAME BACKTEST (1h + 15m Confirmation)")
        println("=".repeat(90))
        println("Testing 1h regime + 15m confirmation for higher win rate")
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

        println("⏳ Fetching data...")
        val candles1h = async {
            BinanceDataLoader.fetchHistoricalCandles("BTCUSDT", "1h", 800)
        }
        val candles15m = async {
            BinanceDataLoader.fetchHistoricalCandles("BTCUSDT", "15m", 1000)
        }

        val all1h = candles1h.await()
        val all15m = candles15m.await()

        println("✅ Data loaded:")
        println("  1h:  ${all1h.size} candles")
        println("  15m: ${all15m.size} candles")
        println()

        val prime1h = all1h.take(300)
        val prime15m = all15m.take(300)

        val test15m = all15m.drop(300)

        println("Test Configuration:")
        println("  Strategy:             1h regime → 15m entry")
        println("  Test Period:          ${test15m.size} candles (~${test15m.size / 4} hours)")
        println("  ADX Trend:            ${config.strategy.adxTrendThreshold}")
        println("  ADX Range:            ${config.strategy.adxRangeThreshold}")
        println("  Stop Loss:            ${config.strategy.stopLossAtrMultiplier}× ATR")
        println("  Take Profit:          ${config.strategy.takeProfitAtrMultiplier}× ATR")
        println()

        val engine1h = MakeTradingDecisionUseCase(AnalyzeCandlesUseCase(), config)
        val engine15m = MakeTradingDecisionUseCase(AnalyzeCandlesUseCase(), config)

        val initialCapital = BigDecimal("500.00")
        var equity = initialCapital
        val openTrades = mutableListOf<NoSafeguardsBacktestTest.Trade>()
        val closedTrades = mutableListOf<NoSafeguardsBacktestTest.Trade>()
        val equityCurve = mutableListOf<BigDecimal>()

        println("🔬 SIMULATING ${test15m.size} CANDLES...")
        println("-".repeat(90))

        test15m.forEachIndexed { index, candle15m ->
            val history15m = (prime15m + test15m.take(index + 1)).takeLast(200)

            val index1h = (300 + (index / 4)).coerceAtMost(all1h.size - 1)
            val history1h = all1h.take(index1h + 1).takeLast(200)

            if (history1h.size < 200 || history15m.size < 200) {
                equityCurve.add(equity)
                return@forEachIndexed
            }

            openTrades.filter { it.isOpen }.forEach { trade ->
                val hitStopLoss = when (trade.direction) {
                    OrderSide.BUY -> candle15m.low <= trade.stopLoss
                    OrderSide.SELL -> candle15m.high >= trade.stopLoss
                }

                val hitTakeProfit = when (trade.direction) {
                    OrderSide.BUY -> candle15m.high >= trade.takeProfit
                    OrderSide.SELL -> candle15m.low <= trade.takeProfit
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

            val regime1h = engine1h.execute(history1h, candle15m.close)
            val decision15m = engine15m.execute(history15m, candle15m.close)

            val canTrade = when {
                regime1h is Decision.Wait -> false
                decision15m is Decision.Wait -> false
                regime1h is Decision.Trend && decision15m is Decision.Trend ->
                    regime1h.direction == decision15m.direction
                else -> false
            }

            if (canTrade && decision15m is Decision.Trend) {
                val newTrade = NoSafeguardsBacktestTest.Trade(
                    entryCandle = index,
                    direction = decision15m.direction,
                    entryPrice = decision15m.entryPrice,
                    stopLoss = decision15m.stopLoss,
                    takeProfit = decision15m.takeProfit
                )
                openTrades.add(newTrade)

                val totalTrades = closedTrades.size + openTrades.size
                if (totalTrades <= 20) {
                    println("  Trade #${totalTrades}: ${decision15m.direction} OPENED @ ${decision15m.entryPrice.setScale(2, RoundingMode.HALF_UP)} | " +
                        "SL: ${decision15m.stopLoss.setScale(2, RoundingMode.HALF_UP)} | " +
                        "TP: ${decision15m.takeProfit.setScale(2, RoundingMode.HALF_UP)}")
                }
            }

            equityCurve.add(equity)

            if ((index + 1) % 100 == 0) {
                println("  Progress: ${index + 1}/${test15m.size} candles | " +
                    "Open: ${openTrades.size} | Closed: ${closedTrades.size} | " +
                    "Equity: \$${equity.setScale(2, RoundingMode.HALF_UP)}")
            }
        }

        openTrades.filter { it.isOpen }.forEach { trade ->
            trade.exitCandle = test15m.size - 1
            trade.exitPrice = test15m.last().close
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
        val sharpeRatio = if (stdDev > 0.0) (avgReturn / stdDev) * sqrt(365.0 * 96.0) else 0.0

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
        println("📊 MULTI-TIMEFRAME RESULTS (1h + 15m)")
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
        println("📈 COMPARISON:")
        println("  15m alone:        19.5% win rate, 185 trades, -3.28% PnL")
        println("  1h alone:         30.9% win rate, 181 trades, -0.96% PnL")
        println("  Multi-TF (1h+15m): ${"%.1f".format(winRate)}% win rate, ${closedTrades.size} trades, ${"%.2f".format(pnlPercent)}% PnL")
        println()

        when {
            winRate >= 48.0 && closedTrades.size >= 30 -> {
                println("✅ BREAKTHROUGH! Multi-timeframe filtering works!")
                println("   Win rate improved to ${"%.1f".format(winRate)}%")
                println("   → This is the edge we needed!")
            }
            winRate >= 40.0 && closedTrades.size >= 20 -> {
                println("⚠️  PROMISING! Improvement but not profitable yet")
                println("   Win rate: ${"%.1f".format(winRate)}% (need 48%+)")
                println("   → Consider further optimization")
            }
            closedTrades.size < 20 -> {
                println("⚠️  TOO CONSERVATIVE! Not enough trades")
                println("   Only ${closedTrades.size} trades")
                println("   → Loosen filters")
            }
            else -> {
                println("❌ NO IMPROVEMENT over single timeframe")
                println("   Win rate: ${"%.1f".format(winRate)}%")
                println("   → Strategy may not be viable")
            }
        }
        println("=".repeat(90))
    }
}
