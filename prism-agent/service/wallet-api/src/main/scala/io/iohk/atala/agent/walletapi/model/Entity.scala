package io.iohk.atala.agent.walletapi.model

import java.time.Instant
import java.util.UUID

case class Entity(id: UUID, name: String, walletId: UUID, createdAt: Instant, updatedAt: Instant) {
  def withUpdatedAt(updatedAt: Instant = Instant.now()): Entity = copy(updatedAt = updatedAt)
}

object Entity {

  val ZeroWalletId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
  def apply(name: String, walletId: UUID): Entity =
    Entity(UUID.randomUUID(), name, walletId, Instant.now(), Instant.now())
  def apply(name: String): Entity = Entity(UUID.randomUUID(), name, ZeroWalletId, Instant.now(), Instant.now())
}
