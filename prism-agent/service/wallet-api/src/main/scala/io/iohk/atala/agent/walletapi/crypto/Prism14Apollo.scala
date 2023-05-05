package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.castor.core.model.did.EllipticCurve
import scala.util.Try
import java.security.KeyFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import java.security.spec.{ECPublicKeySpec, ECPrivateKeySpec}
import io.iohk.atala.agent.walletapi.util.Prism14CompatUtil.*
import io.iohk.atala.prism.crypto.EC

object Prism14CryptoApollo extends Apollo {

  type PublicKey = Prism14ECPublicKey
  type PrivateKey = Prism14ECPrivateKey

  opaque type Prism14ECPublicKey = io.iohk.atala.prism.crypto.keys.ECPublicKey
  opaque type Prism14ECPrivateKey = io.iohk.atala.prism.crypto.keys.ECPrivateKey

  given ecPublicKey: ECPublicKey[Prism14ECPublicKey] = new ECPublicKey[Prism14ECPublicKey] {
    def curve(curve: EllipticCurve): Try[EllipticCurve] = Try(EllipticCurve.SECP256K1)

    def fromXY(curve: EllipticCurve, x: BigInt, y: BigInt): Try[Prism14ECPublicKey] = Try {
      EC.INSTANCE.toPublicKeyFromBigIntegerCoordinates(x.toKotlinBigInt, y.toKotlinBigInt)
    }

    def toJavaPublicKey(publicKey: Prism14ECPublicKey): java.security.PublicKey = {
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
      keyFactory.generatePublic(ecPublicKeySpec)
    }
  }

  given ecPrivateKey: ECPrivateKey[Prism14ECPrivateKey] = new ECPrivateKey[Prism14ECPrivateKey] {
    def curve(curve: EllipticCurve): Try[EllipticCurve] = Try(EllipticCurve.SECP256K1)

    def fromBytes(curve: EllipticCurve, bytes: Array[Byte]): Try[Prism14ECPrivateKey] = Try {
      EC.INSTANCE.toPrivateKeyFromBytes(bytes)
    }

    def toJavaPrivateKey(privateKey: Prism14ECPrivateKey): java.security.PrivateKey = {
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
      keyFactory.generatePrivate(ecPrivateKeySpec)
    }

    def signBytes(privateKey: Prism14ECPrivateKey, bytes: Array[Byte]): Try[Array[Byte]] = Try {
      val signature = EC.INSTANCE.signBytes(bytes, privateKey)
      signature.getEncoded
    }
  }

  given ecKeyGen: ECKeyGen[Prism14ECPublicKey, Prism14ECPrivateKey] =
    new ECKeyGen[Prism14ECPublicKey, Prism14ECPrivateKey] {
      def generateKeyPair(curve: EllipticCurve): Try[(Prism14ECPublicKey, Prism14ECPrivateKey)] = Try {
        val keyPair = EC.INSTANCE.generateKeyPair()
        (keyPair.getPublicKey, keyPair.getPrivateKey)
      }
    }
}
