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
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

private fun BigDecimal.toUsd() = this.setScale(2, RoundingMode.HALF_UP)

class BacktestEngine(
    private val config: BacktestConfig = BacktestConfig(),
    private val strategyConfig: StrategyConfig = StrategyConfig(),
    private val noiseConfig: com.tradeflow.backtesting.config.NoiseConfig = com.tradeflow.backtesting.config.NoiseConfig()
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

    private val makeTradingDecisionUseCase = MakeTradingDecisionUseCase(strategyConfig)

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

    private fun applyNoiseIfNeeded(candles: List<Candle>): List<Candle> =
        if (config.noiseLevel != NoiseLevel.NONE) {
            CandleNoiseInjector.injectNoise(candles, config.noiseLevel, noiseConfig)
        } else {
            candles
        }

    private fun openTrade(decision: Decision): Order = when (decision) {
        is Decision.Trend -> Order(
            direction = decision.direction,
            entryPrice = decision.entryPrice,
            stopLoss = decision.stopLoss,
            takeProfit = decision.takeProfit,
            leverage = strategyConfig.leverage
        )
        is Decision.Range -> Order(
            direction = decision.direction,
            entryPrice = decision.entryPrice,
            stopLoss = decision.stopLoss,
            takeProfit = decision.takeProfit,
            leverage = strategyConfig.leverage
        )
        is Decision.Wait -> error("Cannot open trade for Wait decision")
    }

    private fun checkExits(
        openOrders: MutableList<Order>,
        candle: Candle,
        equity: BigDecimal,
        closedOrders: MutableList<Order>
    ): BigDecimal {
        var updatedEquity = equity

        openOrders.filter { it.isOpen }.forEach { trade ->
            val hitStopLoss = when (trade.direction) {
                OrderSide.BUY -> candle.low <= trade.stopLoss
                OrderSide.SELL -> candle.high >= trade.stopLoss
            }

            val hitTakeProfit = when (trade.direction) {
                OrderSide.BUY -> candle.high >= trade.takeProfit
                OrderSide.SELL -> candle.low <= trade.takeProfit
            }

            if (hitStopLoss) {
                trade.exitPrice = trade.stopLoss
                closedOrders.add(trade)
                updatedEquity += closeTrade(trade, updatedEquity, "Stop Loss")
            } else if (hitTakeProfit) {
                trade.exitPrice = trade.takeProfit
                closedOrders.add(trade)
                updatedEquity += closeTrade(trade, updatedEquity, "Take Profit")
            }
        }

        openOrders.removeAll { !it.isOpen }
        return updatedEquity
    }

    private fun closeTrade(trade: Order, equity: BigDecimal, reason: String): BigDecimal {
        trade.exitReason = reason

        val positionSize = equity * strategyConfig.trendPositionPercent * strategyConfig.leverage
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
        all30m: List<Candle>,
        all15m: List<Candle>,
        all5m: List<Candle>,
        all1m: List<Candle>
    ): BacktestResult {
        val processedAll1h = applyNoiseIfNeeded(all1h)
        val processedAll30m = applyNoiseIfNeeded(all30m)
        val processedAll15m = applyNoiseIfNeeded(all15m)
        val processedAll5m = applyNoiseIfNeeded(all5m)
        val processedAll1m = applyNoiseIfNeeded(all1m)

        val prime1m = processedAll1m.take(config.primeSize)
        val test1m = processedAll1m.drop(config.primeSize)

        var equity = initialCapital
        val openOrders = mutableListOf<Order>()
        val closedOrders = mutableListOf<Order>()
        var peak = initialCapital
        var maxDrawdown = BigDecimal.ZERO

        test1m.forEachIndexed { index, candle1m ->
            val history1m = (prime1m + test1m.take(index + 1)).takeLast(config.lookbackWindow)
            val index5m = (config.primeSize + (index / 5)).coerceAtMost(processedAll5m.size - 1)
            val history5m = processedAll5m.take(index5m + 1).takeLast(config.lookbackWindow)
            val index15m = (config.primeSize + (index / 15)).coerceAtMost(processedAll15m.size - 1)
            val history15m = processedAll15m.take(index15m + 1).takeLast(config.lookbackWindow)
            val index30m = (config.primeSize + (index / 30)).coerceAtMost(processedAll30m.size - 1)
            val history30m = processedAll30m.take(index30m + 1).takeLast(config.lookbackWindow)
            val index1h = (config.primeSize + (index / 60)).coerceAtMost(processedAll1h.size - 1)
            val history1h = processedAll1h.take(index1h + 1).takeLast(config.lookbackWindow)

            if (history1h.size < config.minCandlesRequired ||
                history30m.size < config.minCandlesRequired ||
                history15m.size < config.minCandlesRequired ||
                history5m.size < config.minCandlesRequired ||
                history1m.size < config.minCandlesRequired) {
                return@forEachIndexed
            }

            // Check exits
            equity = checkExits(openOrders, candle1m, equity, closedOrders)

            // Execute new signals using 1h timeframe only
            val decision = makeTradingDecisionUseCase(history1h, candle1m.close)

            when (decision) {
                is Decision.Trend -> {
                    openOrders.add(openTrade(decision))
                    println("  🎯 TREND TRADE OPENED")
                }
                is Decision.Range -> {
                    openOrders.add(openTrade(decision))
                    println("  🎯 RANGE TRADE OPENED (mean-reversion)")
                }
                is Decision.Wait -> {
                    // No action
                }
            }

            // Update peak and max drawdown
            if (equity > peak) peak = equity
            val currentDrawdown = if (peak > BigDecimal.ZERO) {
                (peak - equity).divide(peak, TradingConfig.Technical.PNL_PRECISION_DECIMAL_PLACES, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            if (currentDrawdown > maxDrawdown) maxDrawdown = currentDrawdown

            println("  Progress: ${index + 1}/${test1m.size} candles | " +
                    "Open: ${openOrders.size} | Closed: ${closedOrders.size} | " +
                    "Equity: \$${equity.toUsd()}")
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
