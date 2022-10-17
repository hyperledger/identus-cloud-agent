package io.iohk.atala.agent.custodian.keystore

import io.iohk.atala.agent.custodian.model.ECKeyPair
import io.iohk.atala.castor.core.model.did.PrismDID
import zio.*

/** A simple single-user DID key storage */
private[custodian] trait DIDKeyStorage {

  /** Returns a mapping of key-id to key-pair */
  def listKeys(did: PrismDID): Task[Map[String, ECKeyPair]]

  def getKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]]

  def upsertKey(did: PrismDID, keyId: String, keyPair: ECKeyPair): Task[Unit]

  /** Returns the deleted key-pair if exists, otherwise return None */
  def removeKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]]

}
