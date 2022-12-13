package io.iohk.atala.pollux.vc.jwt

import io.iohk.atala.castor.core.model.did.w3c.{
  DIDResolutionErrorRepr,
  PublicKeyJwk,
  PublicKeyRepr,
  ServiceRepr,
  makeW3CResolver
}
import io.iohk.atala.castor.core.service.DIDService
import zio.{Task, UIO}

import java.time.Instant

trait DidResolver {
  def resolve(didUrl: String): UIO[DIDResolutionResult]
}

trait DIDResolutionResult

sealed case class DIDResolutionFailed(
    error: DIDResolutionError
) extends DIDResolutionResult

sealed case class DIDResolutionSucceeded(
    didDocument: DIDDocument,
    didDocumentMetadata: DIDDocumentMetadata
) extends DIDResolutionResult

sealed trait DIDResolutionError(error: String, message: String)
case class InvalidDid(message: String) extends DIDResolutionError("invalidDid", message)
case class NotFound(message: String) extends DIDResolutionError("notFound", message)
case class RepresentationNotSupported(message: String) extends DIDResolutionError("RepresentationNotSupported", message)
case class UnsupportedDidMethod(message: String) extends DIDResolutionError("unsupportedDidMethod", message)
case class InvalidPublicKeyLength(message: String) extends DIDResolutionError("invalidPublicKeyLength", message)
case class InvalidPublicKeyType(message: String) extends DIDResolutionError("invalidPublicKeyType", message)
case class UnsupportedPublicKeyType(message: String) extends DIDResolutionError("unsupportedPublicKeyType", message)
case class Error(error: String, message: String) extends DIDResolutionError(error, message)

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
    id: String,
    alsoKnowAs: Vector[String],
    controller: Vector[String],
    verificationMethod: Vector[VerificationMethod] = Vector.empty,
    authentication: Vector[VerificationMethod] = Vector.empty,
    assertionMethod: Vector[VerificationMethod] = Vector.empty,
    keyAgreement: Vector[VerificationMethod] = Vector.empty,
    capabilityInvocation: Vector[VerificationMethod] = Vector.empty,
    capabilityDelegation: Vector[VerificationMethod] = Vector.empty,
    service: Vector[Service] = Vector.empty
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
case class Service(id: String, `type`: String, serviceEndpoint: Vector[String])

/** An adapter for translating Castor resolver to resolver defined in JWT library */
class PrismDidResolver(didService: DIDService) extends DidResolver {

  private val w3cResolver = makeW3CResolver(didService)

  override def resolve(didUrl: String): UIO[DIDResolutionResult] = {
    w3cResolver(didUrl)
      .fold(
        { error =>
          val polluxError = error match {
            case e @ DIDResolutionErrorRepr.InvalidDID                 => InvalidDid(e.value)
            case e @ DIDResolutionErrorRepr.InvalidDIDUrl              => InvalidDid(e.value)
            case e @ DIDResolutionErrorRepr.NotFound                   => NotFound(e.value)
            case e @ DIDResolutionErrorRepr.RepresentationNotSupported => RepresentationNotSupported(e.value)
            case e @ DIDResolutionErrorRepr.InternalError              => Error(e.value, e.value)
            case e @ DIDResolutionErrorRepr.InvalidPublicKeyLength     => InvalidPublicKeyLength(e.value)
            case e @ DIDResolutionErrorRepr.InvalidPublicKeyType       => InvalidPublicKeyType(e.value)
            case e @ DIDResolutionErrorRepr.UnsupportedPublicKeyType   => UnsupportedPublicKeyType(e.value)
          }
          DIDResolutionFailed(polluxError)
        },
        { case (didDocumentMetadata, didDocument) =>
          DIDResolutionSucceeded(
            didDocument = DIDDocument(
              id = didDocument.id,
              alsoKnowAs = Vector.empty,
              controller = Vector(didDocument.controller),
              verificationMethod = didDocument.verificationMethod.map(toPolluxVerificationMethodModel).toVector,
              service = didDocument.service.map(toPolluxServiceModel).toVector
            ),
            didDocumentMetadata = DIDDocumentMetadata(
              deactivated = Some(didDocumentMetadata.deactivated)
            )
          )
        }
      )
  }

  private def toPolluxServiceModel(service: ServiceRepr): Service = {
    Service(
      id = service.id,
      `type` = service.`type`,
      serviceEndpoint = service.serviceEndpoint.toVector
    )
  }

  private def toPolluxVerificationMethodModel(verificationMethod: PublicKeyRepr): VerificationMethod = {
    VerificationMethod(
      id = verificationMethod.id,
      `type` = verificationMethod.`type`,
      controller = verificationMethod.controller,
      publicKeyJwk = Some(toPolluxJwtModel(verificationMethod.publicKeyJwk))
    )
  }

  private def toPolluxJwtModel(jwk: PublicKeyJwk): JsonWebKey = {
    JsonWebKey(
      crv = Some(jwk.crv),
      kty = jwk.kty,
      x = Some(jwk.x),
      y = Some(jwk.y)
    )
  }

}
