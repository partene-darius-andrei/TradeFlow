package com.tradeflow.core.domain.repository

import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Granularity
import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.Ticker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ExchangeWebSocket {
    val connectionState: StateFlow<ConnectionState>

    fun connect()
    fun disconnect()

    fun subscribeTicker(productIds: List<String>): Flow<Ticker>
    fun subscribeCandles(productId: String, granularity: Granularity): Flow<Candle>

    fun subscribeOrderUpdates(): Flow<Order>
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
}
