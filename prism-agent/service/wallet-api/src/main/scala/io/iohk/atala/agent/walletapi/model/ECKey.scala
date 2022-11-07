package io.iohk.atala.agent.walletapi.model

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.util.ConversionUtilsKt
import io.iohk.atala.agent.walletapi.model.ECCoordinates.ECCoordinate
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.prism.crypto as prismcrypto
import io.iohk.atala.agent.walletapi.util.Prism14CompatUtils.*

import scala.collection.immutable.ArraySeq

object ECCoordinates {

  // Uses prism-crypto ECCoordinate under the hood in order to reuse
  // existing padding functionality, but it only supports secp256k1 curve.
  // When more elliptic-curves support are added, it should be migrated to BigInt or Apollo primitive.
  opaque type ECCoordinate = prismcrypto.keys.ECCoordinate

  object ECCoordinate {
    def fromBigInt(i: BigInt): ECCoordinate = prismcrypto.keys.ECCoordinate(i.toKotlinBigInt)
  }

  extension (c: ECCoordinate) {
    def toPaddedByteArray(curve: EllipticCurve): Array[Byte] = {
      curve match {
        case EllipticCurve.SECP256K1 => c.bytes() // prism-crypto pads with "secp256k1" bytes size
      }
    }
    def toBigInt: BigInt = c.getCoordinate.toScalaBigInt
  }

}

final case class ECPoint(x: ECCoordinate, y: ECCoordinate)

final case class ECPublicKey(p: ECPoint) extends AnyVal {
  def toEncoded(curve: EllipticCurve): Array[Byte] = {
    curve match {
      case EllipticCurve.SECP256K1 =>
        /** Guarantees to return a list of 65 bytes in the following form:
          *
          * 0x04 ++ xBytes ++ yBytes
          *
          * Where `xBytes` and `yBytes` represent a 32-byte coordinates of a point on the secp256k1 elliptic curve,
          * which follow the formula below:
          *
          * y^2 == x^3 + 7
          *
          * @return
          *   a list of 65 bytes that represent uncompressed public key
          */
        val x = p.x.toBigInt.toKotlinBigInt
        val y = p.y.toBigInt.toKotlinBigInt
        io.iohk.atala.prism.crypto.EC.INSTANCE.toPublicKeyFromBigIntegerCoordinates(x, y).getEncoded
    }
  }
}

// Internal data should be BigInt and padded according to the private-key byte-size
// when converting to bytearray. For the time being, this only wraps the
// secp256k1 private-key bytes created from prism-crypto in order to reuse padding functionality.
// When more elliptic-curves support are added, it should be migrated to BigInt or Apollo primitive.
final case class ECPrivateKey private[walletapi] (n: ArraySeq[Byte]) extends AnyVal {
  def toPaddedByteArray(curve: EllipticCurve): Array[Byte] = {
    curve match {
      case EllipticCurve.SECP256K1 => n.toArray
    }
  }
}

final case class ECKeyPair(publicKey: ECPublicKey, privateKey: ECPrivateKey)

object ECKeyPair {
  private[walletapi] def fromPrism14ECKeyPair(keyPair: io.iohk.atala.prism.crypto.keys.ECKeyPair): ECKeyPair = {
    val publicKeyPoint = keyPair.getPublicKey.getCurvePoint
    val privateKey = keyPair.getPrivateKey.getEncoded
    ECKeyPair(
      publicKey = ECPublicKey(
        p = ECPoint(
          x = ECCoordinate.fromBigInt(publicKeyPoint.getX.getCoordinate.toScalaBigInt),
          y = ECCoordinate.fromBigInt(publicKeyPoint.getY.getCoordinate.toScalaBigInt)
        )
      ),
      privateKey = ECPrivateKey(
        n = ArraySeq.from(privateKey)
      )
    )
  }

}

object ECSignatures {

  opaque type ECSignature = prismcrypto.signature.ECSignature

  object ECSignature {
    def fromPrism14Signature(signature: io.iohk.atala.prism.crypto.signature.ECSignature): ECSignature = signature
  }

  extension (s: ECSignature) {
    def toByteArray: Array[Byte] = s.getData
  }

}
