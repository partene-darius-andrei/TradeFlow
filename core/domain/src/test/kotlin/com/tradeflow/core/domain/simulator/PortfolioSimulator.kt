package com.tradeflow.core.domain.simulator

import com.tradeflow.core.domain.model.Balance
import com.tradeflow.core.domain.model.Order
import com.tradeflow.core.domain.model.OrderSide
import com.tradeflow.core.domain.model.Portfolio
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

class PortfolioSimulator(
    startingCapitalUsd: BigDecimal,
    private val productId: String = "BTC-USD"
) {
    private var usdBalance: BigDecimal = startingCapitalUsd
    private var btcBalance: BigDecimal = BigDecimal.ZERO
    private var usdHold: BigDecimal = BigDecimal.ZERO
    private var btcHold: BigDecimal = BigDecimal.ZERO
    var highWaterMark: BigDecimal = startingCapitalUsd
        private set

    companion object {
        const val TAKER_FEE_PERCENT = 0.004  // 0.4% (market orders)
        const val MAKER_FEE_PERCENT = 0.0025 // 0.25% (limit orders)
    }

    fun reserveForOrder(order: Order, orderPrice: BigDecimal) {
        when (order.side) {
            OrderSide.BUY -> {
                val cost = order.size * orderPrice
                require(usdBalance >= cost) { "Insufficient USD to reserve: have $usdBalance, need $cost" }
                usdBalance -= cost
                usdHold += cost
            }
            OrderSide.SELL -> {
                require(btcBalance >= order.size) { "Insufficient BTC to reserve: have $btcBalance, need ${order.size}" }
                btcBalance -= order.size
                btcHold += order.size
            }
        }
    }

    fun releaseReservedFunds(order: Order, orderPrice: BigDecimal) {
        when (order.side) {
            OrderSide.BUY -> {
                val cost = order.size * orderPrice
                usdHold -= cost
                usdBalance += cost
            }
            OrderSide.SELL -> {
                btcHold -= order.size
                btcBalance += order.size
            }
        }
    }

    fun applyFill(order: Order, fillPrice: BigDecimal, isMaker: Boolean) {
        val feePercent = if (isMaker) MAKER_FEE_PERCENT else TAKER_FEE_PERCENT

        when (order.side) {
            OrderSide.BUY -> {
                // Funds already reserved in hold, just release them
                val cost = order.size * fillPrice
                usdHold -= cost

                // Receive BTC minus fees (fees paid in BTC)
                val btcReceived = order.size * (BigDecimal.ONE - BigDecimal(feePercent))
                btcBalance += btcReceived
            }
            OrderSide.SELL -> {
                // BTC already reserved in hold, just release it
                btcHold -= order.size

                // Receive USD minus fees (fees paid in USD)
                val usdReceived = order.size * fillPrice * (BigDecimal.ONE - BigDecimal(feePercent))
                usdBalance += usdReceived
            }
        }
    }

    fun calculateEquity(currentBtcPrice: BigDecimal): BigDecimal {
        // Total equity includes both available and hold balances
        val totalBtc = btcBalance + btcHold
        val totalUsd = usdBalance + usdHold
        val btcValueInUsd = totalBtc * currentBtcPrice
        return (totalUsd + btcValueInUsd).setScale(2, RoundingMode.HALF_UP)
    }

    fun updateHighWaterMark(currentBtcPrice: BigDecimal) {
        val equity = calculateEquity(currentBtcPrice)
        if (equity > highWaterMark) {
            highWaterMark = equity
        }
    }

    fun getPortfolio(currentBtcPrice: BigDecimal): Portfolio {
        val equity = calculateEquity(currentBtcPrice)

        return Portfolio(
            balances = listOf(
                Balance(currency = "USD", available = usdBalance, hold = usdHold),
                Balance(currency = "BTC", available = btcBalance, hold = btcHold)
            ),
            totalEquityUsd = equity,
            timestamp = Instant.now()
        )
    }

    fun getUsdBalance(): BigDecimal = usdBalance
    fun getBtcBalance(): BigDecimal = btcBalance

    fun reset(startingCapitalUsd: BigDecimal) {
        usdBalance = startingCapitalUsd
        btcBalance = BigDecimal.ZERO
        highWaterMark = startingCapitalUsd
    }
}
