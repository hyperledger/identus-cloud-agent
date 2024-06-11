package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.{CredentialStatusList, CredentialStatusListWithCreds, DidCommID}
import org.hyperledger.identus.pollux.core.model.error.CredentialStatusListServiceError.StatusListNotFound
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
  ): URIO[WalletAccessContext, Unit] =
    for {
      // TODO validate IssueCredentialRecord id exists and fail with NotFound if not
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
