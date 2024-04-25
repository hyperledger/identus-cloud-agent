package org.hyperledger.identus.agent.walletapi.service.handler

import org.hyperledger.identus.agent.walletapi.model.CreateDIDKey
import org.hyperledger.identus.agent.walletapi.model.ManagedDIDState
import org.hyperledger.identus.agent.walletapi.model.ManagedDIDTemplate
import org.hyperledger.identus.agent.walletapi.model.PublicationState
import org.hyperledger.identus.agent.walletapi.model.WalletSeed
import org.hyperledger.identus.agent.walletapi.model.error.CreateManagedDIDError
import org.hyperledger.identus.agent.walletapi.storage.DIDNonSecretStorage
import org.hyperledger.identus.agent.walletapi.storage.DIDSecretStorage
import org.hyperledger.identus.agent.walletapi.storage.WalletSecretStorage
import org.hyperledger.identus.agent.walletapi.util.OperationFactory
import org.hyperledger.identus.castor.core.model.did.PrismDIDOperation
import org.hyperledger.identus.shared.crypto.Apollo
import org.hyperledger.identus.shared.crypto.Ed25519KeyPair
import org.hyperledger.identus.shared.crypto.X25519KeyPair
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

private[walletapi] class DIDCreateHandler(
    apollo: Apollo,
    nonSecretStorage: DIDNonSecretStorage,
    secretStorage: DIDSecretStorage,
    walletSecretStorage: WalletSecretStorage,
)(
    masterKeyId: String
) {
  def materialize(
      didTemplate: ManagedDIDTemplate
  ): ZIO[WalletAccessContext, CreateManagedDIDError, DIDCreateMaterial] = {
    val operationFactory = OperationFactory(apollo)
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      seed <- walletSecretStorage.getWalletSeed
        .someOrElseZIO(ZIO.dieMessage(s"Wallet seed for wallet $walletId does not exist"))
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      didIndex <- nonSecretStorage
        .getMaxDIDIndex()
        .mapBoth(
          CreateManagedDIDError.WalletStorageError.apply,
          maybeIdx => maybeIdx.map(_ + 1).getOrElse(0)
        )
      generated <- operationFactory.makeCreateOperation(masterKeyId, seed.toByteArray)(didIndex, didTemplate)
      (createOperation, keys) = generated
      state = ManagedDIDState(createOperation, didIndex, PublicationState.Created())
    } yield DIDCreateMaterialImpl(nonSecretStorage, secretStorage)(createOperation, state, keys)
  }
}

private[walletapi] trait DIDCreateMaterial {
  def operation: PrismDIDOperation.Create
  def state: ManagedDIDState
  def persist: RIO[WalletAccessContext, Unit]
}

private[walletapi] class DIDCreateMaterialImpl(nonSecretStorage: DIDNonSecretStorage, secretStorage: DIDSecretStorage)(
    val operation: PrismDIDOperation.Create,
    val state: ManagedDIDState,
    keys: CreateDIDKey
) extends DIDCreateMaterial {
  def persist: RIO[WalletAccessContext, Unit] = {
    val did = operation.did
    val operationHash = operation.toAtalaOperationHash
    for {
      _ <- nonSecretStorage
        .insertManagedDID(did, state, keys.hdKeys, keys.randKeyMeta)
        .mapError(CreateManagedDIDError.WalletStorageError.apply)
      _ <- ZIO.foreach(keys.randKeys.toList) { case (keyId, key) =>
        key.keyPair match {
          case kp: Ed25519KeyPair => secretStorage.insertPrismDIDKeyPair(did, keyId, operationHash, kp)
          case kp: X25519KeyPair  => secretStorage.insertPrismDIDKeyPair(did, keyId, operationHash, kp)
        }
      }
    } yield ()
  }
}
