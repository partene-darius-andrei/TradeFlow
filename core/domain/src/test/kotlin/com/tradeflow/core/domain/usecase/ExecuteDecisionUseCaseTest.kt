package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.BracketOrderRepository
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

class ExecuteDecisionUseCaseTest {

    private val exchangeRepository: ExchangeRepository = mockk()
    private val bracketOrderRepository: BracketOrderRepository = mockk()
    private val riskManager: RiskManager = mockk()
    private val manageGridOrdersUseCase: ManageGridOrdersUseCase = mockk()

    private val useCase = ExecuteDecisionUseCase(
        exchangeRepository,
        bracketOrderRepository,
        riskManager,
        manageGridOrdersUseCase
    )

    private val productId = "BTC-USD"
    private val currentPrice = BigDecimal("40000")
    private val portfolio = createTestPortfolio()

    @Test
    fun `execute returns skipped for Wait decision`() = runTest {
        val decision = Decision.Wait("Waiting for confirmation")

        val result = useCase.execute(decision, portfolio, currentPrice, productId)

        assertTrue(result is ExecutionResult.Skipped)
    }

    @Test
    fun `execute cancels buy orders in Defense mode`() = runTest {
        val decision = Decision.Defense(
            reason = "Price below SMA200",
            currentPrice = currentPrice,
            sma200 = BigDecimal("45000")
        )

        val openOrders = listOf(
            createTestOrder("buy1", OrderSide.BUY),
            createTestOrder("sell1", OrderSide.SELL),
            createTestOrder("buy2", OrderSide.BUY)
        )

        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(openOrders)
        coEvery { exchangeRepository.cancelOrders(any()) } returns Result.success(2)

        val result = useCase.execute(decision, portfolio, currentPrice, productId)

        assertTrue(result is ExecutionResult.Success)
        assertTrue((result as ExecutionResult.Success).message.contains("Canceled 2 buy orders"))

        coVerify { exchangeRepository.cancelOrders(listOf("buy1", "buy2")) }
    }

    @Test
    fun `execute skips Defense when no buy orders`() = runTest {
        val decision = Decision.Defense(
            reason = "Price below SMA200",
            currentPrice = currentPrice,
            sma200 = BigDecimal("45000")
        )

        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(emptyList())

        val result = useCase.execute(decision, portfolio, currentPrice, productId)

        assertTrue(result is ExecutionResult.Skipped)
        assertTrue((result as ExecutionResult.Skipped).reason.contains("No buy orders"))
    }

    @Test
    fun `execute places bracket order for Trend decision`() = runTest {
        val decision = Decision.Trend(
            direction = OrderSide.BUY,
            entryPrice = currentPrice,
            stopLoss = BigDecimal("38000"),
            takeProfit = BigDecimal("44000"),
            positionSize = BigDecimal("0.0005"),
            adx = 30.0,
            atr = BigDecimal("1000")
        )

        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(emptyList())
        every { riskManager.calculateTrendPositionSize(portfolio, currentPrice) } returns BigDecimal("0.0005")
        every { riskManager.validateOrder(any(), portfolio, currentPrice) } returns RiskCheck.Approved
        coEvery {
            bracketOrderRepository.placeBracketOrder(any(), any(), any(), any(), any(), any())
        } returns Result.success(createTestOrder("bracket1", OrderSide.BUY))

        val result = useCase.execute(decision, portfolio, currentPrice, productId)

        assertTrue(result is ExecutionResult.Success)
        assertTrue((result as ExecutionResult.Success).message.contains("Placed BUY bracket order"))

        coVerify {
            bracketOrderRepository.placeBracketOrder(
                productId = productId,
                side = OrderSide.BUY,
                size = BigDecimal("0.0005"),
                entryPrice = currentPrice,
                takeProfit = BigDecimal("44000"),
                stopLoss = BigDecimal("38000")
            )
        }
    }

    @Test
    fun `execute skips Trend when already have position`() = runTest {
        val decision = Decision.Trend(
            direction = OrderSide.BUY,
            entryPrice = currentPrice,
            stopLoss = BigDecimal("38000"),
            takeProfit = BigDecimal("44000"),
            positionSize = BigDecimal("0.0005"),
            adx = 30.0,
            atr = BigDecimal("1000")
        )

        val existingOrder = createTestOrder("existing", OrderSide.BUY, OrderType.BRACKET)
        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(listOf(existingOrder))

        val result = useCase.execute(decision, portfolio, currentPrice, productId)

        assertTrue(result is ExecutionResult.Skipped)
        assertTrue((result as ExecutionResult.Skipped).reason.contains("Already have active trend position"))
    }

    @Test
    fun `execute skips Trend when risk check rejects`() = runTest {
        val decision = Decision.Trend(
            direction = OrderSide.BUY,
            entryPrice = currentPrice,
            stopLoss = BigDecimal("38000"),
            takeProfit = BigDecimal("44000"),
            positionSize = BigDecimal("0.0005"),
            adx = 30.0,
            atr = BigDecimal("1000")
        )

        coEvery { exchangeRepository.getOpenOrders(productId) } returns Result.success(emptyList())
        every { riskManager.calculateTrendPositionSize(portfolio, currentPrice) } returns BigDecimal("0.0005")
        every { riskManager.validateOrder(any(), portfolio, currentPrice) } returns
            RiskCheck.Rejected("Position size too large")

        val result = useCase.execute(decision, portfolio, currentPrice, productId)

        assertTrue(result is ExecutionResult.Skipped)
        assertTrue((result as ExecutionResult.Skipped).reason.contains("Risk check rejected"))
    }

    @Test
    fun `execute delegates Range to grid manager`() = runTest {
        val decision = Decision.Range(
            gridSpacing = BigDecimal("600"),
            levels = 5,
            positionSizePerLevel = BigDecimal("0.0002"),
            adx = 15.0,
            atr = BigDecimal("800")
        )

        coEvery {
            manageGridOrdersUseCase.execute(productId, decision, portfolio, currentPrice)
        } returns ExecutionResult.Success("Grid orders placed")

        val result = useCase.execute(decision, portfolio, currentPrice, productId)

        assertTrue(result is ExecutionResult.Success)

        coVerify { manageGridOrdersUseCase.execute(productId, decision, portfolio, currentPrice) }
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
        type: OrderType = OrderType.LIMIT
    ): Order {
        return Order(
            id = id,
            clientOrderId = "client-$id",
            productId = productId,
            side = side,
            type = type,
            status = OrderStatus.OPEN,
            size = BigDecimal("0.001"),
            price = BigDecimal("40000"),
            filledSize = BigDecimal.ZERO,
            avgFilledPrice = null,
            createdAt = Instant.now()
        )
    }
}
