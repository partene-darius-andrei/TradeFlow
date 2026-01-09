package com.tradeflow.core.domain.repository

import com.tradeflow.core.domain.model.Order

/**
 * Repository for querying local trading data (orders, decisions, portfolio snapshots).
 * This separates concerns between:
 * - ExchangeRepository: Remote API calls to Coinbase
 * - TradingDataRepository: Local database queries (Room DAOs)
 */
interface TradingDataRepository {

    /**
     * Get recently filled orders for grid trading logic.
     * Returns orders with status = FILLED, ordered by creation time (most recent first).
     *
     * @param productId Product to filter by (e.g., "BTC-USD")
     * @param limit Maximum number of orders to return
     * @return List of filled orders
     */
    suspend fun getRecentFilledOrders(
        productId: String,
        limit: Int = 50
    ): List<Order>

    /**
     * Get all open orders for the product.
     * Returns orders with status = OPEN or PENDING.
     *
     * @param productId Product to filter by
     * @return List of open orders
     */
    suspend fun getOpenOrders(productId: String): List<Order>
}
