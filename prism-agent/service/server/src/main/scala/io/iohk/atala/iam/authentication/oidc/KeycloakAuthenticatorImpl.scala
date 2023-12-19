package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authorization.core.PermissionManagement
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletAdministrationContext
import zio.*

import java.util.UUID

class KeycloakAuthenticatorImpl(
    client: KeycloakClient,
    keycloakConfig: KeycloakConfig,
    keycloakPermissionService: PermissionManagement.Service[KeycloakEntity],
) extends KeycloakAuthenticator {

  override def isEnabled: Boolean = keycloakConfig.enabled

  override def authenticate(token: String): IO[AuthenticationError, KeycloakEntity] = {
    if (isEnabled) {
      for {
        accessToken <- ZIO
          .fromEither(AccessToken.fromString(token))
          .mapError(AuthenticationError.InvalidCredentials.apply)
        introspection <- client
          .introspectToken(accessToken)
          .mapError(e => AuthenticationError.UnexpectedError(e.message))
        _ <- ZIO
          .fail(AuthenticationError.InvalidCredentials("The accessToken is invalid."))
          .unless(introspection.active)
        entityId <- ZIO
          .fromOption(introspection.sub)
          .mapError(_ => AuthenticationError.UnexpectedError("Subject ID is not found in the accessToken."))
          .flatMap { id =>
            ZIO
              .attempt(UUID.fromString(id))
              .mapError(e => AuthenticationError.UnexpectedError(s"Subject ID in accessToken is not a UUID. $e"))
          }
      } yield KeycloakEntity(entityId, accessToken = Some(accessToken))
    } else ZIO.fail(AuthenticationMethodNotEnabled("Keycloak authentication is not enabled"))
  }

  override def authorize(entity: KeycloakEntity): IO[AuthenticationError, WalletAccessContext] = {
    for {
      entityWithRpt <- populateEntityRpt(entity)
      walletId <- keycloakPermissionService
        .listWalletPermissions(entityWithRpt)
        .mapError(e => AuthenticationError.UnexpectedError(e.message))
        .flatMap {
          case head +: Nil => ZIO.succeed(head)
          case Nil =>
            ZIO.fail(AuthenticationError.ResourceNotPermitted("No wallet permissions found."))
          case ls =>
            ZIO.fail(
              AuthenticationError.UnexpectedError("Too many wallet access granted, the wallet access is ambiguous.")
            )
        }
        .provide(ZLayer.succeed(WalletAdministrationContext.Admin()))
    } yield WalletAccessContext(walletId)
  }

  override def authorizeWalletAdmin(entity: KeycloakEntity): IO[AuthenticationError, WalletAdministrationContext] = {
    val containsAdminRole = entity.accessToken.flatMap(_.containsAdminRole.toOption).getOrElse(false)
    val tenantContext = for {
      entityWithRpt <- populateEntityRpt(entity)
      wallets <- keycloakPermissionService
        .listWalletPermissions(entityWithRpt)
        .mapError(e => AuthenticationError.UnexpectedError(e.message))
        .provide(ZLayer.succeed(WalletAdministrationContext.Admin()))
    } yield WalletAdministrationContext.SelfService(wallets)

    if (containsAdminRole) ZIO.succeed(WalletAdministrationContext.Admin())
    else tenantContext
  }

  private def populateEntityRpt(entity: KeycloakEntity): IO[AuthenticationError, KeycloakEntity] = {
    for {
      token <- ZIO
        .fromOption(entity.accessToken)
        .mapError(_ => AuthenticationError.InvalidCredentials("AccessToken is missing."))
      isRpt <- ZIO
        .fromEither(token.isRpt)
        .mapError(AuthenticationError.InvalidCredentials.apply)
      rptEffect =
        if (isRpt) ZIO.succeed(token)
        else if (keycloakConfig.autoUpgradeToRPT)
          client
            .getRpt(token)
            .mapError(e => AuthenticationError.UnexpectedError(e.message))
        else ZIO.fail(AuthenticationError.InvalidCredentials(s"AccessToken is not RPT."))
      rpt <- rptEffect.logError("Fail to obtail RPT for wallet permissions")
    } yield entity.copy(rpt = Some(rpt))
  }
}

object KeycloakAuthenticatorImpl {
  val layer: RLayer[
    KeycloakClient & KeycloakConfig & PermissionManagement.Service[KeycloakEntity],
    KeycloakAuthenticator
  ] =
    ZLayer.fromFunction(KeycloakAuthenticatorImpl(_, _, _))

  val disabled: ULayer[KeycloakAuthenticator] =
    ZLayer.succeed {
      val notEnabledError = ZIO.fail(AuthenticationMethodNotEnabled("Keycloak authentication is not enabled"))
      new KeycloakAuthenticator {
        override def isEnabled: Boolean = false
        override def authenticate(token: String): IO[AuthenticationError, KeycloakEntity] = notEnabledError
        override def authorize(entity: KeycloakEntity): IO[AuthenticationError, WalletAccessContext] = notEnabledError
        override def authorizeWalletAdmin(
            entity: KeycloakEntity
        ): IO[AuthenticationError, WalletAdministrationContext] =
          notEnabledError
      }
    }
}
