package api_models

import org.openqa.selenium.json.Json

data class CredentialSchema(
    var id: String? = null,
    var name: String? = null,
    var version: String? = null,
    var description: String? = null,
    var author: String? = null,
    var authored: String? = null,
    var kind: String? = null,
    var self: String? = null,
    var schemaType: String? = null,
    var schema: String? = null,
    var tags: List<String>? = listOf(""),
)
