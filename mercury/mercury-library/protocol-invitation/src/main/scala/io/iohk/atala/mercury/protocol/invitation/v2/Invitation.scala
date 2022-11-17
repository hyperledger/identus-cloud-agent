package io.iohk.atala.mercury.protocol.invitation.v2
import cats.implicits._
import io.circe.syntax._
import io.circe.{Encoder, Json}
import io.iohk.atala.mercury.model._
import AttachmentDescriptor.attachmentDescriptorEncoderV2
import io.iohk.atala.mercury.protocol.invitation.InvitationCodec._

/** Out-Of-Band invitation
  * @see
  *   https://identity.foundation/didcomm-messaging/spec/#invitation
  */
final case class Invitation(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = Invitation.`type`,
    from: DidId,
    body: Body,
    attachments: Option[Seq[AttachmentDescriptor]] = None
) {
  assert(`type` == "https://didcomm.org/out-of-band/2.0/invitation")
  def toBase64: String = java.util.Base64.getUrlEncoder.encodeToString(this.asJson.deepDropNullValues.noSpaces.getBytes)

}

object Invitation {
  def `type`: PIURI = "https://didcomm.org/out-of-band/2.0/invitation"
}

case class Body(
    goal_code: String,
    goal: String,
    accept: Seq[String]
)
