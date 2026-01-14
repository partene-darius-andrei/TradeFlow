package com.tradeflow.backtesting.engine

import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal
import java.math.RoundingMode

data class Trade(
    val direction: OrderSide,
    val entryPrice: BigDecimal,
    val stopLoss: BigDecimal,
    val takeProfit: BigDecimal,
    val leverage: BigDecimal = BigDecimal.ONE,
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
