package com.tradeflow.core.data.di

import android.content.Context
import androidx.room.Room
import com.tradeflow.core.data.local.dao.*
import com.tradeflow.core.data.local.database.EngineDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EngineDatabase {
        return Room.databaseBuilder(
            context,
            EngineDatabase::class.java,
            "tradeflow_engine_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideCandleDao(database: EngineDatabase): CandleDao = database.candleDao()

    @Provides
    @Singleton
    fun provideOrderDao(database: EngineDatabase): OrderDao = database.orderDao()

    @Provides
    @Singleton
    fun providePortfolioDao(database: EngineDatabase): PortfolioDao = database.portfolioDao()

    @Provides
    @Singleton
    fun provideDecisionDao(database: EngineDatabase): DecisionDao = database.decisionDao()
}
