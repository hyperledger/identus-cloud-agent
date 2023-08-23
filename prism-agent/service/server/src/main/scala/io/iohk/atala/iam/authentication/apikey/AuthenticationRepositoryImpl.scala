package io.iohk.atala.iam.authentication.apikey

import doobie.*
import doobie.implicits.*
import zio.*
import zio.interop.catz.*

import java.util.UUID

case class AuthenticationRepositoryImpl(xa: Transactor[Task]) extends AuthenticationRepository {

  import AuthenticationRepositorySql.*
  override def insert(
      entityId: UUID,
      authenticationMethodType: AuthenticationMethodType,
      secret: String
  ): IO[AuthenticationRepositoryError, Unit] = {
    val authenticationMethod = AuthenticationMethod(UUID.randomUUID(), authenticationMethodType, entityId, secret)
    AuthenticationRepositorySql
      .insert(authenticationMethod)
      .transact(xa)
      .logError(
        s"insert failed for entityId: $entityId, authenticationMethod: $authenticationMethod, and secret: $secret"
      )
      .mapError(AuthenticationRepositoryError.StorageError.apply)
      .map(_ => ())
  }

  override def getEntityIdByMethodAndSecret(
      method: AuthenticationMethodType,
      secret: String
  ): IO[AuthenticationRepositoryError, UUID] = {
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

  override def deleteById(id: UUID): IO[AuthenticationRepositoryError, Unit] = {
    AuthenticationRepositorySql
      .deleteById(id)
      .transact(xa)
      .logError(s"deleteById failed for id: $id")
      .mapError(AuthenticationRepositoryError.StorageError.apply)
      .map(_ => ())
  }

  override def deleteByMethodAndEntityId(
      method: AuthenticationMethodType,
      entityId: UUID
  ): IO[AuthenticationRepositoryError, Unit] = {
    AuthenticationRepositorySql
      .deleteByMethodAndEntityId(method, entityId)
      .transact(xa)
      .logError(s"deleteByMethodAndEntityId failed for method: $method and entityId: $entityId")
      .mapError(AuthenticationRepositoryError.StorageError.apply)
      .map(_ => ())
  }
}
