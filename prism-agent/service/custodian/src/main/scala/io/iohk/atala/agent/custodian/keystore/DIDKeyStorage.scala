package io.iohk.atala.agent.custodian.keystore

import io.iohk.atala.agent.custodian.model.ECKeyPair
import io.iohk.atala.castor.core.model.did.DID
import zio.*

/** A simple single-user DID key storage */
private[custodian] trait DIDKeyStorage {

  /** Returns a mapping of key-id to key-pair */
  def listKeys(did: DID): Task[Map[String, ECKeyPair]]

  def getKey(did: DID, keyId: String): Task[Option[ECKeyPair]]

  def upsertKey(did: DID, keyId: String, keyPair: ECKeyPair): Task[Unit]

  /** Returns the deleted key-pair if exists, otherwise return None */
  def removeKey(did: DID, keyId: String): Task[Option[ECKeyPair]]

}

// TODO: implement
private[custodian] class InMemoryDIDKeyStorage(store: Ref[Map[DID, Map[String, ECKeyPair]]]) extends DIDKeyStorage {
  override def listKeys(did: DID): Task[Map[String, ECKeyPair]] = ???

  override def getKey(did: DID, keyId: String): Task[Option[ECKeyPair]] = ???

  override def upsertKey(did: DID, keyId: String, keyPair: ECKeyPair): Task[Unit] = ???

  override def removeKey(did: DID, keyId: String): Task[Option[ECKeyPair]] = ???

}

private[custodian] object InMemoryDIDKeyStorage {
  val layer: ULayer[DIDKeyStorage] = {
    ZLayer.fromZIO(
      Ref.make(Map.empty[DID, Map[String, ECKeyPair]]).map(store => InMemoryDIDKeyStorage(store))
    )
  }
}
