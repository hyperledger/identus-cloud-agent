package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.Wallet
import zio.*

trait WalletNonSecretStorage {
  def createWallet(wallet: Wallet): Task[Wallet]
  def listWallet(offset: Option[Int] = None, limit: Option[Int] = None): Task[(Seq[Wallet], Int)]
}
