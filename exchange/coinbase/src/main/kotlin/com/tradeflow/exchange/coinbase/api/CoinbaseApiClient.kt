package com.tradeflow.exchange.coinbase.api

import com.tradeflow.core.domain.auth.AuthTokenProvider
import com.tradeflow.exchange.coinbase.dto.AccountsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import javax.inject.Inject

class CoinbaseApiClient @Inject constructor(
    private val httpClient: HttpClient,
    private val authProvider: AuthTokenProvider
) {
    private val baseUrl = "https://api.coinbase.com"

    suspend fun getAccounts(): Result<AccountsResponseDto> = runCatching {
        val path = "/api/v3/brokerage/accounts"
        val token = authProvider.getToken("GET", path)

        httpClient.get("$baseUrl$path") {
            header("Authorization", "Bearer $token")
        }.body()
    }
}
