package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Balance
import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderStatus
import com.tradeflow.core.domain.model.OrderType
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.usecase.model.ExecutionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertTrue

class HandleEmergencyUseCaseTest {

    private val exchangeRepository: ExchangeRepository = mockk()
    private val useCase = HandleEmergencyUseCase(exchangeRepository)

    private val productId = "BTC-USD"

    @Test
    fun `execute cancels all orders and liquidates BTC`() = runTest {
        val openOrders = listOf(
            createTestOrder("order1", OrderStatus.OPEN),
            createTestOrder("order2", OrderStatus.OPEN)
        )

        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(openOrders)
        coEvery { exchangeRepository.cancelOrders(any()) } returns Result.success(2)
        coEvery { exchangeRepository.getBalances() } returns Result.success(
            listOf(
                Balance("USD", BigDecimal("500"), BigDecimal("500")),
                Balance("BTC", BigDecimal("0.01"), BigDecimal("0.01"))
            )
        )
        coEvery {
            exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, BigDecimal("0.01"))
        } returns Result.success(createTestOrder("sell1", OrderStatus.FILLED))

        val result = useCase.execute(productId)

        assertTrue(result is ExecutionResult.Success)
        assertTrue((result as ExecutionResult.Success).message.contains("Canceled 2 orders"))
        assertTrue(result.message.contains("Market sold 0.01 BTC"))

        coVerify { exchangeRepository.cancelOrders(listOf("order1", "order2")) }
        coVerify { exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, BigDecimal("0.01")) }
    }

    @Test
    fun `execute handles no open orders`() = runTest {
        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(emptyList())
        coEvery { exchangeRepository.getBalances() } returns Result.success(
            listOf(
                Balance("USD", BigDecimal("500"), BigDecimal("500")),
                Balance("BTC", BigDecimal.ZERO, BigDecimal.ZERO)
            )
        )

        val result = useCase.execute(productId)

        assertTrue(result is ExecutionResult.Success)
        assertTrue((result as ExecutionResult.Success).message.contains("No open orders"))
        assertTrue(result.message.contains("No BTC to liquidate"))
    }

    @Test
    fun `execute handles zero BTC balance`() = runTest {
        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(emptyList())
        coEvery { exchangeRepository.getBalances() } returns Result.success(
            listOf(
                Balance("USD", BigDecimal("500"), BigDecimal("500")),
                Balance("BTC", BigDecimal.ZERO, BigDecimal.ZERO)
            )
        )

        val result = useCase.execute(productId)

        assertTrue(result is ExecutionResult.Success)
        coVerify(exactly = 0) { exchangeRepository.placeMarketOrder(any(), any(), any()) }
    }

    @Test
    fun `execute returns failure when cannot fetch balances`() = runTest {
        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(emptyList())
        coEvery { exchangeRepository.getBalances() } returns Result.failure(Exception("API error"))

        val result = useCase.execute(productId)

        assertTrue(result is ExecutionResult.Failed)
        assertTrue((result as ExecutionResult.Failed).error.contains("Cannot fetch balances"))
    }

    @Test
    fun `execute returns failure when market sell fails`() = runTest {
        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(emptyList())
        coEvery { exchangeRepository.getBalances() } returns Result.success(
            listOf(
                Balance("USD", BigDecimal("500"), BigDecimal("500")),
                Balance("BTC", BigDecimal("0.01"), BigDecimal("0.01"))
            )
        )
        coEvery {
            exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, any())
        } returns Result.failure(Exception("Order rejected"))

        val result = useCase.execute(productId)

        assertTrue(result is ExecutionResult.Failed)
        assertTrue((result as ExecutionResult.Failed).error.contains("CRITICAL"))
        assertTrue(result.error.contains("Failed to liquidate"))
    }

    private fun createTestOrder(id: String, status: OrderStatus): Order {
        return Order(
            id = id,
            clientOrderId = "client-$id",
            productId = productId,
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            status = status,
            size = BigDecimal("0.001"),
            price = BigDecimal("40000"),
            filledSize = BigDecimal.ZERO,
            avgFilledPrice = null,
            createdAt = Instant.now()
        )
    }
}
