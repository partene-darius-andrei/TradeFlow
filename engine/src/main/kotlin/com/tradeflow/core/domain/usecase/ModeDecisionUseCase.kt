package com.tradeflow.core.domain.usecase

import com.tradeflow.core.domain.StrategyConfig
import com.tradeflow.core.domain.model.Indicators

class ModeDecisionUseCase(
    private val config: StrategyConfig = StrategyConfig()
) {

    enum class Mode {
        TREND,
        RANGE
    }

    data class ModeResult(
        val mode: Mode,
        val isConfirmed: Boolean,
        val waitReason: String? = null
    )

    private var lastMode: Mode = Mode.TREND
    private var confirmationCount = 0
    private var candidateMode: Mode? = null

    operator fun invoke(indicators: Indicators): ModeResult {
        // Determine desired mode based on Trend Strength (ADX)
        val desiredMode = when {
            indicators.adx >= config.adxTrendThreshold -> Mode.TREND
            indicators.adx <= config.adxRangeThreshold -> Mode.RANGE
            else -> lastMode
        }

        // Apply Hysteresis (require N consecutive confirmations before switching)
        if (desiredMode == lastMode) {
            // Already in desired mode, reset any pending switch
            candidateMode = null
            confirmationCount = 0
            return ModeResult(mode = lastMode, isConfirmed = true)
        }

        // Mode change is desired, apply confirmation logic
        if (desiredMode != candidateMode) {
            // New candidate mode detected, start fresh confirmation
            candidateMode = desiredMode
            confirmationCount = 1
        } else {
            // Same candidate as before, increment confirmation count
            confirmationCount++
        }

        // Check if we have enough confirmations to switch
        val confirmationRequired = config.confirmationCandles
        if (confirmationCount >= confirmationRequired) {
            // ✅ CONFIRMATION COMPLETE - switch to new mode
            lastMode = desiredMode
            candidateMode = null
            confirmationCount = 0
            return ModeResult(mode = lastMode, isConfirmed = true)
        }

        // Still waiting for confirmation
        return ModeResult(
            mode = lastMode,
            isConfirmed = false,
            waitReason = "Confirming mode switch to $desiredMode ($confirmationCount/$confirmationRequired)"
        )
    }
}
