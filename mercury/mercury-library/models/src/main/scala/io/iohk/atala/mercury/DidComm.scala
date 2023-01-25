package io.iohk.atala.mercury

import zio._

import io.iohk.atala.mercury.model._
import java.util.Base64
import scala.util.Try
import scala.util.Failure
import scala.util.Success

trait DidComm {
  def myDid: DidId // TODO
  def packSigned(msg: Message): UIO[SignedMesage]
  def packEncrypted(msg: Message, to: DidId): UIO[EncryptedMessage]
  def packEncryptedAnon(msg: Message, to: DidId): UIO[EncryptedMessage]
  def unpack(str: String): UIO[UnpackMessage]
  def unpackBase64(dataBase64: String): Task[UnpackMessage] = {
    val mDecoder = Option(Base64.getUrlDecoder) // can be null
    Try(mDecoder.map(_.decode(dataBase64))) match
      case Failure(ex: IllegalArgumentException) => ZIO.fail(ex)
      case Failure(ex)                           => ZIO.fail(ex)
      case Success(None)                         => ZIO.fail(new NullPointerException())
      case Success(Some(data))                   => unpack(new String(data))
  }
}

object DidComm {

  def packSigned(msg: Message): URIO[DidComm, SignedMesage] =
    ZIO.serviceWithZIO(_.packSigned(msg))

  def packEncrypted(msg: Message, to: DidId): URIO[DidComm, EncryptedMessage] =
    ZIO.serviceWithZIO(_.packEncrypted(msg, to))

  def packEncryptedAnon(msg: Message, to: DidId): URIO[DidComm, EncryptedMessage] =
    ZIO.serviceWithZIO(_.packEncrypted(msg, to))

  def unpack(str: String): URIO[DidComm, UnpackMessage] =
    ZIO.serviceWithZIO(_.unpack(str))

  def unpackBase64(base64str: String): RIO[DidComm, UnpackMessage] =
    ZIO.serviceWithZIO(_.unpackBase64(base64str))

}
