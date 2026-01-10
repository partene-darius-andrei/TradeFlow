package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.BracketOrderRepository
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.strategy.DecisionEngine
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class CycleResult(
    val execution: ExecutionResult,
    val updatedHighWaterMark: BigDecimal
)

/**
 * Single orchestrator for the trading cycle.
 */
class TradeOrchestrator @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val bracketOrderRepository: BracketOrderRepository,
    private val decisionEngine: DecisionEngine,
) {
    suspend fun runCycle(productId: String, highWaterMark: BigDecimal): CycleResult {
        return try {
            val portfolio = exchangeRepository.getPortfolio().getOrThrow()
            val currentPrice = exchangeRepository.getCurrentPrice(productId).getOrThrow().price
            val candles = exchangeRepository.getCandles(productId, Granularity.FOUR_HOUR).getOrThrow()
            val openOrders = exchangeRepository.getOpenOrders(productId).getOrThrow()

            val currentHighWaterMark = if (portfolio.totalEquityUsd > highWaterMark) {
                portfolio.totalEquityUsd
            } else {
                highWaterMark
            }

            // 1. Risk Check
            if (currentHighWaterMark > BigDecimal.ZERO) {
                val drawdown = (currentHighWaterMark - portfolio.totalEquityUsd)
                    .divide(currentHighWaterMark, 4, RoundingMode.HALF_UP)
                if (drawdown > BigDecimal("0.15")) {
                    exchangeRepository.cancelOrders(openOrders.map { it.id })
                    val btc = portfolio.getBtcBalance()
                    if (btc > BigDecimal("0.00001")) {
                        exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, btc)
                    }
                    return CycleResult(
                        ExecutionResult.Failed("EMERGENCY: 15% Drawdown reached. Liquidated."),
                        currentHighWaterMark
                    )
                }
            }

            // 2. State Check
            val btcBalance = portfolio.getBtcBalance()
            val hasBtcBalance = btcBalance > BigDecimal("0.00001")
            val hasOpenOrders = openOrders.isNotEmpty()
            val isInTrade = hasBtcBalance || hasOpenOrders

            val decision = decisionEngine.evaluate(candles, currentPrice)

            // 3. Execution
            val executionResult = when (decision) {
                is Decision.Wait -> ExecutionResult.Skipped("Wait: ${decision.reason}")
                is Decision.Defense -> {
                    val buys = openOrders.filter { it.side == OrderSide.BUY }.map { it.id }
                    if (buys.isNotEmpty()) exchangeRepository.cancelOrders(buys)

                    if (hasBtcBalance) {
                        exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, btcBalance).getOrThrow()
                        ExecutionResult.Success("Defense: Liquidated holdings.")
                    } else {
                        ExecutionResult.Skipped("Defense: Clean.")
                    }
                }
                is Decision.Trend -> {
                    if (!isInTrade) {
                        val sizeUsd = portfolio.totalEquityUsd * decision.positionSizePercent
                        val btcSize = sizeUsd.divide(decision.entryPrice, 8, RoundingMode.HALF_UP)

                        bracketOrderRepository.placeBracketOrder(
                            productId, decision.direction, btcSize,
                            decision.entryPrice, decision.takeProfit, decision.stopLoss
                        ).getOrThrow()
                        ExecutionResult.Success("Trend: Opened position.")
                    } else ExecutionResult.Skipped("Trend: Already in trade.")
                }
                is Decision.Range -> {
                    if (!isInTrade) {
                        val sizeUsd = portfolio.totalEquityUsd * decision.positionSizePercentPerLevel
                        var ordersPlaced = 0

                        for (level in 1..decision.levels) {
                            val levelPrice = currentPrice - (decision.gridSpacing * BigDecimal(level))
                            val btcSize = sizeUsd.divide(levelPrice, 8, RoundingMode.HALF_UP)

                            exchangeRepository.placeLimitOrder(
                                productId, OrderSide.BUY, btcSize, levelPrice, true
                            ).onSuccess { ordersPlaced++ }
                        }

                        ExecutionResult.Success("Range: Placed $ordersPlaced/${decision.levels} grid orders.")
                    } else if (hasBtcBalance && openOrders.none { it.side == OrderSide.SELL }) {
                        val targetProfitPrice = currentPrice + decision.gridSpacing
                        exchangeRepository.placeLimitOrder(productId, OrderSide.SELL, btcBalance, targetProfitPrice, true).getOrThrow()
                        ExecutionResult.Success("Range: Placed take-profit for grid fill.")
                    } else {
                        ExecutionResult.Skipped("Range: Active.")
                    }
                }
            }

            CycleResult(executionResult, currentHighWaterMark)
        } catch (e: Exception) {
            CycleResult(
                ExecutionResult.Failed("Cycle failed: ${e.message}"),
                highWaterMark
            )
        }
    }
}
