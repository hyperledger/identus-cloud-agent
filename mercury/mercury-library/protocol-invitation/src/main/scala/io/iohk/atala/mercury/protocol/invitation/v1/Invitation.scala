package io.iohk.atala.mercury.protocol.invitation.v1
import io.circe.syntax.*
import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder, Json}
import io.iohk.atala.mercury.model.PIURI

import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.model.AttachmentDescriptor.attachmentDescriptorEncoderV1
import io.iohk.atala.mercury.protocol.invitation.ServiceType

/** Out-Of-Band invitation Example
  * @see
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0434-outofband
  *
  * @param `id`
  * @param label
  * @param goal
  * @param goal_code
  * @param handshake_protocols
  * @param `request~attach`
  * @param services
  */
final case class Invitation(
    `@id`: String = io.iohk.atala.mercury.protocol.invitation.getNewMsgId,
    label: String,
    goal: String,
    goal_code: String,
    accept: Seq[String],
    handshake_protocols: Seq[String],
    `requests~attach`: Seq[AttachmentDescriptor],
    services: Seq[ServiceType]
) {
  val `@type`: PIURI = "https://didcomm.org/out-of-band/2.0/invitation"
}

object Invitation {
  given Encoder[Invitation] = (entity: Invitation) =>
    Json.obj(
      "@id" -> Json.fromString(entity.`@id`),
      "@type" -> Json.fromString(entity.`@type`),
      "label" -> Json.fromString(entity.label),
      "goal" -> Json.fromString(entity.goal),
      "goal_code" -> Json.fromString(entity.goal_code),
      "accept" -> entity.accept.asJson,
      "handshake_protocols" -> entity.handshake_protocols.asJson,
      "requests~attach" -> entity.`requests~attach`.asJson,
      "services" -> entity.services.asJson
    )
  given Decoder[Invitation] = deriveDecoder[Invitation]
}
