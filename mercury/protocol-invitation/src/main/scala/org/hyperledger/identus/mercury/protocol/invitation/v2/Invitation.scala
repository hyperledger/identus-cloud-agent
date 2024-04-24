package org.hyperledger.identus.mercury.protocol.invitation.v2
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import org.hyperledger.identus.mercury.model.AttachmentDescriptor.attachmentDescriptorEncoderV2
import io.circe.generic.semiauto._
import org.hyperledger.identus.mercury.model.{PIURI, AttachmentDescriptor, DidId}

/** Out-Of-Band invitation
  * @see
  *   https://identity.foundation/didcomm-messaging/spec/#invitation
  */
final case class Invitation(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = Invitation.`type`,
    from: DidId,
    body: Invitation.Body,
    attachments: Option[Seq[AttachmentDescriptor]] = None
) {
  assert(`type` == "https://didcomm.org/out-of-band/2.0/invitation")
  def toBase64: String = java.util.Base64.getUrlEncoder.encodeToString(this.asJson.deepDropNullValues.noSpaces.getBytes)

}

object Invitation {

  final case class Body(
      goal_code: Option[String],
      goal: Option[String],
      accept: Seq[String]
  )

  object Body {
    given Encoder[Body] = deriveEncoder[Body]

    given Decoder[Body] = deriveDecoder[Body]
  }

  def `type`: PIURI = "https://didcomm.org/out-of-band/2.0/invitation"
  given Encoder[Invitation] = deriveEncoder[Invitation]
  given Decoder[Invitation] = deriveDecoder[Invitation]

}
