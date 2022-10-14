package io.iohk.atala.mercury.protocol.issuecredential
import io.iohk.atala.mercury.model.PIURI
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._
import io.iohk.atala.mercury.model.AttachmentData

class RequestCredential(id: String, `type`: PIURI, body: ProposeCredential.Body, attachments: AttachmentData)
object RequestCredential {

  def `type`: PIURI = "https://didcomm.org/issue-credential/2.0/request-credential"

  final case class Body(
      goal_code: Option[String],
      comment: Option[String],
      formats: Seq[CredentialFormat]
  )

}
