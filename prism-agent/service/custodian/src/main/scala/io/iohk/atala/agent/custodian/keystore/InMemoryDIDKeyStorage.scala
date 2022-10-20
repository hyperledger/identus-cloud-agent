package io.iohk.atala.agent.custodian.keystore

import io.iohk.atala.agent.custodian.model.ECKeyPair
import io.iohk.atala.castor.core.model.did.DID
import zio.{Ref, Task, ULayer, ZLayer}

private[custodian] class InMemoryDIDKeyStorage(store: Ref[Map[DID, Map[String, ECKeyPair]]]) extends DIDKeyStorage {
  override def listKeys(did: DID): Task[Map[String, ECKeyPair]] = store.get.map(_.getOrElse(did, Map.empty))

  override def getKey(did: DID, keyId: String): Task[Option[ECKeyPair]] = listKeys(did).map(_.get(keyId))

  override def upsertKey(did: DID, keyId: String, keyPair: ECKeyPair): Task[Unit] = store
    .update { currentStore =>
      val currentStoredKeys = currentStore.getOrElse(did, Map.empty)
      val updatedStoredKeys = currentStoredKeys.updated(keyId, keyPair)
      currentStore.updated(did, updatedStoredKeys)
    }

  override def removeKey(did: DID, keyId: String): Task[Option[ECKeyPair]] = store
    .getAndUpdate { currentStore =>
      val currentStoredKeys = currentStore.getOrElse(did, Map.empty)
      val updatedStoredKeys = currentStoredKeys.removed(keyId)
      currentStore.updated(did, updatedStoredKeys)
    }
    .map(_.getOrElse(did, Map.empty).get(keyId))

}

private[custodian] object InMemoryDIDKeyStorage {
  val layer: ULayer[DIDKeyStorage] = {
    ZLayer.fromZIO(
      Ref.make(Map.empty[DID, Map[String, ECKeyPair]]).map(store => InMemoryDIDKeyStorage(store))
    )
  }
}
