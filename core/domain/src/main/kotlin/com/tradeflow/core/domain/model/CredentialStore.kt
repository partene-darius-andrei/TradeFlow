package com.tradeflow.core.domain.model

interface CredentialStore {
    suspend fun getApiKey(): String?
    suspend fun getSecret(): String?
}
