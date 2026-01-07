# 🔐 [SUPERSEDED] SecureKeyStore - Encrypted Credentials Storage

Effort level: Small
Priority: High
Status: Done
Blocked by: Replaced by: 🔐 CORE-DATA: Secure Credential Store

## Objective

Implement secure storage for Coinbase API credentials using EncryptedSharedPreferences.

## File

`data/security/SecureKeyStore.kt`

## Key Points

- Use MasterKey with AES256_GCM scheme
- Store API Key ID (e.g., `organizations/{org}/apiKeys/{key}`)
- Store Private Key PEM (ECDSA P-256 from Coinbase)
- **DO NOT** generate new keys - must use Coinbase-provided keys

## Methods

- `saveCredentials(apiKeyId: String, privateKeyPem: String)`
- `getApiKeyId(): String?`
- `getPrivateKeyPem(): String?`
- `hasCredentials(): Boolean`
- `clearCredentials()`

## Security Notes

- EncryptedSharedPreferences backed by Android Keystore
- Keys never leave device
- Cleared on app uninstall

## Acceptance Criteria

- Can save and retrieve credentials
- Data persists across app restarts
- Data cleared on `clearCredentials()` call