package api_models

data class CredentialSchema(
    var id: String? = null,
    var name: String? = null,
    var version: String? = null,
    var description: String? = null,
    var author: String? = null,
    var authored: String? = null,
    var kind: String? = null,
    var self: String? = null,
    var attributes: List<String>? = listOf(""),
    var tags: List<String>? = listOf(""),
)
