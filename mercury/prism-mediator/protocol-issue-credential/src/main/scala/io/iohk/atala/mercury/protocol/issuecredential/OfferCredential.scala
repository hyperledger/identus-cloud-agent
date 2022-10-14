package io.iohk.atala.mercury.protocol.issuecredential

import io.iohk.atala.mercury.model.PIURI
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._
import io.iohk.atala.mercury.model.AttachmentData

final case class OfferCredential(id: String, `type`: PIURI, body: OfferCredential.Body, attachments: AttachmentData) {
  assert(`type` == OfferCredential.`type`)
}

object OfferCredential {

  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/offer-credential"

  final case class Body(
      goal_code: Option[String],
      comment: Option[String],
      replacement_id: Option[String],
      multiple_available: Option[String],
      credential_preview: Option[CredentialPreview],
      formats: Seq[CredentialFormat]
  )

}
