package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.BracketOrderRepository
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.strategy.DecisionEngine
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Single orchestrator for the trading cycle.
 * Handles risk circuit breaker, evaluation, and execution in one place.
 */
class TradeOrchestrator @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val bracketOrderRepository: BracketOrderRepository,
    private val decisionEngine: DecisionEngine,
) {
    suspend fun runCycle(productId: String, highWaterMark: BigDecimal): ExecutionResult = try {
        // 1. Data Refresh
        val portfolio = exchangeRepository.getPortfolio().getOrThrow()
        val currentPrice = exchangeRepository.getCurrentPrice(productId).getOrThrow().price
        val candles = exchangeRepository.getCandles(productId, Granularity.FOUR_HOUR).getOrThrow()
        val openOrders = exchangeRepository.getOpenOrders(productId).getOrThrow()

        // 2. Risk Circuit Breaker (15% Drawdown)
        if (highWaterMark > BigDecimal.ZERO) {
            val drawdown = (highWaterMark - portfolio.totalEquityUsd)
                .divide(highWaterMark, 4, RoundingMode.HALF_UP)
            if (drawdown > BigDecimal("0.15")) {
                exchangeRepository.cancelOrders(openOrders.map { it.id })
                ExecutionResult.Failed("EMERGENCY: 15% Drawdown reached. All orders canceled.")
            }
        }

        // 3. Evaluation
        val decision = decisionEngine.evaluate(candles, currentPrice)

        // 4. Execution
        when (decision) {
            is Decision.Wait -> ExecutionResult.Skipped("Wait: ${decision.reason}")
            is Decision.Defense -> {
                val buys = openOrders.filter { it.side == OrderSide.BUY }.map { it.id }
                if (buys.isNotEmpty()) {
                    exchangeRepository.cancelOrders(buys)
                    ExecutionResult.Success("Defense: Canceled ${buys.size} buy orders. Reason: ${decision.reason}")
                } else ExecutionResult.Skipped("Defense: No buy orders to cancel.")
            }
            is Decision.Trend -> {
                if (openOrders.none { it.type == OrderType.BRACKET }) {
                    bracketOrderRepository.placeBracketOrder(
                        productId, decision.direction, decision.positionSize,
                        decision.entryPrice, decision.takeProfit, decision.stopLoss
                    ).getOrThrow()
                    ExecutionResult.Success("Trend: Placed bracket order at ${decision.entryPrice}")
                } else ExecutionResult.Skipped("Trend: Position already active.")
            }
            is Decision.Range -> {
                val gridPrice = currentPrice - decision.gridSpacing
                if (openOrders.none { it.side == OrderSide.BUY }) {
                    exchangeRepository.placeLimitOrder(
                        productId, OrderSide.BUY, decision.positionSizePerLevel, gridPrice, true
                    ).getOrThrow()
                    ExecutionResult.Success("Range: Placed grid order at $gridPrice")
                } else ExecutionResult.Skipped("Range: Grid orders already active.")
            }
        }
    } catch (e: Exception) {
        ExecutionResult.Failed("Cycle failed: ${e.message}")
    }
}
