package com.tradeflow.core.domain.auth

interface CredentialStore {
    suspend fun saveCredentials(apiKey: String, secret: String)
    suspend fun getApiKey(): String?
    suspend fun getSecret(): String?
    suspend fun hasCredentials(): Boolean
    suspend fun clearCredentials()
}
