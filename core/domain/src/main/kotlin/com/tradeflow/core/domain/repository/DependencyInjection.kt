package com.tradeflow.core.domain.repository

import com.tradeflow.core.domain.config.RiskProfile
import com.tradeflow.core.domain.config.TradingConfig

object DependencyInjection {
    lateinit var exchangeRepository: ExchangeRepository
    var tradingConfig: TradingConfig = TradingConfig.forProfile(RiskProfile.BALANCED)

    fun setRepository(repository: ExchangeRepository): DependencyInjection {
        this.exchangeRepository = repository
        return this
    }

    fun setTradingConfig(config: TradingConfig): DependencyInjection {
        this.tradingConfig = config
        return this
    }
}
