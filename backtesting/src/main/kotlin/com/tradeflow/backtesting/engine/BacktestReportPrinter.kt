package com.tradeflow.backtesting.engine

import java.math.BigDecimal
import java.math.RoundingMode

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
