package com.tradeflow.exchange.coinbase.di

import com.tradeflow.core.domain.auth.AuthTokenProvider
import com.tradeflow.exchange.coinbase.auth.CoinbaseJwtGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(
        coinbaseJwtGenerator: CoinbaseJwtGenerator
    ): AuthTokenProvider
}
