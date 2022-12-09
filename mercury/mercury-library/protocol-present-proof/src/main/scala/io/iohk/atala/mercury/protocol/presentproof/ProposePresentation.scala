package io.iohk.atala.mercury.protocol.presentproof

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import io.iohk.atala.mercury.model.PIURI
import io.iohk.atala.mercury.model._

/** ALL parameterS are DIDCOMMV2 format and naming conventions and follows the protocol
  * @see
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0454-present-proof-v2
  *
  * @param id
  * @param `type`
  * @param body
  * @param attachments
  */
final case class ProposePresentation(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = ProposePresentation.`type`,
    body: ProposePresentation.Body,
    attachments: Seq[AttachmentDescriptor] = Seq.empty[AttachmentDescriptor],
    // extra
    thid: Option[String] = None,
    from: DidId,
    to: DidId,
) {
  assert(`type` == ProposePresentation.`type`)

  def makeMessage: Message = Message(
    piuri = this.`type`,
    from = Some(this.from),
    to = Seq(this.to),
    thid = this.thid,
    body = this.body.asJson.asObject.get, // TODO get
    attachments = this.attachments
  )
}

object ProposePresentation {
  // def `type`: PIURI = "https://didcomm.org/present-proof/3.0/propose-presentation"
  def `type`: PIURI = "https://didcomm.atalaprism.io/present-proof/3.0/propose-presentation"

  import AttachmentDescriptor.attachmentDescriptorEncoderV2
  given Encoder[ProposePresentation] = deriveEncoder[ProposePresentation]
  given Decoder[ProposePresentation] = deriveDecoder[ProposePresentation]

  /** @param goal_code
    * @param comment
    * @param formats
    */
  final case class Body(
      goal_code: Option[String] = None,
      comment: Option[String] = None,
      // AtalaPrism Extension!
      proof_types: Seq[ProofType] = Seq.empty
  )

  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }

  def makePresentProofProposalFromRequest(msg: Message): ProposePresentation = { // TODO change msg: Message to RequestCredential
    val rp: RequestPresentation = RequestPresentation.readFromMessage(msg)

    ProposePresentation(
      body = ProposePresentation.Body(
        goal_code = rp.body.goal_code,
        comment = rp.body.comment,
      ),
      attachments = rp.attachments,
      thid = rp.thid,
      from = rp.to,
      to = rp.from,
    )
  }

  def readFromMessage(message: Message): ProposePresentation = {
    val body = message.body.asJson.as[ProposePresentation.Body].toOption.get // TODO get

    ProposePresentation(
      id = message.id,
      `type` = message.piuri,
      body = body,
      attachments = message.attachments,
      thid = message.thid,
      from = message.from.get, // TODO get
      to = {
        assert(message.to.length == 1, "The recipient is ambiguous. Need to have only 1 recipient") // TODO return error
        message.to.head
      },
    )
  }

}
