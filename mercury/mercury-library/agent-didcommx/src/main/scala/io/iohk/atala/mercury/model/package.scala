package io.iohk.atala.mercury.model

import scala.jdk.CollectionConverters.*

import org.didcommx.didcomm.model._
import io.circe.JsonObject.apply
import io.circe.JsonObject
import io.circe.Json

final case class EncryptedMessageImp(private val msg: PackEncryptedResult) extends EncryptedMessage {
  def string: String = msg.getPackedMessage
}

final case class SignedMessageImp(private val msg: PackSignedResult) extends SignedMesage {
  def string: String = msg.getPackedMessage
}

final case class UnpackMessageImp(private val msg: UnpackResult) extends UnpackMessage {
  def message: Message = {
    val aux = msg.getMessage

    val thisbody = JsonUtilsForDidCommx.fromJavaMapToJson(aux.getBody)

    val attachments: Seq[AttachmentDescriptor] = Option(aux.getAttachments()).toSeq
      .flatMap(_.asScala.toSeq)
      .map(e => e) // using the given Conversion

    Message(
      `type` = aux.getType(),
      from = Option(aux.getFrom()).map(DidId(_)),
      to = Option(aux.getTo()).toSeq
        .map(_.asScala)
        .flatMap(_.toSeq.map(e => DidId(e))),
      body = thisbody,
      id = aux.getId(),
      createdTime = Option(aux.getCreatedTime()),
      expiresTimePlus = Option(aux.getExpiresTime()),
      attachments = Option(attachments),
      thid = Option(aux.getThid()).filter(!_.isEmpty()),
      pthid = Option(aux.getPthid()).filter(!_.isEmpty()),
      ack = Option(aux.getAck()).map(Seq(_)),
    )
  }
}
