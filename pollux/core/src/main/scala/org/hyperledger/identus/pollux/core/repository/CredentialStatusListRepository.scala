package org.hyperledger.identus.pollux.core.repository

import org.hyperledger.identus.pollux.core.model.*
import org.hyperledger.identus.pollux.vc.jwt.Issuer
import org.hyperledger.identus.shared.models.{WalletAccessContext, WalletId}
import zio.*

import java.util.UUID

trait CredentialStatusListRepository {
  def getCredentialStatusListIds: UIO[Seq[(WalletId, UUID)]]

  def getCredentialStatusListsWithCreds(statusListId: UUID): URIO[WalletAccessContext, CredentialStatusListWithCreds]

  def findById(
      id: UUID
  ): UIO[Option[CredentialStatusList]]

  def getLatestOfTheWallet: URIO[WalletAccessContext, Option[CredentialStatusList]]

  def existsForIssueCredentialRecordId(
      id: DidCommID
  ): URIO[WalletAccessContext, Boolean]

  def createNewForTheWallet(
      jwtIssuer: Issuer,
      statusListRegistryServiceName: String
  ): URIO[WalletAccessContext, CredentialStatusList]

  def allocateSpaceForCredential(
      issueCredentialRecordId: DidCommID,
      credentialStatusListId: UUID,
      statusListIndex: Int
  ): URIO[WalletAccessContext, Unit]

  def revokeByIssueCredentialRecordId(
      issueCredentialRecordId: DidCommID
  ): URIO[WalletAccessContext, Unit]

  def updateStatusListCredential(
      credentialStatusListId: UUID,
      statusListCredential: String
  ): URIO[WalletAccessContext, Unit]

  def markAsProcessedMany(
      credsInStatusListIds: Seq[UUID]
  ): URIO[WalletAccessContext, Unit]
}
