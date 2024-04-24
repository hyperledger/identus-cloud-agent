package org.hyperledger.identus.agent.server.jobs

import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import zio.*

object DIDStateSyncBackgroundJobs {

  val syncDIDPublicationStateFromDlt =
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      _ <- managedDidService.syncManagedDIDState
      _ <- managedDidService.syncUnconfirmedUpdateOperations
    } yield ()

}
