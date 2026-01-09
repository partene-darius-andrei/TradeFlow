package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderStatus
import com.tradeflow.core.domain.model.OrderType
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.repository.TradingDataRepository
import com.tradeflow.core.domain.risk.RiskManager
import com.tradeflow.core.domain.risk.model.PlaceOrderRequest
import com.tradeflow.core.domain.risk.model.RiskCheck
import com.tradeflow.core.domain.usecase.model.ExecutionResult
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Handles the "sell side" of grid trading strategy.
 *
 * When a grid BUY order fills:
 * 1. Calculate profitable SELL price (buy price + grid spacing)
 * 2. Place LIMIT SELL order at that price
 * 3. Profit = spread between buy/sell (minus fees ~0.5%)
 *
 * This completes the grid trading cycle:
 * - ManageGridOrdersUseCase: Places BUY orders below current price
 * - HandleGridFillsUseCase: Places SELL orders when BUYs fill
 * - Result: Profit on each price oscillation in ranging market
 */
class HandleGridFillsUseCase @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val tradingDataRepository: TradingDataRepository,
    private val riskManager: RiskManager
) {

    suspend fun execute(
        productId: String,
        gridSpacing: BigDecimal,
        portfolio: com.tradeflow.core.domain.model.Portfolio,
        currentPrice: BigDecimal
    ): ExecutionResult {
        // Get recent filled BUY orders from local database
        val filledGridBuys = tradingDataRepository.getRecentFilledOrders(productId, limit = 50)
            .filter { order ->
                order.side == OrderSide.BUY &&
                order.type == OrderType.LIMIT &&
                order.price != null &&
                order.filledSize > BigDecimal.ZERO
            }

        if (filledGridBuys.isEmpty()) {
            return ExecutionResult.Skipped("No filled grid BUY orders")
        }

        // Check if we already have open SELL orders
        val openSells = tradingDataRepository.getOpenOrders(productId)
            .filter { it.side == OrderSide.SELL }
        val openSellPrices = openSells.mapNotNull { it.price }.toSet()

        val placedOrders = mutableListOf<String>()
        val skippedOrders = mutableListOf<String>()
        val failedOrders = mutableListOf<String>()

        for (filledBuy in filledGridBuys) {
            val buyPrice = filledBuy.price!!
            val buySize = filledBuy.filledSize

            // Calculate profitable sell price (buy + grid spacing)
            val sellPrice = buyPrice + gridSpacing

            // Check if we already have a SELL order near this price
            val alreadyHasSellOrder = openSellPrices.any { existingPrice ->
                (existingPrice - sellPrice).abs() < gridSpacing.multiply(BigDecimal("0.1"))
            }

            if (alreadyHasSellOrder) {
                skippedOrders.add("$sellPrice (already exists)")
                continue
            }

            // Validate with risk manager
            val request = PlaceOrderRequest(
                productId = productId,
                side = OrderSide.SELL,
                type = OrderType.LIMIT,
                size = buySize,
                price = sellPrice
            )

            val riskCheck = riskManager.validateOrder(request, portfolio, currentPrice)

            when (riskCheck) {
                is RiskCheck.Approved -> {
                    val orderResult = exchangeRepository.placeLimitOrder(
                        productId = productId,
                        side = OrderSide.SELL,
                        size = buySize,
                        price = sellPrice,
                        postOnly = true
                    )

                    if (orderResult.isSuccess) {
                        val profitPercent = ((sellPrice - buyPrice) / buyPrice * BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP)
                        placedOrders.add("SELL @ $sellPrice (+$profitPercent%)")
                    } else {
                        failedOrders.add("$sellPrice: ${orderResult.exceptionOrNull()?.message}")
                    }
                }
                is RiskCheck.Rejected -> {
                    skippedOrders.add("$sellPrice: ${riskCheck.reason}")
                }
            }
        }

        return when {
            placedOrders.isNotEmpty() && failedOrders.isEmpty() ->
                ExecutionResult.Success(
                    "Grid: Placed ${placedOrders.size} SELL orders - ${placedOrders.joinToString(", ")}"
                )
            placedOrders.isNotEmpty() && failedOrders.isNotEmpty() ->
                ExecutionResult.Success(
                    "Grid: Placed ${placedOrders.size} SELLs (${failedOrders.size} failed, ${skippedOrders.size} skipped)"
                )
            skippedOrders.isNotEmpty() ->
                ExecutionResult.Skipped("Grid: ${skippedOrders.joinToString(", ")}")
            else ->
                ExecutionResult.Failed("Grid: All SELL orders failed - ${failedOrders.joinToString("; ")}")
        }
    }
}
