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
final case class ProposeCredential(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = ProposeCredential.`type`,
    body: ProposeCredential.Body,
    attachments: Seq[AttachmentDescriptor] = Seq.empty[AttachmentDescriptor]
) {
  assert(`type` == ProposeCredential.`type`)

  def makeMessage(from: DidId, sendTo: DidId): Message = Message(
    piuri = this.`type`,
    from = Some(from),
    to = Some(sendTo),
    body = this.body.asJson.asObject.get // FIXME
    // attachments = ??? // FIXME
  )
}

object ProposeCredential {
  // TODD will this be version RCF Issue Credential 2.0  as we use didcomm2 message format
  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/propose-credential"

  import AttachmentDescriptor.attachmentDescriptorEncoderV2
  given Encoder[ProposeCredential] = deriveEncoder[ProposeCredential]
  given Decoder[ProposeCredential] = deriveDecoder[ProposeCredential]

  /** @param goal_code
    * @param comment
    * @param credential_preview
    * @param formats
    */
  final case class Body(
      goal_code: Option[String] = None,
      comment: Option[String] = None,
      credential_preview: CredentialPreview, // JSON STRinf
      formats: Seq[CredentialFormat] = Seq.empty[CredentialFormat]
  )

  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }

  def readFromMessage(message: Message): ProposeCredential = {
    val body = message.body.asJson.as[ProposeCredential.Body].toOption.get // TODO get

    ProposeCredential(
      id = message.id,
      `type` = message.piuri,
      body = body,
      attachments = Seq.empty[AttachmentDescriptor] // FIXME TODO
    )
  }

}
