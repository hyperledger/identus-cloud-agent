package io.iohk.atala.agent.custodian.keystore

import io.iohk.atala.agent.custodian.model.ECKeyPair
import io.iohk.atala.castor.core.model.did.PrismDID
import zio.{Ref, Task, ULayer, ZLayer}

private[custodian] class InMemoryDIDKeyStorage(store: Ref[Map[PrismDID, Map[String, ECKeyPair]]])
    extends DIDKeyStorage {
  override def listKeys(did: PrismDID): Task[Map[String, ECKeyPair]] = store.get.map(_.getOrElse(did, Map.empty))

  override def getKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]] = listKeys(did).map(_.get(keyId))

  override def upsertKey(did: PrismDID, keyId: String, keyPair: ECKeyPair): Task[Unit] = store
    .update { currentStore =>
      val currentStoredKeys = currentStore.getOrElse(did, Map.empty)
      val updatedStoredKeys = currentStoredKeys.updated(keyId, keyPair)
      currentStore.updated(did, updatedStoredKeys)
    }

  override def removeKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]] = store
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
      Ref.make(Map.empty[PrismDID, Map[String, ECKeyPair]]).map(store => InMemoryDIDKeyStorage(store))
    )
  }
}
