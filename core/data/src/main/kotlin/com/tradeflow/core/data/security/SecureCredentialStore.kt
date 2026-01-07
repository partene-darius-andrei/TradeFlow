package com.tradeflow.core.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tradeflow.core.domain.auth.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SecureCredentialStore @Inject constructor(
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
        withContext(Dispatchers.IO) {
            prefs.edit()
                .putString(KEY_API_KEY, apiKey)
                .putString(KEY_SECRET, secret)
                .apply()
        }
    }

    override suspend fun getApiKey(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_API_KEY, null)
    }

    override suspend fun getSecret(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_SECRET, null)
    }

    override suspend fun hasCredentials(): Boolean = withContext(Dispatchers.IO) {
        prefs.contains(KEY_API_KEY) && prefs.contains(KEY_SECRET)
    }

    override suspend fun clearCredentials() {
        withContext(Dispatchers.IO) {
            prefs.edit().clear().apply()
        }
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SECRET = "secret"
    }
}
