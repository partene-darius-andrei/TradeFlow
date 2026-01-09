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

        // CRITICAL: Set initial price from first candle before any trading
        val firstCandle = config.historicalCandles.first()
        exchange.advanceTime(firstCandle)

        // Record initial equity
        performanceTracker.recordEquitySnapshot(
            firstCandle.timestamp,
            config.startingCapital
        )

        // Process each candle (skip first as it was used for initialization)
        config.historicalCandles.drop(1).forEachIndexed { index, candle ->
            // 1. Advance time and match pending orders
            exchange.advanceTime(candle)

            // Wait until we have enough candles for indicators
            // +1 because we dropped first candle (it's already in history)
            if (index + 1 < config.strategyConfig.smaPeriod) {
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
                    if (openOrders.isEmpty()) {
                        // Calculate position size using RiskManager
                        val positionSize = riskManager.calculateTrendPositionSize(portfolio, decision.entryPrice)
                        val orderCost = positionSize * decision.entryPrice
                        val usdAvailable = portfolio.balances.find { it.currency == "USD" }?.available ?: BigDecimal.ZERO

                        // Only place order if we have enough available USD
                        if (positionSize > BigDecimal.ZERO && usdAvailable >= orderCost) {
                            exchange.placeLimitOrder(
                                productId = config.productId,
                                side = decision.direction,
                                size = positionSize,
                                price = decision.entryPrice,
                                postOnly = true
                            )
                        }
                    }
                }
                is Decision.Range -> {
                    // Place grid orders
                    val openOrders = exchange.getOpenOrders(config.productId).getOrNull() ?: emptyList()
                    val maxGridOrders = 5
                    val usdAvailable = portfolio.balances.find { it.currency == "USD" }?.available ?: BigDecimal.ZERO

                    if (openOrders.size < maxGridOrders) {
                        // Calculate position size per level using RiskManager
                        val positionSizePerLevel = riskManager.calculateGridPositionSize(
                            portfolio,
                            maxGridOrders,
                            currentPrice
                        )
                        if (positionSizePerLevel > BigDecimal.ZERO) {
                            // Calculate grid price levels and place orders one at a time
                            val ordersToPlace = maxGridOrders - openOrders.size
                            var remainingUsd = usdAvailable
                            repeat(ordersToPlace.coerceAtMost(decision.levels)) { i ->
                                val gridPrice = currentPrice - (decision.gridSpacing * BigDecimal(i + 1))
                                val orderCost = positionSizePerLevel * gridPrice
                                // Only place order if we have enough USD left
                                if (gridPrice > BigDecimal.ZERO && remainingUsd >= orderCost) {
                                    exchange.placeLimitOrder(
                                        productId = config.productId,
                                        side = OrderSide.BUY,
                                        size = positionSizePerLevel,
                                        price = gridPrice,
                                        postOnly = true
                                    )
                                    remainingUsd -= orderCost  // Reserve the funds
                                }
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
