package org.hyperledger.identus.agent.walletapi.service

import org.hyperledger.identus.agent.walletapi.model.Entity
import org.hyperledger.identus.agent.walletapi.model.error.EntityServiceError
import org.hyperledger.identus.agent.walletapi.sql.EntityRepository
import zio.{IO, URLayer, ZIO, ZLayer}

import java.util.UUID

class EntityServiceImpl(repository: EntityRepository) extends EntityService {
  def create(entity: Entity): IO[EntityServiceError, Entity] = {
    for {
      _ <- repository.insert(entity)
      _ <- ZIO.logInfo(s"Entity created: $entity")
    } yield entity
  } logError ("Entity creation failed")

  def getById(entityId: UUID): IO[EntityServiceError, Entity] = {
    for {
      entity <- repository
        .getById(entityId)
        .logError(s"Entity retrieval failed for $entityId")
    } yield entity
  }

  override def getAll(offset: Option[Int], limit: Option[Int]): IO[EntityServiceError, Seq[Entity]] = {
    for {
      entities <- repository
        .getAll(offset.getOrElse(0), limit.getOrElse(100))
        .logError("Entity retrieval failed")
    } yield entities
  }

  def deleteById(entityId: UUID): IO[EntityServiceError, Unit] = {
    for {
      _ <- repository.delete(entityId)
      _ <- ZIO.logInfo(s"Entity deleted: $entityId")
    } yield ()
  } logError (s"Entity deletion failed for $entityId")

  override def updateName(entityId: UUID, name: String): IO[EntityServiceError, Unit] = {
    for {
      _ <- repository
        .updateName(entityId, name)
        .logError(s"Entity name update failed for $entityId")
    } yield ()
  }

  override def assignWallet(entityId: UUID, walletId: UUID): IO[EntityServiceError, Unit] = {
    for {
      _ <- repository
        .updateWallet(entityId, walletId)
        .logError(s"Entity wallet assignment failed for $entityId")
    } yield ()
  }
}

object EntityServiceImpl {
  val layer: URLayer[EntityRepository, EntityService] =
    ZLayer.fromFunction(new EntityServiceImpl(_))
}
