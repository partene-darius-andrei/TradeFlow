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
    suspend fun runCycle(productId: String, highWaterMark: BigDecimal): ExecutionResult {
        return try {
            val portfolio = exchangeRepository.getPortfolio().getOrThrow()
            val currentPrice = exchangeRepository.getCurrentPrice(productId).getOrThrow().price
            val candles = exchangeRepository.getCandles(productId, Granularity.FOUR_HOUR).getOrThrow()
            val openOrders = exchangeRepository.getOpenOrders(productId).getOrThrow()

            // 1. Risk Check
            if (highWaterMark > BigDecimal.ZERO) {
                val drawdown = (highWaterMark - portfolio.totalEquityUsd)
                    .divide(highWaterMark, 4, RoundingMode.HALF_UP)
                if (drawdown > BigDecimal("0.15")) {
                    exchangeRepository.cancelOrders(openOrders.map { it.id })
                    // Emergency liquidate
                    val btc = portfolio.getBtcBalance()
                    if (btc > BigDecimal("0.00001")) {
                        exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, btc)
                    }
                    return ExecutionResult.Failed("EMERGENCY: 15% Drawdown reached. Liquidated.")
                }
            }

            // 2. State Check
            val btcBalance = portfolio.getBtcBalance()
            val hasBtcBalance = btcBalance > BigDecimal("0.00001")
            val hasOpenBuyOrders = openOrders.any { it.side == OrderSide.BUY }
            val isInTrade = hasBtcBalance || hasOpenBuyOrders

            val decision = decisionEngine.evaluate(candles, currentPrice)

            // 3. Execution
            when (decision) {
                is Decision.Wait -> ExecutionResult.Skipped("Wait: ${decision.reason}")
                is Decision.Defense -> {
                    val buys = openOrders.filter { it.side == OrderSide.BUY }.map { it.id }
                    if (buys.isNotEmpty()) exchangeRepository.cancelOrders(buys)
                    
                    // Liquidate existing BTC in Defense mode
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
                        val btcSize = sizeUsd.divide(currentPrice, 8, RoundingMode.HALF_UP)
                        
                        bracketOrderRepository.placeBracketOrder(
                            productId, decision.direction, btcSize, 
                            decision.entryPrice, decision.takeProfit, decision.stopLoss
                        ).getOrThrow()
                        ExecutionResult.Success("Trend: Opened position.")
                    } else ExecutionResult.Skipped("Trend: Already in trade.")
                }
                is Decision.Range -> {
                    if (!isInTrade) {
                        val gridPrice = currentPrice - decision.gridSpacing
                        val sizeUsd = portfolio.totalEquityUsd * decision.positionSizePercentPerLevel
                        val btcSize = sizeUsd.divide(currentPrice, 8, RoundingMode.HALF_UP)
                        
                        exchangeRepository.placeLimitOrder(
                            productId, OrderSide.BUY, btcSize, gridPrice, true
                        ).getOrThrow()
                        
                        // Range Profit taking logic: if we have BTC, we should have a sell order above market
                        ExecutionResult.Success("Range: Placed grid order.")
                    } else if (hasBtcBalance && openOrders.none { it.side == OrderSide.SELL }) {
                        // If we hold BTC from a grid fill, place a sell order at profit
                        val targetProfitPrice = currentPrice + decision.gridSpacing
                        exchangeRepository.placeLimitOrder(productId, OrderSide.SELL, btcBalance, targetProfitPrice, true).getOrThrow()
                        ExecutionResult.Success("Range: Placed take-profit for grid fill.")
                    } else {
                        ExecutionResult.Skipped("Range: Active.")
                    }
                }
            }
        } catch (e: Exception) {
            ExecutionResult.Failed("Cycle failed: ${e.message}")
        }
    }
}
