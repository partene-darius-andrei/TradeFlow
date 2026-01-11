package com.tradeflow.core.domain.model

/**
 * Interface for generating authentication tokens for exchange API requests.
 *
 * **Purpose:** Abstracts the complex JWT token generation logic required by exchanges
 * (like Coinbase) for REST API and WebSocket authentication.
 *
 * **Why an Interface:**
 * - Domain layer defines WHAT is needed (tokens)
 * - Data layer (coinbase module) defines HOW to generate them (JWT algorithm)
 * - Allows easy mocking for tests
 * - Decouples domain from crypto/signature details
 *
 * **Token Types:**
 * 1. **REST API Tokens:** Per-request tokens that include method, path, timestamp
 * 2. **WebSocket Tokens:** Long-lived tokens for real-time feed connection
 *
 * **Implementation:**
 * Implemented by `JwtRepository` in the exchange:coinbase module.
 * Uses ES256 (ECDSA with SHA-256) algorithm per Coinbase CDP specification.
 *
 * **Token Generation Process (REST):**
 * 1. Get API key and secret from CredentialStore
 * 2. Create JWT header: `{"alg": "ES256", "kid": "<api-key>", "typ": "JWT"}`
 * 3. Create JWT payload: `{"sub": "<api-key>", "iss": "coinbase-cloud", "nbf": <timestamp>, "exp": <timestamp+120s>, "uri": "<method> <path>"}`
 * 4. Sign JWT with private key (secret)
 * 5. Return signed JWT token
 *
 * **Token Validity:**
 * - REST tokens: Valid for 120 seconds (2 minutes)
 * - WebSocket tokens: Valid for longer period (implementation-specific)
 * - Tokens are NOT cached - generated fresh for each request
 *
 * **Security:**
 * - Tokens are short-lived to limit exposure if intercepted
 * - Private key (secret) never leaves the application
 * - Each request gets unique token with request-specific claims
 *
 * **Example Usage (REST):**
 * ```kotlin
 * val token = authTokenProvider.getToken("GET", "/api/v3/brokerage/accounts")
 * val response = httpClient.get("https://api.coinbase.com/api/v3/brokerage/accounts") {
 *     header("Authorization", "Bearer $token")
 * }
 * ```
 *
 * **Example Usage (WebSocket):**
 * ```kotlin
 * val wsToken = authTokenProvider.getWebSocketToken()
 * webSocketClient.connect("wss://advanced-trade-ws.coinbase.com") {
 *     send("""{"type": "subscribe", "token": "$wsToken", ...}""")
 * }
 * ```
 *
 * @see JwtRepository for the Coinbase-specific implementation
 * @see CredentialStore for how API keys and secrets are retrieved
 */
interface AuthTokenProvider {
    /**
     * Generates a JWT authentication token for a REST API request.
     *
     * Creates a request-specific JWT token that includes the HTTP method and path
     * in the claims. This token is valid for 120 seconds from generation.
     *
     * **Token Claims:**
     * - `sub`: API key (subject)
     * - `iss`: "coinbase-cloud" (issuer)
     * - `nbf`: Current timestamp (not before)
     * - `exp`: Current timestamp + 120s (expiration)
     * - `uri`: "{method} {path}" (e.g., "GET /api/v3/brokerage/accounts")
     *
     * **Usage:**
     * ```kotlin
     * val token = getToken("POST", "/api/v3/brokerage/orders")
     * // Use token in Authorization: Bearer header
     * ```
     *
     * @param method HTTP method (e.g., "GET", "POST", "DELETE").
     *               Included in JWT `uri` claim for request validation.
     *
     * @param path API endpoint path (e.g., "/api/v3/brokerage/accounts").
     *             Included in JWT `uri` claim for request validation.
     *
     * @return Signed JWT token string ready for use in Authorization header.
     *         Format: "eyJ0eXAiOiJKV1QiLCJhbGc..." (base64-encoded JWT)
     *
     * @throws IllegalStateException if API credentials are not configured
     * @throws Exception if token generation fails (crypto error, etc.)
     */
    suspend fun getToken(method: String, path: String): String
}
