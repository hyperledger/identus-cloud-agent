package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.*
import io.iohk.atala.agent.walletapi.model.error.*
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorage
import io.iohk.atala.castor.core.model.did.*
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.mercury.model.DidId
import zio.*

import java.security.{PrivateKey as JavaPrivateKey, PublicKey as JavaPublicKey}

/** A wrapper around Castor's DIDService providing key-management capability. Analogous to the secretAPI in
  * indy-wallet-sdk.
  */
trait ManagedDIDService {

  private[walletapi] def nonSecretStorage: DIDNonSecretStorage

  def syncManagedDIDState: IO[GetManagedDIDError, Unit]

  def syncUnconfirmedUpdateOperations: IO[GetManagedDIDError, Unit]

  def javaKeyPairWithDID(
      did: CanonicalPrismDID,
      keyId: String
  ): IO[GetKeyError, Option[(JavaPrivateKey, JavaPublicKey)]]

  def getManagedDIDState(did: CanonicalPrismDID): IO[GetManagedDIDError, Option[ManagedDIDState]]

  /** @return A tuple containing a list of items and a count of total items */
  def listManagedDIDPage(offset: Int, limit: Int): IO[GetManagedDIDError, (Seq[ManagedDIDDetail], Int)]

  def publishStoredDID(did: CanonicalPrismDID): IO[PublishManagedDIDError, ScheduleDIDOperationOutcome]

  def createAndStoreDID(didTemplate: ManagedDIDTemplate): IO[CreateManagedDIDError, LongFormPrismDID]

  def updateManagedDID(
      did: CanonicalPrismDID,
      actions: Seq[UpdateManagedDIDAction]
  ): IO[UpdateManagedDIDError, ScheduleDIDOperationOutcome]

  def deactivateManagedDID(did: CanonicalPrismDID): IO[UpdateManagedDIDError, ScheduleDIDOperationOutcome]

  /** PeerDID related methods */
  def createAndStorePeerDID(serviceEndpoint: String): UIO[PeerDID]

  def getPeerDID(didId: DidId): IO[DIDSecretStorageError.KeyNotFoundError, PeerDID]

}

object ManagedDIDService {
  val DEFAULT_MASTER_KEY_ID: String = "master0"
  val reservedKeyIds: Set[String] = Set(DEFAULT_MASTER_KEY_ID)
}
