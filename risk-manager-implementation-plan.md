# Risk Manager Implementation Plan

**Ticket:** 16 - Risk Manager
**Created:** 2026-01-09
**Status:** DRAFT - Awaiting Approval
**Effort:** Medium
**Priority:** HIGH
**Phase:** 3 (API Integration & Service Implementation)

---

## 🎯 Objective

Implement domain-layer risk management service for:
- Position sizing (trend & grid strategies)
- Drawdown monitoring and emergency stop
- Order validation against risk limits
- Grid spacing validation (fee break-even)

**Critical:** Must remain **exchange-agnostic** (pure domain logic, no Coinbase-specific code)

---

## 📋 Requirements Analysis

### Current State

**✅ What EXISTS:**
- Portfolio model: Simple structure with balances + totalEquityUsd
- PortfolioDao: Has `getHighWaterMark()` query
- PortfolioSnapshotEntity: Tracks highWaterMark + drawdownPercent
- TradingDecisionEngine: Produces decisions with position sizing hints
- StrategyConfig: Contains trading parameters (position %, grid spacing, etc.)

**❌ What's MISSING:**
- RiskManager class
- RiskConfig data class
- RiskCheck sealed class (Approved, Rejected)
- DrawdownStatus sealed class (Normal, Warning, LimitBreached)
- PlaceOrderRequest model (for order validation)
- Integration point with decision engine flow

---

## 🏗️ Architecture Design

### Module Structure

```
core/domain/src/main/kotlin/com/tradeflow/core/domain/
├── risk/
│   ├── RiskManager.kt              [NEW] Core risk service
│   ├── RiskConfig.kt               [NEW] Risk limits configuration
│   └── model/
│       ├── RiskCheck.kt            [NEW] Order validation result
│       ├── DrawdownStatus.kt       [NEW] Drawdown monitoring result
│       └── PlaceOrderRequest.kt    [NEW] Order request model
└── strategy/
    └── StrategyConfig.kt           [EXISTS] Trading strategy config
```

### Design Decisions

**1. Keep Portfolio Simple**
- Portfolio.kt remains unchanged (balances + totalEquityUsd)
- RiskManager methods accept additional parameters as needed
- Avoids circular dependencies and keeps domain clean

**2. Separate Risk from Strategy**
- StrategyConfig: Technical indicators, entry/exit logic
- RiskConfig: Risk limits, position sizing rules, drawdown thresholds
- Clear separation of concerns

**3. Integration with Decision Engine**
- TradingDecisionEngine produces decisions with technical analysis
- RiskManager validates decisions before execution
- Trading service orchestrates: Decision → Risk Check → Order Placement

**4. Portfolio Balance Handling**
- Need current BTC balance for exposure calculation
- Pass as parameter: `getBtcBalance(portfolio: Portfolio): BigDecimal`
- Simple helper extension function in Portfolio.kt

---

## 📝 Implementation Steps

### Phase 1: Core Models (2 files, SAFE)

**File 1:** `core/domain/src/main/kotlin/com/tradeflow/core/domain/risk/model/RiskCheck.kt`

```kotlin
package com.tradeflow.core.domain.risk.model

sealed class RiskCheck {
    object Approved : RiskCheck()
    data class Rejected(val reason: String) : RiskCheck()
}
```

**File 2:** `core/domain/src/main/kotlin/com/tradeflow/core/domain/risk/model/DrawdownStatus.kt`

```kotlin
package com.tradeflow.core.domain.risk.model

sealed class DrawdownStatus {
    data class Normal(val drawdownPercent: Double) : DrawdownStatus()
    data class Warning(val drawdownPercent: Double) : DrawdownStatus()
    data class LimitBreached(val drawdownPercent: Double) : DrawdownStatus()
}
```

**Risk:** SAFE - Simple sealed classes, no dependencies

---

### Phase 2: Configuration & Request Models (2 files, SAFE)

**File 3:** `core/domain/src/main/kotlin/com/tradeflow/core/domain/risk/RiskConfig.kt`

```kotlin
package com.tradeflow.core.domain.risk

import java.math.BigDecimal

data class RiskConfig(
    val maxPositionPercent: BigDecimal = BigDecimal("0.05"),      // 5% per trade
    val maxTotalExposurePercent: BigDecimal = BigDecimal("0.10"), // 10% total BTC exposure
    val maxDrawdownPercent: Double = 0.15,                        // 15% drawdown from HWM
    val drawdownWarningPercent: Double = 0.12,                    // 12% warning threshold
    val minGridSpacingPercent: BigDecimal = BigDecimal("0.015")   // 1.5% (fee break-even)
)
```

**File 4:** `core/domain/src/main/kotlin/com/tradeflow/core/domain/risk/model/PlaceOrderRequest.kt`

```kotlin
package com.tradeflow.core.domain.risk.model

import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.OrderType
import java.math.BigDecimal

data class PlaceOrderRequest(
    val productId: String,
    val side: OrderSide,
    val type: OrderType,
    val size: BigDecimal,           // BTC amount to buy/sell
    val price: BigDecimal?,         // Null for market orders
    val stopLoss: BigDecimal? = null,
    val takeProfit: BigDecimal? = null
)
```

**Risk:** SAFE - Data classes, no complex logic

---

### Phase 3: Portfolio Extension (1 file, SAFE)

**File 5:** `core/domain/src/main/kotlin/com/tradeflow/core/domain/model/PortfolioExt.kt`

```kotlin
package com.tradeflow.core.domain.model

import java.math.BigDecimal

fun Portfolio.getBtcBalance(): BigDecimal {
    return balances
        .firstOrNull { it.currency == "BTC" }
        ?.available
        ?: BigDecimal.ZERO
}

fun Portfolio.getUsdBalance(): BigDecimal {
    return balances
        .firstOrNull { it.currency == "USD" || it.currency == "USDT" }
        ?.available
        ?: BigDecimal.ZERO
}
```

**Risk:** SAFE - Simple extension functions

---

### Phase 4: RiskManager Implementation (1 file, MODERATE)

**File 6:** `core/domain/src/main/kotlin/com/tradeflow/core/domain/risk/RiskManager.kt`

```kotlin
package com.tradeflow.core.domain.risk

import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.model.getBtcBalance
import com.tradeflow.core.domain.risk.model.DrawdownStatus
import com.tradeflow.core.domain.risk.model.PlaceOrderRequest
import com.tradeflow.core.domain.risk.model.RiskCheck
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class RiskManager @Inject constructor(
    private val config: RiskConfig = RiskConfig()
) {

    /**
     * Validate order against risk limits before placement
     *
     * Checks:
     * 1. Position size doesn't exceed maxPositionPercent
     * 2. Total BTC exposure doesn't exceed maxTotalExposurePercent
     */
    fun validateOrder(
        request: PlaceOrderRequest,
        portfolio: Portfolio,
        currentPrice: BigDecimal
    ): RiskCheck {
        // Calculate order value in USD
        val orderPrice = request.price ?: currentPrice
        val orderValueUsd = request.size * orderPrice

        // Check position size
        val positionPercent = orderValueUsd
            .divide(portfolio.totalEquityUsd, 4, RoundingMode.HALF_UP)

        if (positionPercent > config.maxPositionPercent) {
            return RiskCheck.Rejected(
                "Position size ${formatPercent(positionPercent)} exceeds limit ${formatPercent(config.maxPositionPercent)}"
            )
        }

        // Check total BTC exposure (only for BUY orders)
        if (request.side == com.tradeflow.core.domain.model.OrderSide.BUY) {
            val currentBtcValue = portfolio.getBtcBalance() * currentPrice
            val currentExposure = currentBtcValue
                .divide(portfolio.totalEquityUsd, 4, RoundingMode.HALF_UP)
            val newExposure = currentExposure + positionPercent

            if (newExposure > config.maxTotalExposurePercent) {
                return RiskCheck.Rejected(
                    "Total exposure ${formatPercent(newExposure)} would exceed limit ${formatPercent(config.maxTotalExposurePercent)}"
                )
            }
        }

        return RiskCheck.Approved
    }

    /**
     * Check drawdown from high water mark
     *
     * Returns:
     * - Normal: Drawdown < warning threshold
     * - Warning: Drawdown >= warning threshold
     * - LimitBreached: Drawdown >= max drawdown (emergency liquidation required)
     */
    fun checkDrawdown(
        currentEquity: BigDecimal,
        highWaterMark: BigDecimal
    ): DrawdownStatus {
        val drawdown = if (highWaterMark > BigDecimal.ZERO) {
            (highWaterMark - currentEquity)
                .divide(highWaterMark, 4, RoundingMode.HALF_UP)
                .toDouble()
        } else {
            0.0
        }

        return when {
            drawdown >= config.maxDrawdownPercent ->
                DrawdownStatus.LimitBreached(drawdown)
            drawdown >= config.drawdownWarningPercent ->
                DrawdownStatus.Warning(drawdown)
            else ->
                DrawdownStatus.Normal(drawdown)
        }
    }

    /**
     * Calculate position size for trend trade
     *
     * Uses maxPositionPercent of total equity
     */
    fun calculateTrendPositionSize(
        portfolio: Portfolio,
        entryPrice: BigDecimal
    ): BigDecimal {
        val riskAmountUsd = portfolio.totalEquityUsd * config.maxPositionPercent
        return riskAmountUsd
            .divide(entryPrice, 8, RoundingMode.HALF_UP)
    }

    /**
     * Calculate position size per grid level
     *
     * Divides maxTotalExposurePercent equally across grid levels
     */
    fun calculateGridPositionSize(
        portfolio: Portfolio,
        gridLevels: Int,
        entryPrice: BigDecimal
    ): BigDecimal {
        require(gridLevels > 0) { "Grid levels must be positive" }

        val totalRiskUsd = portfolio.totalEquityUsd * config.maxTotalExposurePercent
        val perLevelRiskUsd = totalRiskUsd
            .divide(BigDecimal(gridLevels), 8, RoundingMode.HALF_UP)

        return perLevelRiskUsd
            .divide(entryPrice, 8, RoundingMode.HALF_UP)
    }

    /**
     * Validate grid spacing meets minimum fee break-even requirement
     *
     * Minimum 1.5% spacing required at intro tier (0.60% maker fee * 2 = 1.20% round-trip)
     */
    fun validateGridSpacing(spacingPercent: BigDecimal): Boolean {
        return spacingPercent >= config.minGridSpacingPercent
    }

    private fun formatPercent(value: BigDecimal): String {
        return "${(value * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)}%"
    }
}
```

**Risk:** MODERATE - Core business logic, needs careful testing

**Integration Notes:**
- Used in trading service before order placement
- Validates decisions from TradingDecisionEngine
- Emergency liquidation triggered on LimitBreached

---

### Phase 5: Unit Tests (1 file, SAFE)

**File 7:** `core/domain/src/test/kotlin/com/tradeflow/core/domain/risk/RiskManagerTest.kt`

```kotlin
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
        maxPositionPercent = BigDecimal("0.05"),      // 5%
        maxTotalExposurePercent = BigDecimal("0.10"), // 10%
        maxDrawdownPercent = 0.15,                    // 15%
        drawdownWarningPercent = 0.12,                // 12%
        minGridSpacingPercent = BigDecimal("0.015")   // 1.5%
    )

    private val riskManager = RiskManager(testConfig)

    private fun createTestPortfolio(
        usdBalance: BigDecimal = BigDecimal("500"),
        btcBalance: BigDecimal = BigDecimal.ZERO
    ): Portfolio {
        return Portfolio(
            balances = listOf(
                Balance("USD", usdBalance, usdBalance),
                Balance("BTC", btcBalance, btcBalance)
            ),
            totalEquityUsd = usdBalance,
            timestamp = Instant.now()
        )
    }

    // ============================================
    // Order Validation Tests
    // ============================================

    @Test
    fun `validateOrder approves small position`() {
        val portfolio = createTestPortfolio(usdBalance = BigDecimal("500"))
        val request = PlaceOrderRequest(
            productId = "BTC-USD",
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            size = BigDecimal("0.0005"),  // ~$20 at $40k BTC = 4% of $500
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
            size = BigDecimal("0.001"),  // ~$40 at $40k BTC = 8% of $500 (exceeds 5% limit)
            price = BigDecimal("40000")
        )

        val result = riskManager.validateOrder(request, portfolio, BigDecimal("40000"))

        assertTrue(result is RiskCheck.Rejected)
        assertTrue((result as RiskCheck.Rejected).reason.contains("Position size"))
    }

    @Test
    fun `validateOrder rejects excessive total exposure`() {
        // Portfolio already has 8% BTC exposure
        val portfolio = createTestPortfolio(
            usdBalance = BigDecimal("460"),
            btcBalance = BigDecimal("0.001")  // $40 BTC at $40k
        )

        // Try to add another 4% (total would be 12%, exceeds 10% limit)
        val request = PlaceOrderRequest(
            productId = "BTC-USD",
            side = OrderSide.BUY,
            type = OrderType.LIMIT,
            size = BigDecimal("0.0005"),  // ~$20
            price = BigDecimal("40000")
        )

        val result = riskManager.validateOrder(request, portfolio, BigDecimal("40000"))

        assertTrue(result is RiskCheck.Rejected)
        assertTrue((result as RiskCheck.Rejected).reason.contains("Total exposure"))
    }

    @Test
    fun `validateOrder allows SELL when over exposure limit`() {
        // Portfolio has 12% BTC exposure (over limit)
        val portfolio = createTestPortfolio(
            usdBalance = BigDecimal("440"),
            btcBalance = BigDecimal("0.0015")  // $60 BTC at $40k
        )

        // SELL orders should still be allowed (reducing exposure)
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

    // ============================================
    // Drawdown Monitoring Tests
    // ============================================

    @Test
    fun `checkDrawdown returns Normal when below warning threshold`() {
        val currentEquity = BigDecimal("450")  // 10% drawdown from $500
        val highWaterMark = BigDecimal("500")

        val status = riskManager.checkDrawdown(currentEquity, highWaterMark)

        assertTrue(status is DrawdownStatus.Normal)
        assertEquals(0.10, (status as DrawdownStatus.Normal).drawdownPercent, 0.01)
    }

    @Test
    fun `checkDrawdown returns Warning at warning threshold`() {
        val currentEquity = BigDecimal("440")  // 12% drawdown
        val highWaterMark = BigDecimal("500")

        val status = riskManager.checkDrawdown(currentEquity, highWaterMark)

        assertTrue(status is DrawdownStatus.Warning)
        assertEquals(0.12, (status as DrawdownStatus.Warning).drawdownPercent, 0.01)
    }

    @Test
    fun `checkDrawdown returns LimitBreached at max threshold`() {
        val currentEquity = BigDecimal("425")  // 15% drawdown
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

    // ============================================
    // Position Sizing Tests
    // ============================================

    @Test
    fun `calculateTrendPositionSize returns 5 percent of equity`() {
        val portfolio = createTestPortfolio(usdBalance = BigDecimal("500"))
        val entryPrice = BigDecimal("40000")

        val size = riskManager.calculateTrendPositionSize(portfolio, entryPrice)

        // 5% of $500 = $25 / $40k = 0.000625 BTC
        assertEquals(BigDecimal("0.00062500"), size)
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

        // 10% of $500 = $50 total / 5 levels = $10 per level / $40k = 0.00025 BTC
        assertEquals(BigDecimal("0.00025000"), sizePerLevel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateGridPositionSize rejects zero levels`() {
        val portfolio = createTestPortfolio()
        riskManager.calculateGridPositionSize(portfolio, 0, BigDecimal("40000"))
    }

    // ============================================
    // Grid Spacing Validation Tests
    // ============================================

    @Test
    fun `validateGridSpacing accepts valid spacing`() {
        val spacing = BigDecimal("0.020")  // 2.0% spacing

        val result = riskManager.validateGridSpacing(spacing)

        assertTrue(result)
    }

    @Test
    fun `validateGridSpacing accepts minimum spacing`() {
        val spacing = BigDecimal("0.015")  // Exactly 1.5%

        val result = riskManager.validateGridSpacing(spacing)

        assertTrue(result)
    }

    @Test
    fun `validateGridSpacing rejects insufficient spacing`() {
        val spacing = BigDecimal("0.010")  // 1.0% spacing (too small)

        val result = riskManager.validateGridSpacing(spacing)

        assertTrue(!result)
    }
}
```

**Risk:** SAFE - Tests only, no production code impact

---

### Phase 6: DI Module Configuration (1 file, SAFE)

**File 8:** `core/domain/src/main/kotlin/com/tradeflow/core/domain/di/RiskModule.kt`

```kotlin
package com.tradeflow.core.domain.di

import com.tradeflow.core.domain.risk.RiskConfig
import com.tradeflow.core.domain.risk.RiskManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RiskModule {

    @Provides
    @Singleton
    fun provideRiskConfig(): RiskConfig = RiskConfig()

    @Provides
    @Singleton
    fun provideRiskManager(config: RiskConfig): RiskManager {
        return RiskManager(config)
    }
}
```

**Risk:** SAFE - Simple DI configuration

---

## 🧪 Testing Strategy

### Unit Tests Coverage

**Scenarios to Test:**

1. **Order Validation:**
   - ✅ Approve small position (under 5%)
   - ✅ Reject oversized position (over 5%)
   - ✅ Reject excessive total exposure (BTC > 10%)
   - ✅ Allow SELL orders even when over exposure
   - ✅ Handle market orders (price = null)

2. **Drawdown Monitoring:**
   - ✅ Normal status (< 12%)
   - ✅ Warning status (12-15%)
   - ✅ LimitBreached status (>= 15%)
   - ✅ Handle zero high water mark
   - ✅ Handle negative equity (edge case)

3. **Position Sizing:**
   - ✅ Trend: 5% of equity
   - ✅ Grid: 10% divided across levels
   - ✅ Handle very small accounts ($10-50)
   - ✅ Handle zero grid levels (error case)

4. **Grid Validation:**
   - ✅ Accept valid spacing (>= 1.5%)
   - ✅ Reject invalid spacing (< 1.5%)
   - ✅ Edge case: Exactly 1.5%

### Integration Testing

**Will be tested in Phase 3 (Trading Service):**
- RiskManager integrated with TradingDecisionEngine
- Real portfolio data from Coinbase
- Order placement flow: Decision → Risk Check → API Call
- Emergency liquidation on drawdown breach

---

## 📊 Implementation Checklist

### Phase 1: Core Models
- [ ] Create RiskCheck.kt
- [ ] Create DrawdownStatus.kt
- [ ] Verify compilation

### Phase 2: Configuration
- [ ] Create RiskConfig.kt
- [ ] Create PlaceOrderRequest.kt
- [ ] Verify compilation

### Phase 3: Portfolio Extensions
- [ ] Create PortfolioExt.kt
- [ ] Add getBtcBalance()
- [ ] Add getUsdBalance()
- [ ] Verify compilation

### Phase 4: RiskManager
- [ ] Create RiskManager.kt
- [ ] Implement validateOrder()
- [ ] Implement checkDrawdown()
- [ ] Implement calculateTrendPositionSize()
- [ ] Implement calculateGridPositionSize()
- [ ] Implement validateGridSpacing()
- [ ] Verify compilation

### Phase 5: Unit Tests
- [ ] Create RiskManagerTest.kt
- [ ] Write order validation tests (4 tests)
- [ ] Write drawdown monitoring tests (4 tests)
- [ ] Write position sizing tests (3 tests)
- [ ] Write grid spacing tests (3 tests)
- [ ] Run all tests → verify GREEN

### Phase 6: DI Configuration
- [ ] Create RiskModule.kt
- [ ] Configure Hilt providers
- [ ] Verify DI graph compiles

### Phase 7: Documentation
- [ ] Update CLAUDE.md with RiskManager status
- [ ] Move ticket 16 to "done" folder
- [ ] Update roadmap.md progress

---

## 🔄 Integration Flow (Future)

When Trading Service is implemented:

```kotlin
// In TradingService decision loop
val decision = decisionEngine.evaluate(candles, currentPrice)

when (decision) {
    is Decision.Trend -> {
        // Calculate proper position size
        val positionSize = riskManager.calculateTrendPositionSize(
            portfolio,
            decision.entryPrice
        )

        // Create order request
        val request = PlaceOrderRequest(
            productId = "BTC-USD",
            side = decision.direction,
            type = OrderType.BRACKET,
            size = positionSize,
            price = decision.entryPrice,
            stopLoss = decision.stopLoss,
            takeProfit = decision.takeProfit
        )

        // Validate against risk limits
        val riskCheck = riskManager.validateOrder(request, portfolio, currentPrice)

        when (riskCheck) {
            is RiskCheck.Approved -> exchangeRepository.placeBracketOrder(request)
            is RiskCheck.Rejected -> logger.warn("Order rejected: ${riskCheck.reason}")
        }
    }

    is Decision.Range -> {
        // Similar flow for grid orders
        val sizePerLevel = riskManager.calculateGridPositionSize(
            portfolio,
            decision.levels,
            currentPrice
        )
        // ... validate and place grid orders
    }

    else -> { /* Wait or Defense - no action */ }
}

// Check drawdown every loop iteration
val drawdownStatus = riskManager.checkDrawdown(
    portfolio.totalEquityUsd,
    highWaterMark
)

if (drawdownStatus is DrawdownStatus.LimitBreached) {
    logger.error("EMERGENCY: Drawdown limit breached!")
    exchangeRepository.cancelAllOrders()
    exchangeRepository.marketSellAllBtc()
    stopSelf()
}
```

---

## 🎯 Success Criteria

**Must Satisfy:**
- [ ] ✅ No exchange-specific code (pure domain logic)
- [ ] ✅ All limits configurable via RiskConfig
- [ ] ✅ Position sizing for both trend and grid strategies
- [ ] ✅ Comprehensive unit tests (14+ test cases)
- [ ] ✅ All tests pass (GREEN build)
- [ ] ✅ Integration points clearly documented

**Quality Checks:**
- [ ] Code follows Kotlin conventions
- [ ] No TODO or FIXME comments
- [ ] All public methods documented
- [ ] Error cases handled gracefully
- [ ] BigDecimal precision set appropriately

---

## ⚠️ Known Limitations

1. **No DAO integration in Phase 1:**
   - RiskManager doesn't query PortfolioDao directly
   - Trading Service will pass highWaterMark as parameter
   - Reason: Keeps RiskManager testable and domain-focused

2. **Simple BTC/USD assumption:**
   - Current implementation assumes BTC-USD or BTC-USDT
   - Multi-asset portfolios need enhancement
   - Sufficient for Phase 3 MVP

3. **No order history analysis:**
   - Doesn't check recent order frequency
   - Doesn't prevent rapid-fire orders
   - Will add in future if overtrading becomes issue

4. **Static configuration:**
   - RiskConfig not persisted to database
   - Changes require code modification
   - Fine for MVP, can add UI settings later

---

## 🚀 Post-Implementation Next Steps

**Immediate (Phase 3 continuation):**
1. Implement REST API Client (Ticket 13)
2. Integrate RiskManager into Trading Service
3. Test with small real orders ($10-20)

**Future Enhancements:**
1. Add order frequency limits (max orders per hour)
2. Implement risk adjustment based on volatility
3. Add UI for RiskConfig modification
4. Track risk metrics over time (risk-adjusted returns)

---

## 📝 Post-Mortem (To be filled after implementation)

**What Went Well:**
- [TBD]

**What Went Wrong:**
- [TBD]

**Lessons Learned:**
- [TBD]

**Improvements for Next Ticket:**
- [TBD]

---

**Plan Status:** 🟡 DRAFT - Awaiting approval from @dariuspartene

**Estimated Implementation Time:** 2-3 hours (including tests)

**Ready to proceed?** Please review and confirm before I start implementation.
