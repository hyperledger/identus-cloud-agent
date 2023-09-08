package io.iohk.atala.agent.walletapi.memory

import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

class WalletSecretStorageInMemory(storeRef: Ref[Map[WalletId, WalletSeed]]) extends WalletSecretStorage {

  override def setWalletSeed(seed: WalletSeed): RIO[WalletAccessContext, Unit] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      _ <- storeRef.update(_.updated(walletId, seed))
    } yield ()
  }

  override def getWalletSeed: RIO[WalletAccessContext, Option[WalletSeed]] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      seed <- storeRef.get.map(_.get(walletId))
    } yield seed
  }

}

object WalletSecretStorageInMemory {
  val layer: ULayer[WalletSecretStorage] =
    ZLayer.fromZIO(
      Ref
        .make(Map.empty[WalletId, WalletSeed])
        .map(WalletSecretStorageInMemory(_))
    )
}
