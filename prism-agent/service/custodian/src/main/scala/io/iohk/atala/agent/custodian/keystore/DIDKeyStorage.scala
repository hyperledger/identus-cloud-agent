package io.iohk.atala.agent.custodian.keystore

import io.iohk.atala.agent.custodian.model.ECKeyPair
import io.iohk.atala.castor.core.model.did.DID
import zio.*

/** A simple single-user DID key storage.
  *
  * @tparam Tx
  *   A storage implementation requirement with potentially transaction guarantee such as Connection, Scope, etc.
  * @tparam E
  *   A potential error type for each storage implementation
  */
private[custodian] trait DIDKeyStorage[Tx, E] {

  /** Returns a mapping of key-id to key-pair */
  def listKeys(did: DID): ZIO[Tx, E, Map[String, ECKeyPair]]

  def getKey(did: DID, keyId: String): ZIO[Tx, E, Option[ECKeyPair]]

  def upsertKey(did: DID, keyId: String, keyPair: ECKeyPair): ZIO[Tx, E, Unit]

  /** Returns the deleted key-pair if exists, otherwise return None */
  def removeKey(did: DID, keyId: String): ZIO[Tx, E, Option[ECKeyPair]]

  /** Wraps storage effect and commit a transaction */
  def transact[A](effect: ZIO[Tx, E, A]): IO[E, A]

}

// TODO: implement
private[custodian] class InMemoryDIDKeyStorage(store: Ref[Map[DID, Map[String, ECKeyPair]]])
    extends DIDKeyStorage[Any, Nothing] {

  override def upsertKey(did: DID, keyId: String, keyPair: ECKeyPair): ZIO[Any, Nothing, Unit] = ???

  override def listKeys(did: DID): ZIO[Any, Nothing, Map[String, ECKeyPair]] = ???

  override def getKey(did: DID, keyId: String): ZIO[Any, Nothing, Option[ECKeyPair]] = ???

  override def removeKey(did: DID, keyId: String): ZIO[Any, Nothing, Option[ECKeyPair]] = ???

  override def transact[A](effect: ZIO[Any, Nothing, A]): IO[Nothing, A] = ???

}

private[custodian] object InMemoryDIDKeyStorage {
  val layer: ULayer[DIDKeyStorage[Any, Nothing]] = {
    ZLayer.fromZIO(
      Ref.make(Map.empty[DID, Map[String, ECKeyPair]]).map(store => InMemoryDIDKeyStorage(store))
    )
  }
}
