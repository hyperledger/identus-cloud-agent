package io.iohk.atala.pollux.vc.jwt

import java.time.Instant

trait DIDResolutionResult(
    `@context`: Vector[String] = Vector("https://w3id.org/did-resolution/v1")
)
trait DIDResolutionFailed(
    error: DIDResolutionError
) extends DIDResolutionResult
trait DIDResolutionSucceeded(
    didDocument: DIDDocument,
    contentType: String,
    didDocumentMetadata: DIDDocumentMetadata
) extends DIDResolutionResult

trait DIDResolutionError(error: String, message: String) {
  class InvalidDid(message: String) extends DIDResolutionError("invalidDid", message)
  class NotFound(message: String) extends DIDResolutionError("notFound", message)
  class RepresentationNotSupported(message: String) extends DIDResolutionError("RepresentationNotSupported", message)
  class UnsupportedDidMethod(message: String) extends DIDResolutionError("unsupportedDidMethod", message)
  class Error(error: String, message: String) extends DIDResolutionError(error, message)
}
trait DIDDocumentMetadata(
    created: Option[Instant],
    updated: Option[Instant],
    deactivated: Option[Boolean],
    versionId: Option[Instant],
    nextUpdate: Option[Instant],
    nextVersionId: Option[Instant],
    equivalentId: Option[Instant],
    canonicalId: Option[Instant]
)

trait DIDDocument(
    `@context`: Vector[String] = Vector("https://www.w3.org/ns/did/v1"),
    id: String,
    alsoKnowAs: Vector[String],
    controller: Vector[String],
    verificationMethod: Vector[VerificationMethod],
    service: Vector[Service]
)
trait VerificationMethod(
    id: String,
    `type`: String,
    controller: String,
    publicKeyBase58: Option[String],
    publicKeyBase64: Option[String],
    publicKeyJwk: Option[JsonWebKey],
    publicKeyHex: Option[String],
    publicKeyMultibase: Option[String],
    blockchainAccountId: Option[String],
    ethereumAddress: Option[String]
)
trait JsonWebKey(
    alg: Option[String],
    crv: Option[String],
    e: Option[String],
    ext: Option[Boolean],
    key_ops: Vector[String],
    kid: Option[String],
    kty: String,
    n: Option[String],
    use: Option[String],
    x: Option[String],
    y: Option[String]
)
trait Service(id: String, `type`: String, serviceEndpoint: Vector[ServiceEndpoint])
trait ServiceEndpoint(id: String, `type`: String)
