package com.tradeflow.core.domain.risk

import com.tradeflow.core.domain.model.Balance
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderType
import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.risk.model.DrawdownStatus
import com.tradeflow.core.domain.risk.model.PlaceOrderRequest
import com.tradeflow.core.domain.risk.model.RiskCheck
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RiskManagerTest {

    private val testConfig = RiskConfig(
        maxPositionPercent = BigDecimal("0.05"),
        maxTotalExposurePercent = BigDecimal("0.10"),
        maxDrawdownPercent = 0.15,
        drawdownWarningPercent = 0.12,
        minGridSpacingPercent = BigDecimal("0.015")
    )

    private val riskManager = RiskManager(testConfig)

    private fun createTestPortfolio(
        usdBalance: BigDecimal = BigDecimal("500"),
        btcBalance: BigDecimal = BigDecimal.ZERO,
        btcPrice: BigDecimal = BigDecimal("40000")
    ): Portfolio {
        val totalEquity = usdBalance + (btcBalance * btcPrice)
        return Portfolio(
            balances = listOf(
                Balance("USD", usdBalance, usdBalance),
                Balance("BTC", btcBalance, btcBalance)
            ),
            totalEquityUsd = totalEquity,
            timestamp = Instant.now()
        )
    }

    @Test
    fun `validateOrder approves small position`() {
        val portfolio = createTestPortfolio(usdBalance = BigDecimal("500"))
        val request = PlaceOrderRequest(
            productId = "BTC-USD",
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            size = BigDecimal("0.0005"),
            price = BigDecimal("40000")
        )

        val result = riskManager.validateOrder(request, portfolio, BigDecimal("40000"))

        assertTrue(result is RiskCheck.Approved)
    }

    @Test
    fun `validateOrder rejects oversized position`() {
        val portfolio = createTestPortfolio(usdBalance = BigDecimal("500"))
        val request = PlaceOrderRequest(
            productId = "BTC-USD",
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            size = BigDecimal("0.001"),
            price = BigDecimal("40000")
        )

        val result = riskManager.validateOrder(request, portfolio, BigDecimal("40000"))

        assertTrue(result is RiskCheck.Rejected)
        assertTrue((result as RiskCheck.Rejected).reason.contains("Position size"))
    }

    @Test
    fun `validateOrder rejects excessive total exposure`() {
        val portfolio = createTestPortfolio(
            usdBalance = BigDecimal("460"),
            btcBalance = BigDecimal("0.001")
        )

        val request = PlaceOrderRequest(
            productId = "BTC-USD",
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            size = BigDecimal("0.0005"),
            price = BigDecimal("40000")
        )

        val result = riskManager.validateOrder(request, portfolio, BigDecimal("40000"))

        assertTrue(result is RiskCheck.Rejected)
        assertTrue((result as RiskCheck.Rejected).reason.contains("Total exposure"))
    }

    @Test
    fun `validateOrder allows SELL when over exposure limit`() {
        val portfolio = createTestPortfolio(
            usdBalance = BigDecimal("440"),
            btcBalance = BigDecimal("0.0015")
        )

        val request = PlaceOrderRequest(
            productId = "BTC-USD",
            side = OrderSide.SELL,
            type = OrderType.LIMIT,
            size = BigDecimal("0.0005"),
            price = BigDecimal("40000")
        )

        val result = riskManager.validateOrder(request, portfolio, BigDecimal("40000"))

        assertTrue(result is RiskCheck.Approved)
    }

    @Test
    fun `validateOrder uses current price for market orders`() {
        val portfolio = createTestPortfolio(usdBalance = BigDecimal("500"))
        val request = PlaceOrderRequest(
            productId = "BTC-USD",
            side = OrderSide.BUY,
            type = OrderType.MARKET,
            size = BigDecimal("0.0005"),
            price = null
        )

        val result = riskManager.validateOrder(request, portfolio, BigDecimal("40000"))

        assertTrue(result is RiskCheck.Approved)
    }

    @Test
    fun `checkDrawdown returns Normal when below warning threshold`() {
        val currentEquity = BigDecimal("450")
        val highWaterMark = BigDecimal("500")

        val status = riskManager.checkDrawdown(currentEquity, highWaterMark)

        assertTrue(status is DrawdownStatus.Normal)
        assertEquals(0.10, (status as DrawdownStatus.Normal).drawdownPercent, 0.01)
    }

    @Test
    fun `checkDrawdown returns Warning at warning threshold`() {
        val currentEquity = BigDecimal("440")
        val highWaterMark = BigDecimal("500")

        val status = riskManager.checkDrawdown(currentEquity, highWaterMark)

        assertTrue(status is DrawdownStatus.Warning)
        assertEquals(0.12, (status as DrawdownStatus.Warning).drawdownPercent, 0.01)
    }

    @Test
    fun `checkDrawdown returns LimitBreached at max threshold`() {
        val currentEquity = BigDecimal("425")
        val highWaterMark = BigDecimal("500")

        val status = riskManager.checkDrawdown(currentEquity, highWaterMark)

        assertTrue(status is DrawdownStatus.LimitBreached)
        assertEquals(0.15, (status as DrawdownStatus.LimitBreached).drawdownPercent, 0.01)
    }

    @Test
    fun `checkDrawdown handles zero high water mark`() {
        val currentEquity = BigDecimal("100")
        val highWaterMark = BigDecimal.ZERO

        val status = riskManager.checkDrawdown(currentEquity, highWaterMark)

        assertTrue(status is DrawdownStatus.Normal)
        assertEquals(0.0, (status as DrawdownStatus.Normal).drawdownPercent)
    }

    @Test
    fun `calculateTrendPositionSize returns 5 percent of equity`() {
        val portfolio = createTestPortfolio(usdBalance = BigDecimal("500"))
        val entryPrice = BigDecimal("40000")

        val size = riskManager.calculateTrendPositionSize(portfolio, entryPrice)

        assertEquals(BigDecimal("0.00062500"), size)
    }

    @Test
    fun `calculateTrendPositionSize handles small accounts`() {
        val portfolio = createTestPortfolio(usdBalance = BigDecimal("50"))
        val entryPrice = BigDecimal("40000")

        val size = riskManager.calculateTrendPositionSize(portfolio, entryPrice)

        assertEquals(BigDecimal("0.00006250"), size)
    }

    @Test
    fun `calculateGridPositionSize divides exposure across levels`() {
        val portfolio = createTestPortfolio(usdBalance = BigDecimal("500"))
        val gridLevels = 5
        val entryPrice = BigDecimal("40000")

        val sizePerLevel = riskManager.calculateGridPositionSize(
            portfolio,
            gridLevels,
            entryPrice
        )

        assertEquals(BigDecimal("0.00025000"), sizePerLevel)
    }

    @Test
    fun `calculateGridPositionSize handles single level`() {
        val portfolio = createTestPortfolio(usdBalance = BigDecimal("500"))
        val gridLevels = 1
        val entryPrice = BigDecimal("40000")

        val sizePerLevel = riskManager.calculateGridPositionSize(
            portfolio,
            gridLevels,
            entryPrice
        )

        assertEquals(BigDecimal("0.00125000"), sizePerLevel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateGridPositionSize rejects zero levels`() {
        val portfolio = createTestPortfolio()
        riskManager.calculateGridPositionSize(portfolio, 0, BigDecimal("40000"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateGridPositionSize rejects negative levels`() {
        val portfolio = createTestPortfolio()
        riskManager.calculateGridPositionSize(portfolio, -5, BigDecimal("40000"))
    }

    @Test
    fun `validateGridSpacing accepts valid spacing`() {
        val spacing = BigDecimal("0.020")

        val result = riskManager.validateGridSpacing(spacing)

        assertTrue(result)
    }

    @Test
    fun `validateGridSpacing accepts minimum spacing`() {
        val spacing = BigDecimal("0.015")

        val result = riskManager.validateGridSpacing(spacing)

        assertTrue(result)
    }

    @Test
    fun `validateGridSpacing rejects insufficient spacing`() {
        val spacing = BigDecimal("0.010")

        val result = riskManager.validateGridSpacing(spacing)

        assertTrue(!result)
    }

    @Test
    fun `validateGridSpacing rejects very small spacing`() {
        val spacing = BigDecimal("0.005")

        val result = riskManager.validateGridSpacing(spacing)

        assertTrue(!result)
    }

    @Test
    fun `validateOrder handles portfolio with zero USD balance`() {
        val portfolio = createTestPortfolio(
            usdBalance = BigDecimal.ZERO,
            btcBalance = BigDecimal("0.01")
        )

        val request = PlaceOrderRequest(
            productId = "BTC-USD",
            side = OrderSide.SELL,
            type = OrderType.LIMIT,
            size = BigDecimal("0.001"),
            price = BigDecimal("40000")
        )

        val result = riskManager.validateOrder(request, portfolio, BigDecimal("40000"))

        assertTrue(result is RiskCheck.Rejected)
    }

    @Test
    fun `checkDrawdown handles very large drawdown`() {
        val currentEquity = BigDecimal("100")
        val highWaterMark = BigDecimal("1000")

        val status = riskManager.checkDrawdown(currentEquity, highWaterMark)

        assertTrue(status is DrawdownStatus.LimitBreached)
        assertEquals(0.90, (status as DrawdownStatus.LimitBreached).drawdownPercent, 0.01)
    }
}
