package org.hyperledger.identus.agent.server.jobs

import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import zio.*
import org.hyperledger.identus.shared.models.{WalletId, WalletAccessContext}
import org.hyperledger.identus.messaging.Message

object DIDStateSyncBackgroundJobs {

  val syncDIDPublicationStateFromDlt =
    for {
      managedDidService <- ZIO.service[ManagedDIDService]
      _ <- managedDidService.syncManagedDIDState
      _ <- managedDidService.syncUnconfirmedUpdateOperations
    } yield ()

  def handleMessage(message: Message[WalletId, WalletId]) : RIO[ManagedDIDService, Unit] =
    syncDIDPublicationStateFromDlt
      .provideSomeLayer(ZLayer.succeed(WalletAccessContext(message.value)))
      .catchAll(t => ZIO.logErrorCause("Unable to syncing DID publication state", Cause.fail(t)))
}
