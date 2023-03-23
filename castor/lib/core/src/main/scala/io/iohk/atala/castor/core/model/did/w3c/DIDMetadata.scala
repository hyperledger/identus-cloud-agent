package io.iohk.atala.castor.core.model.did.w3c

import java.time.Instant

// errors are based on https://www.w3.org/TR/did-spec-registries/#error
enum DIDResolutionErrorRepr(val value: String) {
  case InvalidDID(msg: String) extends DIDResolutionErrorRepr("invalidDid")
  case InvalidDIDUrl(msg: String) extends DIDResolutionErrorRepr("invalidDidUrl")
  case NotFound extends DIDResolutionErrorRepr("notFound")
  case RepresentationNotSupported extends DIDResolutionErrorRepr("representationNotSupported")
  case InternalError(msg: String) extends DIDResolutionErrorRepr("internalError")
  case InvalidPublicKeyLength extends DIDResolutionErrorRepr("invalidPublicKeyLength")
  case InvalidPublicKeyType extends DIDResolutionErrorRepr("invalidPublicKeyType")
  case UnsupportedPublicKeyType extends DIDResolutionErrorRepr("unsupportedPublicKeyType")
}

final case class DIDDocumentMetadataRepr(deactivated: Boolean, canonicalId: String, versionId: String)
