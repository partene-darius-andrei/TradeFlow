package com.tradeflow.standalone

import com.tradeflow.core.domain.repository.DependencyInjection
import com.tradeflow.core.domain.usecase.UpdatePortfolioUseCase
import com.tradeflow.exchange.coinbase.repository.CoinbaseRepository
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("=".repeat(60))
    println("TradeFlow - Standalone Trading Bot")
    println("=".repeat(60))
    println()

    val repository = CoinbaseRepository.create()
    DependencyInjection.setRepository(repository)

    println("✓ Dependencies initialized")
    println()

    println("-".repeat(60))
    println("Fetching portfolio...")
    println("-".repeat(60))
    println()

    val updatePortfolio = UpdatePortfolioUseCase()

    runCatching {
        updatePortfolio.execute().getOrThrow()
    }.onSuccess { portfolio ->
        println("✅ SUCCESS - Portfolio updated")
        println()
        println("Total Equity: \$${portfolio.totalEquityUsd}")
        println("Timestamp: ${portfolio.timestamp}")
        println()
        println("Balances:")
        portfolio.balances.forEach { balance ->
            if (balance.total.signum() > 0) {
                println("  ${balance.currency}: ${balance.total} (available: ${balance.available}, hold: ${balance.hold})")
            }
        }
    }.onFailure { e ->
        println("❌ FAILURE")
        println()
        println("Error: ${e.message}")
        e.printStackTrace()
    }

    (DependencyInjection.exchangeRepository as? CoinbaseRepository)?.close()
    println()
    println("✓ HTTP client closed")

    println()
    println("=".repeat(60))
    println("Done!")
    println("=".repeat(60))
}
