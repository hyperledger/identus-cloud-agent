package io.iohk.atala.castor.core.model.did

import io.iohk.atala.shared.models.Base64UrlStrings.*

sealed trait PublicKeyJwk

object PublicKeyJwk {
  final case class ECPublicKeyData(
      crv: EllipticCurve,
      x: Base64UrlString,
      y: Base64UrlString
  ) extends PublicKeyJwk
}
