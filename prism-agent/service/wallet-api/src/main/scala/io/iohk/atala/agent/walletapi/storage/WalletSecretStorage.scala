package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.WalletSeed
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

trait WalletSecretStorage {
  def setWalletSeed(seed: WalletSeed): RIO[WalletAccessContext, Unit]
  def getWalletSeed: RIO[WalletAccessContext, Option[WalletSeed]]
}
