package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.shared.models.WalletId
import zio.*

trait WalletManagementService {
  def createWallet(seed: Option[WalletSeed] = None): Task[WalletId]
  def listWallets: Task[Seq[WalletId]]
}
