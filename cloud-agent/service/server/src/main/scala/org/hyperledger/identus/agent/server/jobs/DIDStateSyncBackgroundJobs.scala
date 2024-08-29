package org.hyperledger.identus.agent.server.jobs

import org.hyperledger.identus.agent.walletapi.model.error.GetManagedDIDError
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

object DIDStateSyncBackgroundJobs {

  val syncDIDPublicationStateFromDlt: ZIO[WalletAccessContext with ManagedDIDService, GetManagedDIDError, Unit] =
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      _ <- managedDidService.syncManagedDIDState
      _ <- managedDidService.syncUnconfirmedUpdateOperations
    } yield ()

}
