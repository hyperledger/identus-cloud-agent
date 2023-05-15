package io.iohk.atala.castor.core.model.did

import io.iohk.atala.shared.models.Base64UrlString
import io.iohk.atala.prism.crypto.EC

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

  final case class ECCompressedKeyData(
      crv: EllipticCurve,
      data: Base64UrlString
  ) extends PublicKeyData {
    def toUncompressedKeyData: ECKeyData = {
      val prism14PublicKey = EC.INSTANCE.toPublicKeyFromCompressed(data.toByteArray)
      val ecPoint = prism14PublicKey.getCurvePoint()
      ECKeyData(
        crv = crv,
        x = Base64UrlString.fromByteArray(ecPoint.getX().bytes()),
        y = Base64UrlString.fromByteArray(ecPoint.getY().bytes())
      )
    }
  }
}
