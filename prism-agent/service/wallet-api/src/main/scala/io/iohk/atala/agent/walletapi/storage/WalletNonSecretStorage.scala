package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.shared.models.WalletId
import zio.*

trait WalletNonSecretStorage {
  def createWallet: Task[WalletId]
  def listWallet(offset: Option[Int] = None, limit: Option[Int] = None): Task[(Seq[WalletId], Int)]
}
