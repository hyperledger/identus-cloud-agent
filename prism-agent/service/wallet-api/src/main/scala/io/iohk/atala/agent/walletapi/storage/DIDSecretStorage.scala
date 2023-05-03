package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.ECKeyPair
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError._
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.shared.models.HexString
import zio.*
import io.iohk.atala.mercury.PeerDID
import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.mercury.model.DidId
import scala.collection.immutable.ArraySeq

/** A simple single-user DID key storage */
private[walletapi] trait DIDSecretStorage {

  /** Returns a list of keys */
  def listKeys(did: PrismDID): Task[Seq[(String, ArraySeq[Byte], ECKeyPair)]]

  /** Returns the key of confirmed operation */
  def getKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]]

  def insertKey(did: PrismDID, keyId: String, keyPair: ECKeyPair, operationHash: Array[Byte]): Task[Int]

  /** Returns the deleted key-pair if exists, otherwise return None */
  def removeKey(did: PrismDID, keyId: String): Task[Int]

  /** Remove all secrets related to the DID */
  def removeDIDSecret(did: PrismDID): Task[Int]

  /** PeerDID related methods. TODO: Refactor to abstract over PrismDID & PeerDID and merge methods
    */
  def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): Task[Int]

  def getKey(did: DidId, keyId: String): Task[Option[OctetKeyPair]]

}
