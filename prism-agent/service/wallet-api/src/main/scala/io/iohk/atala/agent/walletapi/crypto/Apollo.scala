package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.castor.core.model.did.EllipticCurve
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import io.iohk.atala.agent.walletapi.util.Prism14CompatUtil.*

import java.security.KeyFactory
import java.security.spec.{ECPrivateKeySpec, ECPublicKeySpec}
import scala.util.Try

trait Apollo {
  type PublicKey
  type PrivateKey
  type KeyPair = (PublicKey, PrivateKey)

  given ecPucliKey: ECPublicKey[PublicKey]
  given ecPrivateKey: ECPrivateKey[PrivateKey]
  given ecKeyGen: ECKeyGen[PublicKey, PrivateKey]
}

trait ECPublicKey[Pub] {
  def curve(curve: EllipticCurve): Try[EllipticCurve]
  def fromXY(curve: EllipticCurve, x: BigInt, y: BigInt): Try[Pub]
  def toJavaPublicKey(publicKey: Pub): java.security.PublicKey
}

trait ECPrivateKey[Priv] {
  def curve(curve: EllipticCurve): Try[EllipticCurve]
  def fromBytes(curve: EllipticCurve, bytes: Array[Byte]): Try[Priv]
  def toJavaPrivateKey(privateKey: Priv): java.security.PrivateKey
  def sign(privateKey: Priv, bytes: Array[Byte]): Try[Array[Byte]]
}

trait ECKeyGen[Pub, Priv] {
  def generateKeyPair(curve: EllipticCurve): Try[(Pub, Priv)]
}
