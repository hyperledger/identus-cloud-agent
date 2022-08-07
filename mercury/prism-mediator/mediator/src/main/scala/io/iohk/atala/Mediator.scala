package io.iohk.atala

import sttp.model.StatusCode
import sttp.tapir.EndpointOutput.OneOf
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir.{oneOf, oneOfVariant}

object Mediator {
  import io.circe.generic.auto._

  case class ConnectionId(connectionId: String) extends AnyVal
  case class Message(connectionId: String, msg: String)

  val httpErrors: OneOf[ErrorInfo, ErrorInfo] = oneOf[ErrorInfo](
    oneOfVariant(StatusCode.InternalServerError, jsonBody[InternalServerError]),
    oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequest]),
    oneOfVariant(StatusCode.NotFound, jsonBody[NotFound])
  )

}
