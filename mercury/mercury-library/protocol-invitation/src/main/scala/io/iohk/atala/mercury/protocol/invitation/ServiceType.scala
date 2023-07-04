package io.iohk.atala.mercury.protocol.invitation
import cats.implicits._
import io.circe.syntax._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, HCursor}

sealed trait ServiceType

object ServiceType {

  given Encoder[ServiceType] = (a: ServiceType) => {
    a match
      case data @ Did(_)                 => data.did.asJson
      case data @ Service(_, _, _, _, _) => data.asJson
  }

  given Decoder[ServiceType] = List[Decoder[ServiceType]](
    Decoder[Did].widen,
    Decoder[Service].widen,
  ).reduceLeft(_ or _)
}

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

object Service {
  given Encoder[Service] = deriveEncoder[Service]
  given Decoder[Service] = deriveDecoder[Service]
}

case class Did(did: String) extends ServiceType

object Did {
  given Encoder[Did] = deriveEncoder[Did]
  given Decoder[Did] = deriveDecoder[Did]
}
