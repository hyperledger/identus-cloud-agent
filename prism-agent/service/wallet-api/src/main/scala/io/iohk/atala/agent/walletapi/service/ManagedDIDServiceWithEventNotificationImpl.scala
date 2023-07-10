package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.model.{ManagedDIDState, ManagedDIDDetail}
import io.iohk.atala.agent.walletapi.model.error.CommonWalletStorageError
import io.iohk.atala.agent.walletapi.storage.{DIDNonSecretStorage, DIDSecretStorage}
import io.iohk.atala.agent.walletapi.util.SeedResolver
import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.castor.core.model.error
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.event.notification.{Event, EventNotificationService}
import zio.{IO, RLayer, Semaphore, ZIO, ZLayer}

class ManagedDIDServiceWithEventNotificationImpl(
    didService: DIDService,
    didOpValidator: DIDOperationValidator,
    override private[walletapi] val secretStorage: DIDSecretStorage,
    override private[walletapi] val nonSecretStorage: DIDNonSecretStorage,
    apollo: Apollo,
    seed: Array[Byte],
    createDIDSem: Semaphore,
    eventNotificationService: EventNotificationService
) extends ManagedDIDServiceImpl(
      didService,
      didOpValidator,
      secretStorage,
      nonSecretStorage,
      apollo,
      seed,
      createDIDSem
    ) {

  private val didStatusUpdatedEventName = "DIDStatusUpdated"

  override protected def computeNewDIDStateFromDLTAndPersist[E](
      did: CanonicalPrismDID
  )(using
      c1: Conversion[CommonWalletStorageError, E],
      c2: Conversion[DIDOperationError, E]
  ): IO[E, Boolean] = {
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
    DIDOperationValidator & DIDService & DIDSecretStorage & DIDNonSecretStorage & Apollo & SeedResolver &
      EventNotificationService,
    ManagedDIDService
  ] = ZLayer.fromZIO {
    for {
      didService <- ZIO.service[DIDService]
      didOpValidator <- ZIO.service[DIDOperationValidator]
      secretStorage <- ZIO.service[DIDSecretStorage]
      nonSecretStorage <- ZIO.service[DIDNonSecretStorage]
      apollo <- ZIO.service[Apollo]
      seed <- ZIO.serviceWithZIO[SeedResolver](_.resolve)
      createDIDSem <- Semaphore.make(1)
      eventNotificationService <- ZIO.service[EventNotificationService]
    } yield ManagedDIDServiceWithEventNotificationImpl(
      didService,
      didOpValidator,
      secretStorage,
      nonSecretStorage,
      apollo,
      seed,
      createDIDSem,
      eventNotificationService
    )
  }
}
