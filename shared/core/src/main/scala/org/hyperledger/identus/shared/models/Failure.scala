package org.hyperledger.identus.shared.models

trait Failure {
  val statusCode: StatusCode
  val userFacingMessage: String
}

sealed class StatusCode(val code: Int)

object StatusCode {
  val BadRequest: StatusCode = StatusCode(400)
  val NotFound: StatusCode = StatusCode(404)

  val InternalServerError: StatusCode = StatusCode(500)
}
