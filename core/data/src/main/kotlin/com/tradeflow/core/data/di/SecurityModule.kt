package com.tradeflow.core.data.di

import com.tradeflow.core.data.security.StaticCredentialStore
import com.tradeflow.core.domain.model.CredentialStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideCredentialStore(
        @Named("coinbase_api_key") apiKey: String,
        @Named("coinbase_api_secret") apiSecret: String
    ): CredentialStore {
        return StaticCredentialStore(apiKey, apiSecret)
    }
}
