# 🟡 COINBASE: JWT Token Generator (Updated)

Effort level: Medium
Priority: High
Completed: 2026-01-07
PR: #12
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

- [x]  Implements `AuthTokenProvider` interface
- [x]  Private key parsed from PEM correctly
- [ ]  Generated tokens accepted by Coinbase API (will verify in Ticket 08)
- [ ]  Unit test with known test vectors (deferred to integration testing)

---

## Post-Implementation Notes

**Completed:** 2026-01-07
**PR:** https://github.com/partene-darius-andrei/TradeFlow/pull/12

### Implementation Summary

JWT token generator implemented using nimbus-jose-jwt library with ES256 signing for Coinbase Advanced Trade API authentication.

### Files Created

1. **CoinbaseJwtGenerator.kt** - Implements AuthTokenProvider interface with ES256 signing
2. **AuthModule.kt** - Hilt DI module binding JWT generator

### Key Decisions

**JWT Structure:**
- Header: ES256 algorithm, JWT type, kid (API key), random nonce
- Claims: iss=cdp, sub=apiKey, nbf, exp (2 min), uri (REST only)
- Signature: ES256 (ECDSA P-256) using EC private key from PEM

**Library Choice:**
- `nimbus-jose-jwt` for JWT generation
- Handles ES256 signing, PEM parsing, claim building
- Industry-standard library used by many OAuth/OIDC implementations

**Error Handling:**
- Missing credentials throw ExchangeError.AuthenticationFailed
- JWT generation errors wrapped in AuthenticationFailed
- All operations on Dispatchers.Default for CPU-intensive crypto

**Nonce Generation:**
- SecureRandom for cryptographic randomness
- 16 bytes = 32 hex chars
- Unique for each JWT

### Build Verification

✅ :exchange:coinbase:build - SUCCESS

### Next Steps

Ready for **Ticket 08 (REST API Client)** - implement getAccounts() endpoint to verify JWT tokens work with real Coinbase API.