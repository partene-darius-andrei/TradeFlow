package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.simulator.SimulatedExchange
import com.tradeflow.core.domain.usecase.CycleResult
import com.tradeflow.core.domain.usecase.ExecuteTradingCycleUseCase
import com.tradeflow.core.domain.util.BinanceDataLoader
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.test.assertTrue
import kotlin.math.sqrt

data class TradeResult(
    val pnl: BigDecimal,
    val isWin: Boolean,
    val entryPrice: BigDecimal,
    val exitPrice: BigDecimal,
    val side: OrderSide
)

class RealTradeSimulationTest {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

    @Test
    fun `analyze PnL and equity curve over 30 days of real data`() = runBlocking {
        val config = TradingConfig.forProfile(RiskProfile.BALANCED)
        val initialCapital = BigDecimal("500.00")
        val exchange = SimulatedExchange(
            initialUsd = initialCapital,
            tradingConfig = config
        )

        val engine = MakeTradingDecisionUseCase(
            taService = AnalyzeCandlesUseCase(),
            config = config
        )
        val orchestrator = ExecuteTradingCycleUseCase(
            exchangeRepository = exchange,
            makeDecisionUseCase = engine,
            config = config,
            trailingStopManager = com.tradeflow.core.domain.risk.TrailingStopManager(config)
        )

        val allCandles = BinanceDataLoader.fetchHistoricalCandles(interval = "4h", limit = 400)
        val primeHistory = allCandles.take(200)
        val simulationDays = allCandles.drop(200)
        
        exchange.setHistory(primeHistory)
        
        println("\n🚀 SIMULATION LOG START")
        println("--------------------------------------------------------------------------------")
        println("INITIAL CAPITAL: $500.00 USD | FEE: 0.60% | STRATEGY: Hysteresis Trend/Range")
        println("--------------------------------------------------------------------------------")

        var highWaterMark = initialCapital
        val equityCurve = mutableListOf<BigDecimal>()
        val trades = mutableListOf<TradeResult>()
        var previousPosition: PerpetualPosition? = null

        simulationDays.forEachIndexed { index, candle ->
            exchange.advanceTime(candle)

            val currentEquity = exchange.getTotalEquity()
            equityCurve.add(currentEquity)

            val cycleResult = orchestrator.runCycle("BTC-USD", highWaterMark)
            highWaterMark = cycleResult.updatedHighWaterMark

            // Track completed trades (when position closes)
            val currentPosition = exchange.getPerpetualPosition("BTC-USD").getOrNull()
            if (previousPosition != null && currentPosition == null) {
                // Position closed - record the trade
                val pnl = previousPosition.unrealizedPnl
                trades.add(TradeResult(
                    pnl = pnl,
                    isWin = pnl > BigDecimal.ZERO,
                    entryPrice = previousPosition.entryPrice,
                    exitPrice = previousPosition.currentPrice,
                    side = previousPosition.side
                ))
            }
            previousPosition = currentPosition

            val timestamp = dateFormatter.format(candle.timestamp)
            val pnl = currentEquity - initialCapital
            val pnlPct = if (initialCapital > BigDecimal.ZERO) pnl.divide(initialCapital, 4, RoundingMode.HALF_UP) * BigDecimal("100") else BigDecimal.ZERO
            val sign = if (pnl >= BigDecimal.ZERO) "+" else ""

            val resultMsg = when(cycleResult.execution) {
                is ExecutionResult.Success -> "✅ ${cycleResult.execution.message}"
                is ExecutionResult.Skipped -> "◽ ${cycleResult.execution.reason}"
                is ExecutionResult.Failed -> "❌ ${cycleResult.execution.error}"
            }

            // Log EVERY candle for full visibility
            println("[$timestamp] | BTC: ${candle.close.setScale(2, RoundingMode.HALF_UP)} | $resultMsg | Equity: ${currentEquity.setScale(2, RoundingMode.HALF_UP)} | PnL: $sign${pnl.setScale(2, RoundingMode.HALF_UP)} ($sign${pnlPct.setScale(2, RoundingMode.HALF_UP)}%)")
        }

        val finalEquity = exchange.getTotalEquity()
        val totalPnL = finalEquity - initialCapital
        val totalPnLPct = if (initialCapital > BigDecimal.ZERO) totalPnL.divide(initialCapital, 4, RoundingMode.HALF_UP) * BigDecimal("100") else BigDecimal.ZERO

        // Calculate comprehensive performance metrics
        val winningTrades = trades.filter { it.isWin }
        val losingTrades = trades.filter { !it.isWin }
        val winRate = if (trades.isNotEmpty()) (winningTrades.size.toDouble() / trades.size * 100) else 0.0

        val totalWins = winningTrades.sumOf { it.pnl }
        val totalLosses = losingTrades.sumOf { it.pnl }.abs()
        val profitFactor = if (totalLosses > BigDecimal.ZERO) totalWins.divide(totalLosses, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

        val avgWin = if (winningTrades.isNotEmpty()) totalWins.divide(BigDecimal(winningTrades.size), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val avgLoss = if (losingTrades.isNotEmpty()) totalLosses.divide(BigDecimal(losingTrades.size), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val riskRewardRatio = if (avgLoss > BigDecimal.ZERO) avgWin.divide(avgLoss, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

        // Calculate max drawdown
        var maxDrawdown = BigDecimal.ZERO
        var peak = initialCapital
        equityCurve.forEach { equity ->
            if (equity > peak) peak = equity
            val drawdown = if (peak > BigDecimal.ZERO) (peak - equity).divide(peak, 4, RoundingMode.HALF_UP) * BigDecimal("100") else BigDecimal.ZERO
            if (drawdown > maxDrawdown) maxDrawdown = drawdown
        }

        // Calculate Sharpe ratio (simplified - assumes 4h candles)
        val returns = mutableListOf<Double>()
        for (i in 1 until equityCurve.size) {
            val ret = (equityCurve[i] - equityCurve[i-1]).divide(equityCurve[i-1], 6, RoundingMode.HALF_UP).toDouble()
            returns.add(ret)
        }
        val avgReturn = if (returns.isNotEmpty()) returns.average() else 0.0
        val stdDev = if (returns.size > 1) sqrt(returns.map { (it - avgReturn) * (it - avgReturn) }.average()) else 0.0
        val sharpeRatio = if (stdDev > 0.0) (avgReturn / stdDev) * sqrt(365.0 * 6.0) else 0.0  // Annualized (6 candles/day)

        println("--------------------------------------------------------------------------------")
        println("🏁 FINAL PROFIT & LOSS STATEMENT")
        println("--------------------------------------------------------------------------------")
        println("STARTING BALANCE: $initialCapital USD")
        println("FINAL BALANCE:    ${finalEquity.setScale(2, RoundingMode.HALF_UP)} USD")
        println("NET PROFIT/LOSS:  ${if (totalPnL >= BigDecimal.ZERO) "+" else ""}${totalPnL.setScale(2, RoundingMode.HALF_UP)} USD")
        println("PERCENTAGE GAIN:  ${if (totalPnL >= BigDecimal.ZERO) "+" else ""}${totalPnLPct.setScale(2, RoundingMode.HALF_UP)}%")
        println("--------------------------------------------------------------------------------")
        println("📊 PERFORMANCE METRICS")
        println("--------------------------------------------------------------------------------")
        println("TOTAL TRADES:     ${trades.size}")
        println("WINNING TRADES:   ${winningTrades.size}")
        println("LOSING TRADES:    ${losingTrades.size}")
        println("WIN RATE:         ${"%.1f".format(winRate)}%")
        println("PROFIT FACTOR:    ${profitFactor.setScale(2, RoundingMode.HALF_UP)}")
        println("AVG WIN:          $${avgWin.setScale(2, RoundingMode.HALF_UP)}")
        println("AVG LOSS:         -$${avgLoss.setScale(2, RoundingMode.HALF_UP)}")
        println("RISK/REWARD:      ${riskRewardRatio.setScale(2, RoundingMode.HALF_UP)}:1")
        println("MAX DRAWDOWN:     ${maxDrawdown.setScale(2, RoundingMode.HALF_UP)}%")
        println("SHARPE RATIO:     ${"%.2f".format(sharpeRatio)}")
        println("--------------------------------------------------------------------------------")
        println("🚀 SIMULATION LOG END\n")

        val targetBalance = BigDecimal("480.00")
        assertTrue(
            finalEquity >= targetBalance,
            "❌ FAILED: Balance $finalEquity is below target $targetBalance (max acceptable loss: 4%)"
        )
    }
}
