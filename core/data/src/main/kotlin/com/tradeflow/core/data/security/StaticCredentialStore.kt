package com.tradeflow.core.data.security

import com.tradeflow.core.domain.auth.CredentialStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticCredentialStore @Inject constructor(
    private val apiKey: String,
    private val apiSecret: String
) : CredentialStore {

    override suspend fun saveCredentials(apiKey: String, secret: String) {
        // No-op: credentials are injected at build time
    }

    override suspend fun getApiKey(): String? = apiKey.takeIf { it.isNotBlank() }

    override suspend fun getSecret(): String? = apiSecret.takeIf { it.isNotBlank() }

    override suspend fun hasCredentials(): Boolean =
        apiKey.isNotBlank() && apiSecret.isNotBlank()

    override suspend fun clearCredentials() {
        // No-op: credentials are injected at build time
    }
}
