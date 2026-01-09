package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderStatus
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.usecase.model.ExecutionResult
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class ManageOrdersUseCase @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val staleOrderThreshold: Duration = Duration.ofHours(48)
) {

    suspend fun cancelStaleOrders(productId: String): ExecutionResult {
        val openOrders = exchangeRepository.getOpenOrders(productId)
            .getOrNull() ?: return ExecutionResult.Failed("Cannot fetch open orders")

        val now = Instant.now()
        val staleOrders = openOrders.filter { order ->
            Duration.between(order.createdAt, now) > staleOrderThreshold &&
            order.status == OrderStatus.OPEN
        }

        if (staleOrders.isEmpty()) {
            return ExecutionResult.Skipped("No stale orders found")
        }

        val orderIds = staleOrders.map { it.id }
        val cancelResult = exchangeRepository.cancelOrders(orderIds)

        return if (cancelResult.isSuccess) {
            val canceledCount = cancelResult.getOrNull() ?: 0
            ExecutionResult.Success("Canceled $canceledCount stale orders (older than 48 hours)")
        } else {
            ExecutionResult.Failed("Failed to cancel stale orders: ${cancelResult.exceptionOrNull()?.message}")
        }
    }

    suspend fun reconcileOrders(productId: String, localOrders: List<Order>): ExecutionResult {
        val exchangeOrders = exchangeRepository.getOpenOrders(productId)
            .getOrNull() ?: return ExecutionResult.Failed("Cannot fetch exchange orders")

        val exchangeOrderIds = exchangeOrders.map { it.id }.toSet()
        val localOpenOrderIds = localOrders
            .filter { it.status == OrderStatus.OPEN || it.status == OrderStatus.PENDING }
            .map { it.id }
            .toSet()

        val orphanedLocalOrders = localOpenOrderIds - exchangeOrderIds

        val newExchangeOrders = exchangeOrderIds - localOpenOrderIds

        val statusChanges = exchangeOrders
            .filter { exchangeOrder ->
                val localOrder = localOrders.find { it.id == exchangeOrder.id }
                localOrder != null && localOrder.status != exchangeOrder.status
            }
            .map { it.id to it.status }

        return ExecutionResult.Success(
            "Reconciliation: ${orphanedLocalOrders.size} orphaned, " +
            "${newExchangeOrders.size} new, " +
            "${statusChanges.size} status changes"
        )
    }

    data class ReconciliationResult(
        val orphanedLocalOrders: List<String>,
        val newExchangeOrders: List<String>,
        val statusChanges: List<Pair<String, OrderStatus>>
    )
}
