package com.tradeflow.core.domain.model

/**
 * Interface for securely storing and retrieving exchange API credentials.
 *
 * **Purpose:** Abstracts credential storage mechanism (encrypted preferences, keystore,
 * environment variables, etc.) from the domain layer.
 *
 * **Why an Interface:**
 * - Domain layer defines WHAT is needed (credentials)
 * - Data layer defines WHERE they're stored (implementation detail)
 * - Allows easy swapping of storage mechanisms
 * - Enables mocking for tests (hardcoded test credentials)
 *
 * **Credential Types:**
 * 1. **API Key:** Public identifier for your exchange account
 * 2. **Secret:** Private key for signing requests (NEVER expose this!)
 *
 * **Security Considerations:**
 * - **Production:** Credentials should be stored in secure Android Keystore or encrypted
 * - **Development:** Can use environment variables or config files (NOT committed to git!)
 * - **Testing:** Use hardcoded test credentials for simulated exchange
 *
 * **Current Implementation:**
 * - `StaticCredentialStore` in core:data module
 * - **WARNING:** Hardcoded credentials (OK for testing, UNACCEPTABLE for production!)
 * - TODO: Replace with secure Android Keystore implementation
 *
 * **How Credentials Flow:**
 * ```
 * CredentialStore.getApiKey() + getSecret()
 *   ↓
 * AuthTokenProvider (CoinbaseJwtGenerator)
 *   ↓
 * Signed JWT Token
 *   ↓
 * HTTP Request Authorization Header
 *   ↓
 * Exchange API
 * ```
 *
 * **Example Usage:**
 * ```kotlin
 * val apiKey = credentialStore.getApiKey() ?: throw IllegalStateException("API key not configured")
 * val secret = credentialStore.getSecret() ?: throw IllegalStateException("Secret not configured")
 * val jwt = generateJwt(apiKey, secret, method, path)
 * ```
 *
 * **Production TODO:**
 * Replace StaticCredentialStore with:
 * ```kotlin
 * class SecureCredentialStore @Inject constructor(
 *     private val keystore: AndroidKeyStore
 * ) : CredentialStore {
 *     override suspend fun getApiKey(): String? =
 *         keystore.getEncrypted("coinbase_api_key")
 *
 *     override suspend fun getSecret(): String? =
 *         keystore.getEncrypted("coinbase_secret")
 * }
 * ```
 *
 * **Test Implementation:**
 * ```kotlin
 * class FakeCredentialStore : CredentialStore {
 *     override suspend fun getApiKey() = "test-api-key"
 *     override suspend fun getSecret() = "test-secret"
 * }
 * ```
 *
 * @see AuthTokenProvider for how credentials are used to generate tokens
 * @see StaticCredentialStore for the current (insecure) implementation
 */
interface CredentialStore {
    /**
     * Retrieves the exchange API key.
     *
     * The API key is a public identifier (like a username) that identifies your
     * exchange account. It's included in JWT tokens and HTTP headers.
     *
     * **Security:**
     * - Not as sensitive as the secret (can be exposed in logs)
     * - But still should not be committed to source control
     * - Safe to include in network requests (sent in JWT payload)
     *
     * **Example API Key Format (Coinbase):**
     * ```
     * organizations/abc123-def456-ghi789/apiKeys/key-uuid
     * ```
     *
     * @return API key string, or null if not configured.
     *         Null indicates credentials have not been set up yet.
     *
     * @throws Exception if credential retrieval fails (decryption error, etc.)
     */
    suspend fun getApiKey(): String?

    /**
     * Retrieves the exchange API secret (private key).
     *
     * The secret is a private key used to cryptographically sign requests.
     * **NEVER log this, expose it in UI, or commit it to source control!**
     *
     * **Security:**
     * - **EXTREMELY SENSITIVE** - gives full account access
     * - Used to sign JWT tokens but NEVER sent to the exchange
     * - Should be stored encrypted (Android Keystore in production)
     * - Treat like a password - never expose anywhere
     *
     * **Example Secret Format (Coinbase):**
     * ```
     * -----BEGIN EC PRIVATE KEY-----
     * MHcCAQEEIBc7a... (base64-encoded ECDSA private key)
     * -----END EC PRIVATE KEY-----
     * ```
     *
     * @return Secret key string (PEM-formatted private key), or null if not configured.
     *         Null indicates credentials have not been set up yet.
     *
     * @throws Exception if credential retrieval fails (decryption error, etc.)
     */
    suspend fun getSecret(): String?
}
