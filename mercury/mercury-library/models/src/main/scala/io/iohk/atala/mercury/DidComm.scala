package io.iohk.atala.mercury

import zio._

import io.iohk.atala.mercury.model._
import java.util.Base64

trait DidComm {
  def myDid: DidId // TODO
  def packSigned(msg: Message): UIO[SignedMesage]
  def packEncrypted(msg: Message, to: DidId): UIO[EncryptedMessage]
  def unpack(str: String): UIO[UnpackMessage]
  def unpackBase64(dataBase64: String): UIO[UnpackMessage] = {
    val data = new String(Base64.getUrlDecoder.decode(dataBase64))
    unpack(data)
    // ZIO.succeed(didComm.unpack(new UnpackParams.Builder(data).build()))
  }
}

object DidComm {

  def packSigned(msg: Message): URIO[DidComm, SignedMesage] =
    ZIO.serviceWithZIO(_.packSigned(msg))

  def packEncrypted(msg: Message, to: DidId): URIO[DidComm, EncryptedMessage] =
    ZIO.serviceWithZIO(_.packEncrypted(msg, to))

  def unpack(str: String): URIO[DidComm, UnpackMessage] =
    ZIO.serviceWithZIO(_.unpack(str))

  def unpackBase64(base64str: String): URIO[DidComm, UnpackMessage] =
    ZIO.serviceWithZIO(_.unpackBase64(base64str))

}
