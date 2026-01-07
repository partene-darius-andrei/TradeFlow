# 🔐 INFRA - Credential Store

Effort level: Small
Priority: High
Status: Not started

## Objective

Secure storage for exchange API credentials. Abstracted for multi-exchange support.

## Files

```
domain/repository/CredentialStore.kt  # Interface
data/security/SecureCredentialStore.kt  # Implementation
```

## Interface

```kotlin
interface CredentialStore {
    suspend fun saveCredentials(credentials: ExchangeCredentials)
    suspend fun getCredentials(): ExchangeCredentials?
    suspend fun clearCredentials()
    fun hasCredentials(): Boolean
}

data class ExchangeCredentials(
    val exchange: Exchange,
    val apiKeyId: String,
    val privateKey: ECPrivateKey  // Or generic PrivateKey
)
```

## Implementation

```kotlin
@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) : CredentialStore {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "exchange_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    override suspend fun saveCredentials(credentials: ExchangeCredentials) {
        prefs.edit {
            putString(KEY_EXCHANGE, [credentials.exchange.name](http://credentials.exchange.name))
            putString(KEY_API_KEY_ID, credentials.apiKeyId)
            putString(KEY_PRIVATE_KEY, credentials.privateKey.toPem())
        }
    }
    
    override suspend fun getCredentials(): ExchangeCredentials? {
        val exchange = prefs.getString(KEY_EXCHANGE, null) ?: return null
        val apiKeyId = prefs.getString(KEY_API_KEY_ID, null) ?: return null
        val privateKeyPem = prefs.getString(KEY_PRIVATE_KEY, null) ?: return null
        
        return ExchangeCredentials(
            exchange = Exchange.valueOf(exchange),
            apiKeyId = apiKeyId,
            privateKey = privateKeyPem.toECPrivateKey()
        )
    }
    
    override suspend fun clearCredentials() {
        prefs.edit { clear() }
    }
    
    override fun hasCredentials(): Boolean {
        return prefs.contains(KEY_API_KEY_ID)
    }
    
    companion object {
        private const val KEY_EXCHANGE = "exchange"
        private const val KEY_API_KEY_ID = "api_key_id"
        private const val KEY_PRIVATE_KEY = "private_key"
    }
}
```

## PEM Parsing

```kotlin
fun String.toECPrivateKey(): ECPrivateKey {
    val pem = this
        .replace("-----BEGIN EC PRIVATE KEY-----", "")
        .replace("-----END EC PRIVATE KEY-----", "")
        .replace("\\s".toRegex(), "")
    
    val decoded = Base64.decode(pem, Base64.DEFAULT)
    val keySpec = PKCS8EncodedKeySpec(decoded)
    val keyFactory = KeyFactory.getInstance("EC")
    return keyFactory.generatePrivate(keySpec) as ECPrivateKey
}
```

## Security Notes

- Uses Android Keystore for master key
- AES-256-GCM encryption for values
- Never log credentials
- Clear clipboard after paste in UI

## Acceptance Criteria

- [ ]  Implements CredentialStore interface
- [ ]  Uses EncryptedSharedPreferences
- [ ]  PEM parsing works for Coinbase keys
- [ ]  clearCredentials() wipes all data