package com.tradeflow.core.domain.simulator

import com.tradeflow.core.domain.indicator.ADXCalculator
import com.tradeflow.core.domain.indicator.ATRCalculator
import com.tradeflow.core.domain.indicator.SMACalculator
import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.risk.RiskManager
import com.tradeflow.core.domain.strategy.StrategyConfig
import com.tradeflow.core.domain.strategy.TradingDecisionEngine
import java.math.BigDecimal

data class BacktestConfig(
    val startingCapital: BigDecimal,
    val productId: String,
    val historicalCandles: List<Candle>,
    val strategyConfig: StrategyConfig
)

class BacktestEngine {

    suspend fun runBacktest(config: BacktestConfig): BacktestResult {
        // Initialize components
        val exchange = SimulatedExchangeRepository(config.startingCapital, config.productId)
        val decisionEngine = TradingDecisionEngine(
            smaCalculator = SMACalculator(),
            adxCalculator = ADXCalculator(),
            atrCalculator = ATRCalculator()
        )
        val riskManager = RiskManager()
        val performanceTracker = PerformanceTracker(config.startingCapital)

        // Record initial equity
        performanceTracker.recordEquitySnapshot(
            config.historicalCandles.first().timestamp,
            config.startingCapital
        )

        // Process each candle
        config.historicalCandles.forEachIndexed { index, candle ->
            // 1. Advance time and match pending orders
            exchange.advanceTime(candle)

            // Wait until we have enough candles for indicators
            if (index < config.strategyConfig.smaPeriod) {
                return@forEachIndexed
            }

            // 2. Get current market state
            val recentCandles = exchange.getCandleHistory().takeLast(config.strategyConfig.smaPeriod + 50)
            val currentPrice = candle.close
            val portfolio = exchange.getPortfolio().getOrNull() ?: return@forEachIndexed

            // 3. Make trading decision
            val decision = decisionEngine.evaluate(recentCandles, currentPrice)

            // 4. Execute decision with risk management
            when (decision) {
                is Decision.Wait -> {
                    // No action
                }
                is Decision.Defense -> {
                    // Cancel all open buy orders
                    val openOrders = exchange.getOpenOrders(config.productId).getOrNull() ?: emptyList()
                    val buyOrderIds = openOrders.filter { it.side == OrderSide.BUY }.map { it.id }
                    if (buyOrderIds.isNotEmpty()) {
                        exchange.cancelOrders(buyOrderIds)
                    }
                }
                is Decision.Trend -> {
                    // Check if we should enter a trend position
                    val openOrders = exchange.getOpenOrders(config.productId).getOrNull() ?: emptyList()
                    if (openOrders.isEmpty() && decision.positionSize > BigDecimal.ZERO) {
                        // Place limit entry order (Decision already contains position size)
                        exchange.placeLimitOrder(
                            productId = config.productId,
                            side = decision.direction,
                            size = decision.positionSize,
                            price = decision.entryPrice,
                            postOnly = true
                        )
                    }
                }
                is Decision.Range -> {
                    // Place grid orders
                    val openOrders = exchange.getOpenOrders(config.productId).getOrNull() ?: emptyList()
                    val maxGridOrders = 5
                    if (openOrders.size < maxGridOrders && decision.positionSizePerLevel > BigDecimal.ZERO) {
                        // Calculate grid price levels
                        val ordersToPlace = maxGridOrders - openOrders.size
                        repeat(ordersToPlace.coerceAtMost(decision.levels)) { i ->
                            val gridPrice = currentPrice - (decision.gridSpacing * BigDecimal(i + 1))
                            if (gridPrice > BigDecimal.ZERO) {
                                exchange.placeLimitOrder(
                                    productId = config.productId,
                                    side = OrderSide.BUY,
                                    size = decision.positionSizePerLevel,
                                    price = gridPrice,
                                    postOnly = true
                                )
                            }
                        }
                    }
                }
            }

            // 5. Check for emergency stop (max drawdown)
            val equity = portfolio.totalEquityUsd
            val drawdownPercent = if (exchange.getHighWaterMark() > BigDecimal.ZERO) {
                ((exchange.getHighWaterMark() - equity) / exchange.getHighWaterMark() * BigDecimal("100"))
            } else {
                BigDecimal.ZERO
            }

            if (drawdownPercent >= BigDecimal("15")) {
                // Emergency liquidation
                val openOrders = exchange.getOpenOrders(config.productId).getOrNull() ?: emptyList()
                if (openOrders.isNotEmpty()) {
                    exchange.cancelOrders(openOrders.map { it.id })
                }

                // Sell all BTC
                val btcBalance = portfolio.balances.find { it.currency == "BTC" }?.available ?: BigDecimal.ZERO
                if (btcBalance > BigDecimal.ZERO) {
                    exchange.placeMarketOrder(
                        productId = config.productId,
                        side = OrderSide.SELL,
                        size = btcBalance
                    )
                }

                // Stop simulation
                return@runBacktest performanceTracker.generateReport()
            }

            // 6. Record equity snapshot and fills
            performanceTracker.recordEquitySnapshot(candle.timestamp, equity)

            // Record fills from this candle
            val fills = exchange.getFilledOrders().takeLast(100)  // Recent fills
            fills.forEach { order ->
                if (order.status == OrderStatus.FILLED) {
                    val strategyName = when (decision) {
                        is Decision.Trend -> "TREND"
                        is Decision.Range -> "RANGE"
                        else -> "DEFENSE"
                    }

                    val fill = Fill(order, order.avgFilledPrice ?: currentPrice, isMaker = order.type == OrderType.LIMIT)
                    performanceTracker.recordFill(fill, strategyName, currentPrice, equity)
                }
            }
        }

        // Generate final report
        return performanceTracker.generateReport()
    }
}
