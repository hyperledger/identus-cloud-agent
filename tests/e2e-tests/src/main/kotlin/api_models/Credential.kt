package api_models

data class Credential(
    var automaticIssuance: Boolean = false,
    var awaitConfirmation: Boolean = false,
    var createdAt: String = "",
    var protocolState: String = "",
    var recordId: String = "",
    var thid: String = "",
    var role: String = "",
    var schemaId: String? = "",
    var subjectId: String = "",
    var updatedAt: String = "",
    var validityPeriod: Int = 0,
    var claims: LinkedHashMap<String, String> = LinkedHashMap(),
    var jwtCredential: String = "",
    var issuingDID: String = "",
    var connectionId: String = "",
)
