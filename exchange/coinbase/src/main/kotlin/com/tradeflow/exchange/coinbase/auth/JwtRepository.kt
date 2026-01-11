package com.tradeflow.exchange.coinbase.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.tradeflow.core.domain.model.AuthTokenProvider
import com.tradeflow.core.domain.model.CredentialStore
import com.tradeflow.core.domain.model.ExchangeError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.time.Instant
import java.util.Date

class JwtRepository(
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

    private suspend fun generateJwt(
        apiKey: String,
        secret: String,
        uri: String?
    ): String = withContext(Dispatchers.Default) {
        try {
            val now = Instant.now().epochSecond

            val header = JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(apiKey)
                .customParam("nonce", generateNonce())
                .build()

            val claimsBuilder = JWTClaimsSet.Builder()
                .issuer("cdp")
                .subject(apiKey)
                .claim("nbf", now)
                .expirationTime(Date.from(Instant.ofEpochSecond(now + 120)))

            uri?.let { claimsBuilder.claim("uri", it) }

            val claims = claimsBuilder.build()
            val ecKey = parseECKey(secret)

            val signedJWT = SignedJWT(header, claims)
            val signer = ECDSASigner(ecKey)
            signedJWT.sign(signer)

            signedJWT.serialize()
        } catch (e: Exception) {
            throw ExchangeError.AuthenticationFailed("Failed to generate JWT: ${e.message}")
        }
    }

    private fun parseECKey(pemString: String): ECKey {
        try {
            val normalizedPem = pemString
                .replace("\\n", "\n")
                .trim()
            return ECKey.parseFromPEMEncodedObjects(normalizedPem) as ECKey
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse EC private key: ${e.message}", e)
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
