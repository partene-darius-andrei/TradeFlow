# 🚨 DOMAIN - Risk Manager

Effort level: Medium
Priority: High
Status: Not started

## Objective

Domain service for risk validation and position sizing. **Exchange-agnostic.**

## File

`domain/risk/RiskManager.kt`

## Risk Limits (Configurable)

```kotlin
data class RiskConfig(
    val maxPositionPercent: Double = 0.05,      // 5% per trade
    val maxTotalExposurePercent: Double = 0.10, // 10% total
    val maxDrawdownPercent: Double = 0.15,      // 15% drawdown limit
    val minGridSpacingPercent: Double = 0.015   // 1.5% (fee break-even)
)
```

## Implementation

```kotlin
class RiskManager @Inject constructor(
    private val config: RiskConfig,
    private val portfolioDao: PortfolioDao
) {
    
    /**
     * Validate order against risk limits
     */
    fun validateOrder(request: PlaceOrderRequest, portfolio: Portfolio): RiskCheck {
        val orderValue = request.size * (request.price ?: portfolio.currentPrice)
        val positionPercent = orderValue / portfolio.totalEquity
        
        // Check position size
        if (positionPercent > config.maxPositionPercent) {
            return RiskCheck.Rejected(
                "Position size ${positionPercent.toPercent()} exceeds limit ${config.maxPositionPercent.toPercent()}"
            )
        }
        
        // Check total exposure
        val currentExposure = portfolio.btcValue / portfolio.totalEquity
        val newExposure = currentExposure + positionPercent
        if (newExposure > config.maxTotalExposurePercent) {
            return RiskCheck.Rejected(
                "Total exposure ${newExposure.toPercent()} would exceed limit ${config.maxTotalExposurePercent.toPercent()}"
            )
        }
        
        return RiskCheck.Approved
    }
    
    /**
     * Check if drawdown limit breached
     */
    suspend fun checkDrawdown(currentEquity: BigDecimal): DrawdownStatus {
        val hwm = portfolioDao.getHighWaterMark() ?: currentEquity
        val drawdown = if (hwm > [BigDecimal.ZERO](http://BigDecimal.ZERO)) {
            ((hwm - currentEquity) / hwm).toDouble()
        } else 0.0
        
        return when {
            drawdown >= config.maxDrawdownPercent -> DrawdownStatus.LimitBreached(drawdown)
            drawdown >= config.maxDrawdownPercent * 0.8 -> DrawdownStatus.Warning(drawdown)
            else -> DrawdownStatus.Normal(drawdown)
        }
    }
    
    /**
     * Calculate position size for trend trade
     */
    fun calculateTrendPositionSize(
        portfolio: Portfolio,
        entryPrice: BigDecimal
    ): BigDecimal {
        val riskAmount = portfolio.totalEquity * BigDecimal(config.maxPositionPercent)
        return riskAmount / entryPrice
    }
    
    /**
     * Calculate grid position size
     */
    fun calculateGridPositionSize(
        portfolio: Portfolio,
        gridLevels: Int,
        entryPrice: BigDecimal
    ): BigDecimal {
        val totalRisk = portfolio.totalEquity * BigDecimal(config.maxTotalExposurePercent)
        val perLevelRisk = totalRisk / BigDecimal(gridLevels)
        return perLevelRisk / entryPrice
    }
    
    /**
     * Validate grid spacing meets fee requirements
     */
    fun validateGridSpacing(spacingPercent: Double): Boolean {
        return spacingPercent >= config.minGridSpacingPercent
    }
}

sealed class RiskCheck {
    object Approved : RiskCheck()
    data class Rejected(val reason: String) : RiskCheck()
}

sealed class DrawdownStatus {
    data class Normal(val percent: Double) : DrawdownStatus()
    data class Warning(val percent: Double) : DrawdownStatus()
    data class LimitBreached(val percent: Double) : DrawdownStatus()
}
```

## Usage in TradingService

```kotlin
// In strategy loop
val drawdownStatus = riskManager.checkDrawdown(currentEquity)
if (drawdownStatus is DrawdownStatus.LimitBreached) {
    emergencyLiquidate()
    stopSelf()
    return
}
```

## Acceptance Criteria

- [ ]  No exchange-specific code
- [ ]  All limits configurable
- [ ]  Position sizing for both trend and grid
- [ ]  Unit tests with various scenarios