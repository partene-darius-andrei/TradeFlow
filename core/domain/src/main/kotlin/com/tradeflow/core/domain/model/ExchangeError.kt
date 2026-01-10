package com.tradeflow.core.domain.model

import java.math.BigDecimal

/**
 * Sealed hierarchy of exchange-related errors for type-safe error handling.
 *
 * **Purpose:** Categorizes exchange API errors into specific types for targeted handling.
 * Extends Exception to support throwing and catching.
 *
 * **Six Error Types:**
 * 1. **AuthenticationFailed:** Invalid API credentials
 * 2. **RateLimited:** Too many requests, need to wait
 * 3. **InsufficientFunds:** Not enough balance for order
 * 4. **OrderRejected:** Exchange rejected order (invalid params, etc.)
 * 5. **NetworkError:** HTTP/network failure
 * 6. **Unknown:** Unclassified error
 *
 * **Why Sealed Class:**
 * - Compiler enforces exhaustive when() handling
 * - Prevents missing error cases
 * - Type-safe error inspection (no string matching)
 *
 * **Usage Pattern:**
 * ```kotlin
 * val result = exchangeRepository.placeLimitOrder(...)
 * result.fold(
 *     onSuccess = { order -> log.info("Order placed: ${order.id}") },
 *     onFailure = { error ->
 *         when (error) {
 *             is ExchangeError.RateLimited -> {
 *                 log.warn("Rate limited, wait ${error.retryAfterSeconds}s")
 *                 delay(error.retryAfterSeconds * 1000L)
 *                 retry()
 *             }
 *             is ExchangeError.InsufficientFunds -> {
 *                 log.error("Insufficient funds: need ${error.required}, have ${error.available}")
 *                 // Don't retry, insufficient balance
 *             }
 *             is ExchangeError.AuthenticationFailed -> {
 *                 log.error("Auth failed: ${error.message}")
 *                 // Critical: check API credentials
 *             }
 *             is ExchangeError.NetworkError -> {
 *                 log.warn("Network error: ${error.cause}")
 *                 // Retry with exponential backoff
 *             }
 *             is ExchangeError.OrderRejected -> {
 *                 log.warn("Order rejected: ${error.reason}")
 *                 // Check order parameters
 *             }
 *             is ExchangeError.Unknown -> {
 *                 log.error("Unknown error: ${error.message}")
 *             }
 *         }
 *     }
 * )
 * ```
 *
 * **Error Recovery Strategies:**
 * - **RateLimited:** Wait and retry
 * - **NetworkError:** Retry with backoff
 * - **InsufficientFunds:** Don't retry, fix balance
 * - **OrderRejected:** Check params, potentially retry
 * - **AuthenticationFailed:** Critical, check credentials
 * - **Unknown:** Log and alert, manual investigation
 *
 * @see ExchangeRepository for methods that return Result<T, ExchangeError>
 */
sealed class ExchangeError : Exception() {
    /**
     * Authentication failed - invalid API credentials.
     *
     * **Causes:**
     * - Invalid API key
     * - Invalid secret key
     * - Expired JWT token
     * - Incorrect signature
     *
     * **Recovery:**
     * - Verify API key and secret are correct
     * - Check JWT generation logic
     * - Don't retry automatically (credentials won't fix themselves)
     *
     * **Example:**
     * ```
     * message: "Invalid signature"
     * message: "API key not found"
     * ```
     *
     * @property message Human-readable error description from exchange.
     */
    data class AuthenticationFailed(override val message: String) : ExchangeError()

    /**
     * Rate limit exceeded - too many requests to exchange API.
     *
     * **Causes:**
     * - Exceeded requests per second limit
     * - Exceeded requests per minute limit
     * - Burst of rapid requests
     *
     * **Recovery:**
     * - Wait for `retryAfterSeconds` before next request
     * - Implement exponential backoff
     * - Reduce request frequency
     *
     * **Typical Limits (Coinbase):**
     * - 10 requests/second per API key
     * - Burst allowance of 100 requests
     *
     * **Example:**
     * ```
     * retryAfterSeconds: 60 (wait 1 minute)
     * ```
     *
     * @property retryAfterSeconds Seconds to wait before retrying.
     *           Exchange may specify this, or we calculate based on rate limit rules.
     */
    data class RateLimited(val retryAfterSeconds: Int) : ExchangeError()

    /**
     * Insufficient funds - account balance too low for order.
     *
     * **Causes:**
     * - Order size exceeds available balance
     * - Funds locked in other orders
     * - Minimum order size not met
     *
     * **Recovery:**
     * - Don't retry (balance won't increase automatically)
     * - Reduce order size
     * - Cancel other orders to free up funds
     * - Wait for existing orders to fill
     *
     * **Example:**
     * ```
     * required: $100.00
     * available: $50.00
     * message: "Order requires $100 but only $50 available"
     * ```
     *
     * @property required Amount needed for the order (USD or BTC).
     * @property available Amount currently available (USD or BTC).
     */
    data class InsufficientFunds(val required: BigDecimal, val available: BigDecimal) : ExchangeError()

    /**
     * Order rejected by exchange - invalid parameters or conditions.
     *
     * **Common Reasons:**
     * - Price too far from market (limit order outside tolerance)
     * - Size below minimum threshold (dust order)
     * - Invalid order type for product
     * - Market closed or halted
     * - Post-only order would execute immediately
     *
     * **Recovery:**
     * - Check and fix order parameters
     * - Verify product is tradeable
     * - May retry with corrected params
     *
     * **Example Reasons:**
     * ```
     * "Price outside of +/- 5% from last trade"
     * "Size below minimum: 0.00001 BTC"
     * "Post-only order would execute immediately"
     * ```
     *
     * @property reason Human-readable rejection reason from exchange.
     */
    data class OrderRejected(val reason: String) : ExchangeError()

    /**
     * Network error - HTTP request failed.
     *
     * **Causes:**
     * - Timeout
     * - Connection refused
     * - DNS resolution failure
     * - TLS/SSL error
     * - Exchange server error (500)
     *
     * **Recovery:**
     * - Retry with exponential backoff
     * - Check network connectivity
     * - Verify exchange API endpoint is reachable
     *
     * **Example Causes:**
     * ```
     * SocketTimeoutException
     * ConnectException: Connection refused
     * UnknownHostException: api.exchange.com
     * ```
     *
     * @property cause Underlying throwable that caused the network error.
     */
    data class NetworkError(override val cause: Throwable) : ExchangeError()

    /**
     * Unknown error - unclassified exchange error.
     *
     * **Causes:**
     * - New error type from exchange not yet categorized
     * - Unexpected response format
     * - Exchange internal error with unclear cause
     *
     * **Recovery:**
     * - Log full error details for investigation
     * - May retry cautiously
     * - Alert monitoring system
     * - Consider adding new specific error type if recurring
     *
     * **Example:**
     * ```
     * message: "Internal server error"
     * message: "Service unavailable"
     * ```
     *
     * @property message Human-readable error description.
     */
    data class Unknown(override val message: String) : ExchangeError()
}
