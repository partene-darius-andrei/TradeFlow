package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.getBtcBalance
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.usecase.model.ExecutionResult
import java.math.BigDecimal
import javax.inject.Inject

class HandleEmergencyUseCase @Inject constructor(
    private val exchangeRepository: ExchangeRepository
) {

    suspend fun execute(productId: String): ExecutionResult {
        val steps = mutableListOf<String>()

        val openOrders = exchangeRepository.getOpenOrders(productId)
            .getOrNull() ?: emptyList()

        if (openOrders.isNotEmpty()) {
            val orderIds = openOrders.map { it.id }
            val cancelResult = exchangeRepository.cancelOrders(orderIds)

            when {
                cancelResult.isSuccess -> {
                    val canceledCount = cancelResult.getOrNull() ?: 0
                    steps.add("Canceled $canceledCount orders")
                }
                else -> {
                    steps.add("Failed to cancel orders: ${cancelResult.exceptionOrNull()?.message}")
                }
            }
        } else {
            steps.add("No open orders to cancel")
        }

        val balancesResult = exchangeRepository.getBalances()
        if (balancesResult.isFailure) {
            return ExecutionResult.Failed(
                "Emergency liquidation failed: Cannot fetch balances - ${balancesResult.exceptionOrNull()?.message}"
            )
        }

        val balances = balancesResult.getOrThrow()
        val portfolio = com.tradeflow.core.domain.model.Portfolio(
            balances = balances,
            totalEquityUsd = BigDecimal.ZERO,
            timestamp = java.time.Instant.now()
        )
        val btcBalance = portfolio.getBtcBalance()

        if (btcBalance > BigDecimal.ZERO) {
            val marketSellResult = exchangeRepository.placeMarketOrder(
                productId = productId,
                side = OrderSide.SELL,
                size = btcBalance
            )

            when {
                marketSellResult.isSuccess -> {
                    steps.add("Market sold $btcBalance BTC")
                }
                else -> {
                    return ExecutionResult.Failed(
                        "CRITICAL: Failed to liquidate $btcBalance BTC - ${marketSellResult.exceptionOrNull()?.message}"
                    )
                }
            }
        } else {
            steps.add("No BTC to liquidate")
        }

        return ExecutionResult.Success("Emergency liquidation complete: ${steps.joinToString(", ")}")
    }
}
