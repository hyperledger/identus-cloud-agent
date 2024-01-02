package io.iohk.atala.iam.authentication.oidc

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.iam.authentication.AuthenticationError
import io.iohk.atala.iam.authentication.AuthenticationError.AuthenticationMethodNotEnabled
import io.iohk.atala.iam.authentication.AuthenticationError.InvalidCredentials
import io.iohk.atala.iam.authentication.AuthenticatorWithAuthZ
import io.iohk.atala.iam.authentication.Credentials
import io.iohk.atala.shared.utils.Traverse.*
import pdi.jwt.JwtCirce
import pdi.jwt.JwtClaim
import pdi.jwt.JwtOptions
import zio.*
import zio.json.ast.Json
import zio.json.ast.JsonCursor

import java.util.UUID

final class AccessToken private (token: String, claims: JwtClaim) {
  override def toString(): String = token

  def isRpt: Either[String, Boolean] =
    Json.decoder
      .decodeJson(claims.content)
      .flatMap(_.asObject.toRight("JWT payload must be a JSON object"))
      .map(_.contains("authorization"))

  def containsAdminRole: Either[String, Boolean] =
    clientRoles.map(_.contains("agent-admin"))

  def clientRoles: Either[String, Seq[String]] =
    Json.decoder
      .decodeJson(claims.content)
      .flatMap { json =>
        for {
          resourceAccess <- json.get(JsonCursor.field("resource_access"))
          client <- resourceAccess.get(JsonCursor.field("prism-agent"))
          roleJson <- client.get(JsonCursor.field("roles"))
          roles <- roleJson.asArray
            .toRight("roles claim is not a JSON array of strings.")
            .map(_.flatMap(_.asString))
        } yield roles
      }

}

object AccessToken {
  def fromString(token: String): Either[String, AccessToken] =
    JwtCirce
      .decode(token, JwtOptions(false, false, false))
      .map(claims => AccessToken(token, claims))
      .toEither
      .left
      .map(e => s"JWT token cannot be decoded. ${e.getMessage()}")
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
