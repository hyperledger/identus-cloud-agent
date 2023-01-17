package io.iohk.atala.mercury.protocol.issuecredential
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.iohk.atala.mercury.model.AttachmentDescriptor.attachmentDescriptorEncoderV2
final case class Attribute(name: String, value: String, mimeType: Option[String] = None)
object Attribute {
  given Encoder[Attribute] = deriveEncoder[Attribute]
  given Decoder[Attribute] = deriveDecoder[Attribute]
}

/** https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2#preview-credential
  * @param `@type`
  * @param attributes
  */
final case class CredentialPreview(
    `type`: String = "https://didcomm.org/issue-credential/2.0/credential-preview",
    attributes: Seq[Attribute]
)

object CredentialPreview {
  given Encoder[CredentialPreview] = deriveEncoder[CredentialPreview]
  given Decoder[CredentialPreview] = deriveDecoder[CredentialPreview]
}

/** @param attach_id
  * @param format
  *   know Format:
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2#propose-attachment-registry
  *   - dif/credential-manifest@v1.0
  *   - aries/ld-proof-vc-detail@v1.0
  *   - hlindy/cred-filter@v2.0
  */
final case class CredentialFormat(attach_id: String, format: String)

object CredentialFormat {
  given Encoder[CredentialFormat] = deriveEncoder[CredentialFormat]
  given Decoder[CredentialFormat] = deriveDecoder[CredentialFormat]
}
