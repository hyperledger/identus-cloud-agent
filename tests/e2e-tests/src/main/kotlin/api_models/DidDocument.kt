package api_models

data class DidDocument(
    var did: W3cCompatibleDid? = null,
    var metadata: DidDocumentMetadata? = null,
)

data class W3cCompatibleDid(
    var assertionMethod: List<DidDocumentAuthentication>? = null,
    var authentication: List<DidDocumentAuthentication>? = null,
    var capabilityInvocation: List<String>? = null,
    var controller: String? = null,
    var id: String? = null,
    var keyAgreement: List<String>? = null,
    var service: List<DidDocumentService>? = null,
    var verificationMethod: List<DidDocumentAuthentication>? = null,
)

data class DidDocumentAuthentication(
    var controller: String? = null,
    var id: String? = null,
    var publicKeyJwk: PublicKeyJwk? = null,
    var type: String? = null,
    var uri: String? = null,
)

data class PublicKeyJwk(
    var crv: String? = null,
    var kty: String? = null,
    var x: String? = null,
    var y: String? = null,
)

data class DidDocumentMetadata(
    var canonicalId: String? = null,
    var deactivated: Boolean? = null,
)

data class DidDocumentService(
    var id: String? = null,
    var serviceEndpoint: List<String>? = null,
    var type: String? = null,
)
