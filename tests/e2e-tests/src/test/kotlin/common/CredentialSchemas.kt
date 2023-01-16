package common

import api_models.CredentialSchema

object CredentialSchemas {
    val STUDENT_SCHEMA = CredentialSchema(
        author = "University",
        name = "Student schema",
        description = "Simple student credentials schema",
        attributes = listOf("name", "age"),
        tags = listOf("school", "students"),
        version = "1.0",
    )
}
