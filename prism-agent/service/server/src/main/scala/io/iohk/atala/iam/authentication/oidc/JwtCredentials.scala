package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.iam.authentication.Credentials

final case class JwtCredentials(token: Option[String]) extends Credentials

final case class JwtAuthenticationError(message: String) extends AuthenticationError

object JwtAuthenticationError {
  val emptyToken = JwtAuthenticationError("Empty bearer token header provided")
}
