package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.ECKeyPair
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError._
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*
import io.iohk.atala.mercury.PeerDID
import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.mercury.model.DidId

/** A simple single-user DID key storage */
private[walletapi] trait DIDSecretStorage {

  /** Returns a mapping of key-id to key-pair */
  def listKeys(did: PrismDID): Task[Map[String, ECKeyPair]]

  def getKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]]

  def upsertKey(did: PrismDID, keyId: String, keyPair: ECKeyPair): Task[Unit]

  /** Returns the deleted key-pair if exists, otherwise return None */
  def removeKey(did: PrismDID, keyId: String): Task[Unit]

  /** Remove all secrets related to the DID */
  def removeDIDSecret(did: PrismDID): Task[Unit]

  /** PeerDID related methods. TODO: Refactor to abstract over PrismDID & PeerDID and merge methods
    */
  def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): UIO[Unit]

  def getKey(did: DidId, keyId: String): IO[KeyNotFoundError, OctetKeyPair]

}
