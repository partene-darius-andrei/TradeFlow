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

class RealTradeSimulationTest {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

    @Test
    fun `analyze PnL and equity curve over 30 days of real data`() = runBlocking {
        val config = TradingConfig.forProfile(RiskProfile.BALANCED)
        val initialCapital = BigDecimal("500.00")
        val exchange = SimulatedExchange(initialCapital)

        com.tradeflow.core.domain.repository.DependencyInjection
            .setRepository(exchange)
            .setTradingConfig(config)
            

        val engine = MakeTradingDecisionUseCase()
        val orchestrator = ExecuteTradingCycleUseCase()

        val allCandles = BinanceDataLoader.fetchHistoricalCandles(interval = "4h", limit = 400)
        val primeHistory = allCandles.take(200)
        val simulationDays = allCandles.drop(200)
        
        exchange.setHistory(primeHistory)
        
        println("\n🚀 SIMULATION LOG START")
        println("--------------------------------------------------------------------------------")
        println("INITIAL CAPITAL: $500.00 USD | FEE: 0.60% | STRATEGY: Hysteresis Trend/Range")
        println("--------------------------------------------------------------------------------")

        var highWaterMark = initialCapital

        simulationDays.forEachIndexed { index, candle ->
            exchange.advanceTime(candle)

            val currentEquity = exchange.getTotalEquity()

            val cycleResult = orchestrator.runCycle("BTC-USD", highWaterMark)
            highWaterMark = cycleResult.updatedHighWaterMark

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

        println("--------------------------------------------------------------------------------")
        println("🏁 FINAL PROFIT & LOSS STATEMENT")
        println("--------------------------------------------------------------------------------")
        println("STARTING BALANCE: $initialCapital USD")
        println("FINAL BALANCE:    ${finalEquity.setScale(2, RoundingMode.HALF_UP)} USD")
        println("TOTAL BTC HELD:   ${exchange.btcBalance} BTC")
        println("NET PROFIT/LOSS:  ${if (totalPnL >= BigDecimal.ZERO) "+" else ""}${totalPnL.setScale(2, RoundingMode.HALF_UP)} USD")
        println("PERCENTAGE GAIN:  ${if (totalPnL >= BigDecimal.ZERO) "+" else ""}${totalPnLPct.setScale(2, RoundingMode.HALF_UP)}%")
        println("--------------------------------------------------------------------------------")
        println("🚀 SIMULATION LOG END\n")

        val targetBalance = BigDecimal("480.00")
        assertTrue(
            finalEquity >= targetBalance,
            "❌ FAILED: Balance $finalEquity is below target $targetBalance (max acceptable loss: 4%)"
        )
    }
}
