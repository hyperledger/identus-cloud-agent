package io.iohk.atala.shared.models

import java.util.UUID
import scala.language.implicitConversions

opaque type WalletId = UUID

object WalletId {
  def fromUUID(uuid: UUID): WalletId = uuid
  def random: WalletId = fromUUID(UUID.randomUUID())

  extension (id: WalletId) { def toUUID: UUID = id }
}

final case class WalletAccessContext(walletId: WalletId)
