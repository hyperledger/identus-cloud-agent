package io.iohk.atala

import zio._

import io.iohk.atala.model._

//TODO RENAME package io.iohk.atala to io.iohk.atala.mercury (everywhere)
//TODO RENAME to DidComm only
trait DIDCommService {
  def packSigned(msg: Message): UIO[SignedMesage]
  def packEncrypted(msg: Message, to: String): UIO[EncryptedMessage]
  def unpack(str: String): UIO[UnpackMesage]
  def unpackBase64(base64str: String): UIO[UnpackMesage] // FIXME TODO Make this a Overloading method
}

object DIDCommService {
  def packSigned(msg: Message): URIO[DIDCommService, SignedMesage] =
    ZIO.serviceWithZIO(_.packSigned(msg))

  def packEncrypted(msg: Message, to: String): URIO[DIDCommService, EncryptedMessage] =
    ZIO.serviceWithZIO(_.packEncrypted(msg, to))

  def unpack(str: String): URIO[DIDCommService, UnpackMesage] =
    ZIO.serviceWithZIO(_.unpack(str))

  def unpackBase64(base64str: String): URIO[DIDCommService, UnpackMesage] =
    ZIO.serviceWithZIO(_.unpack(base64str))

}
