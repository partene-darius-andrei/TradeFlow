package com.tradeflow.exchange.coinbase.di

import com.tradeflow.core.domain.auth.AuthTokenProvider
import com.tradeflow.core.domain.repository.BracketOrderRepository
import com.tradeflow.core.domain.repository.ExchangeRepository
import com.tradeflow.exchange.coinbase.api.CoinbaseApiClient
import com.tradeflow.exchange.coinbase.repository.CoinbaseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExchangeModule {

    @Provides
    @Singleton
    fun provideCoinbaseApiClient(
        httpClient: HttpClient,
        authProvider: AuthTokenProvider
    ): CoinbaseApiClient = CoinbaseApiClient(httpClient, authProvider)

    @Provides
    @Singleton
    fun provideCoinbaseRepository(
        apiClient: CoinbaseApiClient
    ): CoinbaseRepository = CoinbaseRepository(apiClient)

    @Provides
    @Singleton
    fun provideExchangeRepository(
        repository: CoinbaseRepository
    ): ExchangeRepository = repository

    @Provides
    @Singleton
    fun provideBracketOrderRepository(
        repository: CoinbaseRepository
    ): BracketOrderRepository = repository
}
