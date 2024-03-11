package io.iohk.atala.pollux.core.repository

import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.mercury.protocol.issuecredential.{IssueCredential, RequestCredential}
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState
import io.iohk.atala.pollux.vc.jwt.Issuer
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
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
