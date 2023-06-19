package api_models

import kotlinx.serialization.Serializable

@Serializable
data class DidResolutionResult(
    var `@context`: String? = null,
    var didDocument: DidDocument? = null,
    var didDocumentMetadata: DidDocumentMetadata? = null,
    var didResolutionMetadata: DidResolutionMetadata? = null,
): JsonEncoded

@Serializable
data class DidDocument(
    var `@context`: List<String>? = null,
    var assertionMethod: List<VerificationMethodRef>? = null,
    var authentication: List<VerificationMethodRef>? = null,
    var capabilityInvocation: List<VerificationMethodRef>? = null,
    var capabilityDelegation: List<VerificationMethodRef>? = null,
    var controller: String? = null,
    var id: String? = null,
    var keyAgreement: List<String>? = null,
    var service: List<DidDocumentService>? = null,
    var verificationMethod: List<VerificationMethod>? = null,
): JsonEncoded

@Serializable
data class VerificationMethod(
    var controller: String? = null,
    var id: String? = null,
    var publicKeyJwk: PublicKeyJwk? = null,
    var type: String? = null,
): JsonEncoded

typealias VerificationMethodRef = String

@Serializable
data class PublicKeyJwk(
    var crv: String? = null,
    var kty: String? = null,
    var x: String? = null,
    var y: String? = null,
): JsonEncoded

@Serializable
data class DidDocumentMetadata(
    var canonicalId: String? = null,
    var versionId: String? = null,
    var deactivated: Boolean? = null,
): JsonEncoded

@Serializable
data class DidDocumentService(
    var id: String? = null,
    var serviceEndpoint: List<String>? = null,
    var type: String? = null,
): JsonEncoded

@Serializable
data class DidResolutionMetadata(
    var contentType: String? = null,
): JsonEncoded
