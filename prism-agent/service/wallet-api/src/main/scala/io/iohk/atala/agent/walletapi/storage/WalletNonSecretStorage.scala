package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.shared.models.WalletId
import zio.*

trait WalletNonSecretStorage {
  def createWallet(wallet: Wallet): Task[Wallet]
  def getWallet(walletId: WalletId): Task[Option[Wallet]]
  def listWallet(offset: Option[Int] = None, limit: Option[Int] = None): Task[(Seq[Wallet], Int)]
}
