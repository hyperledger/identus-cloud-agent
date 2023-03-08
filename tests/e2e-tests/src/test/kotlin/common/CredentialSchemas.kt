package common

import api_models.CredentialSchema
import org.openqa.selenium.json.Json
import com.google.gson.Gson
import com.google.gson.JsonObject

object CredentialSchemas {
    val CREDENTIAL_SCHEMA_TYPE = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json"

    val SCHEMA_TYPE = "https://json-schema.org/draft/2019-09/schema"

    val JSON_SCHEMA = """{
        "`$`id": "student-schema-1.0",
        "`$`schema": $SCHEMA_TYPE,
        "description": "Student schema",
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "age": {
            "type": "integer",
          }
        }"""

    fun generate_with_name_suffix(suffix: String): CredentialSchema {
        return CredentialSchema(
            author = "University",
            name = "Student schema $suffix",
            description = "Simple student credentials schema",
            schemaType = CREDENTIAL_SCHEMA_TYPE,
            schema = JSON_SCHEMA,
            tags = listOf("school", "students"),
            version = "1.0",
        )
    }

    val STUDENT_SCHEMA = CredentialSchema(
        author = "University",
        name = "Student schema",
        description = "Simple student credentials schema",
        schemaType = SCHEMA_TYPE,
        schema = JSON_SCHEMA,
        tags = listOf("school", "students"),
        version = "1.0",
    )
}
