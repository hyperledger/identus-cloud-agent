package io.iohk.atala.agent.walletapi.service

import io.iohk.atala.agent.walletapi.model.*
import io.iohk.atala.agent.walletapi.model.error.*
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorage
import io.iohk.atala.castor.core.model.did.*
import io.iohk.atala.mercury.PeerDID
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.security.{PrivateKey as JavaPrivateKey, PublicKey as JavaPublicKey}

/** A wrapper around Castor's DIDService providing key-management capability. Analogous to the secretAPI in
  * indy-wallet-sdk.
  */
trait ManagedDIDService {

  private[walletapi] def nonSecretStorage: DIDNonSecretStorage

  def syncManagedDIDState: ZIO[WalletAccessContext, GetManagedDIDError, Unit]

  def syncUnconfirmedUpdateOperations: ZIO[WalletAccessContext, GetManagedDIDError, Unit]

  def javaKeyPairWithDID(
      did: CanonicalPrismDID,
      keyId: String
  ): ZIO[WalletAccessContext, GetKeyError, Option[(JavaPrivateKey, JavaPublicKey)]]

  def getManagedDIDState(did: CanonicalPrismDID): ZIO[WalletAccessContext, GetManagedDIDError, Option[ManagedDIDState]]

  /** @return A tuple containing a list of items and a count of total items */
  def listManagedDIDPage(
      offset: Int,
      limit: Int
  ): ZIO[WalletAccessContext, GetManagedDIDError, (Seq[ManagedDIDDetail], Int)]

  def publishStoredDID(
      did: CanonicalPrismDID
  ): ZIO[WalletAccessContext, PublishManagedDIDError, ScheduleDIDOperationOutcome]

  def createAndStoreDID(
      didTemplate: ManagedDIDTemplate
  ): ZIO[WalletAccessContext, CreateManagedDIDError, LongFormPrismDID]

  def updateManagedDID(
      did: CanonicalPrismDID,
      actions: Seq[UpdateManagedDIDAction]
  ): ZIO[WalletAccessContext, UpdateManagedDIDError, ScheduleDIDOperationOutcome]

  def deactivateManagedDID(
      did: CanonicalPrismDID
  ): ZIO[WalletAccessContext, UpdateManagedDIDError, ScheduleDIDOperationOutcome]

  /** PeerDID related methods */
  def createAndStorePeerDID(serviceEndpoint: String): URIO[WalletAccessContext, PeerDID]

  def getPeerDID(didId: DidId): ZIO[WalletAccessContext, DIDSecretStorageError.KeyNotFoundError, PeerDID]

}

object ManagedDIDService {
  val DEFAULT_MASTER_KEY_ID: String = "master0"
  val reservedKeyIds: Set[String] = Set(DEFAULT_MASTER_KEY_ID)
}
