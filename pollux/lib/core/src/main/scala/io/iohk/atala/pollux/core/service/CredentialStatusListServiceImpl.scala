package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.CredentialStatusList
import io.iohk.atala.pollux.core.repository.CredentialStatusListRepository
import zio.*
import io.iohk.atala.pollux.core.model.error.CredentialStatusListServiceError
import io.iohk.atala.pollux.core.model.error.CredentialStatusListServiceError.*

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

}

object CredentialStatusListServiceImpl {
  val layer: URLayer[CredentialStatusListRepository, CredentialStatusListService] =
    ZLayer.fromFunction(CredentialStatusListServiceImpl(_))
}
