package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authentication.AuthenticationError.InvalidCredentials
import io.iohk.atala.iam.authentication.AuthenticatorWithAuthZ
import io.iohk.atala.iam.authentication.Credentials
import pdi.jwt.JwtCirce
import pdi.jwt.JwtClaim
import pdi.jwt.JwtOptions
import zio.*
import zio.json.ast.Json

import java.util.UUID

opaque type AccessToken = (String, JwtClaim)

object AccessToken {
  def fromString(token: String): Either[String, AccessToken] =
    JwtCirce
      .decode(token, JwtOptions(false, false, false))
      .map(token -> _)
      .toEither
      .left
      .map(e => s"JWT token cannot be decoded. ${e.getMessage()}")

  extension (token: AccessToken) {
    def toString: String = token._1

    def isRpt: Either[String, Boolean] =
      Json.decoder
        .decodeJson(token._2.content)
        .flatMap(_.asObject.toRight("JWT payload must be a JSON object"))
        .map(_.contains("authorization"))
  }
}

final case class KeycloakEntity(id: UUID, accessToken: Option[AccessToken] = None, rpt: Option[AccessToken] = None)
    extends BaseEntity

trait KeycloakAuthenticator extends AuthenticatorWithAuthZ[KeycloakEntity] {
  def authenticate(credentials: Credentials): IO[AuthenticationError, KeycloakEntity] = {
    if (isEnabled) {
      credentials match {
        case JwtCredentials(Some(token)) if token.nonEmpty => authenticate(token)
        case JwtCredentials(Some(_)) =>
          ZIO.logInfo(s"Keycloak authentication is enabled, but bearer token is empty") *>
            ZIO.fail(JwtAuthenticationError.emptyToken)
        case JwtCredentials(None) =>
          ZIO.logInfo(s"Keycloak authentication is enabled, but bearer token is not provided") *>
            ZIO.fail(InvalidCredentials("Bearer token is not provided"))
        case other =>
          ZIO.fail(InvalidCredentials("Bearer token is not provided"))
      }
    } else ZIO.fail(AuthenticationMethodNotEnabled("Keycloak authentication is not enabled"))
  }

  def authenticate(token: String): IO[AuthenticationError, KeycloakEntity]
}
