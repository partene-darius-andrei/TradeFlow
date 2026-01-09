package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.repository.ExchangeRepository
import java.time.Instant
import javax.inject.Inject

class UpdatePortfolioUseCase @Inject constructor(
    private val repository: ExchangeRepository
) {

    suspend fun execute(): Result<Portfolio> = runCatching {
        val balances = repository.getBalances().getOrThrow()
        
        // Temporarily calculate total equity from balances until full API is ready
        val totalEquity = balances.sumOf { it.available + it.hold }
        
        Portfolio(
            balances = balances,
            totalEquityUsd = totalEquity,
            timestamp = Instant.now()
        )
    }
}
