package org.hyperledger.identus.mercury.protocol.revocationnotificaiton

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import org.hyperledger.identus.mercury.model.{PIURI, Message, DidId}

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

  def readFromMessage(message: Message): RevocationNotification =
    val body = message.body.asJson.as[RevocationNotification.Body].toOption.get

    RevocationNotification(
      id = message.id,
      `type` = message.piuri,
      body = body,
      thid = message.thid,
      from = message.from.get, // TODO get
      to = {
        assert(message.to.length == 1, "The recipient is ambiguous. Need to have only 1 recipient")
        message.to.head
      },
    )

}
