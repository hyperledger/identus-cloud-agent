package io.iohk.atala.agent.walletapi.service.handler

import io.iohk.atala.agent.walletapi.model.DIDUpdateLineage
import io.iohk.atala.agent.walletapi.model.ManagedDIDKeyMeta
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.agent.walletapi.model.UpdateDIDKey
import io.iohk.atala.agent.walletapi.model.UpdateManagedDIDAction
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.model.error.UpdateManagedDIDError
import io.iohk.atala.agent.walletapi.model.error.{*, given}
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.agent.walletapi.util.OperationFactory
import io.iohk.atala.castor.core.model.did.PrismDIDOperation
import io.iohk.atala.castor.core.model.did.PrismDIDOperation.Update
import io.iohk.atala.castor.core.model.did.ScheduledDIDOperationStatus
import io.iohk.atala.castor.core.model.did.SignedPrismDIDOperation
import io.iohk.atala.shared.crypto.Apollo
import io.iohk.atala.shared.crypto.Ed25519KeyPair
import io.iohk.atala.shared.crypto.X25519KeyPair
import io.iohk.atala.shared.models.WalletAccessContext
import scala.collection.immutable.ArraySeq
import zio.*

private[walletapi] class DIDUpdateHandler(
    apollo: Apollo,
    nonSecretStorage: DIDNonSecretStorage,
    secretStorage: DIDSecretStorage,
    walletSecretStorage: WalletSecretStorage,
    publicationHandler: PublicationHandler
) {
  def materialize(
      state: ManagedDIDState,
      previousOperationHash: Array[Byte],
      actions: Seq[UpdateManagedDIDAction]
  ): ZIO[WalletAccessContext, UpdateManagedDIDError, DIDUpdateMaterial] = {
    val operationFactory = OperationFactory(apollo)
    val did = state.createOperation.did
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      seed <- walletSecretStorage.getWalletSeed
        .someOrElseZIO(ZIO.dieMessage(s"Wallet seed for wallet $walletId does not exist"))
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
      keyCounter <- nonSecretStorage
        .getHdKeyCounter(did)
        .mapError(UpdateManagedDIDError.WalletStorageError.apply)
        .someOrFail(
          UpdateManagedDIDError.DataIntegrityError("DID is in HD key mode, but its key counter is not found")
        )
      result <- operationFactory.makeUpdateOperation(seed.toByteArray)(
        did,
        previousOperationHash,
        actions,
        keyCounter
      )
      (operation, hdKey) = result
      signedOperation <- publicationHandler.signOperationWithMasterKey[UpdateManagedDIDError](state, operation)
    } yield HdKeyUpdateMaterial(nonSecretStorage, secretStorage)(operation, signedOperation, state, hdKey)
  }
}

private[walletapi] trait DIDUpdateMaterial {

  def operation: PrismDIDOperation.Update

  def signedOperation: SignedPrismDIDOperation

  def state: ManagedDIDState

  def persist: RIO[WalletAccessContext, Unit]

  protected final def persistUpdateLineage(nonSecretStorage: DIDNonSecretStorage): RIO[WalletAccessContext, Unit] = {
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

private class HdKeyUpdateMaterial(nonSecretStorage: DIDNonSecretStorage, secretStorage: DIDSecretStorage)(
    val operation: PrismDIDOperation.Update,
    val signedOperation: SignedPrismDIDOperation,
    val state: ManagedDIDState,
    keys: UpdateDIDKey
) extends DIDUpdateMaterial {

  private def persistKeyMaterial: RIO[WalletAccessContext, Unit] = {
    val did = operation.did
    val operationHash = operation.toAtalaOperationHash
    // OPTIMIZE: refactoring insertKey to a bulk insert (non-secret part)
    for {
      _ <- ZIO.foreach(keys.hdKeys.toList) { case (keyId, keyPath) =>
        val meta = ManagedDIDKeyMeta.HD(keyPath)
        nonSecretStorage.insertKeyMeta(did, keyId, meta, operationHash)
      }
      _ <- ZIO.foreach(keys.randKeyMeta.toList) { case (keyId, rand) =>
        val meta = ManagedDIDKeyMeta.Rand(rand)
        nonSecretStorage.insertKeyMeta(did, keyId, meta, operationHash)
      }
      _ <- ZIO.foreach(keys.randKeys.toList) { case (keyId, key) =>
        key.keyPair match {
          case kp: Ed25519KeyPair => secretStorage.insertPrismDIDKeyPair(did, keyId, operationHash, kp)
          case kp: X25519KeyPair  => secretStorage.insertPrismDIDKeyPair(did, keyId, operationHash, kp)
        }
      }
    } yield ()

  }

  override def persist: RIO[WalletAccessContext, Unit] =
    for {
      _ <- persistKeyMaterial
      _ <- persistUpdateLineage(nonSecretStorage)
    } yield ()

}
