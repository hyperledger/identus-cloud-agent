package io.iohk.atala.shared.models

import scala.language.implicitConversions

opaque type WalletId = Int

object WalletId {
  def fromInt(id: Int): WalletId = id
  extension (id: WalletId) { def toInt: Int = id }

  given Conversion[Int, WalletId] = fromInt
}

final case class WalletAccessContext(walletId: WalletId)

object WalletAccessContext {
  // TODO: remove this!!!
  val placeholder: WalletAccessContext = WalletAccessContext(walletId = 1)
}
