package io.iohk.atala.iam.authentication.apikey

import doobie.*
import doobie.implicits.*
import org.postgresql.util.PSQLException
import zio.*
import zio.interop.catz.*

import java.time.OffsetDateTime
import java.util.UUID

case class JdbcAuthenticationRepository(xa: Transactor[Task]) extends AuthenticationRepository {

  import AuthenticationRepositorySql.*
  override def insert(
      entityId: UUID,
      amt: AuthenticationMethodType,
      secret: String
  ): IO[AuthenticationRepositoryError, Unit] = {
    val authenticationMethod = AuthenticationMethod(amt, entityId, secret)
    AuthenticationRepositorySql
      .insert(authenticationMethod)
      .transact(xa)
      .map(_ => ())
      .logError(
        s"insert failed for entityId: $entityId, authenticationMethodType: $amt, and secret: $secret"
      )
      .mapError {
        case sqlException: PSQLException
            if sqlException.getMessage
              .contains("ERROR: duplicate key value violates unique constraint \"unique_type_secret_constraint\"") =>
          AuthenticationRepositoryError.AuthenticationCompromised(entityId, amt, secret)
        case otherSqlException: PSQLException =>
          AuthenticationRepositoryError.StorageError(otherSqlException)
        case unexpected: Throwable =>
          AuthenticationRepositoryError.UnexpectedError(unexpected)
      }
      .catchSome { case ac @ AuthenticationRepositoryError.AuthenticationCompromised(entityId, amt, secret) =>
        val failure: IO[AuthenticationRepositoryError, Unit] = ZIO.fail(ac)
        deleteByEntityIdAndTypeAndSecret(entityId, amt, secret).map(_ => ()) *> failure
      }
  }

  override def getEntityIdByMethodAndSecret(
      amt: AuthenticationMethodType,
      secret: String
  ): IO[AuthenticationRepositoryError, UUID] = {
    AuthenticationRepositorySql
      .getEntityIdByMethodAndSecret(amt, secret)
      .transact(xa)
      .logError(s"getEntityIdByMethodAndSecret failed for method: $amt and secret: $secret")
      .mapError(AuthenticationRepositoryError.StorageError.apply)
      .flatMap(
        _.headOption.fold(ZIO.fail(AuthenticationRepositoryError.AuthenticationNotFound(amt, secret)))(entityId =>
          ZIO.succeed(entityId)
        )
      )
  }

  override def findAuthenticationMethodByTypeAndSecret(
      amt: AuthenticationMethodType,
      secret: String
  ): IO[AuthenticationRepositoryError, Option[AuthenticationMethod]] = {
    AuthenticationRepositorySql
      .filterByTypeAndSecret(amt, secret)
      .transact(xa)
      .logError(s"findAuthenticationMethodBySecret failed for secret:$secret")
      .map(_.headOption)
      .mapError(AuthenticationRepositoryError.StorageError.apply)
  }

  override def deleteByMethodAndEntityId(
      entityId: UUID,
      amt: AuthenticationMethodType
  ): IO[AuthenticationRepositoryError, Unit] = {
    AuthenticationRepositorySql
      .softDeleteByEntityIdAndType(entityId, amt, Some(OffsetDateTime.now()))
      .transact(xa)
      .logError(s"deleteByMethodAndEntityId failed for method: $amt and entityId: $entityId")
      .mapError(AuthenticationRepositoryError.StorageError.apply)
      .map(_ => ())
  }

  override def deleteByEntityIdAndTypeAndSecret(
      entityId: UUID,
      amt: AuthenticationMethodType,
      secret: String
  ): IO[AuthenticationRepositoryError, Unit] = {
    AuthenticationRepositorySql
      .softDeleteBy(entityId, amt, secret, Some(OffsetDateTime.now()))
      .transact(xa)
      .logError(s"deleteByEntityIdAndSecret failed for id: $entityId and secret: $secret")
      .mapError(AuthenticationRepositoryError.StorageError.apply)
      .map(_ => ())
  }

  def checkDeleted(method: AuthenticationMethodType, secret: String) = {
    AuthenticationRepositorySql
      .getEntityIdByMethodAndSecret(method, secret)
      .transact(xa)
      .logError(s"getEntityIdByMethodAndSecret failed for method: $method and secret: $secret")
      .mapError(AuthenticationRepositoryError.StorageError.apply)
      .flatMap(
        _.headOption.fold(ZIO.fail(AuthenticationRepositoryError.AuthenticationNotFound(method, secret)))(entityId =>
          ZIO.succeed(entityId)
        )
      )
  }
}

object JdbcAuthenticationRepository {
  val layer: URLayer[Transactor[Task], AuthenticationRepository] =
    ZLayer.fromFunction(JdbcAuthenticationRepository(_))
}
