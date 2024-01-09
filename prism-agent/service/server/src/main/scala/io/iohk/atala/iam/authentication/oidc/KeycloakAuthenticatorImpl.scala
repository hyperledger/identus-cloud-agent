package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.agent.walletapi.model.EntityRole
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

  private val roleClaimPath = keycloakConfig.rolesClaimPath.split('.').toSeq

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
      } yield KeycloakEntity(entityId, accessToken = Some(accessToken), roleClaimPath = roleClaimPath)
    } else ZIO.fail(AuthenticationMethodNotEnabled("Keycloak authentication is not enabled"))
  }

  override def authorizeWalletAccess(entity: KeycloakEntity): IO[AuthenticationError, WalletAccessContext] = {
    for {
      role <- ZIO
        .fromOption(entity.accessToken)
        .mapError(_ => AuthenticationError.InvalidCredentials("AccessToken is missing."))
        .map(_.role(roleClaimPath).left.map(AuthenticationError.InvalidCredentials(_)))
        .absolve
      walletId <- keycloakPermissionService
        .listWalletPermissions(entity)
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
    val selfServiceCtx = keycloakPermissionService
      .listWalletPermissions(entity)
      .provide(ZLayer.succeed(WalletAdministrationContext.Admin()))
      .mapBoth(
        e => AuthenticationError.UnexpectedError(e.message),
        wallets => WalletAdministrationContext.SelfService(wallets)
      )

    for {
      role <- ZIO
        .fromOption(entity.accessToken)
        .mapError(_ => AuthenticationError.InvalidCredentials("AccessToken is missing."))
        .map(_.role(roleClaimPath).left.map(AuthenticationError.InvalidCredentials(_)))
        .absolve
      ctx <- role match {
        case EntityRole.Admin  => ZIO.succeed(WalletAdministrationContext.Admin())
        case EntityRole.Tenant => selfServiceCtx
      }
    } yield ctx
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
        override def authorizeWalletAccess(entity: KeycloakEntity): IO[AuthenticationError, WalletAccessContext] =
          notEnabledError
        override def authorizeWalletAdmin(
            entity: KeycloakEntity
        ): IO[AuthenticationError, WalletAdministrationContext] =
          notEnabledError
      }
    }
}
