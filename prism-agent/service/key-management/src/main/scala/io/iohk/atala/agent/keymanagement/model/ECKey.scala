package io.iohk.atala.agent.keymanagement.model

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.util.ConversionUtilsKt
import io.iohk.atala.agent.keymanagement.model.ECCoordinates.ECCoordinate
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.prism.crypto as prismcrypto

import scala.collection.immutable.ArraySeq

object ECCoordinates {

  // Uses prism-crypto ECCoordinate under the hood in order to reuse
  // existing functionalities, but it only supports secp256k1 curve.
  // Once Apollo is integrated the underlying type should be updated.
  opaque type ECCoordinate = prismcrypto.keys.ECCoordinate

  object ECCoordinate {
    def fromBigInt(i: BigInt): ECCoordinate = prismcrypto.keys
      .ECCoordinate(ConversionUtilsKt.fromTwosComplementByteArray(BigInteger.Companion, i.toByteArray))
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

final case class ECPrivateKey(n: ArraySeq[Byte]) extends AnyVal

final case class ECKeyPair(publicKey: ECPublicKey, privateKey: ECPrivateKey)
