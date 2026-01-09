package com.tradeflow.core.data.repository

import com.tradeflow.core.data.local.dao.OrderDao
import com.tradeflow.core.data.mapper.toDomain
import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.repository.TradingDataRepository
import javax.inject.Inject

class TradingDataRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao
) : TradingDataRepository {

    override suspend fun getRecentFilledOrders(productId: String, limit: Int): List<Order> {
        return orderDao.getRecentFilledOrders(productId, limit).map { it.toDomain() }
    }

    override suspend fun getOpenOrders(productId: String): List<Order> {
        return orderDao.getActiveOrdersForProduct(productId).map { it.toDomain() }
    }
}
