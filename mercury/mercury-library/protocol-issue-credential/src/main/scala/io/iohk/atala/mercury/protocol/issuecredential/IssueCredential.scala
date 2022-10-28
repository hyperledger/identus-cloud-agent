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
) {
  assert(`type` == IssueCredential.`type`)

  def makeMessage: Message = Message(
    id = this.id,
    piuri = this.`type`,
    from = Some(this.from),
    to = Some(this.to),
    thid = this.thid,
    body = this.body.asJson.asObject.get,
    attachments = this.attachments,
  )
}

object IssueCredential {

  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/issue-credential"

  final case class Body(
      goal_code: Option[String] = None,
      comment: Option[String] = None,
      replacement_id: Option[String] = None,
      more_available: Option[String] = None,
      formats: Seq[CredentialFormat] = Seq.empty[CredentialFormat],
  )
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
      thid = Some(msg.id),
      from = msg.from.get, // TODO get
      to = msg.from.get, // TODO get
    )
  }
}
