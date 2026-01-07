# 🔐 CORE-DATA: Secure Credential Store

Effort level: Small
Priority: High
Blocked by: EXCHANGE-API: Repository Interfaces
Module: :core:data

## Objective

Implement secure credential storage using EncryptedSharedPreferences.

## Module

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

- [ ]  Implements `CredentialStore` interface
- [ ]  Credentials encrypted at rest
- [ ]  Clear removes all stored data
- [ ]  No credentials in logs