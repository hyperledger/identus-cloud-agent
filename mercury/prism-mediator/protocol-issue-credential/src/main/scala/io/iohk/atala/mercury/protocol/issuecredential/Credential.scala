package io.iohk.atala.mercury.protocol.issuecredential
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._

final case class Attributes(name: String, mimeType: String, value: String)

final case class CredentialPreview(
    `@type`: String = "https://didcomm.org/issue-credential/2.0/credential-preview",
    attributes: Attributes
)
final case class CredentialFormat(attach_id: String, format: String)

object Credential {
  given Encoder[Attributes] = deriveEncoder[Attributes]
  given Decoder[Attributes] = deriveDecoder[Attributes]

  given Encoder[CredentialPreview] = deriveEncoder[CredentialPreview]
  given Decoder[CredentialPreview] = deriveDecoder[CredentialPreview]

  given Encoder[CredentialFormat] = deriveEncoder[CredentialFormat]
  given Decoder[CredentialFormat] = deriveDecoder[CredentialFormat]

}
