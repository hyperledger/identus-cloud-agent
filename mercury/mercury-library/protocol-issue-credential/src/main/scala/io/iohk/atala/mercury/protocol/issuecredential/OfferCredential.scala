package io.iohk.atala.mercury.protocol.issuecredential

import io.iohk.atala.mercury.model.PIURI
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._
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
    `type`: PIURI = ProposeCredential.`type`,
    body: OfferCredential.Body,
    attachments:  Seq[AttachmentDescriptor],
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
    body = ???, // FIXME
    attachments = ??? // FIXME Seq(Attachment(attachments.))
  )
}

object OfferCredential {

  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/offer-credential"

  final case class Body(
      goal_code: Option[String],
      comment: Option[String],
      replacement_id: Option[String],
      multiple_available: Option[String],
      credential_preview: Option[CredentialPreview],
      formats: Seq[CredentialFormat]
  )

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

  def readFromMessage(message: Message): OfferCredential = ??? // FIXME
}
