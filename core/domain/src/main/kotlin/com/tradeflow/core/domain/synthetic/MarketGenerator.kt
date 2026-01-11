package com.tradeflow.core.domain.synthetic

import com.tradeflow.core.domain.model.Candle
import java.math.BigDecimal

/**
 * Interface for synthetic market data generators.
 *
 * Used in backtesting to create realistic price scenarios for strategy validation.
 * Implementations include JumpDiffusionGenerator and StationaryBootstrapGenerator.
 */
interface MarketGenerator {
    /**
     * Generates synthetic candlestick data.
     *
     * @param nSteps Number of candles to generate
     * @param seed Random seed for reproducibility
     * @param noiseLevel Noise multiplier (0.0 = clean data, 1.0 = high noise)
     * @return List of synthetic candles in chronological order
     */
    fun generate(
        nSteps: Int,
        seed: Long,
        noiseLevel: Double = 0.0
    ): List<Candle>

    /**
     * Returns the generator's name for identification.
     * @return Generator name (e.g., "JumpDiffusion", "StationaryBootstrap")
     */
    fun getName(): String
}

/**
 * Configuration for market data generation.
 *
 * @property startPrice Initial price for synthetic data (e.g., 50000 for BTC)
 * @property startTime Beginning timestamp for generated candles
 * @property intervalMinutes Time between candles (240 = 4-hour candles)
 * @property volatilityAnnualized Annualized volatility (0.80 = 80% annual vol)
 * @property drift Annualized expected return (0.10 = 10% annual drift)
 */
data class GenerationConfig(
    val startPrice: BigDecimal = BigDecimal("50000"),
    val startTime: java.time.Instant = java.time.Instant.now(),
    val intervalMinutes: Long = 240,
    val volatilityAnnualized: Double = 0.80,
    val drift: Double = 0.10
)
