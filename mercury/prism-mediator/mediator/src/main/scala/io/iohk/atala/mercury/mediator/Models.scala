package io.iohk.atala.mercury.mediator

import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.OneOf
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.{oneOf, oneOfVariant}
import io.circe.generic.auto._

import javax.xml.crypto.dsig.keyinfo.KeyInfo

case class ConnectionId(connectionId: String)
case class Message(connectionId: String, msg: String)

val httpErrors: OneOf[ErrorInfo, ErrorInfo] = oneOf[ErrorInfo](
  oneOfVariant(StatusCode.InternalServerError, jsonBody[InternalServerError]),
  oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequest]),
  oneOfVariant(StatusCode.NotFound, jsonBody[NotFound])
)

case class PublicKey(id: String, `type`: String, controller: String, publicKeyBase58: String)
case class MediateRequest(id: String, `@type`: String, invitationId: String, publicKey: PublicKey)
case class MediateResponse(id: String, `@type`: String, endpoint: String, routing_keys: Seq[String])
