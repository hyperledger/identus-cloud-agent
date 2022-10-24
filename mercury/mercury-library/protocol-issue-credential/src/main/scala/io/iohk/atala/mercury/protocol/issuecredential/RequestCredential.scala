package io.iohk.atala.mercury.protocol.issuecredential
import io.iohk.atala.mercury.model.PIURI
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._
import io.iohk.atala.mercury.model._

final case class RequestCredential(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = RequestCredential.`type`,
    body: RequestCredential.Body,
    attachments: AttachmentDescriptor,
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
    body = ???, // FIXME
    attachments = ??? // FIXME Seq(Attachment(attachments.))
  )
}
object RequestCredential {

  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/request-credential"

  final case class Body(
      goal_code: Option[String],
      comment: Option[String],
      formats: Seq[CredentialFormat]
  )

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

  def readFromMessage(message: Message): RequestCredential = ??? // FIXME

}
