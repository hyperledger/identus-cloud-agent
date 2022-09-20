package io.iohk.atala.mercury.protocol.invitation
import cats.implicits._
import io.circe.syntax._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.iohk.atala.mercury.protocol.invitation.v1.{Invitation => InvitationV1}
import io.iohk.atala.mercury.protocol.invitation.v2.{Invitation => InvitationV2}
import io.iohk.atala.mercury.model.AttachmentDescriptorCodec._

object InvitationCodec {

  implicit val serviceEncoder: Encoder[Service] = deriveEncoder[Service]
  implicit val serviceDecoder: Decoder[Service] = deriveDecoder[Service]

  implicit val didEncoder: Encoder[Did] = (a: Did) => Json.fromString(a.did)
  implicit val didDecoder: Decoder[Did] = (c: HCursor) => c.value.as[String].map(did => Did(did))

  implicit val serviceTypeEncoder: Encoder[ServiceType] = Encoder.instance {
    case service @ Service(_, _, _, _, _) => service.asJson
    case did @ Did(_)                     => did.asJson
  }

  implicit val serviceTypeDecoder: Decoder[ServiceType] =
    List[Decoder[ServiceType]](
      Decoder[Service].widen,
      Decoder[Did].widen
    ).reduceLeft(_ or _)

  implicit val invitationEncoderV1: Encoder[InvitationV1] = (entity: InvitationV1) =>
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
  implicit val invitationDecoderV1: Decoder[InvitationV1] = deriveDecoder[InvitationV1]

  implicit val invitationEncoderV2: Encoder[InvitationV2] = deriveEncoder[InvitationV2]
  implicit val invitationDecoderV2: Decoder[InvitationV2] = deriveDecoder[InvitationV2]
}
