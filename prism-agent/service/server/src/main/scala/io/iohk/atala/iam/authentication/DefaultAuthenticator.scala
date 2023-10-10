package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.admin.{AdminApiKeyAuthenticator, AdminApiKeyCredentials}
import io.iohk.atala.iam.authentication.apikey.{ApiKeyAuthenticator, ApiKeyCredentials}
import io.iohk.atala.iam.authentication.oidc.KeycloakEntity
import io.iohk.atala.iam.authentication.oidc.{KeycloakAuthenticator, JwtCredentials}
import io.iohk.atala.shared.models.WalletId
import zio.*

case class DefaultAuthenticator(
    adminApiKeyAuthenticator: AdminApiKeyAuthenticator,
    apiKeyAuthenticator: ApiKeyAuthenticator,
    keycloakAuthenticator: KeycloakAuthenticator
) extends Authenticator[BaseEntity], Authorizer[BaseEntity] {

  override def isEnabled = true

  override def authenticate(credentials: Credentials): IO[AuthenticationError, BaseEntity] = credentials match {
    case adminApiKeyCredentials: AdminApiKeyCredentials => adminApiKeyAuthenticator(adminApiKeyCredentials)
    case apiKeyCredentials: ApiKeyCredentials           => apiKeyAuthenticator(apiKeyCredentials)
    case keycloakCredentials: JwtCredentials            => keycloakAuthenticator(keycloakCredentials)
  }

  override def authorize(entity: BaseEntity): IO[AuthenticationError, WalletId] = entity match {
    case entity: Entity => DefaultEntityAuthenticator.authorize(entity)
    case kcEntity: KeycloakEntity => keycloakAuthenticator.authorize(kcEntity)
  }

}

object DefaultAuthenticator {
  val layer: URLayer[AdminApiKeyAuthenticator & ApiKeyAuthenticator & KeycloakAuthenticator, DefaultAuthenticator] =
    ZLayer.fromFunction(DefaultAuthenticator(_, _, _))
}
