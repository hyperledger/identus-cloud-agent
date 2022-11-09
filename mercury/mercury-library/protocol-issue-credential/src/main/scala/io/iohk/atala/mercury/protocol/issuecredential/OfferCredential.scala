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
  * @param thid
  *   needs to be used need replying
  */
final case class OfferCredential(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = OfferCredential.`type`,
    body: OfferCredential.Body,
    attachments: Seq[AttachmentDescriptor],
    // extra
    thid: Option[String] = None,
    from: DidId,
    to: DidId,
) {
  assert(`type` == OfferCredential.`type`)

  def makeMessage: Message = Message(
    id = this.id,
    piuri = this.`type`,
    from = Some(this.from),
    to = Some(to),
    thid = this.thid,
    body = this.body.asJson.asObject.get, // TODO get
    attachments = this.attachments,
  )
}

object OfferCredential {

  import AttachmentDescriptor.attachmentDescriptorEncoderV2

  given Encoder[OfferCredential] = deriveEncoder[OfferCredential]

  given Decoder[OfferCredential] = deriveDecoder[OfferCredential]

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

  def makeOfferToProposeCredential(msg: Message): OfferCredential = { // TODO change msg: Message to ProposeCredential
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
      thid = msg.thid.orElse(Some(pc.id)),
      from = pc.to,
      to = pc.from,
    )
  }

  def readFromMessage(message: Message): OfferCredential = {
    val body = message.body.asJson.as[OfferCredential.Body].toOption.get // TODO get
    OfferCredential(
      id = message.id,
      `type` = message.piuri,
      body = body,
      attachments = message.attachments,
      thid = message.thid,
      from = message.from.get, // TODO get
      to = message.to.get, // TODO get
    )
  }
}
