package api_models

data class VerificationPolicy(
    var id: String? = null,
    var nonce: String? = null,
    var name: String? = null,
    var description: String? = null,
    var constraints: List<Constraint>? = null,
    var createdAt: String? = null,
    var updatedAt: String? = null,
    var kind: String? = null,
    var self: String? = null,
)

data class VerificationPolicyInput(
    var name: String? = null,
    var description: String? = null,
    var constraints: List<Constraint>? = null,
)

data class Constraint(
    var schemaId: String? = null,
    var trustedIssuers: List<String>? = null,
)
