package com.dpart.tradeflow.di

import com.tradeflow.core.domain.config.AdaptiveOptimizer
import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigurationModule {

    @Provides
    @Singleton
    fun provideTradingConfig(): TradingConfig {
        return TradingConfig.forProfile(RiskProfile.BALANCED)
    }

    @Provides
    @Singleton
    fun provideAdaptiveOptimizer(): AdaptiveOptimizer {
        return AdaptiveOptimizer()
    }
}
