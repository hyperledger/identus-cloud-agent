package io.iohk.atala.agent.custodian.model

import io.iohk.atala.agent.custodian.model.ECCoordinates.ECCoordinate

import scala.collection.immutable.ArraySeq

object ECCoordinates {

  opaque type ECCoordinate = BigInt

  object ECCoordinate {
    def fromBigInt(i: BigInt): ECCoordinate = i
  }

}

final case class ECPoint(x: ECCoordinate, y: ECCoordinate)

final case class ECPublicKey(p: ECPoint) extends AnyVal

final case class ECPrivateKey(n: ArraySeq[Byte]) extends AnyVal

final case class ECKeyPair(publicKey: ECPublicKey, privateKey: ECPrivateKey)
