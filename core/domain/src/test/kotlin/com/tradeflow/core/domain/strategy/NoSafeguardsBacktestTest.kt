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

/**
 * NO SAFEGUARDS BACKTEST
 *
 * WARNING: This removes the "one position at a time" restriction.
 * Executes EVERY trend signal without waiting for previous trades to close.
 *
 * Purpose: Show what happens when we "remove safeguards" as user requested.
 * This will generate LOTS of trades for optimization data.
 */
class NoSafeguardsBacktestTest {

    data class Trade(
        val entryCandle: Int,
        val direction: OrderSide,
        val entryPrice: BigDecimal,
        val stopLoss: BigDecimal,
        val takeProfit: BigDecimal,
        var exitCandle: Int? = null,
        var exitPrice: BigDecimal? = null,
        var exitReason: String? = null
    ) {
        val isOpen: Boolean get() = exitCandle == null

        fun calculatePnl(): BigDecimal {
            if (exitPrice == null) return BigDecimal.ZERO

            return when (direction) {
                OrderSide.BUY -> (exitPrice!! - entryPrice) / entryPrice  // LONG: profit if price rises
                OrderSide.SELL -> (entryPrice - exitPrice!!) / entryPrice  // SHORT: profit if price falls
            }
        }
    }

    @Test
    fun `no safeguards - execute EVERY signal - 5m candles`() = runBlocking {
        println("\n⚡ NO SAFEGUARDS BACKTEST (EXECUTE EVERY SIGNAL)")
        println("=".repeat(90))
        println("WARNING: Removes 'one position at a time' restriction")
        println("=".repeat(90))
        println()

        val config = TradingConfig(
            strategy = StrategyParameters(
                confirmationCandles = 1,
                adxTrendThreshold = 5.0,
                adxRangeThreshold = 3.0,
                stopLossAtrMultiplier = BigDecimal("2.0"),
                takeProfitAtrMultiplier = BigDecimal("6.0"),
                trendPositionPercent = BigDecimal("0.10"),
                gridPositionPercentPerLevel = BigDecimal("0.15"),
                leverage = BigDecimal("3.0")
            ),
            risk = RiskParameters(),
            technical = TechnicalParameters(
                minVolumeRatio = 0.1,
                smaPeriod = 20
            ),
            execution = ExecutionParameters(),
            profile = RiskProfile.BALANCED
        )

        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "5m",
            limit = 1500
        )

        val primeHistory = allCandles.take(20)
        val testCandles = allCandles.drop(20)

        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = config
        )

        val initialCapital = BigDecimal("500.00")
        var equity = initialCapital
        val openTrades = mutableListOf<Trade>()
        val closedTrades = mutableListOf<Trade>()

        println("Configuration:")
        println("  Initial Capital:      \$${initialCapital}")
        println("  Volume Threshold:     ${config.technical.minVolumeRatio}x")
        println("  Confirmation Candles: ${config.strategy.confirmationCandles}")
        println("  ADX Trend Threshold:  ${config.strategy.adxTrendThreshold}")
        println("  Stop Loss:            ${config.strategy.stopLossAtrMultiplier}× ATR")
        println("  Take Profit:          ${config.strategy.takeProfitAtrMultiplier}× ATR")
        println("  Position Size:        ${(config.strategy.trendPositionPercent.toDouble() * 100).toInt()}%")
        println("  Leverage:             ${config.strategy.leverage}×")
        println()
        println("Simulating ${testCandles.size} candles...")
        println("-".repeat(90))

        testCandles.forEachIndexed { index, candle ->
            val history = (primeHistory + testCandles.take(index + 1)).takeLast(200)
            val decision = engine.execute(history, candle.close)

            // 1. Check for exits FIRST (stop-loss or take-profit hits)
            openTrades.filter { it.isOpen }.forEach { trade ->
                val hitStopLoss = when (trade.direction) {
                    OrderSide.BUY -> candle.low <= trade.stopLoss   // LONG: Stop hit if low touches SL
                    OrderSide.SELL -> candle.high >= trade.stopLoss  // SHORT: Stop hit if high touches SL
                }

                val hitTakeProfit = when (trade.direction) {
                    OrderSide.BUY -> candle.high >= trade.takeProfit   // LONG: TP hit if high touches TP
                    OrderSide.SELL -> candle.low <= trade.takeProfit   // SHORT: TP hit if low touches TP
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
                        println("  Trade #${closedTrades.size}: ${trade.direction} CLOSED @ ${trade.exitPrice} (SL) | " +
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
                        println("  Trade #${closedTrades.size}: ${trade.direction} CLOSED @ ${trade.exitPrice} (TP) | " +
                            "PnL: ${(pnl.toDouble() * 100).let { "%.2f".format(it) }}% | Equity: \$${equity.setScale(2, RoundingMode.HALF_UP)}")
                    }
                }
            }

            // Remove closed trades from open list
            openTrades.removeAll { !it.isOpen }

            // 2. Execute NEW signals (NO "isInTrade" check)
            when (decision) {
                is Decision.Trend -> {
                    val newTrade = Trade(
                        entryCandle = index,
                        direction = decision.direction,
                        entryPrice = decision.entryPrice,
                        stopLoss = decision.stopLoss,
                        takeProfit = decision.takeProfit
                    )
                    openTrades.add(newTrade)

                    val totalTrades = closedTrades.size + openTrades.size
                    if (totalTrades <= 20) {
                        println("  Trade #${totalTrades}: ${decision.direction} OPENED @ ${decision.entryPrice} | " +
                            "SL: ${decision.stopLoss.setScale(0, RoundingMode.HALF_UP)} | " +
                            "TP: ${decision.takeProfit.setScale(0, RoundingMode.HALF_UP)}")
                    }
                }
                else -> { /* Ignore Wait and Range for now */ }
            }

            // Log progress every 200 candles
            if ((index + 1) % 200 == 0) {
                println("  Progress: ${index + 1}/${testCandles.size} candles | " +
                    "Open: ${openTrades.size} | Closed: ${closedTrades.size} | " +
                    "Equity: \$${equity.setScale(2, RoundingMode.HALF_UP)}")
            }
        }

        // Close remaining open trades at final candle price
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

        println()
        println("=".repeat(90))
        println("📊 NO SAFEGUARDS BACKTEST RESULTS")
        println("=".repeat(90))
        println("Initial Capital:  \$${initialCapital}")
        println("Final Equity:     \$${finalEquity.setScale(2, RoundingMode.HALF_UP)}")
        println("Total PnL:        ${if (totalPnl >= BigDecimal.ZERO) "+" else ""}${totalPnl.setScale(2, RoundingMode.HALF_UP)} (${if (pnlPercent >= 0) "+" else ""}${"%.2f".format(pnlPercent)}%)")
        println()
        println("Total Trades:     ${closedTrades.size}")
        println("Winning Trades:   ${winningTrades.size}")
        println("Losing Trades:    ${losingTrades.size}")
        println("Win Rate:         ${"%.0f".format(winRate)}%")
        println()

        if (winningTrades.isNotEmpty()) {
            val avgWin = winningTrades.map { it.calculatePnl().toDouble() * 100 }.average()
            println("Avg Win:          ${"%.2f".format(avgWin)}%")
        }
        if (losingTrades.isNotEmpty()) {
            val avgLoss = losingTrades.map { it.calculatePnl().toDouble() * 100 }.average()
            println("Avg Loss:         ${"%.2f".format(avgLoss)}%")
        }

        val stopLossExits = closedTrades.count { it.exitReason == "Stop Loss" }
        val takeProfitExits = closedTrades.count { it.exitReason == "Take Profit" }
        val marketCloseExits = closedTrades.count { it.exitReason == "Market Close" }

        println()
        println("Exit Reasons:")
        println("  Stop Loss:      $stopLossExits (${"%.0f".format(stopLossExits.toDouble() / closedTrades.size * 100)}%)")
        println("  Take Profit:    $takeProfitExits (${"%.0f".format(takeProfitExits.toDouble() / closedTrades.size * 100)}%)")
        println("  Market Close:   $marketCloseExits (${"%.0f".format(marketCloseExits.toDouble() / closedTrades.size * 100)}%)")
        println("=".repeat(90))

        if (closedTrades.size > 100) {
            println("\n✅ SUCCESS! Generated ${closedTrades.size} trades for optimization")
            if (pnlPercent > 0) {
                println("   BONUS: Strategy is PROFITABLE without safeguards!")
            } else {
                println("   Strategy unprofitable, but NOW WE HAVE DATA to optimize")
            }
        } else if (closedTrades.size > 20) {
            println("\n⚠️  Generated ${closedTrades.size} trades (moderate)")
        } else {
            println("\n❌ Still too few trades (${closedTrades.size})")
        }
    }
}
