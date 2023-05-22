package io.iohk.atala.agent.walletapi.service.handler

import zio.*
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.agent.walletapi.model.UpdateManagedDIDAction
import io.iohk.atala.agent.walletapi.model.{UpdateDIDRandKey, UpdateDIDHdKey}
import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.model.KeyManagementMode
import io.iohk.atala.agent.walletapi.util.OperationFactory
import io.iohk.atala.castor.core.model.did.PrismDIDOperation
import io.iohk.atala.castor.core.model.did.PrismDIDOperation.Update
import io.iohk.atala.agent.walletapi.storage.{DIDNonSecretStorage, DIDSecretStorage}
import io.iohk.atala.agent.walletapi.model.error.UpdateManagedDIDError

class DIDUpdateHandler(apollo: Apollo, nonSecretStorage: DIDNonSecretStorage, secretStorage: DIDSecretStorage)(
    seed: Array[Byte]
) {

  private val operationFactory = OperationFactory(apollo)

  def materialize(
      state: ManagedDIDState,
      previousOperationHash: Array[Byte],
      actions: Seq[UpdateManagedDIDAction]
  ): IO[UpdateManagedDIDError, DIDUpdateMaterial] = {
    val did = state.createOperation.did
    state.keyMode match {
      case KeyManagementMode.HD =>
        nonSecretStorage
          .getHdKeyCounter(did)
          .mapError(UpdateManagedDIDError.WalletStorageError.apply)
          .someOrFail(UpdateManagedDIDError.DataIntegrityError("Expected a HD key counter, but it was not found"))
          .flatMap { counter =>
            operationFactory.makeUpdateOperationHdKey(seed)(did, previousOperationHash, actions, counter)
          }
          .map { case (operation, hdKey) => ??? }
      case KeyManagementMode.Random =>
        operationFactory
          .makeUpdateOperationRandKey(did, previousOperationHash, actions)
          .map { case (operation, randKey) => RandKeyMaterial(secretStorage)(operation, randKey) }
    }
  }

}

sealed trait DIDUpdateMaterial {
  def operation: PrismDIDOperation.Update
  def persist: Task[Unit]
}

class RandKeyMaterial(secretStorage: DIDSecretStorage)(
    val operation: PrismDIDOperation.Update,
    randKey: UpdateDIDRandKey
) extends DIDUpdateMaterial {
  override def persist: Task[Unit] =
    ZIO.foreachDiscard(randKey.newKeyPairs) { case (keyId, keyPair) =>
      val did = operation.did
      val operationHash = operation.toAtalaOperationHash
      secretStorage.insertKey(did, keyId, keyPair, operationHash)
    }
}

class HdKeyMaterial(nonSecretStorage: DIDNonSecretStorage)(
    val operation: PrismDIDOperation.Update,
    hdKey: UpdateDIDHdKey
) extends DIDUpdateMaterial {

  // TODO: think about rejected operation and how to revert persisted material
  override def persist: Task[Unit] = ???

}
