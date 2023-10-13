package common

import io.iohk.atala.prism.models.*
import models.Schema
import models.SchemaProperty
import java.time.Duration
import java.util.*

object TestConstants {
    val TEST_VERIFICATION_POLICY = VerificationPolicyInput(
        name = "Trusted Issuer and SchemaID",
        description = "Verification Policy with trusted issuer and schemaId",
        constraints = listOf(
            VerificationPolicyConstraint(
                schemaId = "http://atalaprism.io/schemas/1.0/StudentCredential",
                trustedIssuers = listOf(
                    "did:example:123456789abcdefghi",
                    "did:example:123456789abcdefghj"
                )
            )
        )
    )
    val CREDENTIAL_SCHEMA_TYPE = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json"

    val SCHEMA_TYPE = "https://json-schema.org/draft/2020-12/schema"

    val jsonSchema = Schema(
        id = "https://example.com/student-schema-1.0",
        schema = SCHEMA_TYPE,
        description = "Student schema",
        type = "object",
        properties = mutableMapOf(
            "name" to SchemaProperty(type = "string"),
            "age" to SchemaProperty(type = "integer")
        )
    )

    fun generate_with_name_suffix_and_author(suffix: String, author: String): CredentialSchemaInput {
        return CredentialSchemaInput(
            author = author,
            name = "${UUID.randomUUID()} $suffix",
            description = "Simple student credentials schema",
            type = CREDENTIAL_SCHEMA_TYPE,
            schema = jsonSchema,
            tags = listOf("school", "students"),
            version = "1.0.0"
        )
    }

    val STUDENT_SCHEMA = CredentialSchemaInput(
        author = "did:prism:agent",
        name = UUID.randomUUID().toString(),
        description = "Simple student credentials schema",
        type = CREDENTIAL_SCHEMA_TYPE,
        schema = jsonSchema,
        tags = listOf("school", "students"),
        version = "1.0.0"
    )
    val RANDOM_CONSTAND_UUID = UUID.randomUUID().toString()
    val DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN = Duration.ofSeconds(60L)
    val PRISM_DID_AUTH_KEY = ManagedDIDKeyTemplate("auth-1", Purpose.authentication)
    val PRISM_DID_ASSERTION_KEY = ManagedDIDKeyTemplate("assertion-1", Purpose.assertionMethod)
    val PRISM_DID_UPDATE_NEW_AUTH_KEY = ManagedDIDKeyTemplate("auth-2", Purpose.authentication)
    val PRISM_DID_SERVICE = Service(
        "https://foo.bar.com",
        listOf("LinkedDomains"),
        Json("https://foo.bar.com/")
    )
    val PRISM_DID_SERVICE_FOR_UPDATE = Service(
        "https://update.com",
        listOf("LinkedDomains"),
        Json("https://update.com/")
    )
    val PRISM_DID_SERVICE_TO_REMOVE = Service(
        "https://remove.com",
        listOf("LinkedDomains"),
        Json("https://remove.com/")
    )
    val PRISM_DID_UPDATE_NEW_SERVICE_URL = "https://bar.foo.com/"
    val PRISM_DID_UPDATE_NEW_SERVICE = Service(
        "https://new.service.com",
        listOf("LinkedDomains"),
        Json("https://new.service.com/")
    )
    val EVENT_TYPE_CONNECTION_UPDATED = "ConnectionUpdated"
    val EVENT_TYPE_ISSUE_CREDENTIAL_RECORD_UPDATED = "IssueCredentialRecordUpdated"
    val EVENT_TYPE_PRESENTATION_UPDATED = "PresentationUpdated"
    val EVENT_TYPE_DID_STATUS_UPDATED = "DIDStatusUpdated"
    val WRONG_SEED = "wrong seed"
}
