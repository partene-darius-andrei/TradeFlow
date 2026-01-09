package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Balance
import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderStatus
import com.tradeflow.core.domain.model.OrderType
import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.repository.TradingDataRepository
import com.tradeflow.core.domain.risk.RiskManager
import com.tradeflow.core.domain.risk.model.RiskCheck
import com.tradeflow.core.domain.usecase.model.ExecutionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandleGridFillsUseCaseTest {

    private val exchangeRepository: ExchangeRepository = mockk()
    private val tradingDataRepository: TradingDataRepository = mockk()
    private val riskManager: RiskManager = mockk()
    private val useCase = HandleGridFillsUseCase(exchangeRepository, tradingDataRepository, riskManager)

    private val productId = "BTC-USD"
    private val gridSpacing = BigDecimal("1000")
    private val currentPrice = BigDecimal("100000")
    private val portfolio = Portfolio(
        balances = listOf(
            Balance("USD", BigDecimal("50000"), BigDecimal("50000")),
            Balance("BTC", BigDecimal("0.5"), BigDecimal("50000"))
        ),
        totalEquityUsd = BigDecimal("100000"),
        timestamp = Instant.now()
    )

    @Test
    fun `places SELL orders for filled grid BUYs`() = runTest {
        val filledBuy = Order(
            id = "order-1",
            clientOrderId = "client-1",
            productId = productId,
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            status = OrderStatus.FILLED,
            size = BigDecimal("0.1"),
            filledSize = BigDecimal("0.1"),
            price = BigDecimal("99000"),
            avgFilledPrice = BigDecimal("99000"),
            createdAt = Instant.now().minusSeconds(300)
        )

        coEvery { tradingDataRepository.getRecentFilledOrders(productId, 50) } returns listOf(filledBuy)
        coEvery { tradingDataRepository.getOpenOrders(productId) } returns emptyList()
        every { riskManager.validateOrder(any(), any(), any()) } returns RiskCheck.Approved
        coEvery {
            exchangeRepository.placeLimitOrder(productId, OrderSide.SELL, BigDecimal("0.1"), BigDecimal("100000"), true)
        } returns Result.success(mockk())

        val result = useCase.execute(productId, gridSpacing, portfolio, currentPrice)

        // Check result type first
        when (result) {
            is ExecutionResult.Success -> {
                // Test passes - check message content
                assertTrue(result.message.contains("SELL"))
            }
            is ExecutionResult.Failed -> {
                throw AssertionError("Expected Success but got Failed: ${result.error}")
            }
            is ExecutionResult.Skipped -> {
                throw AssertionError("Expected Success but got Skipped: ${result.reason}")
            }
        }

        coVerify {
            exchangeRepository.placeLimitOrder(
                productId = productId,
                side = OrderSide.SELL,
                size = BigDecimal("0.1"),
                price = BigDecimal("100000"),
                postOnly = true
            )
        }
    }

    @Test
    fun `skips when no filled grid BUY orders`() = runTest {
        coEvery { tradingDataRepository.getRecentFilledOrders(productId, 50) } returns emptyList()

        val result = useCase.execute(productId, gridSpacing, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Skipped)
        assertEquals("No filled grid BUY orders", result.reason)
    }

    @Test
    fun `skips when SELL orders already exist at target prices`() = runTest {
        val filledBuy = Order(
            id = "order-1",
            clientOrderId = "client-1",
            productId = productId,
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            status = OrderStatus.FILLED,
            size = BigDecimal("0.1"),
            filledSize = BigDecimal("0.1"),
            price = BigDecimal("99000"),
            avgFilledPrice = BigDecimal("99000"),
            createdAt = Instant.now().minusSeconds(300)
        )

        val existingSell = Order(
            id = "order-2",
            clientOrderId = "client-2",
            productId = productId,
            side = OrderSide.SELL,
            type = OrderType.LIMIT,
            status = OrderStatus.OPEN,
            size = BigDecimal("0.1"),
            filledSize = BigDecimal.ZERO,
            price = BigDecimal("100000"), // Same as target sell price
            avgFilledPrice = null,
            createdAt = Instant.now()
        )

        coEvery { tradingDataRepository.getRecentFilledOrders(productId, 50) } returns listOf(filledBuy)
        coEvery { tradingDataRepository.getOpenOrders(productId) } returns listOf(existingSell)

        val result = useCase.execute(productId, gridSpacing, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Skipped)
        assertTrue(result.reason.contains("already exist"))
    }

    @Test
    fun `handles multiple filled BUY orders`() = runTest {
        val filledBuy1 = Order(
            id = "order-1",
            clientOrderId = "client-1",
            productId = productId,
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            status = OrderStatus.FILLED,
            size = BigDecimal("0.1"),
            filledSize = BigDecimal("0.1"),
            price = BigDecimal("99000"),
            avgFilledPrice = BigDecimal("99000"),
            createdAt = Instant.now().minusSeconds(300)
        )

        val filledBuy2 = Order(
            id = "order-2",
            clientOrderId = "client-2",
            productId = productId,
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            status = OrderStatus.FILLED,
            size = BigDecimal("0.1"),
            filledSize = BigDecimal("0.1"),
            price = BigDecimal("98000"),
            avgFilledPrice = BigDecimal("98000"),
            createdAt = Instant.now().minusSeconds(600)
        )

        coEvery { tradingDataRepository.getRecentFilledOrders(productId, 50) } returns listOf(filledBuy1, filledBuy2)
        coEvery { tradingDataRepository.getOpenOrders(productId) } returns emptyList()
        every { riskManager.validateOrder(any(), any(), any()) } returns RiskCheck.Approved
        coEvery { exchangeRepository.placeLimitOrder(any(), any(), any(), any(), any()) } returns Result.success(mockk())

        val result = useCase.execute(productId, gridSpacing, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Success)
        assertTrue(result.message.contains("Placed 2 SELL"))

        coVerify(exactly = 2) {
            exchangeRepository.placeLimitOrder(
                productId = productId,
                side = OrderSide.SELL,
                size = any(),
                price = any(),
                postOnly = true
            )
        }
    }

    @Test
    fun `handles risk check rejection`() = runTest {
        val filledBuy = Order(
            id = "order-1",
            clientOrderId = "client-1",
            productId = productId,
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            status = OrderStatus.FILLED,
            size = BigDecimal("0.1"),
            filledSize = BigDecimal("0.1"),
            price = BigDecimal("99000"),
            avgFilledPrice = BigDecimal("99000"),
            createdAt = Instant.now().minusSeconds(300)
        )

        coEvery { tradingDataRepository.getRecentFilledOrders(productId, 50) } returns listOf(filledBuy)
        coEvery { tradingDataRepository.getOpenOrders(productId) } returns emptyList()
        every { riskManager.validateOrder(any(), any(), any()) } returns RiskCheck.Rejected("Too large")

        val result = useCase.execute(productId, gridSpacing, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Skipped)
        assertTrue(result.reason.contains("100000: Too large"))
    }

    @Test
    fun `handles order placement failure`() = runTest {
        val filledBuy = Order(
            id = "order-1",
            clientOrderId = "client-1",
            productId = productId,
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            status = OrderStatus.FILLED,
            size = BigDecimal("0.1"),
            filledSize = BigDecimal("0.1"),
            price = BigDecimal("99000"),
            avgFilledPrice = BigDecimal("99000"),
            createdAt = Instant.now().minusSeconds(300)
        )

        coEvery { tradingDataRepository.getRecentFilledOrders(productId, 50) } returns listOf(filledBuy)
        coEvery { tradingDataRepository.getOpenOrders(productId) } returns emptyList()
        every { riskManager.validateOrder(any(), any(), any()) } returns RiskCheck.Approved
        coEvery {
            exchangeRepository.placeLimitOrder(any(), any(), any(), any(), any())
        } returns Result.failure(Exception("Insufficient funds"))

        val result = useCase.execute(productId, gridSpacing, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Failed)
        assertTrue(result.error.contains("Insufficient funds"))
    }
}
