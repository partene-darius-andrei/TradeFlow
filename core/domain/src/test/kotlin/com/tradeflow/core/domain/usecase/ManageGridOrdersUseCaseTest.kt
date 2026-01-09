package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.risk.RiskManager
import com.tradeflow.core.domain.risk.model.RiskCheck
import com.tradeflow.core.domain.usecase.model.ExecutionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertTrue

class ManageGridOrdersUseCaseTest {

    private val exchangeRepository: ExchangeRepository = mockk()
    private val riskManager: RiskManager = mockk()
    private val useCase = ManageGridOrdersUseCase(exchangeRepository, riskManager)

    private val productId = "BTC-USD"
    private val currentPrice = BigDecimal("40000")
    private val portfolio = createTestPortfolio()

    @Test
    fun `execute places grid orders at all levels when none exist`() = runTest {
        val decision = Decision.Range(
            gridSpacing = BigDecimal("600"),
            levels = 5,
            positionSizePerLevel = BigDecimal("0.0002"),
            adx = 15.0,
            atr = BigDecimal("800")
        )

        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(emptyList())
        every { riskManager.validateGridSpacing(any()) } returns true
        every { riskManager.calculateGridPositionSize(portfolio, 5, currentPrice) } returns BigDecimal("0.0002")
        every { riskManager.validateOrder(any(), portfolio, currentPrice) } returns RiskCheck.Approved
        coEvery {
            exchangeRepository.placeLimitOrder(any(), any(), any(), any(), any())
        } returns Result.success(createTestOrder("grid1", OrderSide.BUY))

        val result = useCase.execute(productId, decision, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Success)
        assertTrue((result as ExecutionResult.Success).message.contains("Placed 5 grid orders"))

        coVerify(exactly = 5) {
            exchangeRepository.placeLimitOrder(
                productId = productId,
                side = OrderSide.BUY,
                size = BigDecimal("0.0002"),
                price = any(),
                postOnly = true
            )
        }
    }

    @Test
    fun `execute skips levels that already have orders`() = runTest {
        val decision = Decision.Range(
            gridSpacing = BigDecimal("600"),
            levels = 5,
            positionSizePerLevel = BigDecimal("0.0002"),
            adx = 15.0,
            atr = BigDecimal("800")
        )

        val existingOrders = listOf(
            createTestOrder("grid1", OrderSide.BUY, price = BigDecimal("39400")),
            createTestOrder("grid2", OrderSide.BUY, price = BigDecimal("38800"))
        )

        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(existingOrders)
        every { riskManager.validateGridSpacing(any()) } returns true
        every { riskManager.calculateGridPositionSize(portfolio, 5, currentPrice) } returns BigDecimal("0.0002")
        every { riskManager.validateOrder(any(), portfolio, currentPrice) } returns RiskCheck.Approved
        coEvery {
            exchangeRepository.placeLimitOrder(any(), any(), any(), any(), any())
        } returns Result.success(createTestOrder("new", OrderSide.BUY))

        val result = useCase.execute(productId, decision, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Success)
        coVerify(exactly = 3) {
            exchangeRepository.placeLimitOrder(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `execute skips when grid spacing too small`() = runTest {
        val decision = Decision.Range(
            gridSpacing = BigDecimal("400"),
            levels = 5,
            positionSizePerLevel = BigDecimal("0.0002"),
            adx = 15.0,
            atr = BigDecimal("800")
        )

        every { riskManager.validateGridSpacing(any()) } returns false

        val result = useCase.execute(productId, decision, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Skipped)
        assertTrue((result as ExecutionResult.Skipped).reason.contains("Grid spacing"))
        assertTrue(result.reason.contains("below minimum"))
    }

    @Test
    fun `execute skips when all levels active`() = runTest {
        val decision = Decision.Range(
            gridSpacing = BigDecimal("600"),
            levels = 5,
            positionSizePerLevel = BigDecimal("0.0002"),
            adx = 15.0,
            atr = BigDecimal("800")
        )

        val existingOrders = (1..5).map { level ->
            val price = currentPrice - (BigDecimal("600") * BigDecimal(level))
            createTestOrder("grid$level", OrderSide.BUY, price = price)
        }

        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(existingOrders)
        every { riskManager.validateGridSpacing(any()) } returns true
        every { riskManager.calculateGridPositionSize(portfolio, 5, currentPrice) } returns BigDecimal("0.0002")

        val result = useCase.execute(productId, decision, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Skipped)
        assertTrue((result as ExecutionResult.Skipped).reason.contains("All 5 grid levels active"))
    }

    @Test
    fun `execute handles risk check rejection`() = runTest {
        val decision = Decision.Range(
            gridSpacing = BigDecimal("600"),
            levels = 5,
            positionSizePerLevel = BigDecimal("0.0002"),
            adx = 15.0,
            atr = BigDecimal("800")
        )

        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(emptyList())
        every { riskManager.validateGridSpacing(any()) } returns true
        every { riskManager.calculateGridPositionSize(portfolio, 5, currentPrice) } returns BigDecimal("0.0002")
        every { riskManager.validateOrder(any(), portfolio, currentPrice) } returns
            RiskCheck.Rejected("Total exposure exceeded")

        val result = useCase.execute(productId, decision, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Failed)
        assertTrue((result as ExecutionResult.Failed).error.contains("All grid orders failed"))
    }

    @Test
    fun `execute handles partial success`() = runTest {
        val decision = Decision.Range(
            gridSpacing = BigDecimal("600"),
            levels = 3,
            positionSizePerLevel = BigDecimal("0.0002"),
            adx = 15.0,
            atr = BigDecimal("800")
        )

        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(emptyList())
        every { riskManager.validateGridSpacing(any()) } returns true
        every { riskManager.calculateGridPositionSize(portfolio, 3, currentPrice) } returns BigDecimal("0.0002")
        every { riskManager.validateOrder(any(), portfolio, currentPrice) } returns RiskCheck.Approved

        coEvery {
            exchangeRepository.placeLimitOrder(productId, any(), any(), any(), any())
        } returnsMany listOf(
            Result.success(createTestOrder("grid1", OrderSide.BUY)),
            Result.failure(Exception("Network error")),
            Result.success(createTestOrder("grid3", OrderSide.BUY))
        )

        val result = useCase.execute(productId, decision, portfolio, currentPrice)

        assertTrue(result is ExecutionResult.Success)
        assertTrue((result as ExecutionResult.Success).message.contains("Placed 2 orders"))
        assertTrue(result.message.contains("1 failed"))
    }

    private fun createTestPortfolio(): Portfolio {
        return Portfolio(
            balances = listOf(
                Balance("USD", BigDecimal("500"), BigDecimal("500")),
                Balance("BTC", BigDecimal("0.01"), BigDecimal("0.01"))
            ),
            totalEquityUsd = BigDecimal("500"),
            timestamp = Instant.now()
        )
    }

    private fun createTestOrder(
        id: String,
        side: OrderSide,
        price: BigDecimal = BigDecimal("40000")
    ): Order {
        return Order(
            id = id,
            clientOrderId = "client-$id",
            productId = productId,
            side = side,
            type = OrderType.LIMIT,
            status = OrderStatus.OPEN,
            size = BigDecimal("0.001"),
            price = price,
            filledSize = BigDecimal.ZERO,
            avgFilledPrice = null,
            createdAt = Instant.now()
        )
    }
}
