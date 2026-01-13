package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.config.*
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.usecase.ExecuteTradingCycleUseCase
import com.tradeflow.core.domain.simulator.SimulatedExchange
import com.tradeflow.core.domain.util.BinanceDataLoader
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * ULTRA-AGGRESSIVE: REMOVE ALL BLOCKERS
 *
 * Strategy:
 * - NO volume filter (set to 0.1x minimum)
 * - NO confirmation delay (1 candle instant)
 * - VERY low ADX threshold (5.0)
 * - Tight stops for fast exits (2× ATR)
 * - Trade on EVERY signal
 */
class UltraAggressiveTest {

    @Test
    fun `ultra aggressive - NO FILTERS - make it trade NOW`() = runBlocking {
        println("\n⚡⚡⚡ ULTRA-AGGRESSIVE MODE: NO FILTERS ⚡⚡⚡")
        println("=".repeat(90))
        println("REMOVING ALL SAFEGUARDS. TRADING AT ALL COSTS.")
        println("=".repeat(90))
        println()

        // Use 5-minute candles for MAXIMUM frequency
        val allCandles = BinanceDataLoader.fetchHistoricalCandles(
            symbol = "BTCUSDT",
            interval = "5m",
            limit = 1500
        )

        println("Timeframe:   5-minute candles")
        println("Total Data:  ${allCandles.size} candles (~5 days)")
        println()

        // ULTRA-AGGRESSIVE CONFIG: Remove ALL filters
        val config = TradingConfig(
            strategy = StrategyParameters(
                confirmationCandles = 1,        // INSTANT reaction
                adxTrendThreshold = 5.0,        // Trade almost always
                adxRangeThreshold = 3.0,        // Irrelevant, everything is trend
                stopLossAtrMultiplier = BigDecimal("2.0"),      // TIGHT stop (fast exit)
                takeProfitAtrMultiplier = BigDecimal("6.0"),    // 3:1 reward
                trendPositionPercent = BigDecimal("0.10"),      // 10% per trade (aggressive)
                gridPositionPercentPerLevel = BigDecimal("0.15"),
                leverage = BigDecimal("3.0")    // 3× leverage for more exposure
            ),
            risk = RiskParameters(
                maxPositionPercent = BigDecimal("0.15"),       // Allow large positions
                maxDrawdownPercent = 0.50                       // 50% max loss before emergency stop
            ),
            technical = TechnicalParameters(
                minVolumeRatio = 0.1,           // NO volume filter (accept any)
                smaPeriod = 20                   // SHORT SMA for faster signals on 5m
            ),
            execution = ExecutionParameters(),
            profile = RiskProfile.BALANCED
        )

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

        // Need 20 candles for SMA20
        val primeHistory = allCandles.take(20)
        val simulationCandles = allCandles.drop(20)

        exchange.setHistory(primeHistory)

        var highWaterMark = initialCapital
        val equityCurve = mutableListOf<BigDecimal>()
        var tradeCount = 0
        var winCount = 0
        var previousPosition: com.tradeflow.core.domain.model.PerpetualPosition? = null
        val trades = mutableListOf<TradeRecord>()

        println("🔬 SIMULATING ${simulationCandles.size} CANDLES...")
        println("-".repeat(90))

        simulationCandles.forEachIndexed { index, candle ->
            exchange.advanceTime(candle)

            val currentEquity = exchange.getTotalEquity()
            equityCurve.add(currentEquity)

            val cycleResult = orchestrator.runCycle("BTC-USD", highWaterMark)
            highWaterMark = cycleResult.updatedHighWaterMark

            // Track trades
            val currentPosition = exchange.getPerpetualPosition("BTC-USD").getOrNull()
            if (previousPosition != null && currentPosition == null) {
                tradeCount++
                val pnl = previousPosition.unrealizedPnl
                val isWin = pnl > BigDecimal.ZERO
                if (isWin) winCount++

                trades.add(TradeRecord(
                    tradeNumber = tradeCount,
                    entryPrice = previousPosition.entryPrice,
                    exitPrice = previousPosition.currentPrice,
                    side = previousPosition.side,
                    pnl = pnl,
                    pnlPercent = ((pnl / initialCapital).toDouble() * 100)
                ))

                // Log every 10th trade
                if (tradeCount % 10 == 0) {
                    val pnlSign = if (pnl >= BigDecimal.ZERO) "+" else ""
                    println("Trade #$tradeCount: ${previousPosition.side} @ ${previousPosition.entryPrice.setScale(2, RoundingMode.HALF_UP)} → ${previousPosition.currentPrice.setScale(2, RoundingMode.HALF_UP)} | " +
                        "PnL: $pnlSign${pnl.setScale(2, RoundingMode.HALF_UP)} | Equity: ${currentEquity.setScale(2, RoundingMode.HALF_UP)}")
                }
            }
            previousPosition = currentPosition
        }

        val finalEquity = exchange.getTotalEquity()
        val totalPnL = finalEquity - initialCapital
        val pnlPercent = (totalPnL / initialCapital).toDouble() * 100

        val winRate = if (tradeCount > 0) (winCount.toDouble() / tradeCount * 100) else 0.0

        val returns = mutableListOf<Double>()
        for (i in 1 until equityCurve.size) {
            val ret = (equityCurve[i] - equityCurve[i - 1]).divide(equityCurve[i - 1], 6, RoundingMode.HALF_UP).toDouble()
            returns.add(ret)
        }
        val avgReturn = if (returns.isNotEmpty()) returns.average() else 0.0
        val stdDev = if (returns.size > 1) {
            kotlin.math.sqrt(returns.map { (it - avgReturn) * (it - avgReturn) }.average())
        } else 0.0
        val sharpeRatio = if (stdDev > 0.0) (avgReturn / stdDev) * kotlin.math.sqrt(365.0 * 288.0) else 0.0  // 288 = 5m candles/day

        var maxDrawdown = 0.0
        var peak = initialCapital
        equityCurve.forEach { equity ->
            if (equity > peak) peak = equity
            val dd = ((peak - equity) / peak).toDouble() * 100
            if (dd > maxDrawdown) maxDrawdown = dd
        }

        println()
        println("=".repeat(90))
        println("🏁 FINAL RESULTS")
        println("=".repeat(90))
        println("Configuration:")
        println("  Volume Threshold:     ${config.technical.minVolumeRatio}x (basically disabled)")
        println("  Confirmation Candles: ${config.strategy.confirmationCandles} (instant)")
        println("  ADX Threshold:        ${config.strategy.adxTrendThreshold} (very low)")
        println("  Stop Loss:            ${config.strategy.stopLossAtrMultiplier}× ATR (tight)")
        println("  Take Profit:          ${config.strategy.takeProfitAtrMultiplier}× ATR")
        println("  Position Size:        ${(config.strategy.trendPositionPercent.toDouble() * 100).toInt()}%")
        println("  Leverage:             ${config.strategy.leverage}×")
        println()
        println("Performance:")
        val pnlSign = if (pnlPercent >= 0) "+" else ""
        println("  Total Trades:    $tradeCount")
        println("  Win Rate:        ${"%.0f".format(winRate)}%")
        println("  Final Equity:    ${finalEquity.setScale(2, RoundingMode.HALF_UP)} USD")
        println("  Total PnL:       $pnlSign${totalPnL.setScale(2, RoundingMode.HALF_UP)} USD ($pnlSign${"%.2f".format(pnlPercent)}%)")
        println("  Sharpe Ratio:    ${"%.2f".format(sharpeRatio)}")
        println("  Max Drawdown:    ${"%.2f".format(maxDrawdown)}%")
        println()

        if (tradeCount > 0) {
            println("📊 TRADE BREAKDOWN:")
            val avgWin = trades.filter { it.pnl > BigDecimal.ZERO }.map { it.pnl.toDouble() }.average()
            val avgLoss = trades.filter { it.pnl < BigDecimal.ZERO }.map { it.pnl.toDouble() }.average()
            val avgTradeSize = trades.map { it.pnl.toDouble() }.average()

            println("  Avg Win:     $${if (avgWin.isNaN()) "0.00" else "%.2f".format(avgWin)}")
            println("  Avg Loss:    $${if (avgLoss.isNaN()) "0.00" else "%.2f".format(avgLoss)}")
            println("  Avg Trade:   $${if (avgTradeSize.isNaN()) "0.00" else "%.2f".format(avgTradeSize)}")
            println()

            println("First 5 Trades:")
            trades.take(5).forEach { trade ->
                val pnlSign2 = if (trade.pnl >= BigDecimal.ZERO) "+" else ""
                println("  #${trade.tradeNumber}: ${trade.side} @ ${trade.entryPrice.setScale(2, RoundingMode.HALF_UP)} → ${trade.exitPrice.setScale(2, RoundingMode.HALF_UP)} | " +
                    "PnL: $pnlSign2${trade.pnl.setScale(2, RoundingMode.HALF_UP)} ($pnlSign2${"%.3f".format(trade.pnlPercent)}%)")
            }
            if (trades.size > 5) {
                println("  ... (${trades.size - 5} more trades)")
            }
        }

        println("=".repeat(90))

        if (tradeCount == 0) {
            println("\n❌ CRITICAL: Even with NO FILTERS, system won't trade.")
            println("   This suggests a fundamental issue in ExecuteTradingCycleUseCase logic.")
        } else if (tradeCount < 10) {
            println("\n⚠️  Only $tradeCount trades. Need higher frequency.")
            println("   Consider: Even looser filters or different timeframe.")
        } else {
            println("\n✅ SUCCESS! System is actively trading ($tradeCount trades).")
            if (pnlPercent > 0) {
                println("   PROFITABLE! This configuration works.")
            } else {
                println("   Unprofitable, but NOW WE HAVE DATA TO OPTIMIZE.")
            }
        }
    }

    private data class TradeRecord(
        val tradeNumber: Int,
        val entryPrice: BigDecimal,
        val exitPrice: BigDecimal,
        val side: com.tradeflow.core.domain.model.OrderSide,
        val pnl: BigDecimal,
        val pnlPercent: Double
    )
}
