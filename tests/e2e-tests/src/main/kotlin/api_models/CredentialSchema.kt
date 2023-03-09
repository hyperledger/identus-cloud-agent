package api_models

import com.fasterxml.jackson.databind.JsonNode

data class CredentialSchema(
    var name: String? = null,
    var version: String? = null,
    var tags: List<String>? = listOf(""),
    var description: String? = null,
    var type: String? = null,
    var author: String? = null,
    var authored: String? = null,
    var schema: JsonNode? = null,
    var guid: String? = null,
    var longId: String? = null,
    var id: String? = null,
    var kind: String? = null,
    var self: String? = null,
)

//TODO: this can be removed
data class JsonSchema(
    var `$id`: String? = null,
    var `$schema`: String? = null,
    var description: String? = null,
    var type: String? = null,
    var properties: Map<String, LinkedHashMap<String, String>>? = null,
)
