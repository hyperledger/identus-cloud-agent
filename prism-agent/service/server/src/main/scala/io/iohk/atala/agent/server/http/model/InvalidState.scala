package io.iohk.atala.agent.server.http.model

final case class InvalidState(cause: String) extends RuntimeException(cause)
case object NotImplemented extends RuntimeException("NotImplemented")
