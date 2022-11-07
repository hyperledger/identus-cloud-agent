package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.{CommitmentPurpose, ECKeyPair, StagingDIDUpdateSecret}
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

  override def getDIDCommitmentKey(did: PrismDID, purpose: CommitmentPurpose): Task[Option[ECKeyPair]] =
    store.get.map(
      _.get(did).flatMap(secret =>
        purpose match {
          case CommitmentPurpose.Update   => secret.updateCommitmentSecret
          case CommitmentPurpose.Recovery => secret.recoveryCommitmentSecret
        }
      )
    )

  override def upsertDIDCommitmentKey(
      did: PrismDID,
      purpose: CommitmentPurpose,
      secret: ECKeyPair
  ): Task[Unit] =
    store
      .update { currentStore =>
        val currentSecret = currentStore.get(did)
        val updatedSecret: DIDSecretRecord = purpose match {
          case CommitmentPurpose.Update =>
            currentSecret.fold(DIDSecretRecord(updateCommitmentSecret = Some(secret)))(
              _.copy(updateCommitmentSecret = Some(secret))
            )
          case CommitmentPurpose.Recovery =>
            currentSecret.fold(DIDSecretRecord(recoveryCommitmentSecret = Some(secret)))(
              _.copy(recoveryCommitmentSecret = Some(secret))
            )
        }

        currentStore.updated(did, updatedSecret)
      }

  override def removeDIDSecret(did: PrismDID): Task[Unit] = store.update(_.removed(did))

  override def addStagingDIDUpdateSecret(did: PrismDID, secret: StagingDIDUpdateSecret): Task[Boolean] =
    store.modify { currentStore =>
      val currentSecret = currentStore.get(did)
      val updatedSecret =
        currentSecret.fold(DIDSecretRecord(stagingSecret = Some(secret)))(_.copy(stagingSecret = Some(secret)))
      val hasNoExistingStagingSecret = currentSecret.flatMap(_.stagingSecret).fold(true)(_ => false)
      val updatedStore =
        if (hasNoExistingStagingSecret) currentStore.updated(did, updatedSecret)
        else currentStore
      hasNoExistingStagingSecret -> updatedStore
    }

  override def getStagingDIDUpdateSecret(did: PrismDID): Task[Option[StagingDIDUpdateSecret]] =
    store.get.map(_.get(did).flatMap(_.stagingSecret))

  override def removeStagingDIDUpdateSecret(did: PrismDID): Task[Unit] = store.update { currentStore =>
    val currentSecret = currentStore.get(did)
    currentSecret.fold(currentStore)(i => currentStore.updated(did, i.copy(stagingSecret = None)))
  }

}

private[walletapi] object InMemoryDIDSecretStorage {

  private final case class DIDSecretRecord(
      updateCommitmentSecret: Option[ECKeyPair] = None,
      recoveryCommitmentSecret: Option[ECKeyPair] = None,
      keyPairs: Map[String, ECKeyPair] = Map.empty,
      stagingSecret: Option[StagingDIDUpdateSecret] = None
  )

  val layer: ULayer[DIDSecretStorage] = {
    ZLayer.fromZIO(
      Ref.make(Map.empty[PrismDID, DIDSecretRecord]).map(InMemoryDIDSecretStorage(_))
    )
  }
}
