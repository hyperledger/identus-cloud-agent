package io.iohk.atala.mercury.protocol.issuecredential

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import io.iohk.atala.mercury.model.PIURI
import io.iohk.atala.mercury.model._

/** ALL parameterS are DIDCOMMV2 format and naming conventions and follows the protocol
  * @see
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2
  *
  * @param id
  * @param `type`
  * @param body
  * @param attachments
  */
final case class OfferCredential(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = OfferCredential.`type`,
    body: OfferCredential.Body,
    attachments: Seq[AttachmentDescriptor],
    // extra
    replyingThid: Option[String] = None,
    replyingTo: Option[DidId] = None,
) {
  assert(`type` == OfferCredential.`type`)

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

object OfferCredential {

  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/offer-credential"

  final case class Body(
      goal_code: Option[String] = None,
      comment: Option[String] = None,
      replacement_id: Option[String] = None,
      multiple_available: Option[String] = None,
      credential_preview: CredentialPreview,
      formats: Seq[CredentialFormat] = Seq.empty[CredentialFormat]
  )

  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }

  def makeOfferToProposeCredential(msg: Message): OfferCredential = {
    val pc: ProposeCredential = ProposeCredential.readFromMessage(msg)

    OfferCredential(
      body = OfferCredential.Body(
        goal_code = pc.body.goal_code,
        comment = pc.body.comment,
        replacement_id = None,
        multiple_available = None,
        credential_preview = pc.body.credential_preview,
        formats = pc.body.formats,
      ),
      attachments = pc.attachments,
      replyingThid = Some(msg.id),
      replyingTo = msg.from,
    )
  }

  def readFromMessage(message: Message): OfferCredential = // FIXME
    OfferCredential(
      id = message.id,
      `type` = message.piuri,
      body = OfferCredential.Body(
        goal_code = None, // FIXME TODO
        comment = None, // FIXME TODO
        credential_preview = CredentialPreview(attributes = Seq.empty), // FIXME TODO
        formats = Seq.empty, // FIXME TODO
      ),
      attachments = Seq.empty[AttachmentDescriptor] // FIXME TODO
    )
}
