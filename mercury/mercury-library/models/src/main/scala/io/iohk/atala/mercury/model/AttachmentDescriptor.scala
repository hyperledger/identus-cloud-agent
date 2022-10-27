package io.iohk.atala.mercury.model

import java.util.Base64 as JBase64
import io.circe.{Decoder, Encoder, HCursor, Json, JsonObject}
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import cats.syntax.functor._

/** @see
  *   data in attachments https://identity.foundation/didcomm-messaging/spec/#attachments
  */
sealed trait AttachmentData

final case class Header(kid: String)
object Header {
  given Encoder[Header] = deriveEncoder[Header]

  given Decoder[Header] = deriveDecoder[Header]
}
final case class Jws(header: Header, `protected`: String, signature: String)
object Jws {
  given Encoder[Jws] = deriveEncoder[Jws]

  given Decoder[Jws] = deriveDecoder[Jws]
}
final case class JwsData(base64: String, jws: Jws) extends AttachmentData
object JwsData {
  given Encoder[JwsData] = deriveEncoder[JwsData]

  given Decoder[JwsData] = deriveDecoder[JwsData]
}
final case class Base64(base64: String) extends AttachmentData
object Base64 {
  given Encoder[Base64] = deriveEncoder[Base64]
  given Decoder[Base64] = deriveDecoder[Base64]

}
final case class JsonData(data: JsonObject) extends AttachmentData
object JsonData {
  given Encoder[JsonData] = deriveEncoder[JsonData]
  given Decoder[JsonData] = deriveDecoder[JsonData]
}
object AttachmentData {
  // given Encoder[AttachmentData] = deriveEncoder[AttachmentData]
  given Encoder[AttachmentData] = (a: AttachmentData) => {
    a match
      case data @ JsonData(_)   => data.asJson
      case data @ Base64(_)     => data.asJson
      case data @ JwsData(_, _) => data.asJson
  }

  given Decoder[AttachmentData] = List[Decoder[AttachmentData]](
    Decoder[JsonData].widen,
    Decoder[Base64].widen,
    Decoder[JwsData].widen
  ).reduceLeft(_ or _)
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
    id: String,
    media_type: Option[String] = None,
    data: AttachmentData = Base64(""),
    filename: Option[String] = None,
    lastmod_time: Option[String] = None,
    byte_count: Option[Int] = None,
    description: Option[String] = None
)

object AttachmentDescriptor {

  def buildAttachment[A: Encoder](
      id: String = java.util.UUID.randomUUID.toString,
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

  given attachmentDescriptorEncoderV2: Encoder[AttachmentDescriptor] = deriveEncoder[AttachmentDescriptor]

  given Decoder[AttachmentDescriptor] = deriveDecoder[AttachmentDescriptor]

}
