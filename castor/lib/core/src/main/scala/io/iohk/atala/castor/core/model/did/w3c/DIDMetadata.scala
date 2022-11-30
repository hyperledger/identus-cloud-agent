package io.iohk.atala.castor.core.model.did.w3c

import java.time.Instant

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

final case class DIDDocumentMetadataRepr(deactivated: Boolean)
