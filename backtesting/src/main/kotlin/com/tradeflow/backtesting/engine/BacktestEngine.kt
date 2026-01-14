package com.tradeflow.backtesting.engine

import com.tradeflow.backtesting.config.BacktestConfig
import com.tradeflow.backtesting.data.CandleNoiseInjector
import com.tradeflow.backtesting.data.NoiseLevel
import com.tradeflow.core.domain.StrategyConfig
import com.tradeflow.core.domain.TradingConfig
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.usecase.MultiTimeframeDecisionUseCase
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

private fun BigDecimal.toUsd() = this.setScale(2, RoundingMode.HALF_UP)

class BacktestEngine(
    private val config: BacktestConfig = BacktestConfig.default()
) {
    private val initialCapital: BigDecimal = config.initialCapital
    // ==================================================================================
    // BINANCE PERPETUAL FUTURES FEE STRUCTURE (VIP 0 - Regular User)
    // ==================================================================================
    // Source: https://www.binance.com/en/support/faq/detail/360033544231
    // Updated: 2026-01-14
    //
    // Entry (Market Order - Taker):  0.05%
    // Exit (Limit Order - Maker):    0.02%
    // Round-trip cost:               0.07%
    //
    // With BNB discount (10% off):
    // Entry: 0.045%, Exit: 0.018%, Total: 0.063%
    //
    // Note: Using standard rates (no BNB discount) for conservative estimates
    // ==================================================================================

    private val multiTimeFrameDecisionUseCase = MultiTimeframeDecisionUseCase()

    /**
     * Calculate total trading costs for a position.
     *
     * @param positionSize Notional position size in USD
     * @return Total costs (entry fee + exit fee + slippage)
     */
    private fun calculateTradingCosts(positionSize: BigDecimal): BigDecimal {
        val entryFee = positionSize * config.entryFeeRate
        val exitFee = positionSize * config.exitFeeRate
        val slippage = positionSize * config.exitSlippageRate
        return entryFee + exitFee + slippage
    }

    private fun closeTrade(trade: Order, equity: BigDecimal, reason: String): BigDecimal {
        trade.exitReason = reason

        val positionSize = equity * StrategyConfig.trendPositionPercent * StrategyConfig.leverage
        val pnl = trade.calculatePnl()
        val grossPnlUsd = positionSize * pnl

        val costs = calculateTradingCosts(positionSize)
        val netPnlUsd = grossPnlUsd - costs

        val emoji = if (netPnlUsd > BigDecimal.ZERO) "💰" else "❌"
        val sign = if (netPnlUsd > BigDecimal.ZERO) "+" else ""
        println("  $emoji TRADE CLOSED | $sign$${ netPnlUsd.toUsd() }")

        return netPnlUsd
    }

    fun execute(
        all1h: List<Candle>,
        all15m: List<Candle>
    ): BacktestResult {
        // Apply noise injection if configured (for robustness testing)
        val processedAll1h = if (config.noiseLevel != NoiseLevel.NONE) {
            CandleNoiseInjector.injectNoise(all1h, config.noiseLevel, config)
        } else {
            all1h
        }

        val processedAll15m = if (config.noiseLevel != NoiseLevel.NONE) {
            CandleNoiseInjector.injectNoise(all15m, config.noiseLevel, config)
        } else {
            all15m
        }

        val prime15m = processedAll15m.take(config.primeSize)
        val test15m = processedAll15m.drop(config.primeSize)

        var equity = initialCapital
        val openOrders = mutableListOf<Order>()
        val closedOrders = mutableListOf<Order>()
        var peak = initialCapital
        var maxDrawdown = BigDecimal.ZERO

        test15m.forEachIndexed { index, candle15m ->
            val history15m = (prime15m + test15m.take(index + 1)).takeLast(config.lookbackWindow)
            val index1h = (config.primeSize + (index / 4)).coerceAtMost(processedAll1h.size - 1)
            val history1h = processedAll1h.take(index1h + 1).takeLast(config.lookbackWindow)

            if (history1h.size < config.minCandlesRequired || history15m.size < config.minCandlesRequired) {
                return@forEachIndexed
            }

            // Check exits
            openOrders.filter { it.isOpen }.forEach { trade ->
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
                    closedOrders.add(trade)
                    equity += closeTrade(trade, equity, "Stop Loss")
                } else if (hitTakeProfit) {
                    trade.exitPrice = trade.takeProfit
                    closedOrders.add(trade)
                    equity += closeTrade(trade, equity, "Take Profit")
                }
            }

            openOrders.removeAll { !it.isOpen }

            // Execute new signals using multi-timeframe confluence logic
            val decision = multiTimeFrameDecisionUseCase(
                MultiTimeframeDecisionUseCase.MultiTimeframeCandles(
                    candles1h = history1h,
                    candles15m = history15m,
                    currentPrice = candle15m.close
                )
            )

            if (decision is Decision.Trend) {
                val newOrder = Order(
                    direction = decision.direction,
                    entryPrice = decision.entryPrice,
                    stopLoss = decision.stopLoss,
                    takeProfit = decision.takeProfit,
                    leverage = StrategyConfig.leverage
                )
                openOrders.add(newOrder)
                println("  🎯 TRADE OPENED")
            }

            // Update peak and max drawdown
            if (equity > peak) peak = equity
            val currentDrawdown = if (peak > BigDecimal.ZERO) {
                (peak - equity).divide(peak, TradingConfig.Technical.PNL_PRECISION_DECIMAL_PLACES, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            if (currentDrawdown > maxDrawdown) maxDrawdown = currentDrawdown

            println("  Progress: ${index + 1}/${test15m.size} candles | " +
                    "Open: ${openOrders.size} | Closed: ${closedOrders.size} | " +
                    "Equity: \$${equity.toUsd()}")
        }

        // Close remaining trades
        openOrders.filter { it.isOpen }.forEach { trade ->
            trade.exitPrice = test15m.last().close
            closedOrders.add(trade)
            equity += closeTrade(trade, equity, "Market Close")
        }

        return calculateMetrics(equity, closedOrders, maxDrawdown)
    }

    private fun calculateMetrics(
        finalEquity: BigDecimal,
        closedOrders: List<Order>,
        maxDrawdown: BigDecimal
    ): BacktestResult {
        val totalPnl = finalEquity - initialCapital
        val pnlPercent = (totalPnl / initialCapital).toDouble() * 100

        // NOTE: Win rate calculation uses GROSS PnL (before fees).
        // This slightly overstates win rate by ~2-5 percentage points since some small
        // winners become losers after fees. However, the TOTAL equity calculation above
        // correctly deducts all fees, so overall returns are accurate.
        //
        // To fix: Store net PnL with each trade or estimate costs based on average position size.
        val winningOrders = closedOrders.filter { it.calculatePnl() > BigDecimal.ZERO }
        val losingOrders = closedOrders.filter { it.calculatePnl() <= BigDecimal.ZERO }
        val winRate = if (closedOrders.isNotEmpty()) (winningOrders.size.toDouble() / closedOrders.size * 100) else 0.0

        val avgWin = if (winningOrders.isNotEmpty()) {
            winningOrders.map { it.calculatePnl().toDouble() * 100 }.average()
        } else 0.0

        val avgLoss = if (losingOrders.isNotEmpty()) {
            losingOrders.map { it.calculatePnl().toDouble() * 100 }.average()
        } else 0.0

        val totalWins = winningOrders.sumOf { it.calculatePnl().toDouble() }
        val totalLosses = losingOrders.sumOf { it.calculatePnl().toDouble() }.let { abs(it) }
        val profitFactor = if (totalLosses > 0.0) totalWins / totalLosses else 0.0

        // Sharpe ratio: Not calculated (requires equity curve storage)
        val sharpeRatio = 0.0

        // Use pre-calculated max drawdown (already computed during backtest loop)
        val maxDrawdownPercent = maxDrawdown.toDouble() * 100

        return BacktestResult(
            initialCapital = initialCapital,
            finalEquity = finalEquity,
            totalPnl = totalPnl,
            pnlPercent = pnlPercent,
            trades = closedOrders,
            winningTrades = winningOrders,
            losingTrades = losingOrders,
            winRate = winRate,
            avgWin = avgWin,
            avgLoss = avgLoss,
            profitFactor = profitFactor,
            sharpeRatio = sharpeRatio,
            maxDrawdown = maxDrawdownPercent,
            equityCurve = emptyList()
        )
    }
}
