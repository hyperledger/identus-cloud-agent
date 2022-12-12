package io.iohk.atala.mercury.protocol.presentproof

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.iohk.atala.mercury.model.{AttachmentDescriptor, DidId, Message, PIURI}
import io.iohk.atala.mercury.model.AttachmentDescriptor.attachmentDescriptorEncoderV2
import io.circe.syntax._

/** @param attach_id
  * @param format
  *   know Format:
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2#propose-attachment-registry
  *   - dif/credential-manifest@v1.0
  *   - aries/ld-proof-vc-detail@v1.0
  *   - hlindy/cred-filter@v2.0
  */
final case class PresentationFormat(attach_id: String, format: String)

object PresentationFormat {
  given Encoder[PresentationFormat] = deriveEncoder[PresentationFormat]
  given Decoder[PresentationFormat] = deriveDecoder[PresentationFormat]
}

/** ALL parameterS are DIDCOMMV2 format and naming conventions and follows the protocol
  * @see
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0454-present-proof-v2
  *
  * @param id
  * @param `type`
  * @param body
  * @param attachments
  */
final case class Presentation(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = Presentation.`type`,
    body: Presentation.Body,
    attachments: Seq[AttachmentDescriptor] = Seq.empty[AttachmentDescriptor],
    // extra
    thid: Option[String] = None,
    from: DidId,
    to: DidId,
) {
  assert(`type` == Presentation.`type`)

  def makeMessage: Message = Message(
    piuri = this.`type`,
    from = Some(this.from),
    to = Seq(this.to),
    thid = this.thid,
    body = this.body.asJson.asObject.get, // TODO get
    attachments = Some(this.attachments)
  )
}

object Presentation {
  // def `type`: PIURI = "https://didcomm.org/present-proof/3.0/presentation"
  def `type`: PIURI = "https://didcomm.atalaprism.io/present-proof/3.0/presentation"

  import AttachmentDescriptor.attachmentDescriptorEncoderV2
  given Encoder[Presentation] = deriveEncoder[Presentation]
  given Decoder[Presentation] = deriveDecoder[Presentation]

  /** @param goal_code
    * @param comment
    * @param formats
    */
  final case class Body(
      goal_code: Option[String] = None,
      comment: Option[String] = None,
  )

  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }

  def makePresentationFromRequest(msg: Message): Presentation = { // TODO change msg: Message to RequestCredential
    val rp: RequestPresentation = RequestPresentation.readFromMessage(msg)

    Presentation(
      body = Presentation.Body(
        goal_code = rp.body.goal_code,
        comment = rp.body.comment,
      ),
      attachments = rp.attachments,
      thid = msg.thid,
      from = {
        assert(msg.to.length == 1, "The recipient is ambiguous. Need to have only 1 recipient") // TODO return error
        msg.to.head
      },
      to = msg.from.get, // TODO get
    )
  }

  def readFromMessage(message: Message): Presentation = {
    val body = message.body.asJson.as[Presentation.Body].toOption.get // TODO get
    Presentation(
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
