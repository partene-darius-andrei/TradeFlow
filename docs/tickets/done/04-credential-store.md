# 🔐 CORE-DATA: Secure Credential Store (Updated)

Effort level: Small
Priority: High
Completed: 2026-01-07
PR: #9
Module: :core:data

## Objective

Implement secure credential storage using EncryptedSharedPreferences.

## Module
[build](../../../build)
`:core:data`

## Implements

`CredentialStore` (from `:exchange:api`)

## Implementation

```kotlin
class SecureCredentialStore(
    private val context: Context
) : CredentialStore {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "tradeflow_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    override suspend fun saveCredentials(apiKey: String, secret: String) {
        withContext([Dispatchers.IO](http://Dispatchers.IO)) {
            prefs.edit()
                .putString(KEY_API_KEY, apiKey)
                .putString(KEY_SECRET, secret)
                .apply()
        }
    }
    
    override suspend fun getApiKey(): String? = withContext([Dispatchers.IO](http://Dispatchers.IO)) {
        prefs.getString(KEY_API_KEY, null)
    }
    
    override suspend fun getSecret(): String? = withContext([Dispatchers.IO](http://Dispatchers.IO)) {
        prefs.getString(KEY_SECRET, null)
    }
    
    override suspend fun hasCredentials(): Boolean = withContext([Dispatchers.IO](http://Dispatchers.IO)) {
        prefs.contains(KEY_API_KEY) && prefs.contains(KEY_SECRET)
    }
    
    override suspend fun clearCredentials() {
        withContext([Dispatchers.IO](http://Dispatchers.IO)) {
            prefs.edit().clear().apply()
        }
    }
    
    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SECRET = "secret"
    }
}
```

## Security Notes

- Uses Android Keystore for master key
- AES-256-GCM encryption for values
- Never log credentials
- Clear on app uninstall (default behavior)

## File Structure

```
core/data/src/main/kotlin/com/tradeflow/core/data/
└── security/
    └── SecureCredentialStore.kt
```

## Acceptance Criteria

- [x]  Implements `CredentialStore` interface
- [x]  Credentials encrypted at rest
- [x]  Clear removes all stored data
- [x]  No credentials in logs

---

## Post-Implementation Notes

**Completed:** 2026-01-07
**PR:** https://github.com/partene-darius-andrei/TradeFlow/pull/9

### Implementation Summary

Secure credential storage implemented using EncryptedSharedPreferences with Android Keystore.

### Files Created

1. **SecureCredentialStore.kt** - Implements CredentialStore interface
2. **SecurityModule.kt** - Hilt DI module providing CredentialStore singleton

### Key Decisions

**Encryption:**
- Master key stored in Android Keystore
- AES-256-GCM for values (authenticated encryption)
- AES-256-SIV for keys (deterministic encryption)
- All IO operations on Dispatchers.IO

**Dependency Injection:**
- Separate SecurityModule for clear separation of concerns
- Singleton scope ensures single instance

**Deprecation Warnings:**
- EncryptedSharedPreferences shows deprecation warnings
- Library still in alpha, no stable replacement available
- Safe to use until Google provides migration path

### Build Verification

✅ :core:data:build - SUCCESS
