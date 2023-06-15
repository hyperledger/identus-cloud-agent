package io.iohk.atala.castor.core.model.did

// EC Name is used in JWK https://w3c-ccg.github.io/security-vocab/#publicKeyJwk
// It MUST match the curve name in https://www.iana.org/assignments/jose/jose.xhtml
// in the "JSON Web Key Elliptic Curve" section
//
// name - The curve name in PRISM DID method
// joseName - The curve name accoring to JOSE standard
enum EllipticCurve(val name: String, val joseName: String) {
  case SECP256K1 extends EllipticCurve("secp256k1", "secp256k1")
  case ED25519 extends EllipticCurve("ed25519", "Ed25519")
  case X25519 extends EllipticCurve("x25519", "X25519")
}

object EllipticCurve {

  private val lookup = EllipticCurve.values.map(i => i.name -> i).toMap
  private val joseLookup = EllipticCurve.values.map(i => i.joseName -> i).toMap

  def parseString(s: String): Option[EllipticCurve] = lookup.get(s)

  def parseJoseString(s: String): Option[EllipticCurve] = joseLookup.get(s)

}
