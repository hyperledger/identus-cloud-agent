package org.hyperledger.identus.agent.walletapi.service

import org.hyperledger.identus.agent.walletapi.model.Entity
import org.hyperledger.identus.agent.walletapi.model.error.EntityServiceError
import zio.IO

import java.util.UUID

trait EntityService {
  def create(entity: Entity): IO[EntityServiceError, Entity]

  def getById(entityId: UUID): IO[EntityServiceError, Entity]

  def getAll(offset: Option[Int], limit: Option[Int]): IO[EntityServiceError, Seq[Entity]]

  def deleteById(entityId: UUID): IO[EntityServiceError, Unit]

  def updateName(entityId: UUID, name: String): IO[EntityServiceError, Unit]

  def assignWallet(entityId: UUID, walletId: UUID): IO[EntityServiceError, Unit]
}
