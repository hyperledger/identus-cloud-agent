package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.{CredentialStatusList, CredentialStatusListWithCreds, DidCommID}
import org.hyperledger.identus.pollux.core.model.error.CredentialStatusListServiceError.{
  StatusListNotFound,
  StatusListNotFoundForIssueCredentialRecord
}
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait CredentialStatusListService {
  def getById(
      id: UUID
  ): IO[StatusListNotFound, CredentialStatusList]

  def findById(
      id: UUID
  ): UIO[Option[CredentialStatusList]]

  def revokeByIssueCredentialRecordId(
      id: DidCommID
  ): ZIO[WalletAccessContext, StatusListNotFoundForIssueCredentialRecord, Unit]

  def getCredentialsAndItsStatuses: UIO[Seq[CredentialStatusListWithCreds]]

  def updateStatusListCredential(
      id: UUID,
      statusListCredential: String
  ): URIO[WalletAccessContext, Unit]

  def markAsProcessedMany(
      ids: Seq[UUID]
  ): URIO[WalletAccessContext, Unit]
}
