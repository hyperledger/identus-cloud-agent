package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.shared.models.WalletId
import zio.*
import io.iohk.atala.shared.models.WalletAccessContext

class WalletManagementServiceImpl(
    nonSecretStorage: WalletNonSecretStorage,
    secretStorage: WalletSecretStorage,
) extends WalletManagementService {

  override def createWallet(seed: WalletSeed): Task[WalletId] =
    for {
      walletId <- nonSecretStorage.createWallet
      _ <- secretStorage
        .setWalletSeed(seed)
        .provide(ZLayer.succeed(WalletAccessContext(walletId)))
    } yield walletId

  override def listWallets: Task[Seq[WalletId]] = nonSecretStorage.listWallet
}

object WalletManagementServiceImpl {
  val layer: URLayer[WalletNonSecretStorage & WalletSecretStorage, WalletManagementService] = {
    ZLayer.fromFunction(WalletManagementServiceImpl(_, _))
  }
}
