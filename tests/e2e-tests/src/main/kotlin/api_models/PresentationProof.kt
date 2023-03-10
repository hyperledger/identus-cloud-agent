package api_models

data class PresentationProof(
    var presentationId: String? = null,
    var status: String? = null,
    var connectionId: String? = null,
    var proofs: List<String>? = null,
    var data: List<String>? = null,
)
