package com.tradeflow.core.domain.simulator

import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import kotlin.math.pow
import kotlin.math.sqrt

data class Trade(
    val timestamp: Instant,
    val strategy: String,  // "TREND", "RANGE", or "DEFENSE"
    val side: OrderSide,
    val price: BigDecimal,
    val size: BigDecimal,
    val fees: BigDecimal,
    val pnl: BigDecimal?,  // null for entry, calculated for exit
    val portfolioEquity: BigDecimal
)

data class StrategyStats(
    val trades: Int,
    val pnl: BigDecimal,
    val winRate: Double
)

data class BacktestResult(
    val startingEquity: BigDecimal,
    val finalEquity: BigDecimal,
    val totalPnl: BigDecimal,
    val totalPnlPercent: BigDecimal,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val maxDrawdown: BigDecimal,
    val maxDrawdownPercent: BigDecimal,
    val sharpeRatio: Double,
    val profitFactor: BigDecimal,
    val trendStats: StrategyStats,
    val rangeStats: StrategyStats,
    val equityCurve: List<Pair<Instant, BigDecimal>>,
    val trades: List<Trade>
)

class PerformanceTracker(
    private val startingEquity: BigDecimal
) {
    private val trades = mutableListOf<Trade>()
    private val equityCurve = mutableListOf<Pair<Instant, BigDecimal>>()
    private var currentEquity = startingEquity
    private var peakEquity = startingEquity

    // Track open positions for PnL calculation
    private var openPositionEntry: Trade? = null

    fun recordEquitySnapshot(timestamp: Instant, equity: BigDecimal) {
        currentEquity = equity
        equityCurve.add(timestamp to equity)

        if (equity > peakEquity) {
            peakEquity = equity
        }
    }

    fun recordFill(fill: Fill, strategy: String, currentBtcPrice: BigDecimal, currentEquity: BigDecimal) {
        val order = fill.order
        val feePercent = if (fill.isMaker) PortfolioSimulator.MAKER_FEE_PERCENT else PortfolioSimulator.TAKER_FEE_PERCENT
        val fees = order.size * fill.fillPrice * BigDecimal(feePercent)

        // Calculate PnL if this is closing a position
        val pnl = if (openPositionEntry != null && order.side != openPositionEntry!!.side) {
            // Closing position
            val entry = openPositionEntry!!
            val profit = when (order.side) {
                OrderSide.SELL -> (fill.fillPrice - entry.price) * order.size  // Closing long
                OrderSide.BUY -> (entry.price - fill.fillPrice) * order.size   // Closing short
            }
            val totalFees = entry.fees + fees
            val netPnl = profit - totalFees
            openPositionEntry = null  // Position closed
            netPnl
        } else {
            // Opening position
            openPositionEntry = Trade(
                timestamp = order.createdAt,
                strategy = strategy,
                side = order.side,
                price = fill.fillPrice,
                size = order.size,
                fees = fees,
                pnl = null,
                portfolioEquity = currentEquity
            )
            null  // No PnL yet
        }

        val trade = Trade(
            timestamp = order.createdAt,
            strategy = strategy,
            side = order.side,
            price = fill.fillPrice,
            size = order.size,
            fees = fees,
            pnl = pnl,
            portfolioEquity = currentEquity
        )

        trades.add(trade)
    }

    fun generateReport(): BacktestResult {
        val finalEquity = currentEquity
        val totalPnl = finalEquity - startingEquity
        val totalPnlPercent = (totalPnl / startingEquity * BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP)

        // Calculate trade statistics
        val closedTrades = trades.filter { it.pnl != null }
        val winningTrades = closedTrades.filter { it.pnl!! > BigDecimal.ZERO }
        val losingTrades = closedTrades.filter { it.pnl!! < BigDecimal.ZERO }

        val winRate = if (closedTrades.isNotEmpty()) {
            (winningTrades.size.toDouble() / closedTrades.size) * 100.0
        } else {
            0.0
        }

        // Calculate max drawdown
        var maxDrawdown = BigDecimal.ZERO
        var maxDrawdownPercent = BigDecimal.ZERO

        equityCurve.forEach { (_, equity) ->
            val drawdown = peakEquity - equity
            val drawdownPercent = if (peakEquity > BigDecimal.ZERO) {
                (drawdown / peakEquity * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown
                maxDrawdownPercent = drawdownPercent
            }
        }

        // Calculate Sharpe ratio
        val sharpeRatio = calculateSharpeRatio()

        // Calculate profit factor
        val grossProfit = winningTrades.sumOf { it.pnl!! }
        val grossLoss = losingTrades.sumOf { it.pnl!!.abs() }
        val profitFactor = if (grossLoss > BigDecimal.ZERO) {
            (grossProfit / grossLoss).setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // Per-strategy breakdown
        val trendStats = calculateStrategyStats("TREND")
        val rangeStats = calculateStrategyStats("RANGE")

        return BacktestResult(
            startingEquity = startingEquity,
            finalEquity = finalEquity,
            totalPnl = totalPnl.setScale(2, RoundingMode.HALF_UP),
            totalPnlPercent = totalPnlPercent,
            totalTrades = closedTrades.size,
            winningTrades = winningTrades.size,
            losingTrades = losingTrades.size,
            winRate = winRate,
            maxDrawdown = maxDrawdown.setScale(2, RoundingMode.HALF_UP),
            maxDrawdownPercent = maxDrawdownPercent,
            sharpeRatio = sharpeRatio,
            profitFactor = profitFactor,
            trendStats = trendStats,
            rangeStats = rangeStats,
            equityCurve = equityCurve.toList(),
            trades = trades.toList()
        )
    }

    private fun calculateSharpeRatio(): Double {
        if (equityCurve.size < 2) return 0.0

        val returns = equityCurve.zipWithNext { (_, a), (_, b) ->
            ((b - a) / a).toDouble()
        }

        if (returns.isEmpty()) return 0.0

        val avgReturn = returns.average()
        val variance = returns.map { (it - avgReturn).pow(2) }.average()
        val stdDev = sqrt(variance)

        return if (stdDev > 0.0) {
            (avgReturn / stdDev) * sqrt(365.0)  // Annualized (assuming daily candles)
        } else {
            0.0
        }
    }

    private fun calculateStrategyStats(strategyName: String): StrategyStats {
        val strategyTrades = trades.filter { it.strategy == strategyName && it.pnl != null }
        val pnl = strategyTrades.sumOf { it.pnl!! }
        val wins = strategyTrades.filter { it.pnl!! > BigDecimal.ZERO }
        val winRate = if (strategyTrades.isNotEmpty()) {
            (wins.size.toDouble() / strategyTrades.size) * 100.0
        } else {
            0.0
        }

        return StrategyStats(
            trades = strategyTrades.size,
            pnl = pnl.setScale(2, RoundingMode.HALF_UP),
            winRate = winRate
        )
    }

    fun reset() {
        trades.clear()
        equityCurve.clear()
        currentEquity = startingEquity
        peakEquity = startingEquity
        openPositionEntry = null
    }
}
