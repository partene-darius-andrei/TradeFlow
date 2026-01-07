# 🟡 COINBASE: JWT Token Generator

Effort level: Medium
Priority: High
Status: Not started
Blocked by: EXCHANGE-API: Repository Interfaces
Module: :exchange:coinbase

## Objective

Implement Coinbase-specific JWT authentication.

## Module

`:exchange:coinbase`

## Implementation

```kotlin
class CoinbaseJwtGenerator(
    private val credentialStore: CredentialStore
) : AuthTokenProvider {
    
    override suspend fun getToken(method: String, path: String): String {
        val apiKey = credentialStore.getApiKey() 
            ?: throw ExchangeError.AuthenticationFailed("No API key")
        val secret = credentialStore.getSecret()
            ?: throw ExchangeError.AuthenticationFailed("No secret")
        
        return generateJwt(
            apiKey = apiKey,
            secret = secret,
            uri = "$method [api.coinbase.com](http://api.coinbase.com)$path"
        )
    }
    
    override suspend fun getWebSocketToken(): String {
        // WebSocket JWT has no URI claim
        return generateJwt(
            apiKey = credentialStore.getApiKey()!!,
            secret = credentialStore.getSecret()!!,
            uri = null
        )
    }
    
    private fun generateJwt(
        apiKey: String,
        secret: String,
        uri: String?
    ): String {
        val now = [Instant.now](http://Instant.now)().epochSecond
        
        val header = mapOf(
            "alg" to "ES256",
            "typ" to "JWT",
            "kid" to apiKey,
            "nonce" to generateNonce()
        )
        
        val payload = buildMap {
            put("iss", "cdp")
            put("sub", apiKey)
            put("nbf", now)
            put("exp", now + 120)  // 2 minutes
            uri?.let { put("uri", it) }
        }
        
        return signWithES256(header, payload, secret)
    }
    
    private fun generateNonce(): String = 
        ByteArray(16).apply { SecureRandom().nextBytes(this) }
            .joinToString("") { "%02x".format(it) }
}
```

## Dependencies

- `com.nimbusds:nimbus-jose-jwt` for ES256 signing
- `:exchange:api` for interfaces
- `:core:domain` for error types

## Key Requirements (Validated)

- Algorithm: **ES256** (ECDSA P-256) ✅
- Expiry: **120 seconds** ✅
- Nonce: Random 32-char hex in header ✅
- URI format: `{METHOD} [api.coinbase.com](http://api.coinbase.com){path}` ✅
- WebSocket: No URI claim needed ✅

## File Structure

```
exchange/coinbase/src/main/kotlin/com/tradeflow/exchange/coinbase/
└── auth/
    └── CoinbaseJwtGenerator.kt
```

## Acceptance Criteria

- [ ]  Generated tokens accepted by Coinbase API
- [ ]  Private key parsed from PEM correctly
- [ ]  Implements `AuthTokenProvider` interface
- [ ]  Unit test with known test vectors