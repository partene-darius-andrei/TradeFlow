package com.tradeflow.core.domain.usecase.model

sealed class ExecutionResult {
    data class Success(val message: String) : ExecutionResult()
    data class Skipped(val reason: String) : ExecutionResult()
    data class Failed(val error: String) : ExecutionResult()
}
