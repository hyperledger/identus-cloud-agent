package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.agent.walletapi.util.Prism14CompatUtil.*
import zio.*

import scala.util.Try

final case class ECKeyPair(publicKey: ECPublicKey, privateKey: ECPrivateKey)

trait ECPublicKey {
  def curve: EllipticCurve
  def toJavaPublicKey: java.security.interfaces.ECPublicKey
  def verify(data: Array[Byte], signature: Array[Byte]): Try[Unit]
  def encode: Array[Byte]
}

trait ECPrivateKey {
  def curve: EllipticCurve
  def toJavaPrivateKey: java.security.interfaces.ECPrivateKey
  def sign(data: Array[Byte]): Try[Array[Byte]]
  def encode: Array[Byte]
  def computePublicKey: ECPublicKey
  override final def toString(): String = "<PRIVATE_KEY>"
}

trait ECKeyFactory {
  def publicKeyFromCoordinate(curve: EllipticCurve, x: BigInt, y: BigInt): Try[ECPublicKey]
  def publicKeyFromEncoded(curve: EllipticCurve, bytes: Array[Byte]): Try[ECPublicKey]
  def privateKeyFromEncoded(curve: EllipticCurve, bytes: Array[Byte]): Try[ECPrivateKey]
  def generateKeyPair(curve: EllipticCurve): Task[ECKeyPair]
  def deriveKeyPair(curve: EllipticCurve, seed: Array[Byte])(path: DerivationPath*): Task[ECKeyPair]
  def randomBip32Seed(): Task[Array[Byte]]
}

enum DerivationPath {
  case Normal(i: Int) extends DerivationPath
  case Hardened(i: Int) extends DerivationPath
}

trait Apollo {
  def ecKeyFactory: ECKeyFactory
}

object Apollo {
  val prism14Layer: ULayer[Apollo] = ZLayer.succeed(Prism14Apollo)
}
