package com.tradeflow.backtesting.engine

import com.tradeflow.core.domain.model.Order
import java.math.BigDecimal

data class BacktestResult(
    val initialCapital: BigDecimal,
    val finalEquity: BigDecimal,
    val totalPnl: BigDecimal,
    val pnlPercent: Double,
    val trades: List<Order>,
    val winningTrades: List<Order>,
    val losingTrades: List<Order>,
    val winRate: Double,
    val avgWin: Double,
    val avgLoss: Double,
    val profitFactor: Double,
    val sharpeRatio: Double,
    val maxDrawdown: Double,
    val equityCurve: List<BigDecimal>
)
