package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.{CredentialStatusList, CredentialStatusListWithCreds, DidCommID}
import org.hyperledger.identus.pollux.core.model.error.CredentialStatusListServiceError.{
  StatusListNotFound,
  StatusListNotFoundForIssueCredentialRecord
}
import org.hyperledger.identus.pollux.core.repository.CredentialStatusListRepository
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

class CredentialStatusListServiceImpl(
    credentialStatusListRepository: CredentialStatusListRepository,
) extends CredentialStatusListService {

  def getById(id: UUID): IO[StatusListNotFound, CredentialStatusList] =
    for {
      maybeStatusList <- credentialStatusListRepository.findById(id)
      statusList <- ZIO
        .fromOption(maybeStatusList)
        .mapError(_ => StatusListNotFound(id))
    } yield statusList

  def findById(id: UUID): UIO[Option[CredentialStatusList]] =
    credentialStatusListRepository.findById(id)

  def revokeByIssueCredentialRecordId(
      id: DidCommID
  ): ZIO[WalletAccessContext, StatusListNotFoundForIssueCredentialRecord, Unit] =
    for {
      exists <- credentialStatusListRepository.existsForIssueCredentialRecordId(id)
      _ <- if (exists) ZIO.unit else ZIO.fail(StatusListNotFoundForIssueCredentialRecord(id))
      _ <- credentialStatusListRepository.revokeByIssueCredentialRecordId(id)
    } yield ()

  def getCredentialsAndItsStatuses: UIO[Seq[CredentialStatusListWithCreds]] =
    credentialStatusListRepository.getCredentialStatusListsWithCreds

  def updateStatusListCredential(
      id: UUID,
      statusListCredential: String
  ): URIO[WalletAccessContext, Unit] =
    credentialStatusListRepository.updateStatusListCredential(id, statusListCredential)

  def markAsProcessedMany(
      credsInStatusListIds: Seq[UUID]
  ): URIO[WalletAccessContext, Unit] =
    credentialStatusListRepository.markAsProcessedMany(credsInStatusListIds)

}

object CredentialStatusListServiceImpl {
  val layer: URLayer[CredentialStatusListRepository, CredentialStatusListService] =
    ZLayer.fromFunction(CredentialStatusListServiceImpl(_))
}
