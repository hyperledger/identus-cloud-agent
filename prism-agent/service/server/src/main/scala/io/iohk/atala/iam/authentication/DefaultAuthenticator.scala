package io.iohk.atala.iam.authentication

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.iam.authentication.admin.{AdminApiKeyAuthenticator, AdminApiKeyCredentials}
import io.iohk.atala.iam.authentication.apikey.{ApiKeyAuthenticator, ApiKeyCredentials}
import io.iohk.atala.iam.authentication.oidc.KeycloakEntity
import io.iohk.atala.iam.authentication.oidc.{KeycloakAuthenticator, JwtCredentials}
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletAdministrationContext
import zio.*

case class DefaultAuthenticator(
    adminApiKeyAuthenticator: AdminApiKeyAuthenticator,
    apiKeyAuthenticator: ApiKeyAuthenticator,
    keycloakAuthenticator: KeycloakAuthenticator
) extends AuthenticatorWithAuthZ[BaseEntity] {

  override def isEnabled = true

  override def authenticate(credentials: Credentials): IO[AuthenticationError, BaseEntity] = credentials match {
    case adminApiKeyCredentials: AdminApiKeyCredentials => adminApiKeyAuthenticator(adminApiKeyCredentials)
    case apiKeyCredentials: ApiKeyCredentials           => apiKeyAuthenticator(apiKeyCredentials)
    case keycloakCredentials: JwtCredentials            => keycloakAuthenticator(keycloakCredentials)
  }

  override def authorizeWalletAccessLogic(entity: BaseEntity): IO[AuthenticationError, WalletAccessContext] =
    entity match {
      case entity: Entity           => EntityAuthorizer.authorizeWalletAccess(entity)
      case kcEntity: KeycloakEntity => keycloakAuthenticator.authorizeWalletAccess(kcEntity)
    }

  override def authorizeWalletAdmin(entity: BaseEntity): IO[AuthenticationError, WalletAdministrationContext] =
    entity match {
      case entity: Entity           => EntityAuthorizer.authorizeWalletAdmin(entity)
      case kcEntity: KeycloakEntity => keycloakAuthenticator.authorizeWalletAdmin(kcEntity)
    }

}

object DefaultAuthenticator {
  val layer: URLayer[AdminApiKeyAuthenticator & ApiKeyAuthenticator & KeycloakAuthenticator, DefaultAuthenticator] =
    ZLayer.fromFunction(DefaultAuthenticator(_, _, _))
}
