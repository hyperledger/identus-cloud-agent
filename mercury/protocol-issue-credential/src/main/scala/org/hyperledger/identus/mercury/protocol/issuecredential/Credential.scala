/*
package org.hyperledger.identus.mercury.protocol.issuecredential
import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
final case class Attribute(
    name: String,
    value: String,
    mime_type: Option[String] = None,
)
object Attribute {
  given Encoder[Attribute] = deriveEncoder[Attribute]
  given Decoder[Attribute] = deriveDecoder[Attribute]
}

/** @see
 *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2#preview-credential
 * @param `@type`
 * @param attributes
 */
final case class CredentialPreview(
    `type`: String = "https://didcomm.org/issue-credential/3.0/credential-preview",
    schema_id: Option[String] = None,
    attributes: Seq[Attribute]
)

object CredentialPreview {
  given Encoder[CredentialPreview] = deriveEncoder[CredentialPreview]
  given Decoder[CredentialPreview] = deriveDecoder[CredentialPreview]
}
 */
