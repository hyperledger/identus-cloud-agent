package io.iohk.atala.mercury.protocol.trustping

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.iohk.atala.mercury.model._

/** https://identity.foundation/didcomm-messaging/spec/#trust-ping-protocol-20 */
final case class TrustPing(
    `type`: PIURI = TrustPing.`type`,
    id: String = java.util.UUID.randomUUID().toString,
    from: DidId,
    to: DidId,
    body: TrustPing.Body,
) {
  assert(`type` == TrustPing.`type`)

  def makeMessage: Message = Message(
    id = this.id,
    `type` = this.`type`,
    from = Some(this.from),
    to = Seq(this.to),
    body = this.body.asJson.asObject.get,
  )

  def makeReply = TrustPingResponse(
    thid = this.id,
    from = to,
    to = from,
  )
}

object TrustPing {
  def `type`: PIURI = "https://didcomm.org/trust-ping/2.0/ping"
  case class Body(
      response_requested: Option[Boolean] = Some(true),
  )
  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }
  given Encoder[TrustPing] = deriveEncoder[TrustPing]
  given Decoder[TrustPing] = deriveDecoder[TrustPing]

  /** Parse a generecy DIDComm Message into a TrustPing */
  def fromMessage(message: Message): Either[String, TrustPing] =
    for {
      piuri <-
        if (message.`type` == TrustPing.`type`) Right(message.`type`)
        else Left(s"Message MUST be of the type '${TrustPing.`type`}' instead of '${message.`type`}'")
      body <- message.body.asJson
        .as[TrustPing.Body]
        .left
        .map(ex => "Fail to parse the body of the TrustPing because: " + ex.message)
      ret <- message.to match
        case Seq(onlyOneRecipient) =>
          message.from match
            case Some(from) =>
              Right(
                TrustPing(
                  id = message.id,
                  `type` = piuri,
                  body = body,
                  from = from,
                  to = onlyOneRecipient
                )
              )
            case None => Left("TrustPing needs to define the recipient")
        case _ => Left("The recipient is ambiguous. Need to have only 1 recipient")
    } yield ret

}
