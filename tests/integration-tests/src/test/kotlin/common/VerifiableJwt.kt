package common

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.iohk.atala.automation.restassured.CustomGsonObjectMapperFactory
import models.JwtCredential
import java.time.OffsetDateTime

object VerifiableJwt {
    fun schemaVCv1(): JwtCredential {
        val favoriteColorEnum = JsonArray()
        listOf("red", "orange", "green", "blue", "yellow", "purple").forEach {
            favoriteColorEnum.add(it)
        }

        val favoriteColor = JsonObject()
        favoriteColor.addProperty("type", "string")
        favoriteColor.add("enum", favoriteColorEnum)

        val jsonSchemaPropertiesCredentialSubjectProperties = JsonObject()
        jsonSchemaPropertiesCredentialSubjectProperties.add("favoriteColor", favoriteColor)

        val required = JsonArray()
        required.add("favoriteColor")

        val jsonSchemaPropertiesCredentialSubject = JsonObject()
        jsonSchemaPropertiesCredentialSubject.addProperty("type", "object")
        jsonSchemaPropertiesCredentialSubject.add("properties", jsonSchemaPropertiesCredentialSubjectProperties)
        jsonSchemaPropertiesCredentialSubject.add("required", required)

        val jsonSchemaProperties = JsonObject()
        jsonSchemaProperties.add("credentialSubject", jsonSchemaPropertiesCredentialSubject)

        val jsonSchema = JsonObject()
        jsonSchema.addProperty("${"$"}id", "https://example.com/schemas/favorite-color-schema.json")
        jsonSchema.addProperty("${"$"}schema", "https://json-schema.org/draft/2020-12/schema")
        jsonSchema.addProperty("title", "Favorite Color Schema")
        jsonSchema.addProperty("description", "Favorite Color using JsonSchemaCredential")
        jsonSchema.addProperty("type", "object")
        jsonSchema.add("properties", jsonSchemaProperties)

        val credentialSubject = JsonObject()
        credentialSubject.addProperty("id", "https://example.com/schemas/favorite-color-schema.json")
        credentialSubject.addProperty("type", "JsonSchema")
        credentialSubject.add("jsonSchema", jsonSchema)

        val credentialSchema = JsonObject()
        credentialSchema.addProperty(
            "id",
            "https://www.w3.org/2022/credentials/v2/json-schema-credential-schema.json",
        )
        credentialSchema.addProperty("type", "JsonSchema")
        credentialSchema.addProperty(
            "digestSRI",
            "sha384-S57yQDg1MTzF56Oi9DbSQ14u7jBy0RDdx0YbeV7shwhCS88G8SCXeFq82PafhCrW",
        )

        val verifiableSchema = VerifiableSchemaV1(
            credentialSubject = credentialSubject,
            credentialSchema = credentialSchema,
            type = listOf("VerifiableCredential", "JsonSchemaCredential"),
            context = listOf("https://www.w3.org/ns/credentials/v2", "https://www.w3.org/ns/credentials/examples/v2"),
            id = "https://example.com/credentials/3734",
            issuer = "https://example.com/issuers/14",
            issuanceDate = OffsetDateTime.now(),
        )

        val typeToken = object : TypeToken<Map<String, Any>>() {}.type
        val gson = CustomGsonObjectMapperFactory().create(null, null)
        val json = gson.toJsonTree(verifiableSchema)
        val claims = gson.fromJson<Map<String, Any>>(json, typeToken)
        val jwt = JwtCredential().claims(claims)
        return jwt
    }

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
                statusListCredential = "https://example.com/credential-status/4a6ad192-14b5-4804-8c78-8873c82d2250",
            ),
        )

        val jwt = JwtCredential().issuer("did:prism:issuer").jwtID("jti").subject("did:subject")
            .audience("did:prism:verifier").issueTime(OffsetDateTime.now()).expirationTime(OffsetDateTime.now())
            .notBefore(OffsetDateTime.now()).claim("vc", vc)

        return jwt
    }

    // --- Types to mimic JWT-VC

    // https://www.w3.org/2018/credentials/v1
    // https://www.w3.org/TR/2023/WD-vc-jwt-20230501/
    data class VerifiableCredentialV1(
        val credentialSubject: Any,
        val type: Collection<String>,
        @SerializedName("@context") val context: Collection<String>,
        val credentialStatus: CredentialStatus,
    )

    data class CredentialStatus(
        val statusPurpose: String,
        val statusListIndex: Int,
        val id: String,
        val type: String,
        val statusListCredential: String,
    )

    data class VerifiableSchemaV1(
        @SerializedName("@context") val context: Collection<String>,
        val id: String,
        val type: Collection<String>,
        val issuer: String,
        val issuanceDate: OffsetDateTime,
        val credentialSchema: Any,
        val credentialSubject: Any,
    )
}
