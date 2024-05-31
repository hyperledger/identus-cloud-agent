package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.error.CredentialStatusListServiceError
import org.hyperledger.identus.pollux.core.model.CredentialStatusList
import org.hyperledger.identus.pollux.core.model.CredentialStatusListWithCreds
import org.hyperledger.identus.pollux.core.model.DidCommID
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait CredentialStatusListService {
  def findById(id: UUID): IO[CredentialStatusListServiceError, CredentialStatusList]

  def revokeByIssueCredentialRecordId(id: DidCommID): ZIO[WalletAccessContext, CredentialStatusListServiceError, Unit]

  def getCredentialsAndItsStatuses: IO[CredentialStatusListServiceError, Seq[CredentialStatusListWithCreds]]

  def updateStatusListCredential(
      id: UUID,
      statusListCredential: String
  ): ZIO[WalletAccessContext, CredentialStatusListServiceError, Unit]

  def markAsProcessedMany(ids: Seq[UUID]): ZIO[WalletAccessContext, CredentialStatusListServiceError, Unit]
}
