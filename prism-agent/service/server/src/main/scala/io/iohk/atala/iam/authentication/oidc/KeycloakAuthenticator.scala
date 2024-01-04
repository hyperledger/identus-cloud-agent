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

enum JwtRole(val value: String) {
  case Admin extends JwtRole("agent-admin")
  case Tenant extends JwtRole("agent-tenant")
}

object JwtRole {
  def fromString(s: String): Option[JwtRole] = {
    s match {
      case JwtRole.Admin.value  => Some(JwtRole.Admin)
      case JwtRole.Tenant.value => Some(JwtRole.Tenant)
      case _                    => None
    }
  }
}

final class AccessToken private (token: String, claims: JwtClaim) {

  override def toString(): String = token

  def isRpt: Either[String, Boolean] =
    Json.decoder
      .decodeJson(claims.content)
      .flatMap(_.asObject.toRight("JWT payload must be a JSON object"))
      .map(_.contains("authorization"))

  def role(claimPath: Seq[String]): Either[String, JwtRole] = {
    for {
      uniqueRoles <- extractRoles(claimPath).map(_.distinct)
      r <- uniqueRoles.toList match {
        case Nil => Right(JwtRole.Tenant)
        case r :: Nil => Right(r)
        case _ => Left(s"Multiple roles is not supported yet.")
      }
    } yield r
  }

  /** Return a list of roles that is meaningful to the agent */
  private def extractRoles(claimPath: Seq[String]): Either[String, Seq[JwtRole]] =
    Json.decoder
      .decodeJson(claims.content)
      .flatMap { json =>
        for {
          rolesJson <- claimPath.foldLeft[Either[String, Json]](Right(json)) { case (json, pathSegment) =>
            json.flatMap(_.get(JsonCursor.field(pathSegment)))
          }
          roles <- rolesJson.asArray
            .toRight("Roles claim is not a JSON array of strings.")
            .map(_.flatMap(_.asString))
            .map(_.flatMap(JwtRole.fromString))
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
