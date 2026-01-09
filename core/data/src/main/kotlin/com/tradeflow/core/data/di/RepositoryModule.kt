package com.tradeflow.core.data.di

import com.tradeflow.core.data.repository.TradingDataRepositoryImpl
import com.tradeflow.core.domain.repository.TradingDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTradingDataRepository(
        impl: TradingDataRepositoryImpl
    ): TradingDataRepository
}
