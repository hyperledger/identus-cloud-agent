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

import java.util.UUID
import io.iohk.atala.agent.walletapi.model.EntityRole

enum JwtRole(val name: String) {
  case Admin extends JwtRole("agent-admin")
  case Tenant extends JwtRole("agent-tenant")
}

object JwtRole {
  private val lookup = JwtRole.values.map(i => i.name -> i).toMap
  def parseString(s: String): Option[JwtRole] = lookup.get(s)
}

final class AccessToken private (token: String, claims: JwtClaim) {

  override def toString(): String = token

  def isRpt: Either[String, Boolean] =
    Json.decoder
      .decodeJson(claims.content)
      .flatMap(_.asObject.toRight("JWT payload must be a JSON object"))
      .map(_.contains("authorization"))

  // TODO: move to KeycloakEntity?
  def role(claimPath: Seq[String]): Either[String, JwtRole] = {
    for {
      uniqueRoles <- extractRoles(claimPath).map(_.getOrElse(Nil).distinct)
      r <- uniqueRoles.toList match {
        case Nil      => Right(JwtRole.Tenant)
        case r :: Nil => Right(r)
        case _        => Left(s"Multiple roles is not supported yet.")
      }
    } yield r
  }

  /** Return a list of roles that is meaningful to the agent */
  private def extractRoles(claimPath: Seq[String]): Either[String, Option[Seq[JwtRole]]] =
    Json.decoder
      .decodeJson(claims.content)
      .flatMap { json =>
        val rolesJson = claimPath.foldLeft[Option[Json]](Some(json)) { case (acc, pathSegment) =>
          acc.flatMap(_.asObject).flatMap(_.get(pathSegment))
        }
        rolesJson match {
          case Some(json) =>
            json.asArray
              .toRight("Roles claim is not a JSON array of strings.")
              .map(_.flatMap(_.asString).flatMap(JwtRole.parseString))
              .map(Some(_))
          case None => Right(None)
        }
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

final case class KeycloakEntity(id: UUID, accessToken: Option[AccessToken] = None) extends BaseEntity {
  override def role: Task[EntityRole] = ???
}

trait KeycloakAuthenticator extends AuthenticatorWithAuthZ[KeycloakEntity] {
  def authenticate(credentials: Credentials): IO[AuthenticationError, KeycloakEntity] = {
    if (isEnabled) {
      credentials match {
        case JwtCredentials(Some(token)) if token.nonEmpty => authenticate(token)
        case JwtCredentials(Some(_)) =>
          ZIO.logDebug(s"Keycloak authentication is enabled, but bearer token is empty") *>
            ZIO.fail(JwtAuthenticationError.emptyToken)
        case JwtCredentials(None) =>
          ZIO.logDebug(s"Keycloak authentication is enabled, but bearer token is not provided") *>
            ZIO.fail(InvalidCredentials("Bearer token is not provided"))
        case other =>
          ZIO.fail(InvalidCredentials("Bearer token is not provided"))
      }
    } else ZIO.fail(AuthenticationMethodNotEnabled("Keycloak authentication is not enabled"))
  }

  def authenticate(token: String): IO[AuthenticationError, KeycloakEntity]
}
