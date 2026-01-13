package com.tradeflow.core.domain.di

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.core.domain.risk.RiskManager
import com.tradeflow.core.domain.risk.TrailingStopManager
import com.tradeflow.core.domain.usecase.AnalyzeCandlesUseCase
import com.tradeflow.core.domain.usecase.ExecuteTradingCycleUseCase
import com.tradeflow.core.domain.usecase.MakeTradingDecisionUseCase
import com.tradeflow.core.domain.usecase.UpdatePortfolioUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin dependency injection modules for TradeFlow domain layer.
 *
 * **STATUS:** Available but not yet integrated (v3.0 POC phase).
 *
 * **Current State:**
 * - Koin dependencies added to build.gradle.kts
 * - Module definitions created (this file)
 * - NOT yet used in production code (still using DependencyInjection object)
 *
 * **Why Not Fully Integrated:**
 * - POC/playground stage - existing DependencyInjection object works fine
 * - Full migration would require updating all use cases + tests
 * - Provides option for future migration without blocking current work
 *
 * **How to Integrate (Future):**
 * 1. In Main.kt or test setup:
 *    ```kotlin
 *    startKoin {
 *        modules(domainModule, repositoryModule)
 *    }
 *    ```
 *
 * 2. Replace DependencyInjection usage with Koin injection:
 *    ```kotlin
 *    // OLD
 *    class ExecuteTradingCycleUseCase(
 *        private val repository: ExchangeRepository = DependencyInjection.exchangeRepository
 *    )
 *
 *    // NEW
 *    class ExecuteTradingCycleUseCase(
 *        private val repository: ExchangeRepository
 *    )
 *    // Koin will inject automatically
 *    ```
 *
 * 3. Get instances via Koin:
 *    ```kotlin
 *    val useCase: ExecuteTradingCycleUseCase = get()
 *    ```
 *
 * **Benefits of Koin (vs current DependencyInjection object):**
 * - Thread-safe by default
 * - Better testability (easy to override dependencies in tests)
 * - No global mutable state
 * - Compile-time safety with Koin DSL
 * - Lifecycle management (singleton, factory, scoped)
 *
 * **Migration Effort:** ~2-3 hours to fully migrate all use cases and tests.
 *
 * @see org.koin.core.context.startKoin for Koin initialization
 * @see org.koin.core.component.get for dependency retrieval
 */

/**
 * Domain layer module containing use cases, services, and managers.
 *
 * **Singletons:**
 * - TradingConfig: Configuration loaded once and reused
 * - AnalyzeCandlesUseCase: Stateless technical analysis
 * - MakeTradingDecisionUseCase: Stateful decision engine (maintains mode between calls)
 * - RiskManager: Stateless risk validation
 * - TrailingStopManager: Stateless trailing stop calculation
 * - UpdatePortfolioUseCase: Stateless portfolio fetching
 *
 * **Factory (new instance each time):**
 * - ExecuteTradingCycleUseCase: Potentially could be singleton, but factory is safer for now
 */
val domainModule = module {
    // Configuration
    single { TradingConfig.forProfile(RiskProfile.BALANCED) }

    // Use Cases
    single { AnalyzeCandlesUseCase() }
    single { MakeTradingDecisionUseCase(taService = get(), config = get()) }
    single { UpdatePortfolioUseCase(repository = get()) }
    factory {
        ExecuteTradingCycleUseCase(
            exchangeRepository = get(),
            makeDecisionUseCase = get(),
            config = get(),
            trailingStopManager = get()
        )
    }

    // Risk Management
    single { RiskManager(config = get()) }
    single { TrailingStopManager(config = get()) }
}

/**
 * Repository module for exchange implementations.
 *
 * **Note:** Repository implementation must be provided externally
 * (e.g., CoinbaseRepository for live trading, SimulatedExchange for backtesting).
 *
 * **Usage:**
 * ```kotlin
 * // In Main.kt (live trading)
 * startKoin {
 *     modules(domainModule, module {
 *         single<ExchangeRepository> { CoinbaseRepository.create() }
 *     })
 * }
 *
 * // In tests (backtesting)
 * startKoin {
 *     modules(domainModule, module {
 *         single<ExchangeRepository> { SimulatedExchange(initialUsd = BigDecimal("1000")) }
 *     })
 * }
 * ```
 */
val repositoryModule = module {
    // Repository must be provided by caller (live or simulated)
    // Example: single<ExchangeRepository> { CoinbaseRepository.create() }
}

/**
 * Complete module list for easy initialization.
 *
 * **Usage:**
 * ```kotlin
 * startKoin {
 *     modules(allModules)
 * }
 * ```
 */
val allModules = listOf(domainModule, repositoryModule)
