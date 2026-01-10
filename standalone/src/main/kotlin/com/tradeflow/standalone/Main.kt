package com.tradeflow.standalone

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.SecureRandom
import java.time.Instant
import java.util.*

/**
 * Standalone Kotlin application - Fetch Coinbase balance
 *
 * This is a self-contained JVM application that replicates what the Android app was doing:
 * 1. Load credentials from local.properties
 * 2. Generate JWT token using ES256 algorithm
 * 3. Make authenticated API request to Coinbase
 * 4. Print account balances
 *
 * Usage:
 *   ./gradlew :standalone:run
 */
fun main() = runBlocking {
    println("=".repeat(60))
    println("TradeFlow - Standalone Balance Checker")
    println("=".repeat(60))
    println()

    // 1. Load credentials
    val credentials = loadCredentials()
    println("✓ Credentials loaded")
    println("  API Key: ${credentials.apiKey.take(30)}...")
    println("  Secret: ${credentials.secret.take(40)}... (${credentials.secret.length} chars)")
    println()

    // 2. Initialize HTTP client
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

    // 3. Generate JWT token
    val method = "GET"
    val path = "/api/v3/brokerage/accounts"
    val token = generateJwtToken(
        apiKey = credentials.apiKey,
        secret = credentials.secret,
        method = method,
        path = path
    )
    println("✓ JWT token generated")
    println("  Token: ${token.take(50)}...")
    println()

    // 4. Fetch accounts
    println("-".repeat(60))
    println("Fetching Coinbase account balances...")
    println("-".repeat(60))
    println()

    try {
        val response: AccountsResponse = httpClient.get("https://api.coinbase.com$path") {
            header("Authorization", "Bearer $token")
        }.body()

        println("✅ SUCCESS - Fetched ${response.accounts.size} account(s)")
        println()

        response.accounts.forEach { account ->
            val available = account.availableBalance.value.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
            val hold = account.hold.value.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
            val total = available + hold

            println("Account: ${account.name}")
            println("  Currency: ${account.currency}")
            println("  Available: ${account.availableBalance.value} ${account.availableBalance.currency}")
            println("  Hold: ${account.hold.value} ${account.hold.currency}")
            println("  Total: $total ${account.currency}")
            println("  UUID: ${account.uuid}")
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
 * Generate Coinbase JWT token using ES256 algorithm.
 */
suspend fun generateJwtToken(
    apiKey: String,
    secret: String,
    method: String,
    path: String
): String {
    val now = Instant.now().epochSecond

    val header = JWSHeader.Builder(JWSAlgorithm.ES256)
        .keyID(apiKey)
        .customParam("nonce", generateNonce())
        .build()

    val claims = JWTClaimsSet.Builder()
        .issuer("cdp")
        .subject(apiKey)
        .claim("nbf", now)
        .expirationTime(Date.from(Instant.ofEpochSecond(now + 120)))
        .claim("uri", "$method api.coinbase.com$path")
        .build()

    val ecKey = parseECKey(secret)
    val signedJWT = SignedJWT(header, claims)
    val signer = ECDSASigner(ecKey)
    signedJWT.sign(signer)

    return signedJWT.serialize()
}

/**
 * Parse PEM-encoded EC private key.
 */
fun parseECKey(pemString: String): ECKey {
    val normalizedPem = pemString
        .replace("\\n", "\n")
        .trim()
    return ECKey.parseFromPEMEncodedObjects(normalizedPem) as ECKey
}

/**
 * Generate cryptographic nonce for JWT.
 */
fun generateNonce(): String {
    val bytes = ByteArray(16)
    SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

/**
 * Load credentials from local.properties or environment variables.
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
// Data classes for API response
// ============================================================================

data class Credentials(
    val apiKey: String,
    val secret: String
)

@Serializable
data class AccountsResponse(
    val accounts: List<AccountDto>
)

@Serializable
data class AccountDto(
    val uuid: String,
    val name: String,
    val currency: String,
    @SerialName("available_balance")
    val availableBalance: MoneyDto,
    val hold: MoneyDto
)

@Serializable
data class MoneyDto(
    val value: String,
    val currency: String
)
