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
    var highWaterMark: BigDecimal = startingCapitalUsd
        private set

    companion object {
        const val TAKER_FEE_PERCENT = 0.004  // 0.4% (market orders)
        const val MAKER_FEE_PERCENT = 0.0025 // 0.25% (limit orders)
    }

    fun applyFill(order: Order, fillPrice: BigDecimal, isMaker: Boolean) {
        val feePercent = if (isMaker) MAKER_FEE_PERCENT else TAKER_FEE_PERCENT

        when (order.side) {
            OrderSide.BUY -> {
                // Spend USD to buy BTC
                val cost = order.size * fillPrice
                require(usdBalance >= cost) { "Insufficient USD balance: have $usdBalance, need $cost" }

                // Deduct cost from USD
                usdBalance -= cost

                // Receive BTC minus fees (fees paid in BTC)
                val btcReceived = order.size * (BigDecimal.ONE - BigDecimal(feePercent))
                btcBalance += btcReceived
            }
            OrderSide.SELL -> {
                // Sell BTC to receive USD
                require(btcBalance >= order.size) { "Insufficient BTC balance: have $btcBalance, need ${order.size}" }

                // Deduct BTC sold
                btcBalance -= order.size

                // Receive USD minus fees (fees paid in USD)
                val usdReceived = order.size * fillPrice * (BigDecimal.ONE - BigDecimal(feePercent))
                usdBalance += usdReceived
            }
        }
    }

    fun calculateEquity(currentBtcPrice: BigDecimal): BigDecimal {
        val btcValueInUsd = btcBalance * currentBtcPrice
        return (usdBalance + btcValueInUsd).setScale(2, RoundingMode.HALF_UP)
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
                Balance(currency = "USD", available = usdBalance, hold = BigDecimal.ZERO),
                Balance(currency = "BTC", available = btcBalance, hold = BigDecimal.ZERO)
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
