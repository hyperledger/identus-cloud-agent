package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.castor.core.model.did.EllipticCurve

import scala.util.{Failure, Success, Try}
import java.security.KeyFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveSpec

import java.security.spec.{ECPrivateKeySpec, ECPublicKeySpec}
import io.iohk.atala.agent.walletapi.util.Prism14CompatUtil.*
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.crypto.signature.ECSignature
import zio.*

final case class Prism14ECPublicKey(publicKey: io.iohk.atala.prism.crypto.keys.ECPublicKey) extends ECPublicKey {

  override def curve: EllipticCurve = EllipticCurve.SECP256K1

  override def encode: Array[Byte] = publicKey.getEncodedCompressed

  override def toJavaPublicKey: java.security.interfaces.ECPublicKey = {
    val x = publicKey.getCurvePoint.getX.getCoordinate.toScalaBigInt
    val y = publicKey.getCurvePoint.getY.getCoordinate.toScalaBigInt
    val keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider())
    val ecParameterSpec = ECNamedCurveTable.getParameterSpec(EllipticCurve.SECP256K1.name)
    val ecNamedCurveSpec = ECNamedCurveSpec(
      ecParameterSpec.getName(),
      ecParameterSpec.getCurve(),
      ecParameterSpec.getG(),
      ecParameterSpec.getN()
    )
    val ecPublicKeySpec = ECPublicKeySpec(java.security.spec.ECPoint(x.bigInteger, y.bigInteger), ecNamedCurveSpec)
    keyFactory.generatePublic(ecPublicKeySpec).asInstanceOf[java.security.interfaces.ECPublicKey]
  }

  override def verify(data: Array[Byte], signature: Array[Byte]): Try[Unit] = Try {
    val sig = EC.INSTANCE.toSignatureFromBytes(signature)
    EC.INSTANCE.verifyBytes(data, publicKey, sig)
  }.flatMap(isValid => if (isValid) Success(()) else Failure(Exception("The signature verification does not match")))

  override def hashCode(): Int = publicKey.getHexEncoded().hashCode()

  override def equals(x: Any): Boolean = {
    x match {
      case Prism14ECPublicKey(otherPK) => publicKey.getHexEncoded() == otherPK.getHexEncoded()
      case _                           => false
    }
  }

}

final case class Prism14ECPrivateKey(privateKey: io.iohk.atala.prism.crypto.keys.ECPrivateKey) extends ECPrivateKey {

  override def curve: EllipticCurve = EllipticCurve.SECP256K1

  override def toJavaPrivateKey: java.security.interfaces.ECPrivateKey = {
    val bytes = privateKey.getEncoded()
    val keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider())
    val ecParameterSpec = ECNamedCurveTable.getParameterSpec(EllipticCurve.SECP256K1.name)
    val ecNamedCurveSpec = ECNamedCurveSpec(
      ecParameterSpec.getName(),
      ecParameterSpec.getCurve(),
      ecParameterSpec.getG(),
      ecParameterSpec.getN()
    )
    val ecPrivateKeySpec = ECPrivateKeySpec(java.math.BigInteger(1, bytes), ecNamedCurveSpec)
    keyFactory.generatePrivate(ecPrivateKeySpec).asInstanceOf[java.security.interfaces.ECPrivateKey]
  }

  override def encode: Array[Byte] = privateKey.getEncoded

  override def sign(data: Array[Byte]): Try[Array[Byte]] =
    Try(EC.INSTANCE.signBytes(data, privateKey).getEncoded)

  override def computePublicKey: ECPublicKey =
    Prism14ECPublicKey(EC.INSTANCE.toPublicKeyFromPrivateKey(privateKey))

  override def hashCode(): Int = privateKey.getHexEncoded().hashCode()

  override def equals(x: Any): Boolean = {
    x match {
      case Prism14ECPrivateKey(otherPK) => privateKey.getHexEncoded() == otherPK.getHexEncoded()
      case _                            => false
    }
  }

}

object Prism14ECKeyFactory extends ECKeyFactory {

  override def privateKeyFromEncoded(curve: EllipticCurve, bytes: Array[Byte]): Try[ECPrivateKey] =
    curve match {
      case EllipticCurve.SECP256K1 =>
        Try(
          Prism14ECPrivateKey(EC.INSTANCE.toPrivateKeyFromBytes(bytes))
        )
    }

  override def publicKeyFromEncoded(curve: EllipticCurve, bytes: Array[Byte]): Try[ECPublicKey] =
    curve match {
      case EllipticCurve.SECP256K1 =>
        Try(EC.INSTANCE.toPublicKeyFromBytes(bytes))
          .orElse(Try(EC.INSTANCE.toPublicKeyFromCompressed(bytes)))
          .map(Prism14ECPublicKey.apply)
    }

  override def publicKeyFromCoordinate(curve: EllipticCurve, x: BigInt, y: BigInt): Try[ECPublicKey] =
    curve match {
      case EllipticCurve.SECP256K1 =>
        Try(Prism14ECPublicKey(EC.INSTANCE.toPublicKeyFromBigIntegerCoordinates(x.toKotlinBigInt, y.toKotlinBigInt)))
    }

  override def generateKeyPair(curve: EllipticCurve): Task[ECKeyPair] = {
    curve match {
      case EllipticCurve.SECP256K1 =>
        ZIO.attempt {
          val keyPair = EC.INSTANCE.generateKeyPair()
          ECKeyPair(Prism14ECPublicKey(keyPair.getPublicKey), Prism14ECPrivateKey(keyPair.getPrivateKey))
        }
    }
  }

}

object Prism14Apollo extends Apollo {
  override def ecKeyFactory: ECKeyFactory = Prism14ECKeyFactory
}
