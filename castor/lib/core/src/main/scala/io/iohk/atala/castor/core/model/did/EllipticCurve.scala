package io.iohk.atala.castor.core.model.did

sealed trait EllipticCurve

object EllipticCurve {
  case object SECP256K1 extends EllipticCurve
}
