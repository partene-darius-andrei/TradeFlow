package com.tradeflow.core.domain.model

import java.math.BigDecimal

sealed class Decision {
    data class Wait(val reason: String) : Decision()

    data class Defense(
        val reason: String,
        val currentPrice: BigDecimal,
        val sma200: BigDecimal
    ) : Decision() {
        init {
            require(currentPrice > BigDecimal.ZERO) { "Current price must be positive: $currentPrice" }
            require(sma200 > BigDecimal.ZERO) { "SMA200 must be positive: $sma200" }
        }
    }

    data class Trend(
        val direction: OrderSide,
        val entryPrice: BigDecimal,
        val stopLoss: BigDecimal,
        val takeProfit: BigDecimal,
        val positionSizePercent: BigDecimal,
        val adx: Double,
        val atr: BigDecimal
    ) : Decision() {
        init {
            require(entryPrice > BigDecimal.ZERO) { "Entry price must be positive: $entryPrice" }
            require(atr > BigDecimal.ZERO) { "ATR must be positive: $atr" }
            require(positionSizePercent > BigDecimal.ZERO && positionSizePercent <= BigDecimal.ONE) {
                "Position size must be between 0 and 1: $positionSizePercent"
            }

            when (direction) {
                OrderSide.BUY -> {
                    require(stopLoss < entryPrice) {
                        "For LONG: stopLoss ($stopLoss) must be < entryPrice ($entryPrice)"
                    }
                    require(takeProfit > entryPrice) {
                        "For LONG: takeProfit ($takeProfit) must be > entryPrice ($entryPrice)"
                    }
                }
                OrderSide.SELL -> {
                    require(stopLoss > entryPrice) {
                        "For SHORT: stopLoss ($stopLoss) must be > entryPrice ($entryPrice)"
                    }
                    require(takeProfit < entryPrice) {
                        "For SHORT: takeProfit ($takeProfit) must be < entryPrice ($entryPrice)"
                    }
                }
            }
        }
    }

    data class Range(
        val gridSpacing: BigDecimal,
        val levels: Int,
        val positionSizePercentPerLevel: BigDecimal,
        val adx: Double,
        val atr: BigDecimal
    ) : Decision() {
        init {
            require(gridSpacing > BigDecimal.ZERO) { "Grid spacing must be positive: $gridSpacing" }
            require(levels > 0) { "Levels must be positive: $levels" }
            require(positionSizePercentPerLevel > BigDecimal.ZERO && positionSizePercentPerLevel <= BigDecimal.ONE) {
                "Position size per level must be between 0 and 1: $positionSizePercentPerLevel"
            }
            require(atr > BigDecimal.ZERO) { "ATR must be positive: $atr" }
        }
    }
}
