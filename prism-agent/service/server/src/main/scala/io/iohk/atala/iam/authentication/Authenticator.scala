package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.api.http.ErrorResponse
import zio.IO

trait Credentials

trait AuthenticationError {
  def message: String
}

object AuthenticationError {

  case class InvalidCredentials(message: String) extends AuthenticationError

  case class AuthenticationMethodNotEnabled(message: String) extends AuthenticationError

  case class UnexpectedError(message: String) extends AuthenticationError

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
  def authenticate(credentials: Credentials): IO[AuthenticationError, Entity]

  def apply(credentials: Credentials): IO[AuthenticationError, Entity] = authenticate(credentials)
}
