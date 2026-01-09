package com.tradeflow.core.domain.simulator

import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderStatus
import java.math.BigDecimal

data class Fill(
    val order: Order,
    val fillPrice: BigDecimal,
    val isMaker: Boolean = true  // Limit orders are maker orders
)

class OrderBook(
    private val productId: String = "BTC-USD"
) {
    private val buyOrders = mutableListOf<Order>()  // sorted descending by price (best bid first)
    private val sellOrders = mutableListOf<Order>()  // sorted ascending by price (best ask first)

    fun addOrder(order: Order) {
        require(order.productId == productId) { "Order product ID ${order.productId} doesn't match book $productId" }
        require(order.status == OrderStatus.OPEN) { "Can only add OPEN orders to book" }

        when (order.side) {
            OrderSide.BUY -> {
                buyOrders.add(order)
                buyOrders.sortByDescending { it.price ?: BigDecimal.ZERO }  // Highest bid first
            }
            OrderSide.SELL -> {
                sellOrders.add(order)
                sellOrders.sortBy { it.price ?: BigDecimal.ZERO }  // Lowest ask first
            }
        }
    }

    fun matchOrders(candle: Candle): List<Fill> {
        val fills = mutableListOf<Fill>()

        // Match BUY limit orders (fill if candle low touches order price)
        val buyIterator = buyOrders.iterator()
        while (buyIterator.hasNext()) {
            val order = buyIterator.next()
            val limitPrice = order.price ?: continue

            // BUY order fills when market price drops to or below limit price
            if (candle.low <= limitPrice) {
                fills.add(Fill(
                    order = order.copy(
                        status = OrderStatus.FILLED,
                        filledSize = order.size
                    ),
                    fillPrice = limitPrice,  // Fill at limit price (maker)
                    isMaker = true
                ))
                buyIterator.remove()
            }
        }

        // Match SELL limit orders (fill if candle high touches order price)
        val sellIterator = sellOrders.iterator()
        while (sellIterator.hasNext()) {
            val order = sellIterator.next()
            val limitPrice = order.price ?: continue

            // SELL order fills when market price rises to or above limit price
            if (candle.high >= limitPrice) {
                fills.add(Fill(
                    order = order.copy(
                        status = OrderStatus.FILLED,
                        filledSize = order.size
                    ),
                    fillPrice = limitPrice,  // Fill at limit price (maker)
                    isMaker = true
                ))
                sellIterator.remove()
            }
        }

        return fills
    }

    fun removeOrder(orderId: String): Boolean {
        val buyRemoved = buyOrders.removeIf { it.id == orderId }
        val sellRemoved = sellOrders.removeIf { it.id == orderId }
        return buyRemoved || sellRemoved
    }

    fun getOrder(orderId: String): Order? {
        return buyOrders.find { it.id == orderId } ?: sellOrders.find { it.id == orderId }
    }

    fun getOpenOrders(): List<Order> {
        return (buyOrders + sellOrders).toList()
    }

    fun getOpenOrders(productId: String): List<Order> {
        return if (productId == this.productId) {
            getOpenOrders()
        } else {
            emptyList()
        }
    }

    fun clear() {
        buyOrders.clear()
        sellOrders.clear()
    }

    fun getOrderCount(): Int = buyOrders.size + sellOrders.size

    fun hasOrder(orderId: String): Boolean {
        return buyOrders.any { it.id == orderId } || sellOrders.any { it.id == orderId }
    }

    override fun toString(): String {
        return """
            OrderBook($productId)
            BUY orders: ${buyOrders.size} (best bid: ${buyOrders.firstOrNull()?.price ?: "none"})
            SELL orders: ${sellOrders.size} (best ask: ${sellOrders.firstOrNull()?.price ?: "none"})
        """.trimIndent()
    }
}
