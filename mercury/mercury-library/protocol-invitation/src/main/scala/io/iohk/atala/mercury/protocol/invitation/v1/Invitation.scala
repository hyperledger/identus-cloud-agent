package io.iohk.atala.mercury.protocol.invitation.v1
import cats.implicits._
import io.circe.syntax._
import io.circe.generic.semiauto._
import io.circe.{Encoder, Json}
import io.iohk.atala.mercury.model.PIURI

import scala.annotation.targetName
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.protocol.invitation.ServiceType

/** Out-Of-Band invitation Example
  * @see
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0434-outofband
  *
  * @param `id`
  * @param label
  * @param goal
  * @param goal_code
  * @param handshake_protocols
  * @param `request~attach`
  * @param services
  */
final case class Invitation(
    `@id`: String = io.iohk.atala.mercury.protocol.invitation.getNewMsgId,
    label: String,
    goal: String,
    goal_code: String,
    accept: Seq[String],
    handshake_protocols: Seq[String],
    `requests~attach`: Seq[AttachmentDescriptor],
    services: Seq[ServiceType]
) {
  val `@type`: PIURI = "https://didcomm.org/out-of-band/2.0/invitation"
}
