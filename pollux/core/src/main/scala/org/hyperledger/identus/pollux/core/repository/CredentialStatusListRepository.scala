package org.hyperledger.identus.pollux.core.repository

import org.hyperledger.identus.pollux.core.model.*
import org.hyperledger.identus.pollux.vc.jwt.Issuer
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait CredentialStatusListRepository {
  def getLatestOfTheWallet: RIO[WalletAccessContext, Option[CredentialStatusList]]

  def findById(id: UUID): Task[Option[CredentialStatusList]]

  def createNewForTheWallet(
      jwtIssuer: Issuer,
      statusListRegistryUrl: String
  ): RIO[WalletAccessContext, CredentialStatusList]

  def allocateSpaceForCredential(
      issueCredentialRecordId: DidCommID,
      credentialStatusListId: UUID,
      statusListIndex: Int
  ): RIO[WalletAccessContext, Unit]

  def revokeByIssueCredentialRecordId(
      issueCredentialRecordId: DidCommID
  ): RIO[WalletAccessContext, Boolean]

  def getCredentialStatusListsWithCreds: Task[List[CredentialStatusListWithCreds]]

  def updateStatusListCredential(
      credentialStatusListId: UUID,
      statusListCredential: String
  ): RIO[WalletAccessContext, Unit]

  def markAsProcessedMany(
      credsInStatusListIds: Seq[UUID]
  ): RIO[WalletAccessContext, Unit]
}
