# 🎫 [SUPERSEDED] JwtGenerator - ES256 Token Generation

Effort level: Medium
Priority: High
Status: Done
Blocked by: Replaced by: 🟡 COINBASE: JWT Token Generator

## Objective

Generate JWT tokens for Coinbase API authentication.

## File

`data/security/JwtGenerator.kt`

## Critical Requirements (From Research)

- Algorithm: **ES256** (ECDSA P-256)
- Token expiry: **120 seconds** (2 minutes)
- Nonce: Random hex string (16 bytes = 32 chars)
- URI format: `{METHOD} {host}{path}` (no https://)

## JWT Structure

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

## Methods

- `generateRestToken(method: String, host: String, path: String): String`
- `generateWebSocketToken(): String` (no URI claim needed)

## Dependencies

- nimbus-jose-jwt library
- SecureKeyStore for retrieving private key

## Acceptance Criteria

- Generated tokens validate against Coinbase API
- Private key loaded from PEM format correctly
- Token expires after 2 minutes