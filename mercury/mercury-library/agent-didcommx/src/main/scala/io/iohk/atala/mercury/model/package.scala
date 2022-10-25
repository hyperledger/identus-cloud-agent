package io.iohk.atala.mercury.model

import scala.jdk.CollectionConverters.*

import org.didcommx.didcomm.model._

final case class EncryptedMessageImp(private val msg: PackEncryptedResult) extends EncryptedMessage {
  def string: String = msg.getPackedMessage
}

final case class SignedMessageImp(private val msg: PackSignedResult) extends SignedMesage {
  def string: String = msg.getPackedMessage
}

final case class UnpackMessageImp(private val msg: UnpackResult) extends UnpackMessage {
  def message: Message = { // FIXME TODO
    val aux = msg.getMessage
    Message(
      piuri = aux.getType(),
      from = Some(DidId(aux.getFrom())), // FIXME some ... and none
      to = aux.getTo().asScala.toSeq.map(e => DidId(e)).headOption, // FIXME to need to be a Seq
      body = Map.empty, // FIXME TODO
      id = aux.getId(),
      createdTime = aux.getCreatedTime(),
      expiresTimePlus = aux.getExpiresTime(),
      attachments = Seq.empty, // FIXME aux.getAttachments(),
      thid = Option(aux.getThid()).filter(!_.isEmpty()),
      pthid = Option(aux.getPthid()).filter(!_.isEmpty()),
      ack = Option(aux.getAck()).toSeq.filter(!_.isEmpty()),
    )
  }
}
