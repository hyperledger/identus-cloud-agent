package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.model.{CommitmentPurpose, ECKeyPair, StagingDIDUpdateSecret}
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*

/** A simple single-user DID key storage */
private[walletapi] trait DIDSecretStorage {

  /** Returns a mapping of key-id to key-pair */
  def listKeys(did: PrismDID): Task[Map[String, ECKeyPair]]

  def getKey(did: PrismDID, keyId: String): Task[Option[ECKeyPair]]

  def upsertKey(did: PrismDID, keyId: String, keyPair: ECKeyPair): Task[Unit]

  def getDIDCommitmentKey(did: PrismDID, purpose: CommitmentPurpose): Task[Option[ECKeyPair]]

  def upsertDIDCommitmentKey(did: PrismDID, purpose: CommitmentPurpose, secret: ECKeyPair): Task[Unit]

  /** Remove all secrets related to the DID */
  def removeDIDSecret(did: PrismDID): Task[Unit]

  /** Set staging secret for UpdateOperation. Only 1 staging secret per DID may be present at a time. If staging secret
    * already exists, no change is applied.
    *
    * @return
    *   true if operation success. false if staging secret already exists
    */
  def addStagingDIDUpdateSecret(did: PrismDID, secret: StagingDIDUpdateSecret): Task[Boolean]

  def getStagingDIDUpdateSecret(did: PrismDID): Task[Option[StagingDIDUpdateSecret]]

  def removeStagingDIDUpdateSecret(did: PrismDID): Task[Unit]

}
