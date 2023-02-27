package api_models

data class DocumentTemplate(
    val publicKeys: List<PublicKey>,
    val services: List<Service>,
)
