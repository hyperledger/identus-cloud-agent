package api_models

import kotlinx.serialization.Serializable

@Serializable
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
    var validityPeriod: Double = 0.0,
    var claims: LinkedHashMap<String, String> = LinkedHashMap(),
    var jwtCredential: String = "",
    var issuingDID: String = "",
    var connectionId: String = "",
    var metaRetries: Int = 0,
): JsonEncoded

object CredentialState {
    const val OFFER_RECEIVED = "OfferReceived"
    const val REQUEST_RECEIVED = "RequestReceived"
    const val CREDENTIAL_SENT = "CredentialSent"
    const val CREDENTIAL_RECEIVED = "CredentialReceived"
}
