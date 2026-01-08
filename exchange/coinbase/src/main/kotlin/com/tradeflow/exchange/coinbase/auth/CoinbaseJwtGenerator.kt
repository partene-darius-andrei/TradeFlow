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

    private fun parsePemPrivateKey(pemString: String): ECPrivateKey {
        try {
            // Handle escaped newlines (from environment variables or properties files)
            val normalizedPem = pemString.replace("\\n", "\n")

            timber.log.Timber.d("PEM string length: ${normalizedPem.length}")
            timber.log.Timber.d("PEM starts with: ${normalizedPem.take(50)}")
            timber.log.Timber.d("PEM ends with: ${normalizedPem.takeLast(50)}")

            StringReader(normalizedPem).use { reader ->
                PEMParser(reader).use { pemParser ->
                    val pemObject = pemParser.readObject()
                        ?: throw IllegalArgumentException("No PEM object found in string. Check that the private key is in valid PEM format.")

                    timber.log.Timber.d("PEM object type: ${pemObject::class.java.simpleName}")

                    val converter = JcaPEMKeyConverter()
                    val privateKey = when (pemObject) {
                        is PEMKeyPair -> {
                            // Traditional EC PRIVATE KEY format
                            val keyPair = converter.getKeyPair(pemObject)
                            keyPair.private
                        }
                        is PrivateKeyInfo -> {
                            // PKCS8 format (BEGIN PRIVATE KEY)
                            converter.getPrivateKey(pemObject)
                        }
                        is org.bouncycastle.openssl.PEMEncryptedKeyPair -> {
                            throw IllegalArgumentException("Encrypted PEM keys are not supported. Use an unencrypted EC private key.")
                        }
                        else -> throw IllegalArgumentException("Unexpected PEM object type: ${pemObject::class.java.simpleName}. Expected PEMKeyPair or PrivateKeyInfo.")
                    }

                    val ecPrivateKey = privateKey as? ECPrivateKey
                        ?: throw IllegalArgumentException("Private key is not an EC key. Got: ${privateKey::class.java.simpleName}")

                    timber.log.Timber.d("Successfully parsed EC private key")
                    return ecPrivateKey
                }
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to parse PEM private key")
            throw IllegalArgumentException(
                """
                Failed to parse PEM private key: ${e.message}

                Expected one of these formats:
                1. EC format:
                -----BEGIN EC PRIVATE KEY-----
                ...
                -----END EC PRIVATE KEY-----

                2. PKCS8 format:
                -----BEGIN PRIVATE KEY-----
                ...
                -----END PRIVATE KEY-----

                Make sure your local.properties has the key with actual newlines, not \n literals.
                """.trimIndent(), e
            )
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
