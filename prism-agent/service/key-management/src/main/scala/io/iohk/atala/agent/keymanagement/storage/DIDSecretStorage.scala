package io.iohk.atala.agent.keymanagement.storage

import io.iohk.atala.agent.keymanagement.model.{CommitmentPurpose, ECKeyPair}
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*

/** A simple single-user DID key storage */
private[keymanagement] trait DIDSecretStorage {

  /** Returns a mapping of key-id to key-pair */
  def listKeys(did: PrismDID): Task[Map[String, ECKeyPair]]

  def getKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]]

  def upsertKey(did: PrismDID, keyId: String, keyPair: ECKeyPair): Task[Unit]

  /** Returns the deleted key-pair if exists, otherwise return None */
  def removeKey(did: PrismDID, keyId: String): Task[Unit]

  def getDIDCommitmentRevealValue(did: PrismDID, purpose: CommitmentPurpose): Task[Option[HexString]]

  def upsertDIDCommitmentRevealValue(did: PrismDID, purpose: CommitmentPurpose, revealValue: HexString): Task[Unit]

  /** Remove all secrets related to the DID */
  def removeDIDSecret(did: PrismDID): Task[Unit]

}
