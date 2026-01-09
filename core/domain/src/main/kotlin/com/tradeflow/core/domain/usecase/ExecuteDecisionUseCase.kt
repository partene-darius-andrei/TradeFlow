package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderType
import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.repository.BracketOrderRepository
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.risk.RiskManager
import com.tradeflow.core.domain.risk.model.PlaceOrderRequest
import com.tradeflow.core.domain.risk.model.RiskCheck
import com.tradeflow.core.domain.usecase.model.ExecutionResult
import java.math.BigDecimal
import javax.inject.Inject

class ExecuteDecisionUseCase @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val bracketOrderRepository: BracketOrderRepository,
    private val riskManager: RiskManager,
    private val manageGridOrdersUseCase: ManageGridOrdersUseCase
) {

    suspend fun execute(
        decision: Decision,
        portfolio: Portfolio,
        currentPrice: BigDecimal,
        productId: String
    ): ExecutionResult {
        return when (decision) {
            is Decision.Wait -> ExecutionResult.Skipped("Waiting for confirmation")
            is Decision.Defense -> executeDefense(productId, decision)
            is Decision.Trend -> executeTrend(productId, decision, portfolio, currentPrice)
            is Decision.Range -> executeRange(productId, decision, portfolio, currentPrice)
        }
    }

    private suspend fun executeDefense(productId: String, decision: Decision.Defense): ExecutionResult {
        val openOrders = exchangeRepository.getOpenOrders(productId)
            .getOrNull() ?: return ExecutionResult.Failed("Cannot fetch open orders")

        val buyOrders = openOrders.filter { it.side == OrderSide.BUY }

        if (buyOrders.isEmpty()) {
            return ExecutionResult.Skipped("Defense mode: No buy orders to cancel")
        }

        val orderIds = buyOrders.map { it.id }
        val cancelResult = exchangeRepository.cancelOrders(orderIds)

        return if (cancelResult.isSuccess) {
            val canceledCount = cancelResult.getOrNull() ?: 0
            ExecutionResult.Success("Defense: Canceled $canceledCount buy orders. Reason: ${decision.reason}")
        } else {
            ExecutionResult.Failed("Defense: Failed to cancel orders - ${cancelResult.exceptionOrNull()?.message}")
        }
    }

    private suspend fun executeTrend(
        productId: String,
        decision: Decision.Trend,
        portfolio: Portfolio,
        currentPrice: BigDecimal
    ): ExecutionResult {
        val openOrders = exchangeRepository.getOpenOrders(productId)
            .getOrNull() ?: return ExecutionResult.Failed("Cannot fetch open orders")

        val hasActiveTrendPosition = openOrders.any {
            it.type == OrderType.BRACKET && it.side == decision.direction
        }

        if (hasActiveTrendPosition) {
            return ExecutionResult.Skipped("Already have active trend position")
        }

        val positionSize = riskManager.calculateTrendPositionSize(portfolio, decision.entryPrice)

        val request = PlaceOrderRequest(
            productId = productId,
            side = decision.direction,
            type = OrderType.BRACKET,
            size = positionSize,
            price = decision.entryPrice,
            stopLoss = decision.stopLoss,
            takeProfit = decision.takeProfit
        )

        val riskCheck = riskManager.validateOrder(request, portfolio, currentPrice)

        return when (riskCheck) {
            is RiskCheck.Approved -> {
                val orderResult = bracketOrderRepository.placeBracketOrder(
                    productId = productId,
                    side = decision.direction,
                    size = positionSize,
                    entryPrice = decision.entryPrice,
                    takeProfit = decision.takeProfit,
                    stopLoss = decision.stopLoss
                )

                if (orderResult.isSuccess) {
                    val order = orderResult.getOrThrow()
                    ExecutionResult.Success(
                        "Trend: Placed ${decision.direction} bracket order " +
                        "(size: $positionSize BTC, entry: ${decision.entryPrice}, " +
                        "TP: ${decision.takeProfit}, SL: ${decision.stopLoss}). Order ID: ${order.id}"
                    )
                } else {
                    ExecutionResult.Failed("Trend: Order placement failed - ${orderResult.exceptionOrNull()?.message}")
                }
            }
            is RiskCheck.Rejected -> ExecutionResult.Skipped("Trend: Risk check rejected - ${riskCheck.reason}")
        }
    }

    private suspend fun executeRange(
        productId: String,
        decision: Decision.Range,
        portfolio: Portfolio,
        currentPrice: BigDecimal
    ): ExecutionResult {
        return manageGridOrdersUseCase.execute(productId, decision, portfolio, currentPrice)
    }
}
