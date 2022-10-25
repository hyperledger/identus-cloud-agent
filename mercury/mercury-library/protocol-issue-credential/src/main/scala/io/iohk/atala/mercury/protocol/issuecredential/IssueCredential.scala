package io.iohk.atala.mercury.protocol.issuecredential

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
final case class IssueCredential(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = IssueCredential.`type`,
    body: IssueCredential.Body,
    attachments:  Seq[AttachmentDescriptor],
    // extra
    replyingThid: Option[String] = None,
    replyingTo: Option[DidId] = None,
) {
  assert(`type` == IssueCredential.`type`)

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

object IssueCredential {

  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/issue-credential"

  final case class Body(
      goal_code: Option[String],
      comment: Option[String],
      replacement_id: Option[String],
      multiple_available: Option[String],
      credential_preview: Option[CredentialPreview],
      formats: Seq[CredentialFormat],
  )

  def makeIssueCredentialFromRequestCredential(msg: Message): IssueCredential = {
    val rc: RequestCredential = RequestCredential.readFromMessage(msg)

    IssueCredential(
      body = IssueCredential.Body(
        goal_code = rc.body.goal_code,
        comment = rc.body.comment,
        replacement_id = None,
        multiple_available = None,
        credential_preview = None,
        formats = rc.body.formats,
      ),
      attachments = rc.attachments,
      replyingThid = Some(msg.id),
      replyingTo = msg.from,
    )
  }
}
