package com.tradeflow.core.domain.model

import java.math.BigDecimal

sealed class ExchangeError : Exception() {
    data class AuthenticationFailed(override val message: String) : ExchangeError()
    data class RateLimited(val retryAfterSeconds: Int) : ExchangeError()
    data class InsufficientFunds(val required: BigDecimal, val available: BigDecimal) : ExchangeError()
    data class OrderRejected(val reason: String) : ExchangeError()
    data class NetworkError(override val cause: Throwable) : ExchangeError()
    data class Unknown(override val message: String) : ExchangeError()
}
