package io.iohk.atala.mercury.protocol.connection

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.iohk.atala.mercury.model.{DidId, Message, PIURI}
import io.iohk.atala.mercury.protocol.connection.ConnectionRequest.Body
import io.circe.syntax.*

object ConnectionResponse {
  def `type`: PIURI = "https://atalaprism.io/mercury/connections/1.0/response"

  final case class Body(
      goal_code: Option[String] = None,
      goal: Option[String] = None,
      accept: Seq[String] = Seq.empty
  )

  object Body {
    given Encoder[Body] = deriveEncoder[Body]
    given Decoder[Body] = deriveDecoder[Body]
  }

  def readFromMessage(message: Message): ConnectionResponse = {
    val body = message.body.asJson.as[ConnectionRequest.Body].toOption.get // TODO get
    ConnectionResponse(
      id = message.id,
      `type` = message.piuri,
      body = body,
      thid = message.thid,
      from = message.from.get, // TODO get
      to = message.to.get, // TODO get
    )
  }

}

final case class ConnectionResponse(
    `type`: PIURI,
    id: String = java.util.UUID.randomUUID().toString,
    from: DidId,
    to: DidId,
    thid: Option[String],
    body: Body,
) {
  assert(`type` == "https://atalaprism.io/mercury/connections/1.0/response")

  def makeMessage: Message = Message(
    id = this.id,
    piuri = this.`type`,
    from = Some(this.from),
    to = Some(this.to),
    thid = this.thid,
    body = this.body.asJson.asObject.get,
  )
}
