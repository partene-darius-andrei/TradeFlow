package com.tradeflow.core.domain.config

import java.math.BigDecimal

/**
 * Configuration parameters for exchange simulation in backtesting.
 *
 * These parameters control the realistic modeling of exchange behavior:
 * - **Fees:** Trading commissions charged by the exchange
 * - **Slippage:** Price impact when executing market orders
 * - **Funding Rate:** Perpetual futures funding cost (charged every 8 hours)
 *
 * **Usage in Backtesting:**
 * ```kotlin
 * val simParams = ExchangeSimulationParameters(
 *     takerFeeRate = BigDecimal("0.004"),  // 0.4% Coinbase Advanced Trade
 *     slippagePercent = BigDecimal("0.001") // 0.1% slippage
 * )
 * val exchange = SimulatedExchange(
 *     initialUsd = BigDecimal("1000"),
 *     parameters = simParams
 * )
 * ```
 *
 * **Default Values (Coinbase Advanced Trade Tier 1):**
 * - Taker fee: 0.4% (market orders)
 * - Maker fee: 0.25% (limit orders)
 * - Slippage: 0.1% (market impact)
 * - Funding rate: 0.01% per 8 hours (perpetual futures)
 *
 * @property takerFeeRate Fee charged for market orders (taking liquidity).
 *           Example: 0.004 = 0.4% fee on order value.
 *           Coinbase Advanced Trade Tier 1: 0.4%
 *
 * @property makerFeeRate Fee charged for limit orders (providing liquidity).
 *           Example: 0.0025 = 0.25% fee on order value.
 *           Coinbase Advanced Trade Tier 1: 0.25%
 *
 * @property fundingRatePerInterval Funding rate charged per interval for perpetual futures.
 *           Example: 0.0001 = 0.01% charged every 8 hours.
 *           Typical perpetual futures rate: 0.01% per 8H (0.03% daily)
 *
 * @property fundingIntervalHours How often funding rate is charged (hours).
 *           Standard: 8 hours (3 times per day).
 *
 * @property slippagePercent Market impact slippage for market orders.
 *           Example: 0.001 = 0.1% slippage.
 *           - BUY orders: fill at price × (1 + slippage) = pay slightly more
 *           - SELL orders: fill at price × (1 - slippage) = receive slightly less
 *           Typical for liquid BTC markets: 0.05-0.15%
 *
 * @see SimulatedExchange for how these parameters are applied in backtesting
 */
data class ExchangeSimulationParameters(
    val takerFeeRate: BigDecimal = BigDecimal("0.004"),
    val makerFeeRate: BigDecimal = BigDecimal("0.0025"),
    val fundingRatePerInterval: BigDecimal = BigDecimal("0.0001"),
    val fundingIntervalHours: Int = 8,
    val slippagePercent: BigDecimal = BigDecimal("0.001")
)
