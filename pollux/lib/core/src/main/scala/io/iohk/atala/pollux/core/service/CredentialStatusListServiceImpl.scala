package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.CredentialStatusList
import io.iohk.atala.pollux.core.repository.CredentialStatusListRepository
import zio.*
import io.iohk.atala.pollux.core.model.error.CredentialStatusListServiceError
import io.iohk.atala.pollux.core.model.error.CredentialStatusListServiceError.*
import io.iohk.atala.pollux.core.model.DidCommID
import io.iohk.atala.shared.models.WalletAccessContext

import java.util.UUID

class CredentialStatusListServiceImpl(
    credentialStatusListRepository: CredentialStatusListRepository,
) extends CredentialStatusListService {

  def findById(id: UUID): IO[CredentialStatusListServiceError, CredentialStatusList] =
    for {
      maybeStatusList <- credentialStatusListRepository.findById(id).mapError(RepositoryError.apply)
      statuslist <- ZIO
        .getOrFailWith(RecordIdNotFound(id))(
          maybeStatusList
        )
    } yield statuslist

  def revokeByIssueCredentialRecordId(
      id: DidCommID
  ): ZIO[WalletAccessContext, CredentialStatusListServiceError, Unit] = {
    for {
      revoked <- credentialStatusListRepository.revokeByIssueCredentialRecordId(id).mapError(RepositoryError.apply)
      _ <- if (revoked) ZIO.unit else ZIO.fail(IssueCredentialRecordNotFound(id))
    } yield ()

  }

}

object CredentialStatusListServiceImpl {
  val layer: URLayer[CredentialStatusListRepository, CredentialStatusListService] =
    ZLayer.fromFunction(CredentialStatusListServiceImpl(_))
}
