package com.tradeflow.standalone

import com.tradeflow.core.domain.di.domainModule
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.usecase.UpdatePortfolioUseCase
import com.tradeflow.exchange.coinbase.repository.CoinbaseRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object TradeFlowApp : KoinComponent

fun main() = runBlocking {
    println("=".repeat(60))
    println("TradeFlow - Standalone Trading Bot")
    println("=".repeat(60))
    println()

    val repository = CoinbaseRepository()

    // Initialize Koin with domain module + repository
    startKoin {
        modules(
            domainModule,
            module {
                single<ExchangeRepository> { repository }
            }
        )
    }

    println("✓ Dependencies initialized (Koin)")
    println()

    println("-".repeat(60))
    println("Fetching portfolio...")
    println("-".repeat(60))
    println()

    val updatePortfolio: UpdatePortfolioUseCase = TradeFlowApp.get()

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

    repository.close()
    println()
    println("✓ HTTP client closed")

    stopKoin()

    println()
    println("=".repeat(60))
    println("Done!")
    println("=".repeat(60))
}
