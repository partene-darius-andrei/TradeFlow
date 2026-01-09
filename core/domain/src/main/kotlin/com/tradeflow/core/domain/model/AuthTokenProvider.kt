package com.tradeflow.core.domain.model

interface AuthTokenProvider {
    suspend fun getToken(method: String, path: String): String
    suspend fun getWebSocketToken(): String
}
