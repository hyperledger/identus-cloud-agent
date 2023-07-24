package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.shared.models.WalletId
import zio.*

trait WalletNonSecretStorage {
  def createWallet: Task[WalletId]
  def listWallet: Task[Seq[WalletId]]
}
