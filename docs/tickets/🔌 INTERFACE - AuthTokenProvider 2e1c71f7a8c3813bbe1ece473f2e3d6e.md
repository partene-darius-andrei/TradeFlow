# 🔌 INTERFACE - AuthTokenProvider

Effort level: Small
Priority: High
Status: Not started

## Objective

Abstract authentication token generation for different exchanges.

## File

`domain/repository/AuthTokenProvider.kt`

## Interface Definition

```kotlin
interface AuthTokenProvider {
    /**
     * Generate auth token for REST API request
     * @param method HTTP method (GET, POST, DELETE)
     * @param path Request path (e.g., /api/v3/brokerage/orders)
     * @param body Request body for signing (optional)
     */
    suspend fun generateRestToken(
        method: String,
        path: String,
        body: String? = null
    ): String
    
    /**
     * Generate auth token for WebSocket subscription
     */
    suspend fun generateWebSocketToken(): String
    
    /**
     * Invalidate cached tokens (call on auth failure)
     */
    fun invalidateToken()
    
    /**
     * Check if credentials are configured
     */
    fun hasCredentials(): Boolean
}
```

## Implementation Notes

- Coinbase: JWT with ES256 signing
- Kraken: HMAC-SHA512 with nonce
- Binance: HMAC-SHA256 with timestamp

Each implementation handles its own algorithm.

## Acceptance Criteria

- [ ]  Interface defined
- [ ]  No algorithm-specific types exposed
- [ ]  Supports both REST and WebSocket auth
- [ ]  Credential check method included