package common

import api_models.CredentialSchema
import api_models.JsonSchema
import java.util.*

object CredentialSchemas {
    val CREDENTIAL_SCHEMA_TYPE = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json"

    val SCHEMA_TYPE = "https://json-schema.org/draft/2019-09/schema"

    val JSON_SCHEMA = JsonSchema(
        `$id` = "student-schema-1.0",
        `$schema` = SCHEMA_TYPE,
        description = "Student schema",
        type = "object",
        properties = mapOf(
            "name" to linkedMapOf("type" to "string"),
            "age" to linkedMapOf("type" to "integer"),
        )
    )

    fun generate_with_name_suffix(suffix: String): CredentialSchema {
        return CredentialSchema(
            author = "University",
            name = "${UUID.randomUUID()} $suffix",
            description = "Simple student credentials schema",
            type = "object",
            schema = JSON_SCHEMA,
            tags = listOf("school", "students"),
            version = "1.0",
        )
    }

    val STUDENT_SCHEMA = CredentialSchema(
        author = "University",
        name = UUID.randomUUID().toString(),
        description = "Simple student credentials schema",
        type = "object",
        schema = JSON_SCHEMA,
        tags = listOf("school", "students"),
        version = "1.0",
    )
}
