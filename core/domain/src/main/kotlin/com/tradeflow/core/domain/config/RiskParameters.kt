package com.tradeflow.core.domain.config

import java.math.BigDecimal

/**
 * Risk management configuration parameters that define position sizing limits and circuit breakers.
 *
 * These parameters control HOW MUCH of the portfolio can be risked in various scenarios.
 * They're the core of capital preservation - preventing catastrophic losses from overleveraging
 * or runaway drawdowns.
 *
 * **Key Concepts:**
 * - **Position size:** How much capital goes into a single trade
 * - **Exposure:** Total capital at risk across all open positions
 * - **Drawdown:** Peak-to-trough decline in portfolio value
 * - **Grid spacing:** Distance between grid orders in range mode
 *
 * **Risk Hierarchy (most restrictive wins):**
 * ```
 * 1. Drawdown circuit breaker (stops ALL trading)
 * 2. Total exposure limit (caps aggregate risk)
 * 3. Single position limit (caps individual trade size)
 * ```
 *
 * **Usage in RiskManager:**
 * ```kotlin
 * val riskParams = RiskParameters(
 *     maxPositionPercent = BigDecimal("0.05"),  // 5% max per trade
 *     maxDrawdownPercent = 0.15  // 15% max drawdown before circuit breaker
 * )
 * val positionSize = riskManager.calculatePositionSize(
 *     availableCapital = portfolioBalance,
 *     params = riskParams
 * )
 * ```
 *
 * @property maxPositionPercent Maximum percentage of portfolio to risk on a SINGLE position.
 *           Example: 0.05 = 5% max per trade. With $1000 balance, max position = $50.
 *           **Rationale:** Limits single-trade risk to prevent one bad trade from destroying the account.
 *           Default: 5% (balanced risk, allows diversification).
 *
 * @property maxTotalExposurePercent Maximum percentage of portfolio that can be at risk across ALL open positions.
 *           Example: 0.10 = 10% max aggregate exposure. Prevents over-leveraging when multiple trades are open.
 *           **Rationale:** Two 5% positions = 10% total exposure. Must be >= maxPositionPercent.
 *           Default: 10% (allows 2 max-size positions concurrently).
 *
 * @property maxDrawdownPercent Maximum acceptable drawdown before circuit breaker activates (as decimal, not percent).
 *           Example: 0.15 = 15% drawdown limit. If portfolio drops 15% from peak, ALL trading stops.
 *           **Rationale:** Circuit breaker prevents strategy from bleeding out during adverse conditions.
 *           Default: 15% (aggressive but recoverable).
 *
 * @property drawdownWarningPercent Drawdown threshold that triggers warning but allows continued trading.
 *           Example: 0.12 = 12% warning level. Logs warning but doesn't stop trading.
 *           **Rationale:** Early warning system to alert before circuit breaker hits.
 *           Default: 12% (80% of max drawdown threshold).
 *
 * @property minGridSpacingPercent Minimum price distance between grid levels as percentage of entry price.
 *           Example: 0.015 = 1.5% minimum spacing. If BTC at $100k, grid levels must be >= $1500 apart.
 *           **Rationale:** Prevents "grid spam" where levels are too close, causing excessive fills.
 *           Default: 1.5% (balances granularity vs execution frequency).
 *
 * @property percentDecimalPlaces Decimal places for rounding percentage values in position sizing calculations.
 *           Example: 4 = round to 0.0001 (0.01%). Prevents floating point precision issues.
 *           Default: 4 decimal places.
 *
 * @property btcDecimalPlaces Decimal places for rounding BTC order sizes.
 *           Example: 8 = satoshi precision (0.00000001 BTC). Standard Bitcoin precision.
 *           **Rationale:** Exchanges use satoshi-level precision for BTC amounts.
 *           Default: 8 (satoshi precision).
 *
 * @see RiskManager for how these parameters are enforced during order placement
 * @see RiskProfile for pre-configured parameter sets (AGGRESSIVE, BALANCED, etc.)
 */
data class RiskParameters(
    val maxPositionPercent: BigDecimal = BigDecimal("0.05"),
    val maxTotalExposurePercent: BigDecimal = BigDecimal("0.10"),
    val maxDrawdownPercent: Double = 0.15,
    val drawdownWarningPercent: Double = 0.12,
    val minGridSpacingPercent: BigDecimal = BigDecimal("0.015"),
    val percentDecimalPlaces: Int = 4,
    val btcDecimalPlaces: Int = 8
)
