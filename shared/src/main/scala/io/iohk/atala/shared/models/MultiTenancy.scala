package io.iohk.atala.shared.models

opaque type WalletId = Int

object WalletId {
  def fromInt(id: Int): WalletId = id
  extension (id: WalletId) { def toInt: Int = id }
}

final case class WalletAccessContext(walletId: WalletId)
