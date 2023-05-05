package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.castor.core.model.did.EllipticCurve
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import io.iohk.atala.agent.walletapi.util.Prism14CompatUtil.*

import java.security.KeyFactory
import java.security.spec.{ECPrivateKeySpec, ECPublicKeySpec}
import scala.util.Try

opaque type ECPublicKey2 = java.security.PublicKey

object ECPublicKey2 {
  private[walletapi] def fromPrism14(publicKey: io.iohk.atala.prism.crypto.keys.ECPublicKey): Try[ECPublicKey2] = {
    Try {
      val curvePoint = publicKey.getCurvePoint
      val x = curvePoint.getX.getCoordinate.toScalaBigInt
      val y = curvePoint.getY.getCoordinate.toScalaBigInt
      (x, y)
    }.flatMap { case (x, y) =>
      val curve = EllipticCurve.SECP256K1 // prism14 only uses this curve
      fromECPoint(curve, x, y)
    }
  }

  private[walletapi] def fromECPoint(curve: EllipticCurve, x: BigInt, y: BigInt): Try[ECPublicKey2] = Try {
    val keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider())
    val ecParameterSpec = ECNamedCurveTable.getParameterSpec(curve.name)
    val ecNamedCurveSpec = ECNamedCurveSpec(
      ecParameterSpec.getName(),
      ecParameterSpec.getCurve(),
      ecParameterSpec.getG(),
      ecParameterSpec.getN()
    )
    val ecPublicKeySpec = ECPublicKeySpec(java.security.spec.ECPoint(x.bigInteger, y.bigInteger), ecNamedCurveSpec)
    keyFactory.generatePublic(ecPublicKeySpec)
  }

  extension (k: ECPublicKey2) {
    def toJavaPublicKey: java.security.PublicKey = k
  }
}

opaque type ECPrivateKey2 = java.security.PrivateKey

object ECPrivateKey2 {
  private[walletapi] def fromPrism14(privateKey: io.iohk.atala.prism.crypto.keys.ECPrivateKey): Try[ECPrivateKey2] = {
    Try(privateKey.getD.toByteArray).flatMap { bytes => fromECPrivateKey(EllipticCurve.SECP256K1, bytes) }
  }

  private[walletapi] def fromECPrivateKey(curve: EllipticCurve, bytes: Array[Byte]): Try[ECPrivateKey2] = Try {
    val keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider())
    val ecParameterSpec = ECNamedCurveTable.getParameterSpec(curve.name)
    val ecNamedCurveSpec = ECNamedCurveSpec(
      ecParameterSpec.getName(),
      ecParameterSpec.getCurve(),
      ecParameterSpec.getG(),
      ecParameterSpec.getN()
    )
    val ecPrivateKeySpec = ECPrivateKeySpec(java.math.BigInteger(1, bytes), ecNamedCurveSpec)
    keyFactory.generatePrivate(ecPrivateKeySpec)
  }

  extension (k: ECPrivateKey2) {
    def toJavaPrivateKey: java.security.PrivateKey = k
  }
}

final case class ECKeyPair2(publicKey: ECPublicKey2, privateKey: ECPrivateKey2)

object ECKeyPair2 {
  private[walletapi] def fromPrism14(keyPair: io.iohk.atala.prism.crypto.keys.ECKeyPair): Try[ECKeyPair2] =
    for {
      pub <- ECPublicKey2.fromPrism14(keyPair.getPublicKey)
      priv <- ECPrivateKey2.fromPrism14(keyPair.getPrivateKey)
    } yield ECKeyPair2(publicKey = pub, privateKey = priv)
}
