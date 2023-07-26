package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

class WalletManagementServiceImpl(
    apollo: Apollo,
    nonSecretStorage: WalletNonSecretStorage,
    secretStorage: WalletSecretStorage,
) extends WalletManagementService {

  override def createWallet(seed: Option[WalletSeed]): Task[WalletId] =
    for {
      seed <- seed.fold(apollo.ecKeyFactory.randomBip32Seed().map(_._1).map(WalletSeed.fromByteArray))(ZIO.succeed)
      walletId <- nonSecretStorage.createWallet
      _ <- secretStorage
        .setWalletSeed(seed)
        .provide(ZLayer.succeed(WalletAccessContext(walletId)))
    } yield walletId

  override def listWallets: Task[Seq[WalletId]] = nonSecretStorage.listWallet
}

object WalletManagementServiceImpl {
  val layer: URLayer[Apollo & WalletNonSecretStorage & WalletSecretStorage, WalletManagementService] = {
    ZLayer.fromFunction(WalletManagementServiceImpl(_, _, _))
  }
}
