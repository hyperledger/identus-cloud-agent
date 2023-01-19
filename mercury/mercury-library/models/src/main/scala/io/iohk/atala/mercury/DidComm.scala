package io.iohk.atala.mercury

import zio._

import io.iohk.atala.mercury.model._
import java.util.Base64
import scala.util.Try
import scala.util.Failure
import scala.util.Success

trait DidAgent {
  def id: DidId
}

trait DidOps {
  def packSigned(msg: Message): URIO[DidAgent, SignedMesage]
  def packEncrypted(msg: Message, to: DidId): URIO[DidAgent, EncryptedMessage]
  def packEncryptedAnon(msg: Message, to: DidId): UIO[EncryptedMessage]
  def unpack(str: String): URIO[DidAgent, UnpackMessage]
  def unpackBase64(dataBase64: String): RIO[DidAgent, UnpackMessage] = {
    val mDecoder = Option(Base64.getUrlDecoder) // can be null
    Try(mDecoder.map(_.decode(dataBase64))) match
      case Failure(ex: IllegalArgumentException) => ZIO.fail(ex)
      case Failure(ex)                           => ZIO.fail(ex)
      case Success(None)                         => ZIO.fail(new NullPointerException())
      case Success(Some(data))                   => unpack(new String(data))
  }
}

object DidOps {

  def packSigned(msg: Message): URIO[DidOps & DidAgent, SignedMesage] =
    ZIO.service[DidOps].flatMap(_.packSigned(msg))
    // ZIO.serviceWithZIO(_.packSigned(msg))

  def packEncrypted(msg: Message, to: DidId): URIO[DidOps & DidAgent, EncryptedMessage] =
    ZIO.service[DidOps].flatMap(_.packEncrypted(msg, to))
    // ZIO.serviceWithZIO(_.packEncrypted(msg, to))

  def packEncryptedAnon(msg: Message, to: DidId): URIO[DidOps, EncryptedMessage] =
    ZIO.serviceWithZIO(_.packEncryptedAnon(msg, to))

  def unpack(str: String): URIO[DidOps & DidAgent, UnpackMessage] =
    ZIO.service[DidOps].flatMap(_.unpack(str))
    // ZIO.serviceWithZIO(_.unpack(str))

  def unpackBase64(base64str: String): RIO[DidOps & DidAgent, UnpackMessage] =
    ZIO.service[DidOps].flatMap(_.unpackBase64(base64str))
  // ZIO.serviceWithZIO(_.unpackBase64(base64str))

}
