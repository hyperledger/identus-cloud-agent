package common

import api_models.CredentialSchema

object CredentialSchemas {
    fun generate_with_name_suffix(suffix: String): CredentialSchema {
        return CredentialSchema(
            author = "University",
            name = "Student schema $suffix",
            description = "Simple student credentials schema",
            attributes = listOf("name", "age"),
            tags = listOf("school", "students"),
            version = "1.0"
        )
    }
    val STUDENT_SCHEMA = CredentialSchema(
        author = "University",
        name = "Student schema",
        description = "Simple student credentials schema",
        attributes = listOf("name", "age"),
        tags = listOf("school", "students"),
        version = "1.0"
    )
}
