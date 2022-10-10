package io.iohk.atala.castor.core.model.did

enum EllipticCurve(val name: String) {
  case SECP256K1 extends EllipticCurve("secp256k1")
}

object EllipticCurve {

  private val lookup = EllipticCurve.values.map(i => i.name -> i).toMap

  def parseString(s: String): Option[EllipticCurve] = lookup.get(s)

}
