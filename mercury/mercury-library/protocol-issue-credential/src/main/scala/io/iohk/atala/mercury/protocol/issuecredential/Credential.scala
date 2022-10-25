package io.iohk.atala.mercury.protocol.issuecredential
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._
import io.iohk.atala.mercury.model.AttachmentDescriptor.attachmentDescriptorEncoderV2
final case class Attribute(name: String, value: String, mimeType: Option[String] = None)

/** https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2#preview-credential
  * @param `@type`
  * @param attributes
  */
final case class CredentialPreview(
    `type`: String = "https://didcomm.org/issue-credential/2.0/credential-preview",
    attributes: Seq[Attribute]
)
final case class CredentialFormat(attach_id: String, format: String)

object Credential {
  given Encoder[Attribute] = deriveEncoder[Attribute]
  given Decoder[Attribute] = deriveDecoder[Attribute]

  given Encoder[CredentialPreview] = deriveEncoder[CredentialPreview]
  given Decoder[CredentialPreview] = deriveDecoder[CredentialPreview]

  given Encoder[CredentialFormat] = deriveEncoder[CredentialFormat]
  given Decoder[CredentialFormat] = deriveDecoder[CredentialFormat]

  given Encoder[ProposeCredential.Body] = deriveEncoder[ProposeCredential.Body]
  given Decoder[ProposeCredential.Body] = deriveDecoder[ProposeCredential.Body]

  given Encoder[ProposeCredential] = deriveEncoder[ProposeCredential]

  given Decoder[ProposeCredential] = deriveDecoder[ProposeCredential]
}
