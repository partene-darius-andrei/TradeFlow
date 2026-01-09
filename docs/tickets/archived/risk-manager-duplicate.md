# 🚨 DOMAIN: Risk Manager

Effort level: Medium
Priority: High
Blocked by: DOMAIN: Core Domain Models
Module: :core:domain

## Objective

Implement risk management logic (pure domain, no exchange dependencies).

## Module

`:core:domain` (NO Android dependencies)

## Interface

```kotlin
interface RiskManager {
    fun checkDrawdown(
        currentEquity: BigDecimal,
        highWaterMark: BigDecimal
    ): RiskStatus
    
    fun calculatePositionSize(
        portfolioValue: BigDecimal,
        currentPrice: BigDecimal,
        riskPercent: BigDecimal
    ): BigDecimal
    
    fun validateOrder(
        order: Order,
        portfolio: Portfolio,
        openOrders: List<Order>
    ): OrderValidation
}

sealed class RiskStatus {
    data class Safe(val drawdownPercent: BigDecimal) : RiskStatus()
    data class Warning(val drawdownPercent: BigDecimal, val message: String) : RiskStatus()
    data class Emergency(val drawdownPercent: BigDecimal) : RiskStatus()
}

sealed class OrderValidation {
    object Valid : OrderValidation()
    data class Invalid(val reason: String) : OrderValidation()
}
```

## Implementation

```kotlin
class TradingRiskManager(
    private val config: RiskConfig = RiskConfig()
) : RiskManager {
    
    override fun checkDrawdown(
        currentEquity: BigDecimal,
        highWaterMark: BigDecimal
    ): RiskStatus {
        if (highWaterMark <= [BigDecimal.ZERO](http://BigDecimal.ZERO)) return [RiskStatus.Safe](http://RiskStatus.Safe)([BigDecimal.ZERO](http://BigDecimal.ZERO))
        
        val drawdown = (highWaterMark - currentEquity) / highWaterMark
        val drawdownPercent = drawdown * BigDecimal(100)
        
        return when {
            drawdown >= config.emergencyDrawdownLimit -> 
                RiskStatus.Emergency(drawdownPercent)
            drawdown >= config.warningDrawdownLimit -> 
                RiskStatus.Warning(drawdownPercent, "Approaching drawdown limit")
            else -> 
                [RiskStatus.Safe](http://RiskStatus.Safe)(drawdownPercent)
        }
    }
    
    override fun validateOrder(
        order: Order,
        portfolio: Portfolio,
        openOrders: List<Order>
    ): OrderValidation {
        // Check max position per trade
        val orderValue = order.size * (order.price ?: return OrderValidation.Invalid("No price"))
        val maxAllowed = portfolio.totalEquityUsd * config.maxPositionPerTrade
        if (orderValue > maxAllowed) {
            return OrderValidation.Invalid("Order exceeds ${config.maxPositionPerTrade * 100}% limit")
        }
        
        // Check total exposure
        val currentExposure = openOrders.sumOf { it.size * (it.price ?: [BigDecimal.ZERO](http://BigDecimal.ZERO)) }
        val newExposure = currentExposure + orderValue
        val maxExposure = portfolio.totalEquityUsd * config.maxTotalExposure
        if (newExposure > maxExposure) {
            return OrderValidation.Invalid("Total exposure would exceed ${config.maxTotalExposure * 100}%")
        }
        
        return OrderValidation.Valid
    }
}
```

## Risk Config

```kotlin
data class RiskConfig(
    val maxPositionPerTrade: BigDecimal = "0.05".toBigDecimal(),  // 5%
    val maxTotalExposure: BigDecimal = "0.10".toBigDecimal(),      // 10%
    val warningDrawdownLimit: BigDecimal = "0.10".toBigDecimal(),  // 10%
    val emergencyDrawdownLimit: BigDecimal = "0.15".toBigDecimal() // 15%
)
```

## File Structure

```
core/domain/src/main/kotlin/com/tradeflow/core/domain/
└── risk/
    ├── RiskManager.kt  (interface)
    ├── TradingRiskManager.kt
    ├── RiskConfig.kt
    ├── RiskStatus.kt
    └── OrderValidation.kt
```

## Acceptance Criteria

- [ ]  Drawdown calculated correctly from HWM
- [ ]  Emergency triggered at 15%
- [ ]  Position limits enforced
- [ ]  100% unit test coverage
- [ ]  No Android dependencies