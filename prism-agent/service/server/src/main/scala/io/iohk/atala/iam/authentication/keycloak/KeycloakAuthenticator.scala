package io.iohk.atala.iam.authentication.keycloak

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
      // TODO: implement
      credentials match {
        case KeycloakCredentials(token) if token.nonEmpty => ???
        case KeycloakCredentials(token) =>
          ZIO.logInfo(s"Keycloak authentication is enabled, but bearer token is empty") *>
            ZIO.fail(KeycloakAuthenticationError.emptyToken)
        case other =>
          ZIO.fail(InvalidCredentials("Keycloak token is not provided"))
      }
    } else ZIO.fail(AuthenticationMethodNotEnabled("ApiKey API authentication is not enabled"))
  }

  def authenticate(token: String): IO[AuthenticationError, Entity]
}
