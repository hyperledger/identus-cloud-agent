package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.Entity
import io.iohk.atala.agent.walletapi.model.error.EntityServiceError
import zio.IO

import java.util.UUID

trait EntityService {
  def create(entity: Entity): IO[EntityServiceError, Entity]

  def getById(entityId: UUID): IO[EntityServiceError, Entity]

//    def deleteById(entityId: UUID): IO[EntityServiceError, Unit]
//
//    def updateName(entityId: UUID, name: String): IO[EntityServiceError, Entity]
//
//    def assignWallet(entityId: UUID, walletId: UUID): IO[EntityServiceError, Unit]
}
