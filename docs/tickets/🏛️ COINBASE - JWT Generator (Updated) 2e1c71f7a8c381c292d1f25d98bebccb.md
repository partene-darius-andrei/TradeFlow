# 🏛️ COINBASE - JWT Generator (Updated)

Effort level: Medium
Priority: High
Status: Not started

## Objective

Generate JWT tokens for Coinbase API authentication. Implements `AuthTokenProvider` interface.

## File

`data/exchange/coinbase/CoinbaseJwtGenerator.kt`

## ✅ VALIDATED Requirements

- Algorithm: **ES256** (ECDSA P-256) — or Ed25519 for new keys
- Token expiry: **120 seconds** (2 minutes)
- Nonce: Random hex string (16 bytes = 32 chars)
- URI format: `{METHOD} {host}{path}` (no https://)

## JWT Structure ✅ VALIDATED

**Header:**

```json
{
  "alg": "ES256",
  "typ": "JWT",
  "kid": "organizations/{org}/apiKeys/{key}",
  "nonce": "random_hex_32_chars"
}
```

**Payload:**

```json
{
  "iss": "cdp",
  "sub": "organizations/{org}/apiKeys/{key}",
  "nbf": unix_timestamp,
  "exp": unix_timestamp + 120,
  "uri": "POST [api.coinbase.com/api/v3/brokerage/orders](http://api.coinbase.com/api/v3/brokerage/orders)"
}
```

## Implementation

```kotlin
class CoinbaseJwtGenerator @Inject constructor(
    private val credentialStore: CredentialStore
) : AuthTokenProvider {

    override suspend fun generateRestToken(
        method: String,
        path: String,
        body: String?
    ): String {
        val credentials = credentialStore.getCredentials()
            ?: throw IllegalStateException("No credentials configured")
        
        val now = [Instant.now](http://Instant.now)().epochSecond
        val nonce = generateNonce()
        
        val header = JWSHeader.Builder([JWSAlgorithm.ES](http://JWSAlgorithm.ES)256)
            .keyID(credentials.apiKeyId)
            .customParam("nonce", nonce)
            .build()
        
        val claims = JWTClaimsSet.Builder()
            .issuer("cdp")
            .subject(credentials.apiKeyId)
            .notBeforeTime(Date(now * 1000))
            .expirationTime(Date((now + 120) * 1000))
            .claim("uri", "$method [api.coinbase.com](http://api.coinbase.com)$path")
            .build()
        
        val jwt = SignedJWT(header, claims)
        jwt.sign(ECDSASigner(credentials.privateKey))
        
        return jwt.serialize()
    }
    
    override suspend fun generateWebSocketToken(): String {
        // WebSocket JWT has NO uri claim
        val credentials = credentialStore.getCredentials()
            ?: throw IllegalStateException("No credentials configured")
        
        val now = [Instant.now](http://Instant.now)().epochSecond
        val nonce = generateNonce()
        
        val header = JWSHeader.Builder([JWSAlgorithm.ES](http://JWSAlgorithm.ES)256)
            .keyID(credentials.apiKeyId)
            .customParam("nonce", nonce)
            .build()
        
        val claims = JWTClaimsSet.Builder()
            .issuer("cdp")
            .subject(credentials.apiKeyId)
            .notBeforeTime(Date(now * 1000))
            .expirationTime(Date((now + 120) * 1000))
            .build()  // No URI claim!
        
        val jwt = SignedJWT(header, claims)
        jwt.sign(ECDSASigner(credentials.privateKey))
        
        return jwt.serialize()
    }
    
    override fun invalidateToken() {
        // JWT is stateless, nothing to invalidate
    }
    
    override fun hasCredentials(): Boolean {
        return credentialStore.hasCredentials()
    }
    
    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

## Dependencies

- nimbus-jose-jwt library
- CredentialStore for retrieving private key

## Acceptance Criteria

- [ ]  Implements AuthTokenProvider interface
- [ ]  REST tokens include URI claim
- [ ]  WebSocket tokens omit URI claim
- [ ]  Tokens validate against Coinbase API
- [ ]  Private key loaded from PEM format correctly