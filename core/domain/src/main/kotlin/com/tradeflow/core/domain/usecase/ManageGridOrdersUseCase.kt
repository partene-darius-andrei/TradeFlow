package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderType
import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.risk.RiskManager
import com.tradeflow.core.domain.risk.model.PlaceOrderRequest
import com.tradeflow.core.domain.risk.model.RiskCheck
import com.tradeflow.core.domain.usecase.model.ExecutionResult
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class ManageGridOrdersUseCase @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val riskManager: RiskManager
) {

    suspend fun execute(
        productId: String,
        decision: Decision.Range,
        portfolio: Portfolio,
        currentPrice: BigDecimal
    ): ExecutionResult {
        val gridSpacingPercent = decision.gridSpacing
            .divide(currentPrice, 8, RoundingMode.HALF_UP)

        if (!riskManager.validateGridSpacing(gridSpacingPercent)) {
            return ExecutionResult.Skipped(
                "Range: Grid spacing ${formatPercent(gridSpacingPercent)} below minimum (1.5%)"
            )
        }

        val openOrders = exchangeRepository.getOpenOrders(productId)
            .getOrNull() ?: return ExecutionResult.Failed("Cannot fetch open orders")

        val gridOrders = openOrders.filter { it.type == OrderType.LIMIT && it.side == OrderSide.BUY }

        val gridPrices = calculateGridPrices(currentPrice, decision.gridSpacing, decision.levels)

        val existingPrices = gridOrders.map { it.price }.filterNotNull().toSet()

        val missingPrices = gridPrices.filter { price ->
            existingPrices.none { existingPrice ->
                (existingPrice - price).abs() < decision.gridSpacing.multiply(BigDecimal("0.1"))
            }
        }

        if (missingPrices.isEmpty()) {
            return ExecutionResult.Skipped("Range: All ${decision.levels} grid levels active")
        }

        val positionSizePerLevel = riskManager.calculateGridPositionSize(
            portfolio,
            decision.levels,
            currentPrice
        )

        val placedOrders = mutableListOf<String>()
        val failedOrders = mutableListOf<String>()

        for (gridPrice in missingPrices) {
            val request = PlaceOrderRequest(
                productId = productId,
                side = OrderSide.BUY,
                type = OrderType.LIMIT,
                size = positionSizePerLevel,
                price = gridPrice
            )

            val riskCheck = riskManager.validateOrder(request, portfolio, currentPrice)

            when (riskCheck) {
                is RiskCheck.Approved -> {
                    val orderResult = exchangeRepository.placeLimitOrder(
                        productId = productId,
                        side = OrderSide.BUY,
                        size = positionSizePerLevel,
                        price = gridPrice,
                        postOnly = true
                    )

                    if (orderResult.isSuccess) {
                        placedOrders.add("$gridPrice")
                    } else {
                        failedOrders.add("$gridPrice: ${orderResult.exceptionOrNull()?.message}")
                    }
                }
                is RiskCheck.Rejected -> {
                    failedOrders.add("$gridPrice: ${riskCheck.reason}")
                }
            }
        }

        return when {
            placedOrders.isNotEmpty() && failedOrders.isEmpty() ->
                ExecutionResult.Success(
                    "Range: Placed ${placedOrders.size} grid orders at levels: ${placedOrders.joinToString(", ")}"
                )
            placedOrders.isNotEmpty() && failedOrders.isNotEmpty() ->
                ExecutionResult.Success(
                    "Range: Placed ${placedOrders.size} orders (${failedOrders.size} failed)"
                )
            else ->
                ExecutionResult.Failed("Range: All grid orders failed - ${failedOrders.joinToString("; ")}")
        }
    }

    private fun calculateGridPrices(
        currentPrice: BigDecimal,
        gridSpacing: BigDecimal,
        levels: Int
    ): List<BigDecimal> {
        return (1..levels).map { level ->
            currentPrice - (gridSpacing * BigDecimal(level))
        }
    }

    private fun formatPercent(value: BigDecimal): String {
        return "${(value * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)}%"
    }
}
