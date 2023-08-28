package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.model.ManagedDIDDetail
import io.iohk.atala.agent.walletapi.model.error.CommonWalletStorageError
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.agent.walletapi.storage.{DIDNonSecretStorage, DIDKeySecretStorage}
import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.castor.core.model.error
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.event.notification.{Event, EventNotificationService}
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

class ManagedDIDServiceWithEventNotificationImpl(
    didService: DIDService,
    didOpValidator: DIDOperationValidator,
    override private[walletapi] val secretStorage: DIDKeySecretStorage,
    override private[walletapi] val nonSecretStorage: DIDNonSecretStorage,
    walletSecretStorage: WalletSecretStorage,
    apollo: Apollo,
    createDIDSem: Semaphore,
    eventNotificationService: EventNotificationService
) extends ManagedDIDServiceImpl(
      didService,
      didOpValidator,
      secretStorage,
      nonSecretStorage,
      walletSecretStorage,
      apollo,
      createDIDSem
    ) {

  private val didStatusUpdatedEventName = "DIDStatusUpdated"

  override protected def computeNewDIDStateFromDLTAndPersist[E](
      did: CanonicalPrismDID
  )(using
      c1: Conversion[CommonWalletStorageError, E],
      c2: Conversion[DIDOperationError, E]
  ): ZIO[WalletAccessContext, E, Boolean] = {
    for {
      updated <- super.computeNewDIDStateFromDLTAndPersist(did)
      _ <- ZIO.when(updated) {
        val result = for {
          maybeUpdatedDID <- nonSecretStorage.getManagedDIDState(did)
          updatedDID <- ZIO.fromOption(maybeUpdatedDID)
          producer <- eventNotificationService.producer[ManagedDIDDetail]("DIDDetail")
          _ <- producer.send(Event(didStatusUpdatedEventName, ManagedDIDDetail(did, updatedDID)))
        } yield ()
        result.catchAll(e => ZIO.logError(s"Notification service error: $e"))
      }
    } yield updated
  }
}

object ManagedDIDServiceWithEventNotificationImpl {
  val layer: RLayer[
    DIDOperationValidator & DIDService & DIDKeySecretStorage & DIDNonSecretStorage & WalletSecretStorage & Apollo &
      EventNotificationService,
    ManagedDIDService
  ] = ZLayer.fromZIO {
    for {
      didService <- ZIO.service[DIDService]
      didOpValidator <- ZIO.service[DIDOperationValidator]
      secretStorage <- ZIO.service[DIDKeySecretStorage]
      nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
      walletSecretStorage <- ZIO.service[WalletSecretStorage]
      apollo <- ZIO.service[Apollo]
      createDIDSem <- Semaphore.make(1)
      eventNotificationService <- ZIO.service[EventNotificationService]
    } yield ManagedDIDServiceWithEventNotificationImpl(
      didService,
      didOpValidator,
      secretStorage,
      nonSecretStorage,
      walletSecretStorage,
      apollo,
      createDIDSem,
      eventNotificationService
    )
  }
}
