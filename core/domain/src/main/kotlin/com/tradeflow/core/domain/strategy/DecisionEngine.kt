package com.tradeflow.core.domain.strategy

import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import java.math.BigDecimal

interface DecisionEngine {
    fun evaluate(candles: List<Candle>, currentPrice: BigDecimal): Decision
}
