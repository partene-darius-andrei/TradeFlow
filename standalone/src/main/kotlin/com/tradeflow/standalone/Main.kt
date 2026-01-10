package com.tradeflow.standalone

import com.tradeflow.core.domain.model.CredentialStore
import com.tradeflow.exchange.coinbase.api.CoinbaseApiClient
import com.tradeflow.exchange.coinbase.auth.CoinbaseJwtGenerator
import com.tradeflow.exchange.coinbase.repository.CoinbaseRepository
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

/**
 * Standalone JVM Application - TradeFlow Trading Bot
 *
 * REUSES existing domain logic from:
 * - core:domain - Pure business logic (TechnicalAnalysisService, DecisionEngine, etc.)
 * - exchange:coinbase - Coinbase API integration (Repository, Auth, HTTP)
 *
 * This replaces the Android app with a pure JVM application that:
 * 1. Runs reliably (no Android lifecycle interruptions)
 * 2. Can be deployed to any server/laptop/cloud
 * 3. Reuses 95% of existing domain code
 *
 * Usage:
 *   ./gradlew :standalone:run
 */
fun main() = runBlocking {
    println("=".repeat(60))
    println("TradeFlow - Standalone Trading Bot")
    println("=".repeat(60))
    println()

    // 1. Load credentials from local.properties or environment
    val credentials = loadCredentials()
    println("✓ Credentials loaded")
    println()

    // 2. Initialize HTTP client
    val httpClient = createHttpClient()
    println("✓ HTTP client initialized")
    println()

    // 3. Create dependency chain (manual DI without Hilt)
    val credentialStore = SimpleCredentialStore(
        apiKey = credentials.apiKey,
        secret = credentials.secret
    )
    val authProvider = CoinbaseJwtGenerator(credentialStore)
    val apiClient = CoinbaseApiClient(httpClient, authProvider)
    val repository = CoinbaseRepository(apiClient)
    println("✓ Repository initialized")
    println()

    // 4. Fetch account balances (proof-of-concept)
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

/**
 * Create Ktor HTTP client with JSON serialization and logging.
 */
fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
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
}

/**
 * Load Coinbase credentials from local.properties or environment variables.
 */
fun loadCredentials(): Credentials {
    val localPropertiesFile = File("local.properties")
    val props = Properties()

    if (localPropertiesFile.exists()) {
        props.load(localPropertiesFile.inputStream())
    }

    val apiKey = System.getenv("COINBASE_API_KEY")
        ?: props.getProperty("coinbase.api.key")
        ?: error("COINBASE_API_KEY not found in environment or local.properties")

    val secret = System.getenv("COINBASE_API_SECRET")
        ?: props.getProperty("coinbase.api.secret")
        ?: error("COINBASE_API_SECRET not found in environment or local.properties")

    require(apiKey.isNotBlank()) { "COINBASE_API_KEY is blank" }
    require(secret.isNotBlank()) { "COINBASE_API_SECRET is blank" }

    return Credentials(apiKey, secret)
}

// ============================================================================
// Simple implementations (no Android dependencies)
// ============================================================================

/**
 * Simple in-memory credential store (replaces Android EncryptedSharedPreferences).
 */
class SimpleCredentialStore(
    private val apiKey: String,
    private val secret: String
) : CredentialStore {
    override suspend fun getApiKey(): String = apiKey
    override suspend fun getSecret(): String = secret
}

data class Credentials(
    val apiKey: String,
    val secret: String
)
