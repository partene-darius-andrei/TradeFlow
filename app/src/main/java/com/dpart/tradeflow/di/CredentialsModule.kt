package com.dpart.tradeflow.di

import com.dpart.tradeflow.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CredentialsModule {

    @Provides
    @Singleton
    @Named("coinbase_api_key")
    fun provideCoinbaseApiKey(): String = BuildConfig.COINBASE_API_KEY

    @Provides
    @Singleton
    @Named("coinbase_api_secret")
    fun provideCoinbaseApiSecret(): String = BuildConfig.COINBASE_API_SECRET
}
