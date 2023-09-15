package io.iohk.atala.agent.walletapi.model.error

import java.util.UUID

sealed trait EntityServiceError {
  def message: String
}

object EntityServiceError {
  final case class EntityNotFound(id: UUID, message: String) extends EntityServiceError
  final case class EntityAlreadyExists(id: UUID, message: String) extends EntityServiceError
  final case class EntityStorageError(message: String) extends EntityServiceError
  final case class EntityWalletNotFound(entityId: UUID, walletId: UUID) extends EntityServiceError {
    override def message: String = s"Wallet with id:$walletId not found for entity with id:$entityId"
  }
}
