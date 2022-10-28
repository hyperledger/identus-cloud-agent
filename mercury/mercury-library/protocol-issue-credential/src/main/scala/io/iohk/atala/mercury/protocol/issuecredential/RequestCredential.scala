package io.iohk.atala.mercury.protocol.issuecredential

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import io.iohk.atala.mercury.model.PIURI
import io.iohk.atala.mercury.model._

final case class RequestCredential(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = RequestCredential.`type`,
    body: RequestCredential.Body,
    attachments: Seq[AttachmentDescriptor],
    // extra
    thid: Option[String] = None,
    to: DidId,
) {

  def makeMessage(from: DidId): Message = Message(
    id = this.id,
    piuri = this.`type`,
    from = Some(from),
    to = Some(this.to),
    thid = this.thid,
    body = this.body.asJson.asObject.get, // TODO get
    attachments = this.attachments,
  )
}
object RequestCredential {

  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/request-credential"

  final case class Body(
      goal_code: Option[String] = None,
      comment: Option[String] = None,
      formats: Seq[CredentialFormat] = Seq.empty[CredentialFormat]
  )

  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }

  def makeRequestCredentialFromOffer(msg: Message): RequestCredential = {
    val pc: OfferCredential = OfferCredential.readFromMessage(msg)

    RequestCredential(
      body = RequestCredential.Body(
        goal_code = pc.body.goal_code,
        comment = pc.body.comment,
        formats = pc.body.formats,
      ),
      attachments = pc.attachments,
      thid = Some(msg.id),
      to = msg.from.get, // TODO get
    )
  }

  def readFromMessage(message: Message): RequestCredential = // FIXME
    RequestCredential(
      id = message.id,
      `type` = message.piuri,
      body = RequestCredential.Body(
        goal_code = None, // FIXME TODO
        comment = None, // FIXME TODO
        formats = Seq.empty, // FIXME TODO
      ),
      attachments = Seq.empty[AttachmentDescriptor], // FIXME TODO
      thid = message.thid,
      to = message.to.get, // TODO get
    )

}
