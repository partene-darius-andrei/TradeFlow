# Security & Authentication

**Parent:** [../reference.md](../reference.md)

Secure credential storage and JWT token generation for Coinbase API authentication.

---

## Secure Key Storage

### SecureKeyStore.kt

Encrypted storage for Coinbase API credentials using EncryptedSharedPreferences.

```kotlin
// data/security/SecureKeyStore.kt
package com.dpart.tradeflow.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureKeyStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "engine_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(apiKeyId: String, privateKeyPem: String) {
        prefs.edit()
            .putString(KEY_API_ID, apiKeyId)
            .putString(KEY_PRIVATE_PEM, privateKeyPem)
            .apply()
    }

    fun getApiKeyId(): String? = prefs.getString(KEY_API_ID, null)

    fun getPrivateKeyPem(): String? = prefs.getString(KEY_PRIVATE_PEM, null)

    fun hasCredentials(): Boolean =
        !getApiKeyId().isNullOrBlank() && !getPrivateKeyPem().isNullOrBlank()

    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_API_ID = "coinbase_api_key_id"
        private const val KEY_PRIVATE_PEM = "coinbase_private_key_pem"
    }
}
```

---

## JWT Token Generator

### JwtGenerator.kt

Generates JWT tokens with ES256 signing for Coinbase Advanced Trade API authentication.

```kotlin
// data/security/JwtGenerator.kt
package com.dpart.tradeflow.data.security

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

class JwtGenerator(private val keyStore: SecureKeyStore) {

    /**
     * Generate JWT for REST API calls
     * @param method HTTP method (GET, POST, etc.)
     * @param host API host (api.coinbase.com)
     * @param path API path (/api/v3/brokerage/orders)
     */
    fun generateRestToken(method: String, host: String, path: String): String {
        val uri = "$method $host$path"
        return generateToken(uri)
    }

    /**
     * Generate JWT for WebSocket connections
     * WebSocket JWTs don't need URI claim
     */
    fun generateWebSocketToken(): String {
        return generateToken(null)
    }

    private fun generateToken(uri: String?): String {
        val keyId = keyStore.getApiKeyId()
            ?: throw IllegalStateException("API Key ID not configured")
        val privateKeyPem = keyStore.getPrivateKeyPem()
            ?: throw IllegalStateException("Private Key not configured")

        val privateKey = loadEcPrivateKey(privateKeyPem)
        val now = Instant.now()
        val nonce = UUID.randomUUID().toString().replace("-", "")

        val headerBuilder = JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(JOSEObjectType.JWT)
            .keyID(keyId)
            .customParam("nonce", nonce)

        val claimsBuilder = JWTClaimsSet.Builder()
            .issuer("cdp")
            .subject(keyId)
            .notBeforeTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(120)))

        // Only add URI for REST calls
        if (uri != null) {
            claimsBuilder.claim("uri", uri)
        }

        val signedJwt = SignedJWT(headerBuilder.build(), claimsBuilder.build())
        signedJwt.sign(ECDSASigner(privateKey))

        return signedJwt.serialize()
    }

    private fun loadEcPrivateKey(pem: String): ECPrivateKey {
        // Remove PEM headers and whitespace
        val keyContent = pem
            .replace("-----BEGIN EC PRIVATE KEY-----", "")
            .replace("-----END EC PRIVATE KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")

        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")

        return keyFactory.generatePrivate(keySpec) as ECPrivateKey
    }
}
```

---

## Key Implementation Details

### Encryption at Rest

**EncryptedSharedPreferences:**
- Uses AES256-GCM for value encryption
- Uses AES256-SIV for key encryption
- Master key stored in Android Keystore (hardware-backed if available)
- Auto-generates keys on first use

**Security guarantees:**
- API keys never stored in plaintext
- Keys inaccessible without device unlock
- Protected against file-based attacks

### JWT Token Structure

**Header:**
```json
{
  "alg": "ES256",
  "typ": "JWT",
  "kid": "organizations/{org_id}/apiKeys/{key_id}",
  "nonce": "random_hex_32_chars"
}
```

**Payload (REST):**
```json
{
  "iss": "cdp",
  "sub": "organizations/{org_id}/apiKeys/{key_id}",
  "nbf": 1704067200,
  "exp": 1704067320,
  "uri": "POST api.coinbase.com/api/v3/brokerage/orders"
}
```

**Payload (WebSocket):**
```json
{
  "iss": "cdp",
  "sub": "organizations/{org_id}/apiKeys/{key_id}",
  "nbf": 1704067200,
  "exp": 1704067320
}
```

### Token Expiry

- Tokens expire in **120 seconds** (2 minutes)
- Generate fresh token for each API request
- For WebSocket: Regenerate before 2-minute expiry
- Nonce prevents token replay attacks

---

## Usage Examples

### Saving Credentials (Settings Screen)

```kotlin
val keyStore = SecureKeyStore(context)

// Save API credentials from user input
keyStore.saveCredentials(
    apiKeyId = "organizations/abc123/apiKeys/def456",
    privateKeyPem = """
        -----BEGIN EC PRIVATE KEY-----
        MHcCAQEEIB...
        -----END EC PRIVATE KEY-----
    """.trimIndent()
)

// Check if configured
if (keyStore.hasCredentials()) {
    // Ready to start trading service
}
```

### Generating JWT for REST API

```kotlin
val jwtGenerator = JwtGenerator(keyStore)

// For POST /api/v3/brokerage/orders
val token = jwtGenerator.generateRestToken(
    method = "POST",
    host = "api.coinbase.com",
    path = "/api/v3/brokerage/orders"
)

// Use in Authorization header
val headers = mapOf("Authorization" to "Bearer $token")
```

### Generating JWT for WebSocket

```kotlin
val jwtGenerator = JwtGenerator(keyStore)

// For WebSocket authentication
val token = jwtGenerator.generateWebSocketToken()

// Send in subscribe message
val subscribeMessage = buildJsonObject {
    put("type", "subscribe")
    put("channel", "user")
    putJsonArray("product_ids") { add("BTC-USD") }
    put("jwt", token)
}
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun `secureKeyStore saves and retrieves credentials`() {
    val keyStore = SecureKeyStore(context)

    keyStore.saveCredentials(
        apiKeyId = "test_key_id",
        privateKeyPem = "test_private_key"
    )

    assertEquals("test_key_id", keyStore.getApiKeyId())
    assertEquals("test_private_key", keyStore.getPrivateKeyPem())
    assertTrue(keyStore.hasCredentials())
}

@Test
fun `jwtGenerator produces valid token structure`() {
    val jwtGenerator = JwtGenerator(keyStore)

    val token = jwtGenerator.generateRestToken(
        method = "GET",
        host = "api.coinbase.com",
        path = "/api/v3/brokerage/accounts"
    )

    // Parse token
    val jwt = SignedJWT.parse(token)

    assertEquals("ES256", jwt.header.algorithm.name)
    assertEquals("cdp", jwt.jwtClaimsSet.issuer)
    assertTrue(jwt.jwtClaimsSet.expirationTime.time > System.currentTimeMillis())
}
```

---

## Security Considerations

### API Key Permissions

When creating Coinbase API keys, use **minimal permissions**:
- ✅ View accounts
- ✅ Trade
- ❌ Transfer funds
- ❌ Withdraw

### Private Key Protection

**DO:**
- Store only in EncryptedSharedPreferences
- Clear from memory after JWT generation
- Log out user on device compromise

**DON'T:**
- Log private keys
- Store in regular SharedPreferences
- Transmit over unencrypted channels
- Share across devices

### Token Handling

**DO:**
- Generate fresh token per request
- Use short expiry (2 minutes)
- Include nonce for replay protection

**DON'T:**
- Reuse tokens beyond expiry
- Log tokens in production
- Cache tokens on disk

---

## Navigation

- **[Back to Technical Reference](../reference.md)** - Parent document
- **[Previous: Core Domain](domain.md)** - Domain models and engine
- **[Next: API Clients](clients.md)** - REST and WebSocket implementation
- **[See Also: Coinbase API](../api/coinbase.md)** - Authentication specification
