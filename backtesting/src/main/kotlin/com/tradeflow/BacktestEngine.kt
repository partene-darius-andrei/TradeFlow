package com.tradeflow

import com.tradeflow.core.domain.TradingConfig
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.usecase.MultiTimeframeDecisionUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.sqrt

data class Trade(
    val direction: OrderSide,
    val entryPrice: BigDecimal,
    val stopLoss: BigDecimal,
    val takeProfit: BigDecimal,
    var exitPrice: BigDecimal? = null,
    var exitReason: String? = null
) {
    val isOpen: Boolean get() = exitPrice == null

    fun calculatePnl(): BigDecimal {
        val exit = exitPrice ?: return BigDecimal.ZERO
        return when (direction) {
            OrderSide.BUY -> (exit - entryPrice).divide(entryPrice, 6, RoundingMode.HALF_UP)
            OrderSide.SELL -> (entryPrice - exit).divide(entryPrice, 6, RoundingMode.HALF_UP)
        }
    }
}

data class BacktestResult(
    val initialCapital: BigDecimal,
    val finalEquity: BigDecimal,
    val totalPnl: BigDecimal,
    val pnlPercent: Double,
    val trades: List<Trade>,
    val winningTrades: List<Trade>,
    val losingTrades: List<Trade>,
    val winRate: Double,
    val avgWin: Double,
    val avgLoss: Double,
    val profitFactor: Double,
    val sharpeRatio: Double,
    val maxDrawdown: Double,
    val equityCurve: List<BigDecimal>
)

class BacktestEngine(
    private val initialCapital: BigDecimal = BigDecimal("500.00")
) {
    private val multiTimeframeEngine = MultiTimeframeDecisionUseCase()

    fun execute(
        all1h: List<Candle>,
        all15m: List<Candle>,
        primeSize: Int = 300,
        verbose: Boolean = false
    ): BacktestResult {
        val prime15m = all15m.take(primeSize)
        val test15m = all15m.drop(primeSize)

        var equity = initialCapital
        val openTrades = mutableListOf<Trade>()
        val closedTrades = mutableListOf<Trade>()
        val equityCurve = mutableListOf<BigDecimal>()

        test15m.forEachIndexed { index, candle15m ->
            val history15m = (prime15m + test15m.take(index + 1)).takeLast(200)
            val index1h = (primeSize + (index / 4)).coerceAtMost(all1h.size - 1)
            val history1h = all1h.take(index1h + 1).takeLast(200)

            if (history1h.size < 200 || history15m.size < 200) {
                equityCurve.add(equity)
                return@forEachIndexed
            }

            // Check exits
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
                    trade.exitPrice = trade.stopLoss
                    trade.exitReason = "Stop Loss"
                    closedTrades.add(trade)
                    val pnl = trade.calculatePnl()
                    val pnlUsd = equity * pnl * TradingConfig.Strategy.trendPositionPercent
                    equity += pnlUsd

                    if (verbose && closedTrades.size <= 20) {
                        println("  Trade #${closedTrades.size}: ${trade.direction} CLOSED @ ${trade.exitPrice?.setScale(2, RoundingMode.HALF_UP)} (SL) | " +
                            "PnL: ${(pnl.toDouble() * 100).let { "%.2f".format(it) }}% | Equity: \$${equity.setScale(2, RoundingMode.HALF_UP)}")
                    }
                } else if (hitTakeProfit) {
                    trade.exitPrice = trade.takeProfit
                    trade.exitReason = "Take Profit"
                    closedTrades.add(trade)
                    val pnl = trade.calculatePnl()
                    val pnlUsd = equity * pnl * TradingConfig.Strategy.trendPositionPercent
                    equity += pnlUsd

                    if (verbose && closedTrades.size <= 20) {
                        println("  Trade #${closedTrades.size}: ${trade.direction} CLOSED @ ${trade.exitPrice?.setScale(2, RoundingMode.HALF_UP)} (TP) | " +
                            "PnL: ${(pnl.toDouble() * 100).let { "%.2f".format(it) }}% | Equity: \$${equity.setScale(2, RoundingMode.HALF_UP)}")
                    }
                }
            }

            openTrades.removeAll { !it.isOpen }

            // Execute new signals using multi-timeframe confluence logic
            val decision = multiTimeframeEngine.execute(
                MultiTimeframeDecisionUseCase.MultiTimeframeCandles(
                    candles1h = history1h,
                    candles15m = history15m,
                    currentPrice = candle15m.close
                )
            )

            if (decision is Decision.Trend) {
                val newTrade = Trade(
                    direction = decision.direction,
                    entryPrice = decision.entryPrice,
                    stopLoss = decision.stopLoss,
                    takeProfit = decision.takeProfit
                )
                openTrades.add(newTrade)

                val totalTrades = closedTrades.size + openTrades.size
                if (verbose && totalTrades <= 20) {
                    println("  Trade #${totalTrades}: ${decision.direction} OPENED @ ${decision.entryPrice.setScale(2, RoundingMode.HALF_UP)} | " +
                        "SL: ${decision.stopLoss.setScale(2, RoundingMode.HALF_UP)} | " +
                        "TP: ${decision.takeProfit.setScale(2, RoundingMode.HALF_UP)}")
                }
            }

            equityCurve.add(equity)

            if (verbose && (index + 1) % 200 == 0) {
                println("  Progress: ${index + 1}/${test15m.size} candles | " +
                    "Open: ${openTrades.size} | Closed: ${closedTrades.size} | " +
                    "Equity: \$${equity.setScale(2, RoundingMode.HALF_UP)}")
            }
        }

        // Close remaining trades
        openTrades.filter { it.isOpen }.forEach { trade ->
            trade.exitPrice = test15m.last().close
            trade.exitReason = "Market Close"
            closedTrades.add(trade)
            val pnl = trade.calculatePnl()
            val pnlUsd = equity * pnl * TradingConfig.Strategy.trendPositionPercent
            equity += pnlUsd
        }

        return calculateMetrics(initialCapital, equity, closedTrades, equityCurve)
    }

    private fun calculateMetrics(
        initialCapital: BigDecimal,
        finalEquity: BigDecimal,
        closedTrades: List<Trade>,
        equityCurve: List<BigDecimal>
    ): BacktestResult {
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
        val totalLosses = losingTrades.sumOf { it.calculatePnl().toDouble() }.let { abs(it) }
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

        return BacktestResult(
            initialCapital = initialCapital,
            finalEquity = finalEquity,
            totalPnl = totalPnl,
            pnlPercent = pnlPercent,
            trades = closedTrades,
            winningTrades = winningTrades,
            losingTrades = losingTrades,
            winRate = winRate,
            avgWin = avgWin,
            avgLoss = avgLoss,
            profitFactor = profitFactor,
            sharpeRatio = sharpeRatio,
            maxDrawdown = maxDrawdown,
            equityCurve = equityCurve
        )
    }
}

fun BacktestResult.print(title: String = "BACKTEST RESULTS") {
    println()
    println("=".repeat(90))
    println("📊 $title")
    println("=".repeat(90))
    println("Initial Capital:  \$${initialCapital}")
    println("Final Equity:     \$${finalEquity.setScale(2, RoundingMode.HALF_UP)}")
    println("Total PnL:        ${if (totalPnl >= BigDecimal.ZERO) "+" else ""}${totalPnl.setScale(2, RoundingMode.HALF_UP)} (${if (pnlPercent >= 0) "+" else ""}${"%.2f".format(pnlPercent)}%)")
    println()
    println("Total Trades:     ${trades.size}")
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

    val stopLossExits = trades.count { it.exitReason == "Stop Loss" }
    val takeProfitExits = trades.count { it.exitReason == "Take Profit" }
    val marketCloseExits = trades.count { it.exitReason == "Market Close" }

    println("Exit Reasons:")
    if (trades.isNotEmpty()) {
        println("  Stop Loss:      $stopLossExits (${"%.0f".format(stopLossExits.toDouble() / trades.size * 100)}%)")
        println("  Take Profit:    $takeProfitExits (${"%.0f".format(takeProfitExits.toDouble() / trades.size * 100)}%)")
        println("  Market Close:   $marketCloseExits (${"%.0f".format(marketCloseExits.toDouble() / trades.size * 100)}%)")
    }
    println("=".repeat(90))
}

// ============================================================================
// Main Entry Point
// ============================================================================

fun main() = runBlocking {
    println("\n🌐 MULTI-TIMEFRAME BACKTEST - RELAXED FILTERS")
    println("=".repeat(90))
    println("Balance between quality and frequency")
    println("=".repeat(90))

    val (all1h, all15m) = fetchData(1000, 1500)

    println("Configuration: RELAXED")
    println("  ADX Trend:        ${TradingConfig.Strategy.ADX_TREND_THRESHOLD} (relaxed from 18)")
    println("  Confirmation:     ${TradingConfig.Strategy.CONFIRMATION_CANDLES} candle (relaxed from 2)")
    println("  Volume:           ${TradingConfig.Technical.MIN_VOLUME_RATIO}× (relaxed from 1.0)")
    println("  Test Period:      ${all15m.size - 300} candles")
    println()

    val engine = BacktestEngine()
    val result = engine.execute(all1h, all15m, verbose = false)

    result.print("RELAXED FILTERS RESULTS")

    println()
    analyzeResults(result)
}

private suspend fun fetchData(size1h: Int, size15m: Int): Pair<List<Candle>, List<Candle>> =
    coroutineScope {
        val candles1h = async {
            BinanceDataLoader.fetchHistoricalCandles("BTCUSDT", "1h", limit = size1h)
        }
        val candles15m = async {
            BinanceDataLoader.fetchHistoricalCandles("BTCUSDT", "15m", limit = size15m)
        }
        Pair(candles1h.await(), candles15m.await())
    }

private fun analyzeResults(result: BacktestResult) {
    when {
        result.trades.size >= 50 && result.winRate >= 70.0 && result.pnlPercent > 5.0 -> {
            println("✅ HOLY GRAIL CONFIRMED!")
            println("   ${result.trades.size} trades, ${"%.1f".format(result.winRate)}% win rate, ${"%.2f".format(result.pnlPercent)}% return")
            println("   → Ready for live deployment!")
        }
        result.trades.size >= 30 && result.winRate >= 60.0 && result.pnlPercent > 0.0 -> {
            println("✅ EXCELLENT! Profitable and statistically significant")
            println("   ${result.trades.size} trades, ${"%.1f".format(result.winRate)}% win rate, ${"%.2f".format(result.pnlPercent)}% return")
            println("   → Strong edge confirmed!")
        }
        result.trades.size >= 20 && result.winRate >= 50.0 -> {
            println("⚠️  PROMISING but needs validation")
            println("   ${result.trades.size} trades, ${"%.1f".format(result.winRate)}% win rate")
            println("   → Test on longer period")
        }
        result.trades.size < 20 -> {
            println("⚠️  TOO FEW TRADES")
            println("   Only ${result.trades.size} trades")
            println("   → Loosen filters or extend test period")
        }
        else -> {
            println("❌ NEEDS IMPROVEMENT")
            println("   ${result.trades.size} trades, ${"%.1f".format(result.winRate)}% win rate, ${"%.2f".format(result.pnlPercent)}% return")
        }
    }
}
