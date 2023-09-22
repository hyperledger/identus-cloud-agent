package io.iohk.atala.agent.server.jobs

import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import zio.*

object DIDPublicationBackgroundJobs {

  val syncDIDPublicationStateFromDlt =
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      _ <- managedDidService.syncManagedDIDState
      _ <- managedDidService.syncUnconfirmedUpdateOperations
    } yield ()

}
