package com.tradeflow.exchange.coinbase.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.tradeflow.core.domain.auth.AuthTokenProvider
import com.tradeflow.core.domain.auth.CredentialStore
import com.tradeflow.core.domain.error.ExchangeError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.StringReader
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.time.Instant
import java.util.Date
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
            val now = Instant.now().epochSecond

            // Build JWT header with nonce
            val header = JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(apiKey)
                .customParam("nonce", generateNonce())
                .build()

            // Build JWT claims
            val claimsBuilder = JWTClaimsSet.Builder()
                .issuer("cdp")
                .subject(apiKey)
                .claim("nbf", now)
                .expirationTime(Date.from(Instant.ofEpochSecond(now + 120))) // 2 minutes

            // Add URI claim only for REST API (not WebSocket)
            uri?.let { claimsBuilder.claim("uri", it) }

            val claims = claimsBuilder.build()

            // Parse EC private key from PEM format
            val privateKey = parsePemPrivateKey(secret)

            // Sign JWT with ES256
            val signedJWT = SignedJWT(header, claims)
            val signer = ECDSASigner(privateKey)
            signedJWT.sign(signer)

            signedJWT.serialize()
        } catch (e: Exception) {
            throw ExchangeError.AuthenticationFailed("Failed to generate JWT: ${e.message}")
        }
    }

    private fun parsePemPrivateKey(keyString: String): ECPrivateKey {
        try {
            timber.log.Timber.d("=== KEY PARSING DEBUG ===")
            timber.log.Timber.d("Raw key length: ${keyString.length}")
            timber.log.Timber.d("Raw key (first 50 chars): ${keyString.take(50)}")

            val trimmedKey = keyString.trim()

            // Check if this is a base64-encoded raw key (Coinbase CDP format) or PEM format
            val isRawBase64 = !trimmedKey.startsWith("-----BEGIN")

            if (isRawBase64) {
                timber.log.Timber.d("Detected raw base64 format (Coinbase CDP)")
                return parseRawBase64Key(trimmedKey)
            } else {
                timber.log.Timber.d("Detected PEM format")
                return parsePemFormattedKey(trimmedKey)
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to parse private key")
            throw IllegalArgumentException("Failed to parse private key: ${e.message}", e)
        }
    }

    private fun parseRawBase64Key(base64Key: String): ECPrivateKey {
        timber.log.Timber.d("Parsing raw base64 key")

        // Decode base64 to get raw private key bytes
        val keyBytes = java.util.Base64.getDecoder().decode(base64Key)
        timber.log.Timber.d("Decoded key bytes length: ${keyBytes.size}")

        // Coinbase CDP provides raw EC private key bytes
        // For ES256 (P-256 curve), the private key is 32 bytes
        // If we have 64 bytes, take the first 32 (the private scalar)
        val privateKeyBytes = if (keyBytes.size == 64) {
            timber.log.Timber.d("Key is 64 bytes, using first 32 as private scalar")
            keyBytes.copyOfRange(0, 32)
        } else {
            keyBytes
        }

        // Construct ECPrivateKey from raw bytes using Java KeyFactory
        // P-256 curve parameters (secp256r1)
        val keyFactory = java.security.KeyFactory.getInstance("EC")
        val ecSpec = java.security.spec.ECGenParameterSpec("secp256r1")
        val params = java.security.AlgorithmParameters.getInstance("EC")
        params.init(ecSpec)
        val ecParameterSpec = params.getParameterSpec(java.security.spec.ECParameterSpec::class.java)

        // Create private key from scalar value
        val s = java.math.BigInteger(1, privateKeyBytes)
        val privateKeySpec = java.security.spec.ECPrivateKeySpec(s, ecParameterSpec)
        val privateKey = keyFactory.generatePrivate(privateKeySpec)

        val ecPrivateKey = privateKey as? ECPrivateKey
            ?: throw IllegalArgumentException("Generated key is not an EC private key")

        timber.log.Timber.d("Successfully parsed raw base64 EC private key")
        return ecPrivateKey
    }

    private fun parsePemFormattedKey(pemString: String): ECPrivateKey {
        timber.log.Timber.d("Parsing PEM formatted key")

        // Handle escaped newlines and normalize formatting
        val normalizedPem = pemString
            .replace("\\n", "\n")
            .trim()
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")

        timber.log.Timber.d("Normalized PEM lines: ${normalizedPem.lines().size}")

        StringReader(normalizedPem).use { reader ->
            PEMParser(reader).use { pemParser ->
                val pemObject = pemParser.readObject()
                    ?: throw IllegalArgumentException("No PEM object found in string")

                val converter = JcaPEMKeyConverter()
                val privateKey = when (pemObject) {
                    is PEMKeyPair -> converter.getKeyPair(pemObject).private
                    is PrivateKeyInfo -> converter.getPrivateKey(pemObject)
                    else -> throw IllegalArgumentException("Unexpected PEM object type: ${pemObject::class.java.simpleName}")
                }

                val ecPrivateKey = privateKey as? ECPrivateKey
                    ?: throw IllegalArgumentException("Private key is not an EC key")

                timber.log.Timber.d("Successfully parsed PEM EC private key")
                return ecPrivateKey
            }
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
