package io.iohk.atala.mercury.mediator

sealed trait ErrorInfo
case class NotFound(msg: String) extends ErrorInfo
case class BadRequest(msg: String, errors: List[String] = List.empty) extends ErrorInfo
case class InternalServerError(msg: String) extends ErrorInfo
