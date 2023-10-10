package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.shared.models.WalletId
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.{Configuration => KeycloakAuthzConfig}
import org.keycloak.representations.idm.authorization.AuthorizationRequest
import pdi.jwt.JwtCirce
import pdi.jwt.JwtOptions
import zio.*
import zio.json.ast.Json

import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.Try

class KeycloakAuthenticatorImpl(
    client: AuthzClient,
    keycloakConfig: KeycloakConfig,
    walletService: WalletManagementService
) extends KeycloakAuthenticator {

  override def isEnabled: Boolean = keycloakConfig.enabled

  override def authenticate(token: String): IO[AuthenticationError, KeycloakEntity] = {
    if (isEnabled) {
      for {
        isRpt <- inferIsRpt(token)
        rptEffect =
          if (isRpt) ZIO.succeed(token)
          else if (keycloakConfig.autoUpgradeToRPT) obtainRpt(token)
          else ZIO.fail(AuthenticationError.InvalidCredentials(s"AccessToken is not RPT."))
        rpt <- rptEffect.logError("Fail to obtail RPT for wallet permissions")
        permittedResources <- introspectRpt(rpt)
        walletId <- getPermittedWallet(permittedResources)
      } yield KeycloakEntity(???, ???, token)
    } else ZIO.fail(AuthenticationMethodNotEnabled("Keycloak authentication is not enabled"))
  }

  override def authorize(entity: KeycloakEntity): IO[AuthenticationError, WalletId] = ??? // TODO

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
          ZIO.fail(AuthenticationError.UnexpectedError("Too many wallet access granted, access is ambiguous."))
      }
  }

  private def obtainRpt(accessToken: String): IO[AuthenticationError, String] = {
    ZIO
      .attemptBlocking {
        val authResource = client.authorization(accessToken)
        val request = AuthorizationRequest()
        authResource.authorize(request)
      }
      .logError
      .mapBoth(
        e => AuthenticationError.UnexpectedError(e.getMessage()),
        response => response.getToken()
      )
  }

  private def introspectRpt(rpt: String): IO[AuthenticationError, List[String]] = {
    for {
      introspection <- ZIO
        .attemptBlocking(client.protection().introspectRequestingPartyToken(rpt))
        .logError
        .mapError(e => AuthenticationError.UnexpectedError(e.getMessage()))
      permissions = introspection.getPermissions().asScala.toList
    } yield permissions.map(_.getResourceId())
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
  val layer: RLayer[KeycloakConfig & WalletManagementService, KeycloakAuthenticator] = ZLayer.fromZIO {
    for {
      walletService <- ZIO.service[WalletManagementService]
      keycloakConfig <- ZIO.service[KeycloakConfig]
      config = KeycloakAuthzConfig(
        keycloakConfig.keycloakUrl.toString(),
        keycloakConfig.realmName,
        keycloakConfig.clientId,
        Map("secret" -> keycloakConfig.clientSecret).asJava,
        null
      )
      client <- ZIO.attempt(AuthzClient.create(config))
    } yield KeycloakAuthenticatorImpl(client, keycloakConfig, walletService)
  }
}
