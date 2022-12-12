package common

import api_models.CredentialSchema
import java.util.UUID

object TestConstants {
    val CREDENTIAL_SCHEMAS = CredentialSchemas
    val RANDOM_CONSTAND_UUID = UUID.randomUUID().toString()
}

object CredentialSchemas {
    val STUDENT_SCHEMA = CredentialSchema(
        author = "University",
        name = "Student schema",
        description = "Simple student credentials schema",
        attributes = listOf("name", "age"),
        tags = listOf("school", "students"),
        version = "1.0"
    )
}