package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.agent.walletapi.model.error.EntityServiceError
import io.iohk.atala.agent.walletapi.model.error.EntityServiceError.{
  EntityAlreadyExists,
  EntityNotFound,
  EntityStorageError,
  EntityWalletNotFound
}
import org.postgresql.util.PSQLException
import zio.*
import zio.interop.catz.*

import java.util.UUID

class JdbcEntityRepository(xa: Transactor[Task]) extends EntityRepository {
  import EntityStorageSql.*
  override def insert(entity: Entity): IO[EntityServiceError, Entity] = {
    EntityStorageSql
      .insert(model2db(entity))
      .transact(xa)
      .logError(s"Insert entity failed: $entity")
      .mapError {
        case sqlException: PSQLException
            if sqlException.getMessage.contains("duplicate key value violates unique constraint") =>
          EntityAlreadyExists(entity.id, sqlException.getMessage)
        case sqlException: PSQLException
            if sqlException.getMessage
              .contains("violates foreign key constraint \"entity_wallet_id_fkey\"") =>
          EntityWalletNotFound(entity.id, entity.walletId)
        case other: Throwable =>
          EntityStorageError(other.getMessage)
      }
      .map(db2model)
  }
  override def getById(id: UUID): IO[EntityServiceError, Entity] = {
    EntityStorageSql
      .getById(id)
      .transact(xa)
      .map(_.headOption.map(db2model))
      .logError(s"Get entity by id=$id failed")
      .mapError(throwable => EntityStorageError(throwable.getMessage))
      .flatMap(
        _.fold[ZIO[Any, EntityServiceError, Entity]](ZIO.fail(EntityNotFound(id, s"Get entity by id=$id failed")))(
          ZIO.succeed
        )
      )
  }

  override def updateName(entityId: UUID, name: String): IO[EntityServiceError, Unit] = {
    EntityStorageSql
      .updateName(entityId, name)
      .transact(xa)
      .logError(s"Update entity name=$name by id=$entityId failed")
      .mapError(throwable => EntityStorageError(throwable.getMessage))
      .flatMap { updatedCount =>
        if updatedCount == 1 then ZIO.unit
        else ZIO.fail(EntityNotFound(entityId, s"Update entity name=$name by id=$entityId failed"))
      }
  }

  override def updateWallet(entityId: UUID, walletId: UUID): IO[EntityServiceError, Unit] = {
    EntityStorageSql
      .updateWallet(entityId, walletId)
      .transact(xa)
      .logError(s"Update entity walletId=$walletId by id=$entityId failed")
      .mapError {
        case sqlException: PSQLException
            if sqlException.getMessage
              .contains("violates foreign key constraint \"entity_wallet_id_fkey\"") =>
          EntityWalletNotFound(entityId, walletId)
        case other: Throwable => EntityStorageError(other.getMessage)
      }
      .flatMap(updatedCount =>
        if updatedCount == 1 then ZIO.unit
        else ZIO.fail(EntityNotFound(entityId, s"Update entity walletId=$walletId by id=$entityId failed"))
      )
  }

  override def delete(entityId: UUID): IO[EntityServiceError, Unit] = {
    EntityStorageSql
      .delete(entityId)
      .transact(xa)
      .logError(s"Delete entity failed: id=$entityId")
      .mapError(throwable => EntityStorageError(throwable.getMessage))
      .flatMap(deletedCount =>
        if deletedCount == 1 then ZIO.unit
        else ZIO.fail(EntityNotFound(entityId, s"Delete entity failed: id=$entityId"))
      )
  }

  override def getAll(offset: Index, limit: Index): IO[EntityServiceError, List[Entity]] = {
    EntityStorageSql
      .getAll(offset, limit)
      .transact(xa)
      .logError("Get all entities failed")
      .mapError(throwable => EntityStorageError(throwable.getMessage))
      .map(_.map(db2model))
  }
}

object JdbcEntityRepository {
  val layer: URLayer[Transactor[Task], EntityRepository] =
    ZLayer.fromFunction(new JdbcEntityRepository(_))
}
