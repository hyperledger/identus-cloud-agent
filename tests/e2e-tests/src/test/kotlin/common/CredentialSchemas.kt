package common

import api_models.CredentialSchema
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*

object CredentialSchemas {
    val CREDENTIAL_SCHEMA_TYPE = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json"

    val SCHEMA_TYPE = "https://json-schema.org/draft/2019-09/schema"

    val JSON_SCHEMA = """
        {
          "${"$"}id": "student-schema-1.0",
          "${"$"}schema": "$SCHEMA_TYPE",
          "description": "Student schema",
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "age": {
              "type": "integer"
            }
          }
        }
    """.trimIndent()

    fun generate_with_name_suffix(suffix: String): CredentialSchema {
        return CredentialSchema(
            author = "did:prism:agent",
            name = "${UUID.randomUUID()} $suffix",
            description = "Simple student credentials schema",
            type = CREDENTIAL_SCHEMA_TYPE,
            schema = ObjectMapper().readTree(JSON_SCHEMA),
            tags = listOf("school", "students"),
            version = "1.0.0"
        )
    }

    val STUDENT_SCHEMA = CredentialSchema(
        author = "did:prism:agent",
        name = UUID.randomUUID().toString(),
        description = "Simple student credentials schema",
        type = CREDENTIAL_SCHEMA_TYPE,
        schema = ObjectMapper().readTree(JSON_SCHEMA),
        tags = listOf("school", "students"),
        version = "1.0.0"
    )
}
