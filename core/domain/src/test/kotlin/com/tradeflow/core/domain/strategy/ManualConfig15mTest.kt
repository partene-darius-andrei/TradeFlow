package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.*
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.util.BinanceDataLoader
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.sqrt

/**
 * MANUAL CONFIGURATION TEST - 15m CANDLES
 *
 * Hand-crafted configuration using insights from all optimization attempts:
 * - Volume: 1.2× (realistic, not 1.85× overfitted)
 * - ADX: 15.5 (from champion)
 * - R:R: 3:1 (fixed from inverted 0.87:1)
 * - Timeframe: 15m (cleaner than 5m)
 * - SMA: 50 (appropriate for 15m)
 */
class ManualConfig15mTest {

    @Test
    fun `manual config - 15m candles - realistic parameters`() = runBlocking {
        println("\n⚙️  MANUAL CONFIGURATION TEST (15m Candles)")
        println("=".repeat(90))
        println("Hand-crafted config using optimization insights")
        println("=".repeat(90))
        println()

        // Manual configuration based on learnings
        val config = TradingConfig(
            strategy = StrategyParameters(
                confirmationCandles = 3,              // Good balance
                adxTrendThreshold = 15.5,             // From champion
                adxRangeThreshold = 12.0,             // 3.5 point gap
                stopLossAtrMultiplier = BigDecimal("4.0"),      // Not inverted
                takeProfitAtrMultiplier = BigDecimal("12.0"),   // 3:1 R:R
                trendPositionPercent = BigDecimal("0.05"),      // 5% per trade
                gridPositionPercentPerLevel = BigDecimal("0.08"),
                leverage = BigDecimal("2.0")
            ),
            risk = RiskParameters(),
            technical = TechnicalParameters(
                minVolumeRatio = 1.2,                 // Realistic (not 1.85×)
                smaPeriod = 50                        // Good for 15m
            ),
            execution = ExecutionParameters(),
            profile = RiskProfile.BALANCED
        )

        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "15m",
            limit = 1000
        )

        println("Configuration:")
        println("  Timeframe:            15m")
        println("  Confirmation:         ${config.strategy.confirmationCandles} candles")
        println("  ADX Trend:            ${config.strategy.adxTrendThreshold}")
        println("  ADX Range:            ${config.technical.minVolumeRatio}")
        println("  Stop Loss:            ${config.strategy.stopLossAtrMultiplier}× ATR")
        println("  Take Profit:          ${config.strategy.takeProfitAtrMultiplier}× ATR")
        println("  Risk/Reward:          ${config.strategy.takeProfitAtrMultiplier.divide(config.strategy.stopLossAtrMultiplier, 1, RoundingMode.HALF_UP)}:1")
        println("  Position Size:        ${(config.strategy.trendPositionPercent.toDouble() * 100).toInt()}%")
        println("  Volume Threshold:     ${config.technical.minVolumeRatio}×")
        println("  SMA Period:           ${config.technical.smaPeriod}")
        println("  Leverage:             ${config.strategy.leverage}×")
        println()

        val primeHistory = allCandles.take(config.technical.smaPeriod)
        val testCandles = allCandles.drop(config.technical.smaPeriod)

        println("Data:")
        println("  Total Candles:    ${allCandles.size}")
        println("  Prime History:    ${primeHistory.size}")
        println("  Test Period:      ${testCandles.size} (~${testCandles.size / 4} hours)")
        println()

        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = config
        )

        val initialCapital = BigDecimal("500.00")
        var equity = initialCapital
        val openTrades = mutableListOf<NoSafeguardsBacktestTest.Trade>()
        val closedTrades = mutableListOf<NoSafeguardsBacktestTest.Trade>()
        val equityCurve = mutableListOf<BigDecimal>()

        println("🔬 SIMULATING ${testCandles.size} CANDLES...")
        println("-".repeat(90))

        testCandles.forEachIndexed { index, candle ->
            val history = (primeHistory + testCandles.take(index + 1)).takeLast(200)
            val decision = engine.execute(history, candle.close)

            // Check exits
            openTrades.filter { it.isOpen }.forEach { trade ->
                val hitStopLoss = when (trade.direction) {
                    OrderSide.BUY -> candle.low <= trade.stopLoss
                    OrderSide.SELL -> candle.high >= trade.stopLoss
                }

                val hitTakeProfit = when (trade.direction) {
                    OrderSide.BUY -> candle.high >= trade.takeProfit
                    OrderSide.SELL -> candle.low <= trade.takeProfit
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

            // Execute new signals
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
                println("  Progress: ${index + 1}/${testCandles.size} candles | " +
                    "Open: ${openTrades.size} | Closed: ${closedTrades.size} | " +
                    "Equity: \$${equity.setScale(2, RoundingMode.HALF_UP)}")
            }
        }

        // Close remaining trades
        openTrades.filter { it.isOpen }.forEach { trade ->
            trade.exitCandle = testCandles.size - 1
            trade.exitPrice = testCandles.last().close
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

        // Sharpe ratio
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
        val sharpeRatio = if (stdDev > 0.0) (avgReturn / stdDev) * sqrt(365.0 * 96.0) else 0.0  // 96 = 15m candles/day

        // Max drawdown
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
        println("📊 MANUAL CONFIG RESULTS (15m)")
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

        // Decision logic
        when {
            closedTrades.size >= 30 && pnlPercent > 0 && winRate >= 50.0 -> {
                println("\n✅ EXCELLENT! Ready for Option 2 (constrained optimization)")
                println("   ${closedTrades.size} trades, ${"%.1f".format(winRate)}% win rate, ${"%.2f".format(pnlPercent)}% return")
                println("   15m timeframe works well for this strategy!")
            }
            closedTrades.size >= 30 && winRate >= 48.0 -> {
                println("\n⚠️  PROMISING! High trade count but needs improvement")
                println("   ${closedTrades.size} trades, ${"%.1f".format(winRate)}% win rate")
                println("   → Proceed to Option 2 (constrained optimization) to find better parameters")
            }
            closedTrades.size in 10..29 && pnlPercent >= -2.0 -> {
                println("\n⚠️  MODERATE! Some trades but not enough data")
                println("   ${closedTrades.size} trades, ${"%.1f".format(winRate)}% win rate")
                println("   → Try 1h candles for more reliable signals")
            }
            closedTrades.size < 10 -> {
                println("\n❌ TOO FEW TRADES! Strategy too conservative on 15m")
                println("   Only ${closedTrades.size} trades in ${testCandles.size} candles")
                println("   → Switching to Option 3: Try 1h candles")
            }
            pnlPercent < -5.0 -> {
                println("\n❌ UNPROFITABLE! Large loss on 15m")
                println("   ${closedTrades.size} trades, ${"%.2f".format(pnlPercent)}% loss")
                println("   → Try 1h candles (cleaner, less noise)")
            }
            else -> {
                println("\n⚠️  MIXED RESULTS")
                println("   ${closedTrades.size} trades, ${"%.1f".format(winRate)}% win rate, ${"%.2f".format(pnlPercent)}% return")
                println("   → Consider trying 1h candles for better quality signals")
            }
        }
        println("=".repeat(90))
    }
}
