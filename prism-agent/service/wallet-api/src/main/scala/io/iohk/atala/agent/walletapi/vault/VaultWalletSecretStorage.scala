package io.iohk.atala.agent.walletapi.vault

import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

class VaultWalletSecretStorage(vaultKV: VaultKVClient) extends WalletSecretStorage {

  override def setWalletSeed(seed: WalletSeed): RIO[WalletAccessContext, Unit] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      path = walletSeedPath(walletId)
      alreadyExist <- vaultKV.get[WalletSeed](path).map(_.isDefined)
      _ <- vaultKV
        .set[WalletSeed](path, seed)
        .when(!alreadyExist)
        .someOrFail(Exception(s"Secret on path $path alraedy exists."))
    } yield ()
  }

  override def getWalletSeed: RIO[WalletAccessContext, Option[WalletSeed]] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      path = walletSeedPath(walletId)
      seed <- vaultKV.get[WalletSeed](path)
    } yield seed
  }

  private def walletSeedPath(walletId: WalletId): String = {
    s"secret/${walletId.toUUID}/seed"
  }

}

object VaultWalletSecretStorage {
  def layer: URLayer[VaultKVClient, WalletSecretStorage] = ZLayer.fromFunction(VaultWalletSecretStorage(_))
}
