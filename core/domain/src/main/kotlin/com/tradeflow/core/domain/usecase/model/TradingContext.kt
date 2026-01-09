package com.tradeflow.core.domain.usecase.model

import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Portfolio
import java.math.BigDecimal

data class TradingContext(
    val productId: String,
    val candles: List<Candle>,
    val currentPrice: BigDecimal,
    val portfolio: Portfolio,
    val highWaterMark: BigDecimal
)
