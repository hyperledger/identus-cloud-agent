package io.iohk.atala.castor.core.model.did

import io.iohk.atala.shared.models.Base64UrlStrings.Base64UrlString

import java.time.Instant

final case class PublicKey(
    id: String,
    purpose: VerificationRelationship,
    publicKeyData: PublicKeyData
)

enum InternalKeyPurpose {
  case Master extends InternalKeyPurpose
  case Revocation extends InternalKeyPurpose
}

final case class InternalPublicKey(
    id: String,
    purpose: InternalKeyPurpose,
    publicKeyData: PublicKeyData
)

sealed trait PublicKeyData

object PublicKeyData {
  final case class ECKeyData(
      crv: EllipticCurve,
      x: Base64UrlString,
      y: Base64UrlString
  ) extends PublicKeyData
}
