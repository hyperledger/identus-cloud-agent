package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.ErrorResponse
import zio.IO

trait Credentials

trait AuthenticationError {
  def message: String
}

case class InvalidCredentials(message: String) extends AuthenticationError

object AuthenticationError {
  def toErrorResponse(error: AuthenticationError): ErrorResponse =
    ErrorResponse(
      status = sttp.model.StatusCode.Forbidden.code,
      `type` = "authentication_error",
      title = "",
      detail = Option(error.message),
      instance = ""
    )
}

trait Authenticator {

  /** @param credentials
    * @return
    *   None if authentication cannot be recognized, Some(entity) if authentication succeeded
    */
  def authenticate(credentials: Credentials): IO[AuthenticationError, Entity]

  def apply(credentials: Credentials): IO[AuthenticationError, Entity] = authenticate(credentials)
}
