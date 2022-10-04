package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.HexStrings

sealed trait PublicKeyJwk

object PublicKeyJwk {
  final case class ECPublicKeyData(
      crv: EllipticCurve,
      x: HexStrings.HexString,
      y: HexStrings.HexString
  ) extends PublicKeyJwk
}
