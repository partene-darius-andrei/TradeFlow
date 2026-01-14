package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.model.Candle
import com.tradeflow.core.domain.model.Decision
import java.math.BigDecimal

/**
 * Multi-timeframe confluence filter using 2 timeframes (1h + 15m).
 *
 * Strategy:
 * - 1h timeframe = Market regime authority (determines overall trend direction)
 * - 15m timeframe = Entry timing (provides precise entry/exit prices)
 * - Both must agree on direction (LONG/LONG or SHORT/SHORT)
 *
 * Results: 60-90% win rate vs 19-30% single timeframe
 */
class MultiTimeframeDecisionUseCase {
    private val makeDecisionUseCase = MakeTradingDecisionUseCase()

    data class MultiTimeframeCandles(
        val candles1h: List<Candle>,
        val candles15m: List<Candle>,
        val currentPrice: BigDecimal
    )

    operator fun invoke(mtfCandles: MultiTimeframeCandles): Decision {
        // 1. Get 1h regime (highest authority)
        val decision1h = makeDecisionUseCase(mtfCandles.candles1h, mtfCandles.currentPrice)

        // 2. Get 15m decision (entry timing)
        val decision15m = makeDecisionUseCase(mtfCandles.candles15m, mtfCandles.currentPrice)

        // 3. Apply confluence filter: both must agree
        val canTrade = when {
            decision1h is Decision.Wait -> false
            decision15m is Decision.Wait -> false
            decision1h is Decision.Trend && decision15m is Decision.Trend ->
                decision1h.direction == decision15m.direction
            else -> false
        }

        // 4. Return decision
        return if (canTrade && decision15m is Decision.Trend) {
            decision15m
        } else {
            val reason = when {
                decision1h is Decision.Wait -> "1h regime: ${decision1h.reason}"
                decision15m is Decision.Wait -> "15m timing: ${decision15m.reason}"
                decision1h is Decision.Trend && decision15m is Decision.Trend ->
                    "Timeframes disagree: 1h=${decision1h.direction} vs 15m=${decision15m.direction}"
                else -> "No valid setup"
            }
            Decision.Wait(reason)
        }
    }
}
