package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.shared.models.WalletId
import pdi.jwt.JwtCirce
import pdi.jwt.JwtOptions
import zio.*
import zio.json.ast.Json

import java.util.UUID
import scala.util.Try

class KeycloakAuthenticatorImpl(
    client: KeycloakClient,
    keycloakConfig: KeycloakConfig,
    walletService: WalletManagementService
) extends KeycloakAuthenticator {

  override def isEnabled: Boolean = keycloakConfig.enabled

  override def authenticate(token: String): IO[AuthenticationError, KeycloakEntity] = {
    if (isEnabled) {
      for {
        introspection <- client.introspectToken(token)
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
      } yield KeycloakEntity(entityId, token)
    } else ZIO.fail(AuthenticationMethodNotEnabled("Keycloak authentication is not enabled"))
  }

  override def authorize(entity: KeycloakEntity): IO[AuthenticationError, WalletId] = {
    val token = entity.rawToken
    for {
      isRpt <- inferIsRpt(entity.rawToken)
      rptEffect =
        if (isRpt) ZIO.succeed(token)
        else if (keycloakConfig.autoUpgradeToRPT) client.getRpt(token)
        else ZIO.fail(AuthenticationError.InvalidCredentials(s"AccessToken is not RPT."))
      rpt <- rptEffect.logError("Fail to obtail RPT for wallet permissions")
      permittedResources <- client.checkPermissions(rpt)
      walletId <- getPermittedWallet(permittedResources)
    } yield walletId
  }

  private def getPermittedWallet(resourceIds: Seq[String]): IO[AuthenticationError, WalletId] = {
    val walletIds = resourceIds.flatMap(id => Try(UUID.fromString(id)).toOption).map(WalletId.fromUUID)
    walletService
      .getWallets(walletIds)
      .mapError(e => AuthenticationError.UnexpectedError(e.toThrowable.getMessage()))
      .flatMap {
        case head +: Nil => ZIO.succeed(head.id)
        case Nil =>
          ZIO.fail(AuthenticationError.ResourceNotPermitted("No wallet permissions found."))
        case ls =>
          ZIO.fail(
            AuthenticationError.UnexpectedError("Too many wallet access granted, the wallet access is ambiguous.")
          )
      }
  }

  /** Return true if the token is RPT. Check whether property '.authorization' exists. */
  private def inferIsRpt(token: String): IO[AuthenticationError, Boolean] =
    ZIO
      .fromTry(JwtCirce.decode(token, JwtOptions(false, false, false)))
      .mapError(e => AuthenticationError.InvalidCredentials(s"JWT token cannot be decoded. ${e.getMessage()}"))
      .flatMap { claims =>
        ZIO
          .fromEither(Json.decoder.decodeJson(claims.content))
          .mapError(s => AuthenticationError.InvalidCredentials(s"Unable to decode JWT payload to JSON. $s"))
      }
      .flatMap { json =>
        ZIO
          .fromOption(json.asObject)
          .mapError(_ => AuthenticationError.InvalidCredentials(s"JWT payload must be a JSON object"))
          .map(obj => obj.contains("authorization"))
      }
}

object KeycloakAuthenticatorImpl {
  val layer: RLayer[KeycloakClient & KeycloakConfig & WalletManagementService, KeycloakAuthenticator] =
    ZLayer.fromFunction(KeycloakAuthenticatorImpl(_, _, _))

  val disabled: ULayer[KeycloakAuthenticator] =
    ZLayer.succeed {
      val notEnabledError = ZIO.fail(AuthenticationMethodNotEnabled("Keycloak authentication is not enabled"))
      new KeycloakAuthenticator {
        override def isEnabled: Boolean = false
        override def authenticate(token: String): IO[AuthenticationError, KeycloakEntity] = notEnabledError
        override def authorize(entity: KeycloakEntity): IO[AuthenticationError, WalletId] = notEnabledError
      }
    }
}
