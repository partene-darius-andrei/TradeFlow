package com.tradeflow.core.domain.risk

import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.model.getBtcBalance
import com.tradeflow.core.domain.risk.model.DrawdownStatus
import com.tradeflow.core.domain.risk.model.PlaceOrderRequest
import com.tradeflow.core.domain.risk.model.RiskCheck
import java.math.BigDecimal
import java.math.RoundingMode

class RiskManager(
    private val config: RiskConfig = RiskConfig()
) {

    fun validateOrder(
        request: PlaceOrderRequest,
        portfolio: Portfolio,
        currentPrice: BigDecimal
    ): RiskCheck {
        val orderPrice = request.price ?: currentPrice
        val orderValueUsd = request.size * orderPrice

        val positionPercent = orderValueUsd
            .divide(portfolio.totalEquityUsd, 4, RoundingMode.HALF_UP)

        if (positionPercent > config.maxPositionPercent) {
            return RiskCheck.Rejected(
                "Position size ${formatPercent(positionPercent)} exceeds limit ${formatPercent(config.maxPositionPercent)}"
            )
        }

        if (request.side == OrderSide.BUY) {
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

    fun calculateTrendPositionSize(
        portfolio: Portfolio,
        entryPrice: BigDecimal
    ): BigDecimal {
        val riskAmountUsd = portfolio.totalEquityUsd * config.maxPositionPercent
        return riskAmountUsd
            .divide(entryPrice, 8, RoundingMode.HALF_UP)
    }

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

    fun validateGridSpacing(spacingPercent: BigDecimal): Boolean {
        return spacingPercent >= config.minGridSpacingPercent
    }

    private fun formatPercent(value: BigDecimal): String {
        return "${(value * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)}%"
    }
}
