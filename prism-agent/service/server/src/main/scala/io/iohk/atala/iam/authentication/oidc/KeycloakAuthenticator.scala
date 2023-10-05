package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.Credentials
import zio.*
import io.iohk.atala.iam.authentication.AuthenticationError.InvalidCredentials

trait KeycloakAuthenticator extends Authenticator {
  def authenticate(credentials: Credentials): IO[AuthenticationError, Entity] = {
    if (isEnabled) {
      credentials match {
        case JwtCredentials(Some(token)) if token.nonEmpty => authenticate(token)
        case JwtCredentials(Some(_)) =>
          ZIO.logInfo(s"Keycloak authentication is enabled, but bearer token is empty") *>
            ZIO.fail(JwtAuthenticationError.emptyToken)
        case JwtCredentials(None) =>
          ZIO.logInfo(s"Keycloak authentication is enabled, but bearer token is not provided") *>
            ZIO.fail(InvalidCredentials("bearer token is not provided"))
        case other =>
          ZIO.fail(InvalidCredentials("bearer token is not provided"))
      }
    } else ZIO.fail(AuthenticationMethodNotEnabled("Keycloak authentication is not enabled"))
  }

  def authenticate(token: String): IO[AuthenticationError, Entity]
}
