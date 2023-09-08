package io.iohk.atala.iam.authentication.apikey

import io.getquill.context.json.PostgresJsonExtensions
import io.getquill.doobie.DoobieContext
import io.getquill.*
import zio.IO
import zio.*
import zio.interop.catz.*

import java.util.UUID

enum AuthenticationMethodType(val value: String) {
  case ApiKey extends AuthenticationMethodType("api-key")
}

case class AuthenticationMethod(
    id: UUID,
    `type`: AuthenticationMethodType,
    entityId: UUID,
    secret: String
)

trait AuthenticationRepository {
  def insert(
      entityId: UUID,
      authenticationMethod: AuthenticationMethodType,
      secret: String
  ): zio.IO[AuthenticationRepositoryError, UUID]

  def getEntityIdByMethodAndSecret(
      method: AuthenticationMethodType,
      secret: String
  ): zio.IO[AuthenticationRepositoryError, UUID]

  def deleteById(id: UUID): zio.IO[AuthenticationRepositoryError, Unit]

  def deleteByMethodAndEntityId(
      method: AuthenticationMethodType,
      entityId: UUID
  ): zio.IO[AuthenticationRepositoryError, Unit]

  def deleteByEntityIdAndSecret(id: UUID, secret: String): zio.IO[AuthenticationRepositoryError, Unit]
}

type AuthenticationMethodConfiguration = zio.json.ast.Json

sealed trait AuthenticationRepositoryError {
  def message: String
}

object AuthenticationRepositoryError {
  case class AuthenticationNotFound(authenticationMethodType: AuthenticationMethodType, secret: String)
      extends AuthenticationRepositoryError {
    def message =
      s"Authentication method not found for type ${authenticationMethodType.value} and secret $secret"
  }

  case class ServiceError(message: String) extends AuthenticationRepositoryError
  case class StorageError(cause: Throwable) extends AuthenticationRepositoryError {
    def message = cause.getMessage
  }

  case class UnexpectedError(cause: Throwable) extends AuthenticationRepositoryError {
    def message = cause.getMessage
  }
}

object AuthenticationRepositorySql extends DoobieContext.Postgres(SnakeCase) with PostgresJsonExtensions {

  implicit val authenticationMethodType2String: MappedEncoding[AuthenticationMethodType, String] =
    MappedEncoding[AuthenticationMethodType, String](_.value)

  implicit val string2AuthenticationMethodType: MappedEncoding[String, AuthenticationMethodType] =
    MappedEncoding[String, AuthenticationMethodType](str => AuthenticationMethodType.valueOf(str))

  def insert(authenticationMethod: AuthenticationMethod) = {
    run {
      quote {
        query[AuthenticationMethod].insertValue(lift(authenticationMethod)).returning(_.id)
      }
    }
  }

  def getEntityIdByMethodAndSecret(method: AuthenticationMethodType, secret: String) = {
    run {
      quote {
        query[AuthenticationMethod]
          .filter(am => am.secret == lift(secret) && am.`type` == lift(method))
          .map(_.entityId)
          .take(1)
      }
    }
  }

  def deleteById(id: UUID) = {
    run {
      quote {
        query[AuthenticationMethod].filter(_.id == lift(id)).delete
      }
    }
  }

  def deleteByMethodAndEntityId(method: AuthenticationMethodType, entityId: UUID) = {
    run {
      quote {
        query[AuthenticationMethod].filter(am => am.`type` == lift(method) && am.entityId == lift(entityId)).delete
      }
    }
  }

  def deleteByEntityIdAndSecret(entityId: UUID, secret: String) = {
    run {
      quote {
        query[AuthenticationMethod]
          .filter(am =>
            am.entityId == lift(entityId) &&
              am.secret == lift(secret)
          )
          .delete
      }
    }
  }
}
