package com.tradeflow.core.domain.synthetic

import com.tradeflow.core.domain.model.Candle
import java.math.BigDecimal

interface MarketGenerator {
    fun generate(
        nSteps: Int,
        seed: Long,
        noiseLevel: Double = 0.0
    ): List<Candle>

    fun getName(): String
}

data class GenerationConfig(
    val startPrice: BigDecimal = BigDecimal("50000"),
    val startTime: java.time.Instant = java.time.Instant.now(),
    val intervalMinutes: Long = 240,
    val volatilityAnnualized: Double = 0.80,
    val drift: Double = 0.10
)
