package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.ECKeyPair
import io.iohk.atala.agent.walletapi.storage.InMemoryDIDSecretStorage.DIDSecretRecord
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*

private[walletapi] class InMemoryDIDSecretStorage private (store: Ref[Map[PrismDID, DIDSecretRecord]])
    extends DIDSecretStorage {
  override def listKeys(did: PrismDID): Task[Map[String, ECKeyPair]] =
    store.get.map(_.get(did).map(_.keyPairs).getOrElse(Map.empty))

  override def getKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]] = listKeys(did).map(_.get(keyId))

  override def upsertKey(did: PrismDID, keyId: String, keyPair: ECKeyPair): Task[Unit] =
    store
      .update { currentStore =>
        val currentSecret = currentStore.get(did)
        val currentKeyPairs = currentSecret.map(_.keyPairs).getOrElse(Map.empty)
        val updatedKeyPairs = currentKeyPairs.updated(keyId, keyPair)
        val updatedSecret =
          currentSecret.fold(DIDSecretRecord(keyPairs = updatedKeyPairs))(_.copy(keyPairs = updatedKeyPairs))
        currentStore.updated(did, updatedSecret)
      }

  override def removeKey(did: PrismDID, keyId: String): Task[Unit] = store
    .update { currentStore =>
      currentStore.get(did) match {
        case Some(secret) =>
          val currentKeyPairs = secret.keyPairs
          val updatedKeyPairs = currentKeyPairs.removed(keyId)
          val updatedSecret = secret.copy(keyPairs = updatedKeyPairs)
          currentStore.updated(did, updatedSecret)
        case None => currentStore
      }
    }

  override def removeDIDSecret(did: PrismDID): Task[Unit] = store.update(_.removed(did))

}

private[walletapi] object InMemoryDIDSecretStorage {

  private final case class DIDSecretRecord(
      updateCommitmentRevealValue: Option[HexString] = None,
      recoveryCommitmentRevealValue: Option[HexString] = None,
      keyPairs: Map[String, ECKeyPair] = Map.empty
  )

  val layer: ULayer[DIDSecretStorage] = {
    ZLayer.fromZIO(
      Ref.make(Map.empty[PrismDID, DIDSecretRecord]).map(InMemoryDIDSecretStorage(_))
    )
  }
}
