package io.iohk.atala.pollux.vc.jwt

import zio.{IO, Task}

import java.time.Instant

trait DidResolver {
  def resolve(didUrl: String): IO[String, DIDResolutionResult]
}

trait DIDResolutionResult

sealed case class DIDResolutionFailed(
    error: DIDResolutionError
) extends DIDResolutionResult

sealed case class DIDResolutionSucceeded(
    didDocument: DIDDocument,
    contentType: String,
    didDocumentMetadata: DIDDocumentMetadata
) extends DIDResolutionResult

sealed trait DIDResolutionError(error: String, message: String) {
  class InvalidDid(message: String) extends DIDResolutionError("invalidDid", message)

  class NotFound(message: String) extends DIDResolutionError("notFound", message)

  class RepresentationNotSupported(message: String) extends DIDResolutionError("RepresentationNotSupported", message)
  class UnsupportedDidMethod(message: String) extends DIDResolutionError("unsupportedDidMethod", message)
  class Error(error: String, message: String) extends DIDResolutionError(error, message)
}
case class DIDDocumentMetadata(
    created: Option[Instant] = Option.empty,
    updated: Option[Instant] = Option.empty,
    deactivated: Option[Boolean] = Option.empty,
    versionId: Option[Instant] = Option.empty,
    nextUpdate: Option[Instant] = Option.empty,
    nextVersionId: Option[Instant] = Option.empty,
    equivalentId: Option[Instant] = Option.empty,
    canonicalId: Option[Instant] = Option.empty
)

case class DIDDocument(
    `@context`: Vector[String] = Vector("https://www.w3.org/ns/did/v1"),
    id: String,
    alsoKnowAs: Vector[String],
    controller: Vector[String],
    verificationMethod: Vector[VerificationMethod],
    service: Vector[Service]
)
case class VerificationMethod(
    id: String,
    `type`: String,
    controller: String,
    publicKeyBase58: Option[String] = Option.empty,
    publicKeyBase64: Option[String] = Option.empty,
    publicKeyJwk: Option[JsonWebKey] = Option.empty,
    publicKeyHex: Option[String] = Option.empty,
    publicKeyMultibase: Option[String] = Option.empty,
    blockchainAccountId: Option[String] = Option.empty,
    ethereumAddress: Option[String] = Option.empty
)
case class JsonWebKey(
    alg: Option[String] = Option.empty,
    crv: Option[String] = Option.empty,
    e: Option[String] = Option.empty,
    d: Option[String] = Option.empty,
    ext: Option[Boolean] = Option.empty,
    key_ops: Vector[String] = Vector.empty,
    kid: Option[String] = Option.empty,
    kty: String,
    n: Option[String] = Option.empty,
    use: Option[String] = Option.empty,
    x: Option[String] = Option.empty,
    y: Option[String] = Option.empty
)
case class Service(id: String, `type`: String, serviceEndpoint: Vector[ServiceEndpoint])
case class ServiceEndpoint(id: String, `type`: String)
