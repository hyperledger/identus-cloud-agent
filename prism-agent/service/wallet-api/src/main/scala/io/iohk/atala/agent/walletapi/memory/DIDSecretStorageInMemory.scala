package io.iohk.atala.agent.walletapi.memory

import io.iohk.atala.agent.walletapi.storage.DIDSecret
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.mercury.model.DidId
import zio.*

class DIDSecretStorageInMemory(storeRef: Ref[Map[String, DIDSecret]]) extends DIDSecretStorage {

  private def constructKey(did: DidId, keyId: String, schemaId: String): String = s"${did}_${keyId}_${schemaId}"

  override def getKey(did: DidId, keyId: String, schemaId: String): Task[Option[DIDSecret]] =
    storeRef.get.map(_.get(constructKey(did, keyId, schemaId)))

  override def insertKey(did: DidId, keyId: String, didSecret: DIDSecret): Task[Int] =
    storeRef.modify { store =>
      val key = constructKey(did, keyId, didSecret.schemaId)
      if (store.contains(key)) {
        (1, store) // Already exists, so we're effectively updating it
      } else {
        (0, store.updated(key, didSecret))
      }
    }
}

object DIDSecretStorageInMemory {
  val layer: ULayer[DIDSecretStorage] =
    ZLayer.fromZIO(
      Ref
        .make(Map.empty[String, DIDSecret])
        .map(DIDSecretStorageInMemory(_))
    )

}
