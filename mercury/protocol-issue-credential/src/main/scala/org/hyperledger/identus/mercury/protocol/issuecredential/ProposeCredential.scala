package org.hyperledger.identus.mercury.protocol.issuecredential

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.hyperledger.identus.mercury.model.{AttachmentDescriptor, DidId, Message, PIURI}

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
    attachments: Seq[AttachmentDescriptor] = Seq.empty[AttachmentDescriptor],
    // extra
    thid: Option[String] = None,
    from: DidId,
    to: DidId,
) extends ReadAttachmentsUtils {
  assert(`type` == ProposeCredential.`type`)

  def makeMessage: Message = Message(
    `type` = this.`type`,
    from = Some(this.from),
    to = Seq(this.to),
    body = this.body.asJson.asObject.get, // TODO get
    attachments = Some(this.attachments)
  )
}

object ProposeCredential {
  // TODD will this be version RCF Issue Credential 2.0  as we use didcomm2 message format
  def `type`: PIURI = "https://didcomm.org/issue-credential/3.0/propose-credential"

  def build(
      fromDID: DidId,
      toDID: DidId,
      thid: Option[String] = None,
      goal_code: Option[String] = None,
      comment: Option[String] = None,
      credential_preview: Option[CredentialPreview] = None,
      credentials: Seq[(IssueCredentialProposeFormat, Array[Byte])] = Seq.empty,
  ): ProposeCredential = {
    ProposeCredential(
      thid = thid,
      from = fromDID,
      to = toDID,
      body = Body(goal_code = goal_code, comment = comment, credential_preview = credential_preview),
      attachments = credentials.map { case (format, singleCredential) =>
        AttachmentDescriptor.buildBase64Attachment(payload = singleCredential, format = Some(format.name))
      }.toSeq
    )
  }

  import AttachmentDescriptor.attachmentDescriptorEncoderV2
  given Encoder[ProposeCredential] = deriveEncoder[ProposeCredential]
  given Decoder[ProposeCredential] = deriveDecoder[ProposeCredential]

  /** @param goal_code
    * @param comment
    * @param credential_preview
    *   JSON-LD object that represents the credential data that Issuer is willing to issue.
    */
  final case class Body(
      goal_code: Option[String] = None,
      comment: Option[String] = None,
      credential_preview: Option[CredentialPreview] = None, // JSON string
  )

  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }

  def readFromMessage(message: Message): Either[String, ProposeCredential] = {
    message.body.asJson.as[ProposeCredential.Body] match
      case Left(fail) => Left("Fail to parse ProposeCredential's body: " + fail.getMessage)
      case Right(body) =>
        message.from match
          case None => Left("ProposeCredential MUST have the sender explicit")
          case Some(from) =>
            message.to match
              case firstTo +: Seq() =>
                Right(
                  ProposeCredential(
                    id = message.id,
                    `type` = message.piuri,
                    body = body,
                    attachments = message.attachments.getOrElse(Seq.empty),
                    from = from,
                    to = firstTo,
                  )
                )
              case tos => Left(s"ProposeCredential MUST have only 1 recipient instead has '${tos}'")
  }

}
