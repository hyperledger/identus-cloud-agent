package io.iohk.atala.mercury.protocol.issuecredential

import io.iohk.atala.mercury.model.PIURI
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._
import io.iohk.atala.mercury.model.AttachmentData

/** ALL parameterS are DIDCOMMV2 format and naming conventions and follows the protocol
  * @see
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2
  *
  * @param id
  * @param `type`
  * @param body
  * @param attachments
  */
final case class ProposeCredential(
    id: String,
    `type`: PIURI,
    body: ProposeCredential.Body,
    attachments: AttachmentData
) {
  assert(`type` == ProposeCredential.`type`)
}

object ProposeCredential {
  // TODD will this be version RCF Issue Credential 2.0  as we use didcomm2 message format
  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/propose-credential"

  final case class Body(
      goal_code: Option[String],
      comment: Option[String],
      credential_preview: Option[CredentialPreview], // JSON STRinf
      formats: Seq[CredentialFormat]
  )

}
