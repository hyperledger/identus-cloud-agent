package io.iohk.atala.mercury.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object AttachmentDescriptorCodec {

  implicit val headerEncoder: Encoder[Header] = deriveEncoder[Header]
  implicit val headerDecoder: Decoder[Header] = deriveDecoder[Header]

  implicit val jwsEncoder: Encoder[Jws] = deriveEncoder[Jws]
  implicit val jwsDecoder: Decoder[Jws] = deriveDecoder[Jws]

  implicit val jwsDataEncoder: Encoder[JwsData] = deriveEncoder[JwsData]
  implicit val jwsDataDecoder: Decoder[JwsData] = deriveDecoder[JwsData]

  implicit val base64Encoder: Encoder[Base64] = deriveEncoder[Base64]
  implicit val base64eDecoder: Decoder[Base64] = deriveDecoder[Base64]

  implicit val attachmentDescriptorEncoder: Encoder[AttachmentDescriptor] = deriveEncoder[AttachmentDescriptor]
  implicit val attachmentDescriptorDecoder: Decoder[AttachmentDescriptor] = deriveDecoder[AttachmentDescriptor]

}
