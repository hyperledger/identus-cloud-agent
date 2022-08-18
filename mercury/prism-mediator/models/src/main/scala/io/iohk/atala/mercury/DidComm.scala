package io.iohk.atala.mercury

import zio._

import io.iohk.atala.mercury.model._

trait DidComm {
  def packSigned(msg: Message): UIO[SignedMesage]
  def packEncrypted(msg: Message, to: DidId): UIO[EncryptedMessage]
  def unpack(str: String): UIO[UnpackMesage]
  def unpackBase64(base64str: String): UIO[UnpackMesage] // FIXME TODO Make this a Overloading method
}

object DidComm {

  def packSigned(msg: Message): URIO[DidComm, SignedMesage] =
    ZIO.serviceWithZIO(_.packSigned(msg))

  def packEncrypted(msg: Message, to: DidId): URIO[DidComm, EncryptedMessage] =
    ZIO.serviceWithZIO(_.packEncrypted(msg, to))

  def unpack(str: String): URIO[DidComm, UnpackMesage] =
    ZIO.serviceWithZIO(_.unpack(str))

  def unpackBase64(base64str: String): URIO[DidComm, UnpackMesage] =
    ZIO.serviceWithZIO(_.unpackBase64(base64str))

}
