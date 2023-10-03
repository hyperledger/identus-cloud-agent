package api_models

import kotlinx.serialization.Serializable

@Serializable
data class PresentationProof(
    var presentationId: String? = null,
    var thid: String? = null,
    var status: String? = null,
    var connectionId: String? = null,
    var proofs: List<String>? = null,
    var data: List<String>? = null,
    var role: String? = null,
    var metaRetries: Int = 0,
): JsonEncoded

object PresentationProofStatus {
    const val REQUEST_RECEIVED = "RequestReceived"
    const val REQUEST_REJECTED = "RequestRejected"
    const val PRESENTATION_VERIFIED = "PresentationVerified"
}
