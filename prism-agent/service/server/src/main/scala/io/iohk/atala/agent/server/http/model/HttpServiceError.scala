package io.iohk.atala.agent.server.http.model

sealed trait HttpServiceError[+E]

object HttpServiceError {
  final case class InvalidPayload(msg: String) extends HttpServiceError[Nothing]
  final case class ServiceError[E](cause: E) extends HttpServiceError[E]
}
