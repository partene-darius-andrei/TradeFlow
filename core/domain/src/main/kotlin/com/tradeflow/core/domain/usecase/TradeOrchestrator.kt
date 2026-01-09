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
 */
class TradeOrchestrator @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val bracketOrderRepository: BracketOrderRepository,
    private val decisionEngine: DecisionEngine,
) {
    suspend fun runCycle(productId: String, highWaterMark: BigDecimal): ExecutionResult = try {
        val portfolio = exchangeRepository.getPortfolio().getOrThrow()
        val currentPrice = exchangeRepository.getCurrentPrice(productId).getOrThrow().price
        val candles = exchangeRepository.getCandles(productId, Granularity.FOUR_HOUR).getOrThrow()
        val openOrders = exchangeRepository.getOpenOrders(productId).getOrThrow()

        if (highWaterMark > BigDecimal.ZERO) {
            val drawdown = (highWaterMark - portfolio.totalEquityUsd)
                .divide(highWaterMark, 4, RoundingMode.HALF_UP)
            if (drawdown > BigDecimal("0.15")) {
                exchangeRepository.cancelOrders(openOrders.map { it.id })
                ExecutionResult.Failed("EMERGENCY: 15% Drawdown reached.")
            }
        }

        val decision = decisionEngine.evaluate(candles, currentPrice)

        when (decision) {
            is Decision.Wait -> ExecutionResult.Skipped("Wait: ${decision.reason}")
            is Decision.Defense -> {
                val buys = openOrders.filter { it.side == OrderSide.BUY }.map { it.id }
                if (buys.isNotEmpty()) {
                    exchangeRepository.cancelOrders(buys)
                    ExecutionResult.Success("Defense: Canceled ${buys.size} buy orders.")
                } else ExecutionResult.Skipped("Defense: No buy orders.")
            }
            is Decision.Trend -> {
                if (openOrders.none { it.type == OrderType.BRACKET }) {
                    bracketOrderRepository.placeBracketOrder(
                        productId, decision.direction, decision.positionSize, 
                        decision.entryPrice, decision.takeProfit, decision.stopLoss
                    ).getOrThrow()
                    ExecutionResult.Success("Trend: Placed bracket order.")
                } else ExecutionResult.Skipped("Trend: Active.")
            }
            is Decision.Range -> {
                val gridPrice = currentPrice - decision.gridSpacing
                if (openOrders.none { it.side == OrderSide.BUY }) {
                    exchangeRepository.placeLimitOrder(
                        productId, OrderSide.BUY, decision.positionSizePerLevel, gridPrice, true
                    ).getOrThrow()
                    ExecutionResult.Success("Range: Placed grid order.")
                } else ExecutionResult.Skipped("Range: Active.")
            }
        }
    } catch (e: Exception) {
        ExecutionResult.Failed("Cycle failed: ${e.message}")
    }
}
