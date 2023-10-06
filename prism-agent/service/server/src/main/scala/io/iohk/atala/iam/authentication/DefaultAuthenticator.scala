package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authentication.admin.{AdminApiKeyAuthenticator, AdminApiKeyCredentials}
import io.iohk.atala.iam.authentication.apikey.{ApiKeyAuthenticator, ApiKeyCredentials}
import io.iohk.atala.iam.authentication.oidc.{KeycloakAuthenticator, JwtCredentials}
import zio.*

case class DefaultAuthenticator(
    adminApiKeyAuthenticator: AdminApiKeyAuthenticator,
    apiKeyAuthenticator: ApiKeyAuthenticator,
    keycloakAuthenticator: KeycloakAuthenticator
) extends Authenticator {

  override def isEnabled = true

  override def authenticate(credentials: Credentials): IO[AuthenticationError, Entity] = credentials match {
    case adminApiKeyCredentials: AdminApiKeyCredentials => adminApiKeyAuthenticator(adminApiKeyCredentials)
    case apiKeyCredentials: ApiKeyCredentials           => apiKeyAuthenticator(apiKeyCredentials)
    case keycloakCredentials: JwtCredentials            => keycloakAuthenticator(keycloakCredentials)
  }

}

object DefaultAuthenticator {
  val layer: URLayer[AdminApiKeyAuthenticator & ApiKeyAuthenticator & KeycloakAuthenticator, DefaultAuthenticator] =
    ZLayer.fromFunction(DefaultAuthenticator(_, _, _))
}
