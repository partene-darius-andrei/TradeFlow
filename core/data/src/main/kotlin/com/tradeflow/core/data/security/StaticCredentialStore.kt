package com.tradeflow.core.data.security

import com.tradeflow.core.domain.model.CredentialStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticCredentialStore @Inject constructor(
    private val apiKey: String,
    private val apiSecret: String
) : CredentialStore {

    override suspend fun getApiKey(): String? = apiKey.takeIf { it.isNotBlank() }

    override suspend fun getSecret(): String? = apiSecret.takeIf { it.isNotBlank() }
}
