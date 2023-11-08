package io.iohk.atala.agent.walletapi.model

import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.{ULayer, ZLayer}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

enum EntityRole {
  case Admin, Tenant
}

trait BaseEntity {
  def id: UUID
  def role: EntityRole
}

case class Entity(
    id: UUID,
    name: String,
    walletId: UUID,
    createdAt: Instant,
    updatedAt: Instant,
    role: EntityRole = EntityRole.Tenant
) extends BaseEntity {
  def withUpdatedAt(updatedAt: Instant = Instant.now()): Entity = copy(updatedAt = updatedAt)
  def withTruncatedTimestamp(unit: ChronoUnit = ChronoUnit.MICROS): Entity =
    copy(createdAt = createdAt.truncatedTo(unit), updatedAt.truncatedTo(unit))
}

object Entity {

  val ZeroWalletId: UUID = WalletId.default.toUUID

  def apply(id: UUID, name: String, walletId: UUID): Entity =
    Entity(id, name, walletId, Instant.now(), Instant.now()).withTruncatedTimestamp()

  def apply(name: String, walletId: UUID): Entity =
    apply(UUID.randomUUID(), name, walletId, Instant.now(), Instant.now()).withTruncatedTimestamp()

  def apply(name: String): Entity =
    Entity(UUID.randomUUID(), name, ZeroWalletId, Instant.now(), Instant.now()).withTruncatedTimestamp()

  val Default =
    Entity(
      UUID.fromString("00000000-0000-0000-0000-000000000000"),
      "default",
      ZeroWalletId,
      Instant.EPOCH,
      Instant.EPOCH
    )
  val Admin =
    Entity(
      UUID.fromString("00000000-0000-0000-0000-000000000001"),
      "admin",
      ZeroWalletId,
      Instant.EPOCH,
      Instant.EPOCH,
      EntityRole.Admin
    )

  extension (entity: Entity) {
    def walletAccessContext: WalletAccessContext = WalletAccessContext(WalletId.fromUUID(entity.walletId))
    def wacLayer: ULayer[WalletAccessContext] = ZLayer.succeed(walletAccessContext)
  }
}
