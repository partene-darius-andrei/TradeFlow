package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.risk.RiskManager
import com.tradeflow.core.domain.risk.model.DrawdownStatus
import com.tradeflow.core.domain.strategy.DecisionEngine
import com.tradeflow.core.domain.usecase.model.ExecutionResult
import com.tradeflow.core.domain.usecase.model.TradingContext
import java.math.BigDecimal

class ExecuteTradingCycleUseCase(
    private val exchangeRepository: ExchangeRepository,
    private val decisionEngine: DecisionEngine,
    private val riskManager: RiskManager,
    private val updatePortfolioUseCase: UpdatePortfolioUseCase,
    private val executeDecisionUseCase: ExecuteDecisionUseCase,
    private val handleEmergencyUseCase: HandleEmergencyUseCase,
    private val manageOrdersUseCase: ManageOrdersUseCase
) {

    suspend fun execute(context: TradingContext): TradingCycleResult {
        val steps = mutableListOf<String>()

        val portfolioSnapshot = updatePortfolioUseCase.execute(context.currentPrice)
            .getOrElse {
                return TradingCycleResult.Failed("Portfolio update failed: ${it.message}")
            }

        steps.add("Portfolio updated: $${portfolioSnapshot.portfolio.totalEquityUsd}")

        val drawdownStatus = riskManager.checkDrawdown(
            portfolioSnapshot.portfolio.totalEquityUsd,
            context.highWaterMark
        )

        when (drawdownStatus) {
            is DrawdownStatus.LimitBreached -> {
                steps.add("EMERGENCY: Drawdown ${(drawdownStatus.drawdownPercent * 100).toInt()}% - liquidating")
                val emergencyResult = handleEmergencyUseCase.execute(context.productId)
                return TradingCycleResult.Emergency(
                    drawdownPercent = drawdownStatus.drawdownPercent,
                    steps = steps,
                    emergencyResult = emergencyResult
                )
            }
            is DrawdownStatus.Warning -> {
                steps.add("WARNING: Drawdown ${(drawdownStatus.drawdownPercent * 100).toInt()}%")
            }
            is DrawdownStatus.Normal -> {
                steps.add("Drawdown: ${(drawdownStatus.drawdownPercent * 100).toInt()}% (normal)")
            }
        }

        val decision = decisionEngine.evaluate(context.candles, context.currentPrice)
        steps.add("Decision: ${decision::class.simpleName}")

        val executionResult = executeDecisionUseCase.execute(
            decision = decision,
            portfolio = portfolioSnapshot.portfolio,
            currentPrice = context.currentPrice,
            productId = context.productId
        )

        steps.add(executionResult.toMessage())

        val staleOrderResult = manageOrdersUseCase.cancelStaleOrders(context.productId)
        steps.add(staleOrderResult.toMessage())

        return TradingCycleResult.Success(
            decision = decision,
            executionResult = executionResult,
            portfolioEquity = portfolioSnapshot.portfolio.totalEquityUsd,
            drawdownPercent = drawdownStatus.drawdownPercent(),
            steps = steps
        )
    }

    private fun ExecutionResult.toMessage(): String {
        return when (this) {
            is ExecutionResult.Success -> message
            is ExecutionResult.Skipped -> "Skipped: $reason"
            is ExecutionResult.Failed -> "Failed: $error"
        }
    }

    private fun DrawdownStatus.drawdownPercent(): Double {
        return when (this) {
            is DrawdownStatus.Normal -> drawdownPercent
            is DrawdownStatus.Warning -> drawdownPercent
            is DrawdownStatus.LimitBreached -> drawdownPercent
        }
    }

    sealed class TradingCycleResult {
        data class Success(
            val decision: Decision,
            val executionResult: ExecutionResult,
            val portfolioEquity: BigDecimal,
            val drawdownPercent: Double,
            val steps: List<String>
        ) : TradingCycleResult()

        data class Emergency(
            val drawdownPercent: Double,
            val steps: List<String>,
            val emergencyResult: ExecutionResult
        ) : TradingCycleResult()

        data class Failed(
            val error: String
        ) : TradingCycleResult()
    }
}
