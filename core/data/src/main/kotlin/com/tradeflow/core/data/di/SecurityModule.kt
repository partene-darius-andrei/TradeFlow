package com.tradeflow.core.data.di

import android.content.Context
import com.tradeflow.core.data.security.SecureCredentialStore
import com.tradeflow.core.domain.auth.CredentialStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideCredentialStore(@ApplicationContext context: Context): CredentialStore {
        return SecureCredentialStore(context)
    }
}
