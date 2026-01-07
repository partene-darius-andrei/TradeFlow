package com.tradeflow.core.domain.auth

interface AuthTokenProvider {
    suspend fun getToken(method: String, path: String): String
    suspend fun getWebSocketToken(): String
    fun invalidate()
}
