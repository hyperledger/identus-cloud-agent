package io.iohk.atala.mercury.protocol.invitation
import cats.implicits._
import io.circe.syntax._
import io.circe.generic.semiauto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, HCursor, Json}

sealed trait ServiceType

/** Service block
  * @see
  *   https://github.com/hyperledger/aries-rfcs/tree/main/features/0434-outofband
  * @param id
  * @param `type`
  * @param recipientKeys
  * @param routingKeys
  * @param serviceEndpoint
  */
case class Service(
    id: String,
    `type`: String,
    recipientKeys: Seq[String],
    routingKeys: Option[Seq[String]],
    serviceEndpoint: String,
) extends ServiceType

case class Did(did: String) extends ServiceType
