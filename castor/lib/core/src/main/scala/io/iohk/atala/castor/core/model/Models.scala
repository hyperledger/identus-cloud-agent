package io.iohk.atala.castor.core.model

import io.iohk.atala.castor.core.model.did.{
  DIDDocument,
  EllipticCurve,
  PublicKey,
  PublicKeyJwk,
  DIDStorage,
  Service,
  ServiceType,
  VerificationRelationship
}
import io.iohk.atala.prism.crypto.util.BytesOps

import java.net.URI
import scala.util.Try

// TODO: replace with actual implementation
final case class IrisNotification(foo: String)

object HexStrings {

  opaque type HexString = String

  object HexString {
    def fromString(s: String): Option[HexString] = Try(BytesOps.INSTANCE.hexToBytes(s)).toOption.map(_ => s)
    def fromByteArray(bytes: Array[Byte]): HexString = BytesOps.INSTANCE.bytesToHex(bytes)
  }

  extension (s: HexString) {
    def toByteArray: Array[Byte] = BytesOps.INSTANCE.hexToBytes(s)
  }

}
