package org.hyperledger.identus.mercury.protocol.revocationnotificaiton

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.hyperledger.identus.mercury.model.{DidId, Message, PIURI}

final case class RevocationNotification(
    id: String = java.util.UUID.randomUUID.toString(),
    `type`: PIURI = RevocationNotification.`type`,
    body: RevocationNotification.Body,
    thid: Option[String] = None,
    from: DidId,
    to: DidId,
) {
  assert(`type` == RevocationNotification.`type`)

  def makeMessage: Message = Message(
    id = this.id,
    `type` = this.`type`,
    from = Some(this.from),
    to = Seq(this.to),
    thid = this.thid,
    body = this.body.asJson.asObject.get,
  )
}
object RevocationNotification {

  given Encoder[RevocationNotification] = deriveEncoder[RevocationNotification]
  given Decoder[RevocationNotification] = deriveDecoder[RevocationNotification]

  def `type`: PIURI = "https://atalaprism.io/revocation_notification/1.0/revoke"

  def build(
      fromDID: DidId,
      toDID: DidId,
      thid: Option[String] = None,
      issueCredentialProtocolThreadId: String
  ): RevocationNotification = {
    RevocationNotification(
      thid = thid,
      from = fromDID,
      to = toDID,
      body = Body(
        issueCredentialProtocolThreadId = issueCredentialProtocolThreadId,
        comment = Some("Thread Id used to issue this credential withing issue credential protocol")
      ),
    )
  }

  final case class Body(
      issueCredentialProtocolThreadId: String,
      comment: Option[String] = None,
  )

  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }

  def readFromMessage(message: Message): Either[String, RevocationNotification] =
    message.body.asJson.as[RevocationNotification.Body] match
      case Left(fail) => Left("Fail to parse RevocationNotification's body: " + fail.getMessage)
      case Right(body) =>
        message.from match
          case None => Left("OfferCredential MUST have the sender explicit")
          case Some(from) =>
            message.to match
              case firstTo +: Seq() =>
                Right(
                  RevocationNotification(
                    id = message.id,
                    `type` = message.piuri,
                    body = body,
                    thid = message.thid,
                    from = from,
                    to = firstTo,
                  )
                )
              case tos => Left(s"OfferCredential MUST have only 1 recipient instead has '${tos}'")

}
