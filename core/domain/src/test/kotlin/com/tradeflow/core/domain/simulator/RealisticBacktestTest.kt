package com.tradeflow.core.domain.simulator

import com.tradeflow.core.domain.strategy.StrategyConfig
import com.tradeflow.core.domain.util.BinanceDataLoader
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertTrue

class RealisticBacktestTest {

    @Test
    @Ignore("Slow - fetches from Binance API and runs full year simulation")
    fun `simulate complete 2024 trading with realistic execution`() = runBlocking {
        // Setup
        val candles = BinanceDataLoader.fetchBtcUsdtYear2024()
        println("Loaded ${candles.size} daily candles from 2024")

        val config = BacktestConfig(
            startingCapital = BigDecimal("500"),
            productId = "BTC-USD",
            historicalCandles = candles,
            strategyConfig = StrategyConfig(
                smaPeriod = 200,
                adxPeriod = 14,
                atrPeriod = 14,
                adxTrendThreshold = 25.0,
                adxRangeThreshold = 25.0
            )
        )

        // Execute
        val engine = BacktestEngine()
        val result = engine.runBacktest(config)

        // Display Results
        println("""
            |
            |======================================================
            |    2024 BTC TRADING SIMULATION (Realistic)
            |======================================================
            |
            |Starting Capital:    ${result.startingEquity} USD
            |Final Equity:        ${result.finalEquity} USD
            |Total PnL:           ${result.totalPnl} USD (${result.totalPnlPercent}%)
            |
            |Total Trades:        ${result.totalTrades}
            |Winning Trades:      ${result.winningTrades}
            |Losing Trades:       ${result.losingTrades}
            |Win Rate:            ${String.format("%.2f", result.winRate)}%
            |
            |Max Drawdown:        ${result.maxDrawdown} USD (${result.maxDrawdownPercent}%)
            |Sharpe Ratio:        ${String.format("%.2f", result.sharpeRatio)}
            |Profit Factor:       ${result.profitFactor}
            |
            |======================================================
            |    STRATEGY BREAKDOWN
            |======================================================
            |
            |TREND Strategy:
            |  Trades:     ${result.trendStats.trades}
            |  PnL:        ${result.trendStats.pnl} USD
            |  Win Rate:   ${String.format("%.2f", result.trendStats.winRate)}%
            |
            |RANGE Strategy:
            |  Trades:     ${result.rangeStats.trades}
            |  PnL:        ${result.rangeStats.pnl} USD
            |  Win Rate:   ${String.format("%.2f", result.rangeStats.winRate)}%
            |
            |======================================================
            |    TOP 5 WINNING TRADES
            |======================================================
            |
        """.trimMargin())

        result.trades
            .filter { it.pnl != null }
            .sortedByDescending { it.pnl }
            .take(5)
            .forEach { trade ->
                println("${trade.timestamp} | ${trade.strategy} ${trade.side} @ ${trade.price} | PnL: +${trade.pnl} USD")
            }

        println("""
            |
            |======================================================
            |    TOP 5 LOSING TRADES
            |======================================================
            |
        """.trimMargin())

        result.trades
            .filter { it.pnl != null }
            .sortedBy { it.pnl }
            .take(5)
            .forEach { trade ->
                println("${trade.timestamp} | ${trade.strategy} ${trade.side} @ ${trade.price} | PnL: ${trade.pnl} USD")
            }

        println("\n======================================================\n")

        // Assertions
        assertTrue(result.finalEquity > BigDecimal.ZERO, "Should not blow up account completely")
        assertTrue(result.maxDrawdownPercent < BigDecimal("20"), "Should respect 15% max drawdown with some tolerance")
        assertTrue(result.totalTrades >= 0, "Should execute at least some trades or stay in defense")
    }

    @Test
    fun `backtest engine can process simple candle data`() = runBlocking {
        // Quick test with minimal data
        val candles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "1h",
            limit = 250  // Need at least 200 for SMA-200
        )

        val config = BacktestConfig(
            startingCapital = BigDecimal("500"),
            productId = "BTC-USD",
            historicalCandles = candles,
            strategyConfig = StrategyConfig(
                smaPeriod = 200,
                adxPeriod = 14,
                atrPeriod = 14
            )
        )

        val engine = BacktestEngine()
        val result = engine.runBacktest(config)

        println("""
            |Quick backtest: ${result.totalTrades} trades,
            |PnL: ${result.totalPnl} (${result.totalPnlPercent}%),
            |Final Equity: ${result.finalEquity} USD
        """.trimMargin())

        // Basic sanity checks
        assertTrue(result.finalEquity > BigDecimal.ZERO)
        assertTrue(result.startingEquity == BigDecimal("500"))
    }

    @Test
    fun `performance tracker generates valid report structure`() {
        // Unit test for PerformanceTracker
        val tracker = PerformanceTracker(BigDecimal("1000"))

        val report = tracker.generateReport()

        // Verify report structure
        assertTrue(report.startingEquity == BigDecimal("1000"))
        assertTrue(report.finalEquity == BigDecimal("1000"))  // No trades yet
        assertTrue(report.totalTrades == 0)
        assertTrue(report.winRate == 0.0)
        assertTrue(report.equityCurve.isEmpty())
        assertTrue(report.trades.isEmpty())
    }

    @Test
    fun `simulated exchange handles basic operations`() = runBlocking {
        val exchange = SimulatedExchangeRepository(
            startingCapitalUsd = BigDecimal("1000"),
            productId = "BTC-USD"
        )

        // Test market order
        val marketOrderResult = exchange.placeMarketOrder(
            productId = "BTC-USD",
            side = com.tradeflow.core.domain.model.OrderSide.BUY,
            size = BigDecimal("0.01")
        )

        assertTrue(marketOrderResult.isSuccess)

        // Test limit order
        val limitOrderResult = exchange.placeLimitOrder(
            productId = "BTC-USD",
            side = com.tradeflow.core.domain.model.OrderSide.SELL,
            size = BigDecimal("0.01"),
            price = BigDecimal("100000"),
            postOnly = true
        )

        assertTrue(limitOrderResult.isSuccess)

        // Test get open orders
        val openOrdersResult = exchange.getOpenOrders("BTC-USD")
        assertTrue(openOrdersResult.isSuccess)
        assertTrue(openOrdersResult.getOrNull()?.size == 1)  // Only limit order is open
    }
}
