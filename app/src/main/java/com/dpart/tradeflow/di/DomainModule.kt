package com.dpart.tradeflow.di

import com.tradeflow.core.domain.strategy.DecisionEngine
import com.tradeflow.core.domain.strategy.TradingDecisionEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    @Singleton
    abstract fun bindDecisionEngine(
        impl: TradingDecisionEngine
    ): DecisionEngine
}
