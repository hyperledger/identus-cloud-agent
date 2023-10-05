package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authentication.admin.{AdminApiKeyAuthenticator, AdminApiKeyCredentials}
import io.iohk.atala.iam.authentication.apikey.*
import io.iohk.atala.iam.authentication.keycloak.KeycloakAuthenticator
import zio.*

case class DefaultAuthenticator(
    adminApiKeyAuthenticator: AdminApiKeyAuthenticator,
    apiKeyAuthenticator: ApiKeyAuthenticator,
    keycloakAuthenticator: KeycloakAuthenticator
) extends Authenticator {
  override def isEnabled = true
  override def authenticate(credentials: Credentials): IO[AuthenticationError, Entity] = credentials match {
    case adminApiKeyCredentials: AdminApiKeyCredentials => adminApiKeyAuthenticator(adminApiKeyCredentials)
    case apiKeyCredentials: ApiKeyCredentials =>
      apiKeyAuthenticator(apiKeyCredentials)
        .catchSome { case AuthenticationMethodNotEnabled(_: String) =>
          ZIO.succeed(Entity.Default)
        }
  }
}

object DefaultAuthenticator {
  val layer: URLayer[AdminApiKeyAuthenticator & ApiKeyAuthenticator & KeycloakAuthenticator, Authenticator] =
    ZLayer.fromFunction(DefaultAuthenticator(_, _, _))
}
