package io.iohk.atala.iam.authentication.apikey

import io.getquill.context.json.PostgresJsonExtensions
import io.getquill.doobie.DoobieContext
import io.getquill.*
import zio.IO
import zio.*
import zio.interop.catz.*

import java.time.OffsetDateTime
import java.util.UUID

enum AuthenticationMethodType(val value: String) {
  case ApiKey extends AuthenticationMethodType("apikey")
}

object AuthenticationMethodType {
  def fromString(value: String) = {
    AuthenticationMethodType.values.find(_.value == value).get
  }
}

case class AuthenticationMethod(
    `type`: AuthenticationMethodType,
    entityId: UUID,
    secret: String,
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    deletedAt: Option[OffsetDateTime] = None
) {
  def isDeleted = deletedAt.isDefined
}

trait AuthenticationRepository {
  def insert(
      entityId: UUID,
      amt: AuthenticationMethodType,
      secret: String
  ): zio.IO[AuthenticationRepositoryError, Unit]

  def getEntityIdByMethodAndSecret(
      amt: AuthenticationMethodType,
      secret: String
  ): zio.IO[AuthenticationRepositoryError, UUID]

  def findAuthenticationMethodByTypeAndSecret(
      amt: AuthenticationMethodType,
      secret: String
  ): zio.IO[AuthenticationRepositoryError, Option[AuthenticationMethod]]

  def deleteByMethodAndEntityId(
      entityId: UUID,
      amt: AuthenticationMethodType
  ): zio.IO[AuthenticationRepositoryError, Unit]

  def delete(
      entityId: UUID,
      amt: AuthenticationMethodType,
      secret: String
  ): zio.IO[AuthenticationRepositoryError, Unit]
}

//TODO: reconsider the hierarchy of the service and dal layers
sealed trait AuthenticationRepositoryError {
  def message: String
}

object AuthenticationRepositoryError {

  def hide(secret: String) = secret.take(8) + "****"
  case class AuthenticationNotFound(authenticationMethodType: AuthenticationMethodType, secret: String)
      extends AuthenticationRepositoryError {
    def message =
      s"Authentication method not found for type:${authenticationMethodType.value} and secret:${hide(secret)}"
  }

  case class AuthenticationCompromised(
      entityId: UUID,
      authenticationMethodType: AuthenticationMethodType,
      secret: String
  ) extends AuthenticationRepositoryError {
    def message =
      s"Authentication method is compromised for entityId:$entityId, type:${authenticationMethodType.value}, and secret:${hide(secret)}"
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
    MappedEncoding[String, AuthenticationMethodType](AuthenticationMethodType.fromString)

  def insert(authenticationMethod: AuthenticationMethod) = {
    run {
      quote {
        query[AuthenticationMethod].insertValue(lift(authenticationMethod))
      }
    }
  }

  def getEntityIdByMethodAndSecret(amt: AuthenticationMethodType, secret: String) = {
    run {
      quote {
        query[AuthenticationMethod]
          .filter(am => am.secret == lift(secret) && am.`type` == lift(amt) && am.deletedAt.isEmpty)
          .map(_.entityId)
          .take(1)
      }
    }
  }

  def filterByTypeAndSecret(amt: AuthenticationMethodType, secret: String) = {
    run {
      quote {
        query[AuthenticationMethod]
          .filter(am => am.secret == lift(secret) && am.`type` == lift(amt))
      }
    }
  }

  def softDeleteByEntityIdAndType(
      entityId: UUID,
      amt: AuthenticationMethodType,
      deletedAt: Option[OffsetDateTime]
  ) = {
    run {
      quote {
        query[AuthenticationMethod]
          .filter(am => am.`type` == lift(amt) && am.entityId == lift(entityId))
          .update(_.deletedAt -> lift(deletedAt))
      }
    }
  }

  def softDeleteBy(
      entityId: UUID,
      amt: AuthenticationMethodType,
      secret: String,
      deletedAt: Option[OffsetDateTime]
  ) = {
    run {
      quote {
        query[AuthenticationMethod]
          .filter(am => am.entityId == lift(entityId) && am.`type` == lift(amt) && am.secret == lift(secret))
          .update(_.deletedAt -> lift(deletedAt))
      }
    }
  }
}
