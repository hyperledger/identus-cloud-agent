package common

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import models.JwtCredential
import java.time.OffsetDateTime

object JwtVerifiableCredential {
    fun jwtVCv1(): JwtCredential {
        val credentialSubject = JsonObject()
        credentialSubject.addProperty("id", "did:subject")
        credentialSubject.addProperty("firstName", "John")
        credentialSubject.addProperty("lastName", "Doe")

        val vc = VerifiableCredentialV1(
            credentialSubject = credentialSubject,
            type = listOf("VerifiableCredential", "VerifiablePresentation"),
            context = listOf("https://www.w3.org/2018/credentials/v1"),
            credentialStatus = CredentialStatus(
                statusPurpose = "Revocation",
                statusListIndex = 1,
                id = "https://example.com/credential-status/4a6ad192-14b5-4804-8c78-8873c82d2250#1",
                type = "StatusList2021Entry",
                statusListCredential = "https://example.com/credential-status/4a6ad192-14b5-4804-8c78-8873c82d2250"
            )
        )

        val jwt = JwtCredential()
            .issuer("did:prism:issuer")
            .jwtID("jti")
            .subject("did:subject")
            .audience("did:prism:verifier")
            .issueTime(OffsetDateTime.now())
            .expirationTime(OffsetDateTime.now())
            .notBefore(OffsetDateTime.now())
            .claim("vc", vc)

        return jwt
    }

    fun getV2VC() {

    }

    // --- Types to mimic JWT-VC

    // https://www.w3.org/2018/credentials/v1
    // https://www.w3.org/TR/2023/WD-vc-jwt-20230501/
    data class VerifiableCredentialV1(
        val credentialSubject: Any,
        val type: Collection<String>,
        @SerializedName("@context")
        val context: Collection<String>,
        val credentialStatus: CredentialStatus
    )

    data class CredentialStatus(
        val statusPurpose: String,
        val statusListIndex: Int,
        val id: String,
        val type: String,
        val statusListCredential: String
    )
}
fun main() {
    val jwt = JwtVerifiableCredential.jwtVCv1().sign(JWSAlgorithm.ES256K, Curve.SECP256K1)
    println(jwt)
    val verifier = ECDSAVerifier((JwtCredential.keys["ES256K-secp256k1"] as JwtCredential.EC).value)
    println((JwtCredential.keys["ES256K-secp256k1"] as JwtCredential.EC).value)
    JwtCredential.verify(jwt, verifier)
}
