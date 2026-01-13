package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.config.TradingConfig
import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import com.tradeflow.core.domain.model.OrderSide
import java.math.BigDecimal

class MultiTimeframeDecisionEngine(
    private val config: TradingConfig
) {
    private val taService = AnalyzeCandlesUseCase()

    private val engine1h = MakeTradingDecisionUseCase(taService, config)
    private val engine15m = MakeTradingDecisionUseCase(taService, config)
    private val engine5m = MakeTradingDecisionUseCase(taService, config)
    private val engine1m = MakeTradingDecisionUseCase(taService, config)

    data class MultiTimeframeCandles(
        val candles1h: List<Candle>,
        val candles15m: List<Candle>,
        val candles5m: List<Candle>,
        val candles1m: List<Candle>,
        val currentPrice: BigDecimal
    )

    fun execute(mtfCandles: MultiTimeframeCandles): Decision {
        val regime1h = engine1h.execute(mtfCandles.candles1h, mtfCandles.currentPrice)
        val decision15m = engine15m.execute(mtfCandles.candles15m, mtfCandles.currentPrice)
        val entry5m = engine5m.execute(mtfCandles.candles5m, mtfCandles.currentPrice)
        val momentum1m = engine1m.execute(mtfCandles.candles1m, mtfCandles.currentPrice)

        if (regime1h is Decision.Wait) {
            return Decision.Wait("1h regime: ${regime1h.reason}")
        }

        if (!confirms(regime1h, decision15m)) {
            return Decision.Wait("15m conflicts with 1h regime")
        }

        if (entry5m is Decision.Wait) {
            return Decision.Wait("5m says wait")
        }

        if (strongConflict(regime1h, momentum1m)) {
            return Decision.Wait("1m momentum conflicts with 1h trend")
        }

        return entry5m
    }

    private fun confirms(higher: Decision, lower: Decision): Boolean {
        return when {
            higher is Decision.Wait -> false
            lower is Decision.Wait -> false

            higher is Decision.Trend && lower is Decision.Trend -> {
                higher.direction == lower.direction
            }

            higher is Decision.Range && lower is Decision.Range -> true

            higher is Decision.Trend && lower is Decision.Range -> false
            higher is Decision.Range && lower is Decision.Trend -> false

            else -> false
        }
    }

    private fun strongConflict(regime: Decision, momentum: Decision): Boolean {
        if (regime !is Decision.Trend || momentum !is Decision.Trend) {
            return false
        }

        return regime.direction != momentum.direction
    }
}
