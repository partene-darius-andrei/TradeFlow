package com.tradeflow.exchange.coinbase.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.tradeflow.core.domain.auth.AuthTokenProvider
import com.tradeflow.core.domain.auth.CredentialStore
import com.tradeflow.core.domain.error.ExchangeError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class CoinbaseJwtGenerator @Inject constructor(
    private val credentialStore: CredentialStore
) : AuthTokenProvider {

    override suspend fun getToken(method: String, path: String): String {
        val apiKey = credentialStore.getApiKey()
            ?: throw ExchangeError.AuthenticationFailed("No API key stored")
        val secret = credentialStore.getSecret()
            ?: throw ExchangeError.AuthenticationFailed("No secret key stored")

        return generateJwt(
            apiKey = apiKey,
            secret = secret,
            uri = "$method api.coinbase.com$path"
        )
    }

    override suspend fun getWebSocketToken(): String {
        val apiKey = credentialStore.getApiKey()
            ?: throw ExchangeError.AuthenticationFailed("No API key stored")
        val secret = credentialStore.getSecret()
            ?: throw ExchangeError.AuthenticationFailed("No secret key stored")

        return generateJwt(
            apiKey = apiKey,
            secret = secret,
            uri = null
        )
    }

    override fun invalidate() {
        // No-op: JWTs are generated on-demand with short expiry (2 minutes)
        // No caching needed
    }

    private suspend fun generateJwt(
        apiKey: String,
        secret: String,
        uri: String?
    ): String = withContext(Dispatchers.Default) {
        try {
            val now = Date()
            val expiration = Date(System.currentTimeMillis() + 120000) // 120 seconds

            // Build JWT claims
            val claimsBuilder = JWTClaimsSet.Builder()
                .issuer("cdp")
                .subject(apiKey)
                .notBeforeTime(now)
                .expirationTime(expiration)

            // Add URI claim only for REST API (not WebSocket)
            uri?.let { claimsBuilder.claim("uri", it) }

            val claims = claimsBuilder.build()

            // Build JWT header with nonce
            val header = JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .keyID(apiKey)
                .customParam("nonce", generateNonce())
                .build()

            // Parse Ed25519 private key from base64
            val keyPair = parseEd25519Key(secret)

            // Sign JWT with EdDSA
            val signedJWT = SignedJWT(header, claims)
            val signer = Ed25519Signer(keyPair)
            signedJWT.sign(signer)

            signedJWT.serialize()
        } catch (e: Exception) {
            throw ExchangeError.AuthenticationFailed("Failed to generate JWT: ${e.message}")
        }
    }

    private fun parseEd25519Key(base64Secret: String): OctetKeyPair {
        try {
            // Handle escaped newlines and strip PEM headers if present
            val cleanedSecret = base64Secret
                .replace("\\n", "\n")
                .trim()
                .lines()
                .filter { !it.startsWith("-----") } // Remove PEM headers/footers
                .joinToString("")
                .replace("\n", "")
                .replace(" ", "")

            // Decode the Ed25519 private key from base64
            val decoded = Base64.getDecoder().decode(cleanedSecret)

            // Ed25519 keys are 64 bytes (32 bytes seed + 32 bytes public key)
            if (decoded.size != 64) {
                throw IllegalArgumentException("Invalid Ed25519 key length: ${decoded.size} (expected 64)")
            }

            // Extract the seed (first 32 bytes) and public key (last 32 bytes)
            val seed = decoded.copyOfRange(0, 32)
            val publicKey = decoded.copyOfRange(32, 64)

            // Create OctetKeyPair for Ed25519
            return OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(publicKey))
                .d(Base64URL.encode(seed))
                .keyUse(KeyUse.SIGNATURE)
                .build()
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse Ed25519 private key: ${e.message}", e)
        }
    }

    private fun generateNonce(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
}
