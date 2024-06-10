package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.{CredentialStatusList, CredentialStatusListWithCreds, DidCommID}
import org.hyperledger.identus.pollux.core.model.error.CredentialStatusListServiceError
import org.hyperledger.identus.pollux.core.model.error.CredentialStatusListServiceError.*
import org.hyperledger.identus.pollux.core.repository.CredentialStatusListRepository
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

class CredentialStatusListServiceImpl(
    credentialStatusListRepository: CredentialStatusListRepository,
) extends CredentialStatusListService {

  def findById(id: UUID): IO[CredentialStatusListServiceError, CredentialStatusList] =
    for {
      maybeStatusList <- credentialStatusListRepository.findById(id)
      statuslist <- ZIO
        .getOrFailWith(RecordIdNotFound(id))(
          maybeStatusList
        )
    } yield statuslist

  def revokeByIssueCredentialRecordId(
      id: DidCommID
  ): ZIO[WalletAccessContext, CredentialStatusListServiceError, Unit] = {
    for {
      revoked <- credentialStatusListRepository.revokeByIssueCredentialRecordId(id)
      _ <- if (revoked) ZIO.unit else ZIO.fail(IssueCredentialRecordNotFound(id))
    } yield ()

  }

  def getCredentialsAndItsStatuses: IO[CredentialStatusListServiceError, Seq[CredentialStatusListWithCreds]] = {
    credentialStatusListRepository.getCredentialStatusListsWithCreds
  }

  def updateStatusListCredential(
      id: UUID,
      statusListCredential: String
  ): ZIO[WalletAccessContext, CredentialStatusListServiceError, Unit] = {
    credentialStatusListRepository
      .updateStatusListCredential(id, statusListCredential)
  }

  def markAsProcessedMany(
      credsInStatusListIds: Seq[UUID]
  ): ZIO[WalletAccessContext, CredentialStatusListServiceError, Unit] = {
    credentialStatusListRepository
      .markAsProcessedMany(credsInStatusListIds)
  }

}

object CredentialStatusListServiceImpl {
  val layer: URLayer[CredentialStatusListRepository, CredentialStatusListService] =
    ZLayer.fromFunction(CredentialStatusListServiceImpl(_))
}
