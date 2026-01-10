package com.tradeflow.standalone

import com.tradeflow.core.domain.model.CredentialStore
import com.tradeflow.exchange.coinbase.api.CoinbaseApiClient
import com.tradeflow.exchange.coinbase.auth.CoinbaseJwtGenerator
import com.tradeflow.exchange.coinbase.repository.CoinbaseRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

fun main() = runBlocking {
    println("=".repeat(60))
    println("TradeFlow - Standalone Trading Bot")
    println("=".repeat(60))
    println()

    // Load credentials
    val localPropertiesFile = File("local.properties")
    val props = Properties()
    if (localPropertiesFile.exists()) {
        props.load(localPropertiesFile.inputStream())
    }

    val apiKey = System.getenv("COINBASE_API_KEY")
        ?: props.getProperty("coinbase.api.key")
        ?: error("COINBASE_API_KEY not found")

    val secret = System.getenv("COINBASE_API_SECRET")
        ?: props.getProperty("coinbase.api.secret")
        ?: error("COINBASE_API_SECRET not found")

    println("✓ Credentials loaded")
    println()

    // Create HTTP client
    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("[HTTP] $message")
                }
            }
            level = LogLevel.HEADERS
        }
    }
    println("✓ HTTP client initialized")
    println()

    // Create dependency chain (manual DI)
    val credentialStore = object : CredentialStore {
        override suspend fun getApiKey(): String = apiKey
        override suspend fun getSecret(): String = secret
    }
    val authProvider = CoinbaseJwtGenerator(credentialStore)
    val apiClient = CoinbaseApiClient(httpClient, authProvider)
    val repository = CoinbaseRepository(apiClient)
    println("✓ Repository initialized")
    println()

    // Fetch balances
    println("-".repeat(60))
    println("Fetching Coinbase account balances...")
    println("-".repeat(60))
    println()

    try {
        val balances = repository.getBalances().getOrThrow()

        println("✅ SUCCESS - Fetched ${balances.size} balance(s)")
        println()

        balances.forEach { balance ->
            println("Currency: ${balance.currency}")
            println("  Available: ${balance.available} ${balance.currency}")
            println("  Hold: ${balance.hold} ${balance.currency}")
            println("  Total: ${balance.total} ${balance.currency}")
            println()
        }
    } catch (e: Exception) {
        println("❌ FAILURE")
        println()
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        httpClient.close()
        println("✓ HTTP client closed")
    }

    println()
    println("=".repeat(60))
    println("Done!")
    println("=".repeat(60))
}
