package com.tradeflow.core.domain.simulator

import com.tradeflow.core.domain.config.ExchangeSimulationParameters
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.model.*
import com.tradeflow.core.domain.repository.ExchangeRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.util.UUID

class SimulatedExchange(
    initialUsd: BigDecimal,
    private val tradingConfig: TradingConfig,
    private val parameters: ExchangeSimulationParameters = ExchangeSimulationParameters()
) : ExchangeRepository {

    var usdBalance = initialUsd
    private val openOrders = mutableListOf<Order>()
    var currentPrice: BigDecimal = BigDecimal.ZERO
    private var history = mutableListOf<Candle>()

    // Perpetual futures state
    private var perpetualPosition: PerpetualPosition? = null
    private var lastFundingTime: Instant? = null

    fun advanceTime(newCandle: Candle) {
        this.currentPrice = newCandle.close
        this.history.add(newCandle)

        // Check liquidation BEFORE processing orders
        checkLiquidation(newCandle)

        // Deduct funding rate from perpetual position (every 8 hours)
        deductFundingRate(newCandle.timestamp)

        // Update perpetual position unrealized PnL
        updatePerpetualPositionPnL()

        // Collect group IDs to cancel (OCO logic)
        val groupIdsToCancel = mutableSetOf<String>()

        val iterator = openOrders.iterator()
        while (iterator.hasNext()) {
            val order = iterator.next()

            // Check if limit price was touched
            val limitPrice = order.price ?: currentPrice

            // Determine if this is a stop-loss or take-profit based on position direction
            val position = perpetualPosition
            val hit = if (position != null && order.side != position.side) {
                // This is an exit order (TP or SL)
                // For LONG position (closed with SELL order):
                //   - TP: SELL above entry → triggers when price goes up (high >= TP)
                //   - SL: SELL below entry → triggers when price goes down (low <= SL)
                // For SHORT position (closed with BUY order):
                //   - TP: BUY below entry → triggers when price goes down (low <= TP)
                //   - SL: BUY above entry → triggers when price goes up (high >= SL)

                when (position.side) {
                    OrderSide.BUY -> {
                        // LONG position, exit with SELL
                        if (limitPrice > position.entryPrice) {
                            // Take profit: SELL above entry
                            newCandle.high >= limitPrice
                        } else {
                            // Stop loss: SELL below entry
                            newCandle.low <= limitPrice
                        }
                    }
                    OrderSide.SELL -> {
                        // SHORT position, exit with BUY
                        if (limitPrice < position.entryPrice) {
                            // Take profit: BUY below entry
                            newCandle.low <= limitPrice
                        } else {
                            // Stop loss: BUY above entry
                            newCandle.high >= limitPrice
                        }
                    }
                }
            } else {
                // Standard order matching (entry orders)
                when(order.side) {
                    OrderSide.BUY -> newCandle.low <= limitPrice
                    OrderSide.SELL -> newCandle.high >= limitPrice
                }
            }

            if (hit) {
                // Check if this order is closing a perpetual position
                val isPerpetual = order.productId.contains("PERP", ignoreCase = true)
                val isClosingPerpetual = isPerpetual && perpetualPosition != null

                if (isClosingPerpetual) {
                    // Determine if this is TP or SL based on position
                    val position = perpetualPosition!!
                    val isTakeProfit = when (position.side) {
                        OrderSide.BUY -> order.side == OrderSide.SELL && limitPrice > position.entryPrice
                        OrderSide.SELL -> order.side == OrderSide.BUY && limitPrice < position.entryPrice
                    }

                    // Apply realistic micro-slippage when limit triggers
                    val fillPrice = if (isTakeProfit) {
                        limitPrice * BigDecimal("0.9995")  // TP: -0.05% (slightly worse than limit)
                    } else {
                        limitPrice * BigDecimal("1.0005")  // SL: +0.05% (slightly worse than limit)
                    }

                    // Update current price to fillPrice for PnL calculation
                    val previousPrice = currentPrice
                    currentPrice = fillPrice
                    updatePerpetualPositionPnL()
                    realizePerpetualPosition()
                    currentPrice = previousPrice  // Restore actual price

                    // OCO Logic: Mark group for cancellation (cancel after iteration)
                    val groupId = order.clientOrderId
                    if (groupId.isNotEmpty()) {
                        groupIdsToCancel.add(groupId)
                    }

                    iterator.remove()
                }
                // NOTE: Spot order execution removed - perpetual futures only
            }
        }

        // Cancel all orders in marked groups (after iteration completes)
        groupIdsToCancel.forEach { groupId ->
            cancelOrderGroup(groupId)
        }
    }

    /**
     * Applies realistic slippage to simulated fills (±0.1%).
     * - BUY orders: Fill at slightly higher price (+0.1% slippage)
     * - SELL orders: Fill at slightly lower price (-0.1% slippage)
     */
    private fun applySlippage(price: BigDecimal, side: OrderSide): BigDecimal {
        return when (side) {
            OrderSide.BUY -> price * (BigDecimal.ONE + parameters.slippagePercent) // Pay more
            OrderSide.SELL -> price * (BigDecimal.ONE - parameters.slippagePercent) // Receive less
        }
    }

    /**
     * Cancels all orders in the same OCO group (e.g., TP and SL orders after entry fills).
     */
    private fun cancelOrderGroup(groupId: String) {
        openOrders.removeAll { it.clientOrderId == groupId }
    }

    private fun checkLiquidation(candle: Candle) {
        val position = perpetualPosition ?: return

        val liquidationTriggered = when (position.side) {
            OrderSide.BUY -> candle.low <= position.liquidationPrice
            OrderSide.SELL -> candle.high >= position.liquidationPrice
        }

        if (liquidationTriggered) {
            val liquidationFee = position.margin * BigDecimal("0.05")
            val remainingMargin = position.margin - liquidationFee

            usdBalance += remainingMargin.coerceAtLeast(BigDecimal.ZERO)
            perpetualPosition = null
            lastFundingTime = null

            println("⚠️ LIQUIDATED ${position.side} position at ${position.liquidationPrice}")
        }
    }

    fun setHistory(candles: List<Candle>) {
        this.history = candles.toMutableList()
        this.currentPrice = candles.last().close
    }

    fun getTotalEquity(): BigDecimal {
        // Perpetual futures: equity = usdBalance (margin) + unrealized PnL
        val unrealizedPnl = perpetualPosition?.unrealizedPnl ?: BigDecimal.ZERO
        return usdBalance + unrealizedPnl
    }

    override suspend fun getBalances(): Result<List<Balance>> = Result.success(listOf(
        Balance("USD", usdBalance, BigDecimal.ZERO)
    ))

    override suspend fun getPortfolio(): Result<Portfolio> = Result.success(Portfolio(
        balances = listOf(
            Balance("USD", usdBalance, BigDecimal.ZERO)
        ),
        totalEquityUsd = getTotalEquity(),
        timestamp = Instant.now()
    ))

    override suspend fun getCandles(productId: String, granularity: Granularity, limit: Int): Result<List<Candle>> = 
        Result.success(history.takeLast(limit))

    override suspend fun getCurrentPrice(productId: String): Result<Ticker> = 
        Result.success(Ticker(productId, currentPrice, currentPrice, currentPrice, BigDecimal.ZERO, Instant.now()))

    override suspend fun placeLimitOrder(productId: String, side: OrderSide, size: BigDecimal, price: BigDecimal, postOnly: Boolean): Result<Order> {
        val order = Order(UUID.randomUUID().toString(), UUID.randomUUID().toString(), productId, side, OrderType.LIMIT, OrderStatus.OPEN, size, price, BigDecimal.ZERO, null, Instant.now())
        openOrders.add(order)
        return Result.success(order)
    }

    override suspend fun placeBracketOrder(productId: String, side: OrderSide, size: BigDecimal, entryPrice: BigDecimal, takeProfit: BigDecimal, stopLoss: BigDecimal): Result<Order> {
        // Apply slippage to entry (market order)
        val entryFillPrice = applySlippage(entryPrice, side)

        val entryOrder = Order(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            productId,
            side,
            OrderType.MARKET,
            OrderStatus.FILLED,
            size,
            entryFillPrice,
            size,
            entryFillPrice,
            Instant.now()
        )

        return try {
            // PERPETUAL FUTURES ONLY (all products are perpetual now)
            // Open perpetual futures position with leverage from config
            val leverage = tradingConfig.strategy.leverage
            openPerpetualPosition(productId, side, size, entryFillPrice, leverage)

            // Place TP/SL orders to close perpetual position
            val groupId = UUID.randomUUID().toString()
            val exitSide = if (side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY

            val tpOrder = Order(
                UUID.randomUUID().toString(),
                groupId,
                productId,
                exitSide,
                OrderType.LIMIT,
                OrderStatus.OPEN,
                size,
                takeProfit,
                BigDecimal.ZERO,
                null,
                Instant.now()
            )

            val slOrder = Order(
                UUID.randomUUID().toString(),
                groupId,
                productId,
                exitSide,
                OrderType.LIMIT,
                OrderStatus.OPEN,
                size,
                stopLoss,
                BigDecimal.ZERO,
                null,
                Instant.now()
            )

            openOrders.add(tpOrder)
            openOrders.add(slOrder)

            Result.success(entryOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOpenOrders(productId: String): Result<List<Order>> = Result.success(openOrders)
    override suspend fun cancelOrders(orderIds: List<String>): Result<Int> {
        val count = openOrders.size
        openOrders.clear()
        return Result.success(count)
    }

    override suspend fun placeMarketOrder(productId: String, side: OrderSide, size: BigDecimal): Result<Order> {
        // PERPETUAL FUTURES ONLY
        // Market orders are used only for emergency exits (close perpetual position)
        // This should not be called directly - use closePerpetualPosition() instead

        val fillPrice = applySlippage(currentPrice, side)

        val order = Order(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            productId,
            side,
            OrderType.MARKET,
            OrderStatus.FILLED,
            size,
            fillPrice,
            size,
            fillPrice,
            Instant.now()
        )

        // Note: Actual perpetual position closing handled via closePerpetualPosition()
        // This method kept for interface compatibility only
        return Result.success(order)
    }
    override suspend fun cancelOrder(orderId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getOrder(orderId: String): Result<Order> =
        Result.failure(Exception("getOrder not implemented in SimulatedExchange"))

    // Perpetual futures simulation
    override suspend fun getPerpetualPosition(productId: String): Result<PerpetualPosition?> {
        return Result.success(perpetualPosition)
    }

    override suspend fun closePerpetualPosition(productId: String): Result<Unit> {
        realizePerpetualPosition()
        return Result.success(Unit)
    }

    /**
     * Internal non-suspend function to close perpetual position and realize PnL.
     * Used by both closePerpetualPosition (suspend) and advanceTime (non-suspend).
     */
    private fun realizePerpetualPosition() {
        val position = perpetualPosition ?: return

        // Realize PnL by closing position
        val exitValue = position.size * currentPrice
        // Use maker fee for limit order exits (TP/SL)
        val fee = exitValue * parameters.makerFeeRate

        when (position.side) {
            OrderSide.BUY -> {
                // LONG: Sell BTC to close (PnL already in unrealizedPnl)
                usdBalance += (position.unrealizedPnl + position.margin - fee)
            }
            OrderSide.SELL -> {
                // SHORT: Buy BTC to close (PnL already in unrealizedPnl)
                usdBalance += (position.unrealizedPnl + position.margin - fee)
            }
        }

        perpetualPosition = null
        lastFundingTime = null
    }

    override suspend fun getFundingRate(productId: String): Result<FundingRate> {
        val nextFunding = lastFundingTime?.plusSeconds(parameters.fundingIntervalHours * 3600L)
            ?: Instant.now().plusSeconds(parameters.fundingIntervalHours * 3600L)

        return Result.success(
            FundingRate(
                productId = productId,
                rate = parameters.fundingRatePerInterval,
                nextFundingTime = nextFunding,
                predictedRate = parameters.fundingRatePerInterval
            )
        )
    }

    /**
     * Opens a perpetual futures position with leverage.
     * This is called internally by placeBracketOrder when using perpetual futures.
     */
    private fun openPerpetualPosition(
        productId: String,
        side: OrderSide,
        size: BigDecimal,
        entryPrice: BigDecimal,
        leverage: BigDecimal
    ) {
        val notionalValue = size * entryPrice
        val margin = notionalValue / leverage
        val fee = notionalValue * parameters.takerFeeRate

        // Deduct margin + fees from balance
        if (usdBalance < (margin + fee)) {
            throw Exception("Insufficient funds for perpetual position")
        }
        usdBalance -= (margin + fee)

        // Calculate liquidation price
        val liquidationPrice = when (side) {
            OrderSide.BUY -> entryPrice * (BigDecimal.ONE - (BigDecimal.ONE / leverage))
            OrderSide.SELL -> entryPrice * (BigDecimal.ONE + (BigDecimal.ONE / leverage))
        }

        perpetualPosition = PerpetualPosition(
            productId = productId,
            side = side,
            size = size,
            entryPrice = entryPrice,
            currentPrice = currentPrice,
            unrealizedPnl = BigDecimal.ZERO,
            leverage = leverage,
            margin = margin,
            liquidationPrice = liquidationPrice,
            highWaterMarkPrice = entryPrice,
            timestamp = Instant.now()
        )

        lastFundingTime = Instant.now()
    }

    /**
     * Updates unrealized PnL for open perpetual position based on current price.
     * Also updates high water mark for trailing stop purposes.
     */
    private fun updatePerpetualPositionPnL() {
        val position = perpetualPosition ?: return

        val pnl = when (position.side) {
            OrderSide.BUY -> (currentPrice - position.entryPrice) * position.size
            OrderSide.SELL -> (position.entryPrice - currentPrice) * position.size
        }

        val newHighWaterMark = when (position.side) {
            OrderSide.BUY -> maxOf(currentPrice, position.highWaterMarkPrice)
            OrderSide.SELL -> minOf(currentPrice, position.highWaterMarkPrice)
        }

        perpetualPosition = position.copy(
            currentPrice = currentPrice,
            unrealizedPnl = pnl,
            highWaterMarkPrice = newHighWaterMark
        )
    }

    /**
     * Deducts funding rate from margin every funding interval (default 8 hours).
     * Funding rate is charged to the position holder to maintain perpetual futures price peg.
     */
    private fun deductFundingRate(currentTime: Instant) {
        val position = perpetualPosition ?: return
        val lastFunding = lastFundingTime ?: return

        val hoursSinceLastFunding = Duration.between(lastFunding, currentTime).toHours()

        if (hoursSinceLastFunding >= parameters.fundingIntervalHours) {
            val fundingCost = position.size * position.currentPrice * parameters.fundingRatePerInterval

            // Deduct funding from margin only (returned to balance when position closes)
            val newMargin = position.margin - fundingCost

            if (newMargin <= BigDecimal.ZERO) {
                // Margin exhausted - liquidate position
                perpetualPosition = null
                lastFundingTime = null
                println("⚠️ LIQUIDATED due to funding exhaustion")
            } else {
                perpetualPosition = position.copy(margin = newMargin)
                lastFundingTime = currentTime
            }
        }
    }
}
