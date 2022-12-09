package common

import api_models.CredentialSchema
import java.util.*

object TestConstants {

    val CREDENTIAL_SCHEMA = CredentialSchema(
        author = "University",
        name = "Student schema",
        description = "Simple student credentials schema",
        attributes = listOf("name", "age"),
        tags = listOf("school", "students"),
        version = "1.0"
    )

    val RANDOM_UUID = UUID.randomUUID().toString()
}
