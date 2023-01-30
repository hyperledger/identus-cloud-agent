package io.iohk.atala.mercury.model

import java.util.Base64 as JBase64
import io.circe.{Decoder, Encoder, HCursor, Json, JsonObject}
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import cats.syntax.functor.*

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

final case class LinkData(links: Seq[String], hash: String) extends AttachmentData
object LinkData {
  given Encoder[LinkData] = deriveEncoder[LinkData]
  given Decoder[LinkData] = deriveDecoder[LinkData]

}

final case class JsonData(json: JsonObject) extends AttachmentData
object JsonData {
  given Encoder[JsonData] = deriveEncoder[JsonData]
  given Decoder[JsonData] = deriveDecoder[JsonData]
}
object AttachmentData {
  // given Encoder[AttachmentData] = deriveEncoder[AttachmentData]
  given Encoder[AttachmentData] = (a: AttachmentData) => {
    a match
      case data @ JsonData(_)    => data.asJson
      case data @ Base64(_)      => data.asJson
      case data @ JwsData(_, _)  => data.asJson
      case data @ LinkData(_, _) => data.asJson
  }

  given Decoder[AttachmentData] = List[Decoder[AttachmentData]](
    Decoder[JsonData].widen,
    Decoder[Base64].widen,
    Decoder[JwsData].widen,
    Decoder[LinkData].widen
  ).reduceLeft(_ or _)
}

/** https://identity.foundation/didcomm-messaging/spec/#attachments
  *
  * Example:
  * {{{
  *      {"id":"765d7fda-1b7f-4325-a828-f0c5523a6c19",
  *        "data":{"jws":null,"hash":null,"json":{
  *          "ciphertext":"c1kzLNSIZAuRILhaJg7KY3rx95czEy6VVBFVCN002OWw4D7bLK-ZuPTaTVEvwhfxlwkkwP1xuG9R22XMYlzLUIou6hx3gyeWgsUZS6OZkvuQlRqtPLh1yEZap24WH80cH4_DpX-srxsic5n7cEluuUlC0xvF5th-TLOZcmfUYySBPoKLzrSBcNIZH0GPyeePlnmLhqB5pi--mX3M17DcTpN5miQyJUNaRNv8hj3lKsKiRtGUCL_dzbV4UGRFEZ-fF-LWZtZfNco3LoEwIpX5099sqzc9ZrFi3GbgAWdyJUe075A5h89FgMHYdqdOrGp8HSQCoj4pRTv-SQJJ16APFo-u7GZOVd0kLeJvMBCQhwXlGT4DUpaeMV_52wrPj2FA9jDyqnzpUFPsb_IH7poc-VFrV32NJ6GLhzgwwc3k3vU_s16bHB3GeB-7_GgCu6hT","protected":"eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6Ii1vY3RRYmFobFVYSF81aTQwOHRKN0Y3SmxHQU4yODY1Z1hvTHlHNTBNSHcifSwiYXB2IjoiYUptdEdPMHZIc3loQkpxVnRXU2dHNVN3YjMzVTJWdTRKTWhCS0ZiX0NHSSIsInNraWQiOiJkaWQ6ZXhhbXBsZTphbGljZSNrZXktYWdyZWVtZW50LTEiLCJhcHUiOiJaR2xrT21WNFlXMXdiR1U2WVd4cFkyVWphMlY1TFdGbmNtVmxiV1Z1ZEMweCIsInR5cCI6ImFwcGxpY2F0aW9uXC9kaWRjb21tLWVuY3J5cHRlZCtqc29uIiwiZW5jIjoiQTI1NkNCQy1IUzUxMiIsImFsZyI6IkVDREgtMVBVK0EyNTZLVyJ9",
  *          "recipients":[{
  *            "encrypted_key":"lWDvuQ37k6rotmnmOe7h1UF7Ao1RApL08aWmwjcijhlcH1_kvOvLYV8Dg2jXOZGsz2GsnM_W36JkLDFxM160g91ZsNAbz2rn",
  *            "header":{"kid":"did:example:bob#key-agreement-1"}
  *          }],"tag":"4pWPm37KdyRLZbWGIPJXqY9Mq55mKBHtSqBKUeHQHwc","iv":"THIgCu-Fq2aCiuwS-PcsfQ"
  *       }
  * }}}
  *
  * @see
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
    lastmod_time: Option[Long] = None,
    byte_count: Option[Long] = None,
    description: Option[String] = None
)

object AttachmentDescriptor {

  def buildBase64Attachment(
      id: String = java.util.UUID.randomUUID.toString,
      payload: Array[Byte],
      mediaType: Option[String] = None
  ): AttachmentDescriptor = {
    val encoded = JBase64.getUrlEncoder.encodeToString(payload)
    AttachmentDescriptor(id, mediaType, Base64(encoded))
  }

  def buildJsonAttachment[A](
      id: String = java.util.UUID.randomUUID.toString,
      payload: A,
      mediaType: Option[String] = Some("application/json")
  )(using Encoder[A]): AttachmentDescriptor = {
    val jsonObject = payload.asJson.asObject.getOrElse(JsonObject.empty)
    AttachmentDescriptor(id, mediaType, JsonData(jsonObject)) // use JsonData or Base64 by default?
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
