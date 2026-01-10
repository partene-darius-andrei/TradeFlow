package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.config.AdaptiveOptimizer
import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.BracketOrderRepository
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.strategy.DecisionEngine
import com.tradeflow.core.domain.strategy.TradingDecisionEngine
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class CycleResult(
    val execution: ExecutionResult,
    val updatedHighWaterMark: BigDecimal
)

/**
 * Single orchestrator for the trading cycle with adaptive risk profiles.
 */
class TradeOrchestrator @Inject constructor(
    private val exchangeRepository: ExchangeRepository,
    private val bracketOrderRepository: BracketOrderRepository,
    private val decisionEngine: DecisionEngine,
    private val adaptiveOptimizer: AdaptiveOptimizer
) {
    private var currentConfig: TradingConfig = TradingConfig.forProfile(RiskProfile.BALANCED)
    private var currentProfile: RiskProfile = RiskProfile.BALANCED

    suspend fun runCycle(productId: String, highWaterMark: BigDecimal): CycleResult {
        return try {
            val portfolio = exchangeRepository.getPortfolio().getOrThrow()

            // Adaptive profile switching
            val switchEvent = adaptiveOptimizer.detectProfileSwitch(currentProfile, portfolio.totalEquityUsd)
            if (switchEvent != null) {
                currentConfig = TradingConfig.forProfile(switchEvent.to)
                currentProfile = switchEvent.to
                println("  [ADAPTIVE] ${switchEvent.from} → ${switchEvent.to} | Balance: \$${switchEvent.balance.setScale(2, RoundingMode.HALF_UP)}")
                // Reset decision engine state when profile changes
                (decisionEngine as? TradingDecisionEngine)?.resetState()
            }

            val currentPrice = exchangeRepository.getCurrentPrice(productId).getOrThrow().price
            val candles = exchangeRepository.getCandles(productId, currentConfig.technical.granularity).getOrThrow()
            val openOrders = exchangeRepository.getOpenOrders(productId).getOrThrow()

            val currentHighWaterMark = if (portfolio.totalEquityUsd > highWaterMark) {
                portfolio.totalEquityUsd
            } else {
                highWaterMark
            }

            // 1. Risk Check
            println("  [RISK] Equity: \$${portfolio.totalEquityUsd.setScale(2, RoundingMode.HALF_UP)} | HWM: \$${currentHighWaterMark.setScale(2, RoundingMode.HALF_UP)}")
            if (currentHighWaterMark > BigDecimal.ZERO) {
                val drawdown = (currentHighWaterMark - portfolio.totalEquityUsd)
                    .divide(currentHighWaterMark, currentConfig.risk.percentDecimalPlaces, RoundingMode.HALF_UP)
                println("  [RISK] Drawdown: ${drawdown.multiply(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)}%")
                if (drawdown > BigDecimal.valueOf(currentConfig.risk.maxDrawdownPercent)) {
                    exchangeRepository.cancelOrders(openOrders.map { it.id })
                    val btc = portfolio.getBtcBalance()
                    if (btc > currentConfig.execution.minBtcDustThreshold) {
                        exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, btc)
                    }
                    return CycleResult(
                        ExecutionResult.Failed("EMERGENCY: ${currentConfig.risk.maxDrawdownPercent * 100}% Drawdown reached. Liquidated."),
                        currentHighWaterMark
                    )
                }
            }

            // 2. State Check
            val btcBalance = portfolio.getBtcBalance()
            val hasBtcBalance = btcBalance > currentConfig.execution.minBtcDustThreshold
            val hasOpenOrders = openOrders.isNotEmpty()
            val isInTrade = hasBtcBalance || hasOpenOrders
            println("  [STATE] BTC: $btcBalance | Open Orders: ${openOrders.size} | In Trade: $isInTrade")

            val decision = decisionEngine.evaluate(candles, currentPrice)

            // 3. Execution
            val executionResult = when (decision) {
                is Decision.Wait -> ExecutionResult.Skipped("Wait: ${decision.reason}")
                is Decision.Defense -> {
                    val buys = openOrders.filter { it.side == OrderSide.BUY }.map { it.id }
                    if (buys.isNotEmpty()) exchangeRepository.cancelOrders(buys)

                    if (hasBtcBalance) {
                        exchangeRepository.placeMarketOrder(productId, OrderSide.SELL, btcBalance).getOrThrow()
                        ExecutionResult.Success("Defense: Liquidated holdings.")
                    } else {
                        ExecutionResult.Skipped("Defense: Clean.")
                    }
                }
                is Decision.Trend -> {
                    if (!isInTrade) {
                        val sizeUsd = portfolio.totalEquityUsd * decision.positionSizePercent
                        val btcSize = sizeUsd.divide(decision.entryPrice, 8, RoundingMode.HALF_UP)
                        println("  [EXEC] TREND: Size \$${sizeUsd.setScale(2, RoundingMode.HALF_UP)} = ${btcSize.setScale(8, RoundingMode.HALF_UP)} BTC")

                        bracketOrderRepository.placeBracketOrder(
                            productId, decision.direction, btcSize,
                            decision.entryPrice, decision.takeProfit, decision.stopLoss
                        ).getOrThrow()
                        ExecutionResult.Success("Trend: Opened position.")
                    } else {
                        println("  [EXEC] TREND: Skipped (already in trade)")
                        ExecutionResult.Skipped("Trend: Already in trade.")
                    }
                }
                is Decision.Range -> {
                    if (!isInTrade) {
                        val sizeUsd = portfolio.totalEquityUsd * decision.positionSizePercentPerLevel
                        var ordersPlaced = 0
                        println("  [EXEC] RANGE: \$${sizeUsd.setScale(2, RoundingMode.HALF_UP)} per level × ${decision.levels} levels | Spacing: \$${decision.gridSpacing.setScale(2, RoundingMode.HALF_UP)}")

                        for (level in 1..decision.levels) {
                            val levelPrice = currentPrice - (decision.gridSpacing * BigDecimal(level))
                            val btcSize = sizeUsd.divide(levelPrice, 8, RoundingMode.HALF_UP)

                            if (level == 1) {
                                println("  [EXEC] Grid level 1: BUY ${btcSize.setScale(8, RoundingMode.HALF_UP)} BTC @ \$${levelPrice.setScale(2, RoundingMode.HALF_UP)}")
                            }

                            exchangeRepository.placeLimitOrder(
                                productId, OrderSide.BUY, btcSize, levelPrice, true
                            ).onSuccess { ordersPlaced++ }
                        }

                        ExecutionResult.Success("Range: Placed $ordersPlaced/${decision.levels} grid orders.")
                    } else if (hasBtcBalance && openOrders.none { it.side == OrderSide.SELL }) {
                        val targetProfitPrice = currentPrice + decision.gridSpacing
                        exchangeRepository.placeLimitOrder(productId, OrderSide.SELL, btcBalance, targetProfitPrice, true).getOrThrow()
                        ExecutionResult.Success("Range: Placed take-profit for grid fill.")
                    } else {
                        ExecutionResult.Skipped("Range: Active.")
                    }
                }
            }

            CycleResult(executionResult, currentHighWaterMark)
        } catch (e: Exception) {
            CycleResult(
                ExecutionResult.Failed("Cycle failed: ${e.message}"),
                highWaterMark
            )
        }
    }
}
