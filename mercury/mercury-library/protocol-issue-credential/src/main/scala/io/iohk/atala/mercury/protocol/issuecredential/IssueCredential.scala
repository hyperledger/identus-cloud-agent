package io.iohk.atala.mercury.protocol.issuecredential

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

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
final case class IssueCredential(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = IssueCredential.`type`,
    body: IssueCredential.Body,
    attachments: Seq[AttachmentDescriptor],
    // extra
    thid: Option[String] = None,
    from: DidId,
    to: DidId,
) extends ReadAttachmentsUtils {
  assert(`type` == IssueCredential.`type`)

  def makeMessage: Message = Message(
    id = this.id,
    `type` = this.`type`,
    from = Some(this.from),
    to = Seq(this.to),
    thid = this.thid,
    body = this.body.asJson.asObject.get,
    attachments = Some(this.attachments),
  )
}

object IssueCredential {

  import AttachmentDescriptor.attachmentDescriptorEncoderV2
  given Encoder[IssueCredential] = deriveEncoder[IssueCredential]
  given Decoder[IssueCredential] = deriveDecoder[IssueCredential]

  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/issue-credential"

  def build(
      fromDID: DidId,
      toDID: DidId,
      thid: Option[String] = None,
      credentials: Map[String, Array[Byte]],
  ): IssueCredential = {
    val aux = credentials.map { case (formatName, singleCredential) =>
      val attachment = AttachmentDescriptor.buildBase64Attachment(payload = singleCredential)
      val credentialFormat: CredentialFormat = CredentialFormat(attachment.id, formatName)
      (credentialFormat, attachment)
    }
    IssueCredential(
      thid = thid,
      from = fromDID,
      to = toDID,
      body = Body(formats = aux.keys.toSeq),
      attachments = aux.values.toSeq
    )
  }
  final case class Body(
      goal_code: Option[String] = None,
      comment: Option[String] = None,
      replacement_id: Option[String] = None,
      more_available: Option[String] = None,
      formats: Seq[CredentialFormat] = Seq.empty[CredentialFormat],
  ) extends BodyUtils
  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }

  def makeIssueCredentialFromRequestCredential(msg: Message): IssueCredential = {
    val rc: RequestCredential = RequestCredential.readFromMessage(msg)

    IssueCredential(
      body = IssueCredential.Body(
        goal_code = rc.body.goal_code,
        comment = rc.body.comment,
        replacement_id = None,
        more_available = None,
        formats = rc.body.formats,
      ),
      attachments = rc.attachments,
      thid = msg.thid.orElse(Some(rc.id)),
      from = rc.to,
      to = rc.from,
    )
  }

  def readFromMessage(message: Message): IssueCredential = {
    val body = message.body.asJson.as[IssueCredential.Body].toOption.get // TODO get
    IssueCredential(
      id = message.id,
      `type` = message.piuri,
      body = body,
      attachments = message.attachments.getOrElse(Seq.empty),
      thid = message.thid,
      from = message.from.get, // TODO get
      to = {
        assert(message.to.length == 1, "The recipient is ambiguous. Need to have only 1 recipient") // TODO return error
        message.to.head
      },
    )
  }
}
