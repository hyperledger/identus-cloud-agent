package org.hyperledger.identus.castor.core.model.did

import org.hyperledger.identus.shared.models.Base64UrlString

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

sealed trait PublicKeyData {
  def crv: EllipticCurve
}

object PublicKeyData {
  final case class ECKeyData(crv: EllipticCurve, x: Base64UrlString, y: Base64UrlString) extends PublicKeyData
  final case class ECCompressedKeyData(crv: EllipticCurve, data: Base64UrlString) extends PublicKeyData
}
