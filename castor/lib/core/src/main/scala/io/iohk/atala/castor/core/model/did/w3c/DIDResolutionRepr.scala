package io.iohk.atala.castor.core.model.did.w3c

import java.time.Instant

/** A representation of resolution result in W3C format */
final case class DIDResolutionRepr(
    didResolutionMetadata: DIDResolutionMetadataRepr = DIDResolutionMetadataRepr(),
    didDocument: Option[DIDDocumentRepr] = None,
    didDocumentMetadata: Option[DIDDocumentMetadataRepr] = None
)

final case class DIDResolutionMetadataRepr(
    contentType: "application/did+ld+json" = "application/did+ld+json",
    error: Option[DIDResolutionErrorRepr] = None
)

enum DIDResolutionErrorRepr(val value: String) {
  case InvalidDID extends DIDResolutionErrorRepr("invalidDid")
  case InvalidDIDUrl extends DIDResolutionErrorRepr("invalidDidUrl")
  case NotFound extends DIDResolutionErrorRepr("notFound")
  case RepresentationNotSupported extends DIDResolutionErrorRepr("representationNotSupported")
  case InternalError extends DIDResolutionErrorRepr("internalError")
  case InvalidPublicKeyLength extends DIDResolutionErrorRepr("invalidPublicKeyLength")
  case InvalidPublicKeyType extends DIDResolutionErrorRepr("invalidPublicKeyType")
  case UnsupportedPublicKeyType extends DIDResolutionErrorRepr("unsupportedPublicKeyType")
}

final case class DIDDocumentMetadataRepr(
    deactivated: Option[Boolean]
)
