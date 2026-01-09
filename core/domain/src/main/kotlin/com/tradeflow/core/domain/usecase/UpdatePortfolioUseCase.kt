package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Portfolio
import com.tradeflow.core.domain.repository.ExchangeRepository
import javax.inject.Inject

class UpdatePortfolioUseCase @Inject constructor(
    private val repository: ExchangeRepository
) {
    suspend fun execute(): Result<Portfolio> = repository.getPortfolio()
}
