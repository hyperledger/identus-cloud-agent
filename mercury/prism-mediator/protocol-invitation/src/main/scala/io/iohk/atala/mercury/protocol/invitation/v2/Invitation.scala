package io.iohk.atala.mercury.protocol.invitation.v2
import cats.implicits._
import io.circe.syntax._
import io.circe.generic.semiauto._
import io.circe.{Encoder, Json}
import io.iohk.atala.mercury.model._

import scala.annotation.targetName
import io.iohk.atala.mercury.protocol.invitation.AttachmentDescriptor

/** Out-Of-Band invitation
  * @see
  *   https://identity.foundation/didcomm-messaging/spec/#invitation
  */
final case class Invitation(
    `type`: PIURI,
    id: String,
    from: DidId,
    body: Body,
    attachments: Option[AttachmentDescriptor] // TODO
) {
  assert(`type` == "https://didcomm.org/out-of-band/2.0/invitation")
}

case class Body(
    goal_code: String,
    goal: String,
    accept: Seq[String]
)
