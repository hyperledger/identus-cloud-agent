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
    replyingThid: Option[String] = None,
    replyingTo: Option[DidId] = None,
) {

  def makeMessage(from: DidId): Message = Message(
    id = this.id,
    piuri = this.`type`,
    from = Some(from),
    to = replyingTo,
    thid = replyingThid,
    body = this.body.asJson.asObject.get, // FIXME TODO
    attachments = Seq.empty, // FIXME Seq(Attachment(attachments.))
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
    makeRequestCredentialFromOffer(pc, Some(msg.id), msg.from)
  }

  def makeRequestCredentialFromOffer(
      pc: OfferCredential,
      replyingThid: Option[String],
      replyingTo: Option[DidId]
  ): RequestCredential = {

    RequestCredential(
      body = RequestCredential.Body(
        goal_code = pc.body.goal_code,
        comment = pc.body.comment,
        formats = pc.body.formats,
      ),
      attachments = pc.attachments,
      replyingThid = replyingThid,
      replyingTo = replyingTo,
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
      attachments = Seq.empty[AttachmentDescriptor] // FIXME TODO
    )

}
