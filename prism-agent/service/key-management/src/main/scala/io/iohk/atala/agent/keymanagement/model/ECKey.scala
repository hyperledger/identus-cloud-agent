package io.iohk.atala.agent.keymanagement.model

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.util.ConversionUtilsKt
import io.iohk.atala.agent.keymanagement.model.ECCoordinates.ECCoordinate
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.prism.crypto as prismcrypto
import io.iohk.atala.agent.keymanagement.util.Prism14CompatUtil.*

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
  }

}

final case class ECPoint(x: ECCoordinate, y: ECCoordinate)

final case class ECPublicKey(p: ECPoint) extends AnyVal

// Internal data should be BigInt and padded according to the private-key byte-size
// when converting to bytearray. For the time being, this only wraps the
// secp256k1 private-key bytes created from prism-crypto in order to reuse padding functionality.
// When more elliptic-curves support are added, it should be migrated to BigInt or Apollo primitive.
final case class ECPrivateKey private[keymanagement] (n: ArraySeq[Byte]) extends AnyVal {
  def toPaddedByteArray(curve: EllipticCurve): Array[Byte] = {
    curve match {
      case EllipticCurve.SECP256K1 => n.toArray
    }
  }
}

final case class ECKeyPair(publicKey: ECPublicKey, privateKey: ECPrivateKey)

object ECKeyPair {
  private[keymanagement] def fromPrism14ECKeyPair(keyPair: io.iohk.atala.prism.crypto.keys.ECKeyPair): ECKeyPair = {
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
