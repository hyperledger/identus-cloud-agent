package org.hyperledger.identus.shared.crypto

import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.crypto.derivation.DerivationAxis
import io.iohk.atala.prism.crypto.derivation.KeyDerivation
import zio.*

import scala.jdk.CollectionConverters.*
import scala.util.{Try, Success, Failure}

final case class Prism14Secp256k1PublicKey(publicKey: io.iohk.atala.prism.crypto.keys.ECPublicKey)
    extends Secp256k1PublicKey {

  override def getEncoded: Array[Byte] = getEncodedCompressed

  override def getEncodedUncompressed: Array[Byte] = publicKey.getEncoded()

  override def getEncodedCompressed: Array[Byte] = publicKey.getEncodedCompressed()

  override def getECPoint: ECPoint = {
    val point = publicKey.getCurvePoint
    ECPoint(point.getX().bytes(), point.getY().bytes())
  }

  override def verify(data: Array[Byte], signature: Array[Byte]): Try[Unit] = Try {
    val sig = EC.INSTANCE.toSignatureFromBytes(signature)
    EC.INSTANCE.verifyBytes(data, publicKey, sig)
  }.flatMap(isValid => if (isValid) Success(()) else Failure(Exception("The signature verification does not match")))

}

final case class Prism14Secp256k1PrivateKey(privateKey: io.iohk.atala.prism.crypto.keys.ECPrivateKey)
    extends Secp256k1PrivateKey {

  override def toPublicKey: Secp256k1PublicKey = Prism14Secp256k1PublicKey(
    EC.INSTANCE.toPublicKeyFromPrivateKey(privateKey)
  )

  override def getEncoded: Array[Byte] = privateKey.getEncoded()

  override def sign(data: Array[Byte]): Array[Byte] = EC.INSTANCE.signBytes(data, privateKey).getEncoded

}

object Prism14Secp256k1Ops extends Secp256k1KeyOps {

  override def generateKeyPair: Secp256k1KeyPair = {
    val keyPair = EC.INSTANCE.generateKeyPair()
    Secp256k1KeyPair(
      Prism14Secp256k1PublicKey(keyPair.getPublicKey()),
      Prism14Secp256k1PrivateKey(keyPair.getPrivateKey()),
    )
  }

  override def privateKeyFromEncoded(bytes: Array[Byte]): Try[Secp256k1PrivateKey] =
    Try(Prism14Secp256k1PrivateKey(EC.INSTANCE.toPrivateKeyFromBytes(bytes)))

  override def publicKeyFromEncoded(bytes: Array[Byte]): Try[Secp256k1PublicKey] =
    Try(EC.INSTANCE.toPublicKeyFromBytes(bytes))
      .orElse(Try(EC.INSTANCE.toPublicKeyFromCompressed(bytes)))
      .map(Prism14Secp256k1PublicKey.apply)

  override def publicKeyFromCoordinate(x: Array[Byte], y: Array[Byte]): Try[Secp256k1PublicKey] =
    Try {
      val pk = EC.INSTANCE.toPublicKeyFromByteCoordinates(x, y)
      val point = pk.getCurvePoint()
      val isOnCurve = EC.INSTANCE.isSecp256k1(point)
      if (isOnCurve) Prism14Secp256k1PublicKey(pk)
      else throw Exception("The point is not on the secp256k1 curve")
    }

  override def deriveKeyPair(seed: Array[Byte])(path: DerivationPath*): UIO[Secp256k1KeyPair] =
    ZIO.attempt {
      val extendedKey = path
        .foldLeft(KeyDerivation.INSTANCE.derivationRoot(seed)) { case (extendedKey, p) =>
          val axis = p match {
            case DerivationPath.Hardened(i) => DerivationAxis.hardened(i)
            case DerivationPath.Normal(i)   => DerivationAxis.normal(i)
          }
          extendedKey.derive(axis)
        }
      val prism14KeyPair = extendedKey.keyPair()
      Secp256k1KeyPair(
        Prism14Secp256k1PublicKey(prism14KeyPair.getPublicKey()),
        Prism14Secp256k1PrivateKey(prism14KeyPair.getPrivateKey())
      )
    }.orDie

  override def randomBip32Seed: UIO[(Array[Byte], Seq[String])] =
    ZIO.attemptBlocking {
      val mnemonic = KeyDerivation.INSTANCE.randomMnemonicCode()
      val words = mnemonic.getWords().asScala.toList
      KeyDerivation.INSTANCE.binarySeed(mnemonic, "") -> words
    }.orDie

}
