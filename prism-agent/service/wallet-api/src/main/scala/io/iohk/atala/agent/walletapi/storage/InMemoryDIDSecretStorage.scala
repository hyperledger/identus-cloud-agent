package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.ECKeyPair
import io.iohk.atala.agent.walletapi.storage.InMemoryDIDSecretStorage.{DIDSecretRecord, PeerDIDSecretRecord}
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError._
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*
import io.iohk.atala.mercury.model.DidId
import com.nimbusds.jose.jwk.OctetKeyPair

private[walletapi] class InMemoryDIDSecretStorage private (
    store: Ref[Map[PrismDID, DIDSecretRecord]],
    peerDIDStore: Ref[Map[DidId, PeerDIDSecretRecord]]
) extends DIDSecretStorage {
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

  override def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): UIO[Unit] = {
    for {
      _ <- peerDIDStore
        .update { currentStore =>
          val currentSecret = currentStore.get(did)
          val currentKeyPairs = currentSecret.map(_.keyPairs).getOrElse(Map.empty)
          val updatedKeyPairs = currentKeyPairs.updated(keyId, keyPair)
          val updatedSecret =
            currentSecret.fold(PeerDIDSecretRecord(keyPairs = updatedKeyPairs))(_.copy(keyPairs = updatedKeyPairs))
          currentStore.updated(did, updatedSecret)
        }
      storage <- peerDIDStore.get
      _ <- ZIO.logInfo(s"Peer DID Store content after insert: ${storage.size}")
    } yield ()
  }

  override def getKey(did: DidId, keyId: String): IO[KeyNotFoundError, OctetKeyPair] =
    for {
      storage <- peerDIDStore.get
      _ <- ZIO.logInfo(s"Peer DID Store content before get: ${storage.size}")
      maybeKeyPair <- peerDIDStore.get.map(_.get(did).map(_.keyPairs).flatMap(_.get(keyId)))
      keyPair <- ZIO.fromOption(maybeKeyPair).mapError(_ => KeyNotFoundError(did, keyId))
    } yield keyPair

}

private[walletapi] object InMemoryDIDSecretStorage {

  private final case class DIDSecretRecord(
      updateCommitmentRevealValue: Option[HexString] = None,
      recoveryCommitmentRevealValue: Option[HexString] = None,
      keyPairs: Map[String, ECKeyPair] = Map.empty
  )

  private final case class PeerDIDSecretRecord(
      keyPairs: Map[String, OctetKeyPair] = Map.empty
  )

  val layer: ULayer[DIDSecretStorage] = {
    ZLayer.fromZIO(
      for {
        _ <- ZIO.logInfo(s"Creating InMemoryDIDSecretStorage !!")
        prismDIDStore <- Ref.make(Map.empty[PrismDID, DIDSecretRecord])
        peerDIDStore <- Ref.make(Map.empty[DidId, PeerDIDSecretRecord])
      } yield InMemoryDIDSecretStorage(prismDIDStore, peerDIDStore)
    )
  }
}
