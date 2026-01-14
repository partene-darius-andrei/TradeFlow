package com.tradeflow.backtesting.engine

import com.tradeflow.core.domain.TradingConfig
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.usecase.MultiTimeframeDecisionUseCase
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.sqrt

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
                    val pnlUsd = equity * pnl * TradingConfig.Strategy.getTrendPositionPercent() * TradingConfig.Strategy.getLeverage()
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
                    val pnlUsd = equity * pnl * TradingConfig.Strategy.getTrendPositionPercent() * TradingConfig.Strategy.getLeverage()
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
                    takeProfit = decision.takeProfit,
                    leverage = TradingConfig.Strategy.getLeverage()
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
            val pnlUsd = equity * pnl * TradingConfig.Strategy.getTrendPositionPercent() * TradingConfig.Strategy.getLeverage()
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
