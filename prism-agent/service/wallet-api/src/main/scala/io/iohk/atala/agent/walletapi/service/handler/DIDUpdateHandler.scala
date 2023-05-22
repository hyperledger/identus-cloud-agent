package io.iohk.atala.agent.walletapi.service.handler

import zio.*
import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.model.{UpdateDIDRandKey, UpdateDIDHdKey}
import io.iohk.atala.agent.walletapi.model.DIDUpdateLineage
import io.iohk.atala.agent.walletapi.model.error.{*, given}
import io.iohk.atala.agent.walletapi.model.error.UpdateManagedDIDError
import io.iohk.atala.agent.walletapi.model.KeyManagementMode
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.agent.walletapi.model.UpdateManagedDIDAction
import io.iohk.atala.agent.walletapi.storage.{DIDNonSecretStorage, DIDSecretStorage}
import io.iohk.atala.agent.walletapi.util.OperationFactory
import io.iohk.atala.castor.core.model.did.PrismDIDOperation
import io.iohk.atala.castor.core.model.did.PrismDIDOperation.Update
import io.iohk.atala.castor.core.model.did.ScheduledDIDOperationStatus
import io.iohk.atala.castor.core.model.did.SignedPrismDIDOperation
import scala.collection.immutable.ArraySeq

class DIDUpdateHandler(
    apollo: Apollo,
    nonSecretStorage: DIDNonSecretStorage,
    secretStorage: DIDSecretStorage,
    publicationHandler: PublicationHandler
)(
    seed: Array[Byte]
) {
  def materialize(
      state: ManagedDIDState,
      previousOperationHash: Array[Byte],
      actions: Seq[UpdateManagedDIDAction]
  ): IO[UpdateManagedDIDError, DIDUpdateMaterial] = {
    val operationFactory = OperationFactory(apollo)
    val did = state.createOperation.did
    state.keyMode match {
      case KeyManagementMode.HD =>
        for {
          keyCounter <- nonSecretStorage
            .getHdKeyCounter(did)
            .mapError(UpdateManagedDIDError.WalletStorageError.apply)
            .someOrFail(
              UpdateManagedDIDError.DataIntegrityError("DID is in HD key mode, but its key counter is not found")
            )
          result <- operationFactory.makeUpdateOperationHdKey(seed)(did, previousOperationHash, actions, keyCounter)
          (operation, hdKey) = result
          signedOperation <- publicationHandler.signOperationWithMasterKey[UpdateManagedDIDError](state, operation)
        } yield HdKeyUpdateMaterial(secretStorage, nonSecretStorage)(operation, signedOperation, state, hdKey)
      case KeyManagementMode.Random =>
        for {
          result <- operationFactory
            .makeUpdateOperationRandKey(did, previousOperationHash, actions)
          (operation, randKey) = result
          signedOperation <- publicationHandler.signOperationWithMasterKey[UpdateManagedDIDError](state, operation)
        } yield RandKeyUpdateMaterial(secretStorage, nonSecretStorage)(operation, signedOperation, state, randKey)
    }
  }
}

trait DIDUpdateMaterial {

  def operation: PrismDIDOperation.Update

  def signedOperation: SignedPrismDIDOperation

  def state: ManagedDIDState

  def persist: Task[Unit]

  protected final def persistUpdateLineage(nonSecretStorage: DIDNonSecretStorage): Task[Unit] = {
    val did = operation.did
    for {
      updateLineage <- Clock.instant.map { now =>
        DIDUpdateLineage(
          operationId = ArraySeq.from(signedOperation.toAtalaOperationId),
          operationHash = ArraySeq.from(operation.toAtalaOperationHash),
          previousOperationHash = operation.previousOperationHash,
          status = ScheduledDIDOperationStatus.Pending,
          createdAt = now,
          updatedAt = now
        )
      }
      _ <- nonSecretStorage.insertDIDUpdateLineage(did, updateLineage)
    } yield ()
  }

}

class RandKeyUpdateMaterial(secretStorage: DIDSecretStorage, nonSecretStorage: DIDNonSecretStorage)(
    val operation: PrismDIDOperation.Update,
    val signedOperation: SignedPrismDIDOperation,
    val state: ManagedDIDState,
    randKey: UpdateDIDRandKey
) extends DIDUpdateMaterial {

  private def persistKeyMaterial: Task[Unit] = {
    val did = operation.did
    val operationHash = operation.toAtalaOperationHash
    ZIO.foreachDiscard(randKey.newKeyPairs) { case (keyId, keyPair) =>
      secretStorage.insertKey(did, keyId, keyPair, operationHash)
    }
  }

  override def persist: Task[Unit] =
    for {
      _ <- persistKeyMaterial
      _ <- persistUpdateLineage(nonSecretStorage)
    } yield ()
}

class HdKeyUpdateMaterial(secretStorage: DIDSecretStorage, nonSecretStorage: DIDNonSecretStorage)(
    val operation: PrismDIDOperation.Update,
    val signedOperation: SignedPrismDIDOperation,
    val state: ManagedDIDState,
    hdKey: UpdateDIDHdKey
) extends DIDUpdateMaterial {

  private def persistKeyMaterial: Task[Unit] = {
    val did = operation.did
    val operationHash = operation.toAtalaOperationHash
    ZIO.foreachDiscard(hdKey.newKeyPaths) { case (keyId, keyPath) =>
      nonSecretStorage.insertHdKeyPath(did, keyId, keyPath, operationHash)
    }
  }

  override def persist: Task[Unit] =
    for {
      _ <- persistKeyMaterial
      _ <- persistUpdateLineage(nonSecretStorage)
    } yield ()

}
