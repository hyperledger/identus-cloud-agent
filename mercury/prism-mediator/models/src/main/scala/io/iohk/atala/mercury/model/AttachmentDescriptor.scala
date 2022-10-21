package io.iohk.atala.mercury.model

import java.util.{Base64 => JBase64}
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.Json

/** @see
  *   data in attachments https://identity.foundation/didcomm-messaging/spec/#attachments
  */
sealed trait AttachmentData

final case class Header(kid: String)

final case class Jws(header: Header, `protected`: String, signature: String)

final case class JwsData(base64: String, jws: Jws) extends AttachmentData

final case class Base64(base64: String) extends AttachmentData

object AttachmentData {
  given Encoder[Base64] = deriveEncoder[Base64]
  given Decoder[Base64] = deriveDecoder[Base64]

  given Encoder[JwsData] = deriveEncoder[JwsData]
  given Decoder[JwsData] = deriveDecoder[JwsData]

  given Encoder[Jws] = deriveEncoder[Jws]
  given Decoder[Jws] = deriveDecoder[Jws]

  given Encoder[Header] = deriveEncoder[Header]
  given Decoder[Header] = deriveDecoder[Header]

}

/** @see
  *   https://github.com/hyperledger/aries-rfcs/tree/main/concepts/0017-attachments
  * @param id
  * @param media_type
  * @param data
  * @param filename
  * @param lastmod_time
  * @param byte_count
  * @param description
  */
final case class AttachmentDescriptor(
    id: Option[String] = None,
    media_type: Option[String] = None,
    data: Base64 = Base64(""),
    filename: Option[String] = None,
    lastmod_time: Option[String] = None,
    byte_count: Option[Int] = None,
    description: Option[String] = None
)

object AttachmentDescriptor {

  def buildAttachment[A: Encoder](
      id: Option[String] = None,
      payload: A,
      mediaType: Option[String] = Some("application/json")
  ): AttachmentDescriptor = {
    val encoded = JBase64.getUrlEncoder.encodeToString(payload.asJson.noSpaces.getBytes)
    AttachmentDescriptor(id, mediaType, Base64(encoded))
  }

  given attachmentDescriptorEncoderV1: Encoder[AttachmentDescriptor] = (a: AttachmentDescriptor) => {
    Json.obj(
      "@id" -> a.id.asJson,
      "mime-type" -> a.media_type.asJson,
      "data" -> a.data.asJson
    )
  }
  given Decoder[AttachmentDescriptor] = deriveDecoder[AttachmentDescriptor]

}
