package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.agent.walletapi.model.error.EntityServiceError
import io.iohk.atala.agent.walletapi.sql.EntityRepository
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

  def deleteById(entityId: UUID): IO[EntityServiceError, Unit] = {
    for {
      _ <- repository.delete(entityId)
      _ <- ZIO.logInfo(s"Entity deleted: $entityId")
    } yield ()
  } logError (s"Entity deletion failed for $entityId")
}

object EntityServiceImpl {
  val layer: URLayer[EntityRepository, EntityService] =
    ZLayer.fromFunction(new EntityServiceImpl(_))
}
