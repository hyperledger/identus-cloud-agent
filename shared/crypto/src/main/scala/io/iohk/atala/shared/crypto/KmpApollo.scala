package io.iohk.atala.shared.crypto

import io.iohk.atala.prism.apollo.derivation
import io.iohk.atala.prism.apollo.derivation.MnemonicHelper
import io.iohk.atala.prism.apollo.secp256k1.Secp256k1Lib
import io.iohk.atala.prism.apollo.securerandom.SecureRandom
import io.iohk.atala.prism.apollo.utils.KMMECSecp256k1PrivateKey
import io.iohk.atala.prism.apollo.utils.KMMECSecp256k1PublicKey
import zio.*

import scala.jdk.CollectionConverters.*
import scala.util.{Try, Success, Failure}

final case class KmpSecp256k1PublicKey(publicKey: KMMECSecp256k1PublicKey) extends Secp256k1PublicKey {

  override def getECPoint: ECPoint = {
    val point = publicKey.getCurvePoint()
    val x = BigInt(1, point.getX())
    val y = BigInt(1, point.getY())
    ECPoint(x, y)
  }

  override def getEncoded: Array[Byte] = publicKey.getCompressed()

  override def getEncodedCompressed: Array[Byte] = getEncoded

  override def getEncodedUncompressed: Array[Byte] =
    KmpSecp256k1KeyOps.secpLib.uncompressPublicKey(getEncodedCompressed)

  override def verify(data: Array[Byte], signature: Array[Byte]): Try[Unit] =
    Try(publicKey.verify(signature, data))
      .flatMap(isValid => if (isValid) Success(()) else Failure(Exception("The signature verification does not match")))
}

final case class KmpSecp256k1PrivateKey(privateKey: KMMECSecp256k1PrivateKey) extends Secp256k1PrivateKey {
  override def sign(data: Array[Byte]): Array[Byte] = privateKey.sign(data)

  override def toPublicKey: Secp256k1PublicKey = KmpSecp256k1PublicKey(privateKey.getPublicKey())

  override def getEncoded: Array[Byte] = privateKey.getEncoded()
}

object KmpSecp256k1KeyOps extends Secp256k1KeyOps {
  private[crypto] val secpLib: Secp256k1Lib = Secp256k1Lib()
  private[crypto] val secureRandom: SecureRandom = SecureRandom()

  override def publicKeyFromEncoded(bytes: Array[Byte]): Try[Secp256k1PublicKey] =
    Try {
      val publicKey = KMMECSecp256k1PublicKey.Companion.secp256k1FromBytes(bytes)
      val point = publicKey.getCurvePoint()
      val isOnCurve = KMMECSecp256k1PublicKey.Companion.isPointOnSecp256k1Curve(point)
      if (isOnCurve) KmpSecp256k1PublicKey(publicKey)
      else throw new Exception("The public key is not on the secp256k1 curve")
    }

  override def privateKeyFromEncoded(bytes: Array[Byte]): Try[Secp256k1PrivateKey] =
    Try(KMMECSecp256k1PrivateKey.Companion.secp256k1FromByteArray(bytes)).map(KmpSecp256k1PrivateKey(_))

  override def generateKeyPair: UIO[Secp256k1KeyPair] =
    ZIO.attemptBlocking {
      val randBytes = secureRandom.nextBytes(32)
      val privateKey = KMMECSecp256k1PrivateKey(randBytes)
      val publicKey = privateKey.getPublicKey
      Secp256k1KeyPair(
        KmpSecp256k1PublicKey(publicKey),
        KmpSecp256k1PrivateKey(privateKey)
      )
    }.orDie

  def randomBip32Seed: UIO[(Array[Byte], Seq[String])] =
    ZIO.attemptBlocking {
      val words = MnemonicHelper.Companion.createRandomMnemonics()
      val seed = MnemonicHelper.Companion.createSeed(words, "")
      seed -> words.asScala.toList
    }.orDie

  def deriveKeyPair(seed: Array[Byte])(path: DerivationPath*): UIO[Secp256k1KeyPair] =
    ZIO.attemptBlocking {
      val pathStr = path
        .foldLeft(derivation.DerivationPath.empty()) { case (path, p) =>
          p match {
            case DerivationPath.Hardened(i) => path.derive(derivation.DerivationAxis.hardened(i))
            case DerivationPath.Normal(i)   => path.derive(derivation.DerivationAxis.normal(i))
          }
        }
        .toString()
      val hdKey = derivation.HDKey(seed, 0, 0).derive(pathStr)
      val privateKey = hdKey.getKMMSecp256k1PrivateKey()
      val publicKey = privateKey.getPublicKey()

      Secp256k1KeyPair(
        KmpSecp256k1PublicKey(publicKey),
        KmpSecp256k1PrivateKey(privateKey)
      )
    }.orDie

}
