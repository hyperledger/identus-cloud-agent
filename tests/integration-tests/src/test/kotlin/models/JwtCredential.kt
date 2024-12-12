package models

import com.google.gson.Gson
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.crypto.impl.*
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.hyperledger.identus.client.models.VerificationMethod
import java.io.Serializable
import java.security.Provider
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Date
import kotlin.reflect.KClass

class JwtCredential {
    // helper classes
    interface Key<T> {
        val value: T
    }

    class EC(override val value: ECKey) : Key<ECKey>
    class OKP(override val value: OctetKeyPair) : Key<OctetKeyPair>
    class Secret(override val value: ByteArray) : Key<ByteArray>

    // properties
    var header: JWSHeader? = null
    var payload: Payload? = null
    var signature: Base64URL? = null
    var claimSetBuilder = JWTClaimsSet.Builder()

    companion object {
        val provider: Provider = BouncyCastleProvider()
        val keys: MutableMap<String, Key<out Serializable>> = mutableMapOf()

        fun parseBase64(base64: String): JwtCredential = JwtCredential().parseBase64(base64)

        fun parseJwt(jwt: String): JwtCredential = JwtCredential().parseJwt(jwt)

        fun verify(jwt: String, verification: List<VerificationMethod>): Boolean {
            val signedJWT = SignedJWT.parse(jwt)
            verification
                .map { Gson().toJson(it.publicKeyJwk) }
                .forEach {
                    val result = signedJWT.verify(verifier(it))
                    if (result) return true
                }
            return false
        }

        fun verify(jwt: String, verifier: JWSVerifier): Boolean {
            verifier.jcaContext.provider = provider
            val signedJWT = SignedJWT.parse(jwt)
            val result = signedJWT.verify(verifier)
            return result
        }

        private fun type(algorithm: JWSAlgorithm): KClass<out JWSProvider> {
            if (MACProvider.SUPPORTED_ALGORITHMS.contains(algorithm)) {
                return MACProvider::class
            }
            if (ECDSAProvider.SUPPORTED_ALGORITHMS.contains(algorithm)) {
                return ECDSAProvider::class
            }

            if (EdDSAProvider.SUPPORTED_ALGORITHMS.contains(algorithm)) {
                return EdDSAProvider::class
            }
            throw RuntimeException("Requested [$algorithm] not supported.")
        }

        private fun generateBytes(bytes: Int): ByteArray {
            val randomBytes = ByteArray(bytes)
            SecureRandom().nextBytes(randomBytes)
            return randomBytes
        }

        private fun key(algorithm: JWSAlgorithm, curve: Curve?): Key<out Serializable> {
            val key = keys
                .getOrPut("${algorithm.name}-${curve?.name}") {
                    when (type(algorithm)) {
                        MACProvider::class -> Secret(
                            generateBytes(128),
                        )

                        ECDSAProvider::class -> EC(
                            ECKeyGenerator(curve).provider(provider).keyUse(KeyUse.SIGNATURE).generate(),
                        )

                        EdDSAProvider::class -> OKP(
                            OctetKeyPairGenerator(curve).provider(provider).keyUse(KeyUse.SIGNATURE).generate(),
                        )

                        else -> throw RuntimeException("Requested [$algorithm] not supported.")
                    }
                }
            return key
        }

        fun signer(algorithm: JWSAlgorithm, curve: Curve?): JWSSigner {
            val signer: JWSSigner = when (val key = key(algorithm, curve)) {
                is Secret -> MACSigner(key.value)
                is EC -> ECDSASigner(key.value)
                is OKP -> Ed25519Signer(key.value)
                else -> throw RuntimeException("Unsupported key algorithm: $algorithm and curve: $curve")
            }
            signer.jcaContext.provider = provider
            return signer
        }

        private fun parseKey(key: String): JWK = try {
            ECKey.parse(key)
        } catch (e: Exception) {
            try {
                OctetKeyPair.parse(key)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid key [$key]", e)
            }
        }

        private fun verifier(key: String): JWSVerifier {
            val verifier: JWSVerifier = when (val jwk = parseKey(key)) {
                is ECKey -> ECDSAVerifier(jwk)
                is OctetKeyPair -> Ed25519Verifier(jwk)
                else -> throw RuntimeException("Invalid key [$key]")
            }
            verifier.jcaContext.provider = provider
            return verifier
        }
    }

    fun sign(algorithm: JWSAlgorithm, curve: Curve?): String {
        val jwt = SignedJWT(
            JWSHeader.Builder(algorithm).build(),
            claimSetBuilder.build(),
        )
        jwt.sign(signer(algorithm, curve))
        return jwt.serialize()
    }

    fun parseBase64(base64: String): JwtCredential {
        val jwt = Base64URL.from(base64).decodeToString()
        return parseJwt(jwt)
    }

    fun parseJwt(jwt: String): JwtCredential {
        val signedJWT = SignedJWT.parse(jwt)
        claimSetBuilder = JWTClaimsSet.Builder(signedJWT.jwtClaimsSet)
        header = signedJWT.header
        payload = signedJWT.payload
        signature = signedJWT.signature
        return this
    }

    fun issuer(iss: String): JwtCredential {
        claimSetBuilder.issuer(iss)
        return this
    }

    fun jwtID(jti: String): JwtCredential {
        claimSetBuilder.jwtID(jti)
        return this
    }

    fun audience(aud: String): JwtCredential {
        claimSetBuilder.audience(aud)
        return this
    }

    fun issueTime(iat: OffsetDateTime): JwtCredential {
        claimSetBuilder.issueTime(Date.from(iat.toInstant()))
        return this
    }

    fun subject(sub: String): JwtCredential {
        claimSetBuilder.subject(sub)
        return this
    }

    fun expirationTime(exp: OffsetDateTime): JwtCredential {
        claimSetBuilder.expirationTime(Date.from(exp.toInstant()))
        return this
    }

    fun notBefore(nbf: OffsetDateTime): JwtCredential {
        claimSetBuilder.notBeforeTime(Date.from(nbf.toInstant()))
        return this
    }

    fun claim(key: String, data: Any): JwtCredential {
        claimSetBuilder.claim(key, Gson().fromJson(Gson().toJson(data), Object::class.java))
        return this
    }

    fun claims(claims: Map<String, Any>): JwtCredential {
        claims.forEach { (key, value) -> claimSetBuilder.claim(key, value) }
        return this
    }

    fun serialize(): String = SignedJWT(header!!.toBase64URL(), payload!!.toBase64URL(), signature!!).serialize()
}
