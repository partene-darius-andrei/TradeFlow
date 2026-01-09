package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.model.getBtcBalance
import com.tradeflow.core.domain.repository.ExchangeRepository
import java.math.BigDecimal
import java.time.Instant

class UpdatePortfolioUseCase(
    private val exchangeRepository: ExchangeRepository
) {

    suspend fun execute(currentPrice: BigDecimal): Result<PortfolioSnapshot> {
        return exchangeRepository.getBalances()
            .map { balances ->
                val portfolio = Portfolio(
                    balances = balances,
                    totalEquityUsd = calculateTotalEquity(balances, currentPrice),
                    timestamp = Instant.now()
                )

                PortfolioSnapshot(
                    portfolio = portfolio,
                    btcValue = portfolio.getBtcBalance() * currentPrice
                )
            }
    }

    private fun calculateTotalEquity(
        balances: List<com.tradeflow.core.domain.model.Balance>,
        btcPrice: BigDecimal
    ): BigDecimal {
        val usdBalance = balances
            .firstOrNull { it.currency == "USD" || it.currency == "USDT" }
            ?.available
            ?: BigDecimal.ZERO

        val btcBalance = balances
            .firstOrNull { it.currency == "BTC" }
            ?.available
            ?: BigDecimal.ZERO

        return usdBalance + (btcBalance * btcPrice)
    }

    data class PortfolioSnapshot(
        val portfolio: Portfolio,
        val btcValue: BigDecimal
    )
}
