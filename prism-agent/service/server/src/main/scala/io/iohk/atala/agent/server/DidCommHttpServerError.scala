package io.iohk.atala.agent.server

sealed trait DidCommHttpServerError

object DidCommHttpServerError {
  case class RequestBodyParsingError(error: String) extends DidCommHttpServerError
  case class DIDCommMessageParsingError(error: String) extends DidCommHttpServerError
}
